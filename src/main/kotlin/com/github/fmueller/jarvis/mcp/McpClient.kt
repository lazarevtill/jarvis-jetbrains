package com.github.fmueller.jarvis.mcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.*
import okio.ByteString
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Minimal JSON-RPC 2.0 client over WebSocket for interacting with MCP servers.
 *
 * This client opens a WebSocket connection to the configured URL, sends a single
 * request, waits for the corresponding response, and then closes the connection.
 */
class McpClient(private val json: Json = defaultJson) {

    companion object {
        private val defaultJson = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }

    /**
     * List available tools via `tools/list`.
     */
    fun listTools(wsUrl: String): List<McpTool> {
        val session = RpcWebSocketSession(wsUrl, json)
        return try {
            // Initialize handshake (required by MCP)
            session.request(
                JsonRpcRequest(
                    id = 1,
                    method = "initialize",
                    params = buildJsonObject {
                        put("protocolVersion", "2024-11-05")
                        put("capabilities", buildJsonObject { })
                        put("clientInfo", buildJsonObject {
                            put("name", "Jarvis JetBrains")
                            put("version", "dev")
                        })
                    }
                )
            )

            val resp = session.request(
                JsonRpcRequest(
                    id = 2,
                    method = "tools/list",
                    params = null
                )
            )
            val resultMap = resp?.result ?: return emptyList()
            val resultElement = JsonObject(resultMap)
            val toolsElement = resultElement["tools"] ?: return emptyList()
            try {
                json.decodeFromJsonElement(ListSerializer(McpTool.serializer()), toolsElement)
            } catch (_: Exception) {
                emptyList()
            }
        } finally {
            session.close()
        }
    }

    /**
     * Call a specific tool via `tools/call` with provided arguments.
     */
    fun callTool(wsUrl: String, name: String, arguments: JsonElement?): McpToolCallResult? {
        val session = RpcWebSocketSession(wsUrl, json)
        return try {
            // Initialize first
            session.request(
                JsonRpcRequest(
                    id = 1,
                    method = "initialize",
                    params = buildJsonObject {
                        put("protocolVersion", "2024-11-05")
                        put("capabilities", buildJsonObject { })
                        put("clientInfo", buildJsonObject {
                            put("name", "Jarvis JetBrains")
                            put("version", "dev")
                        })
                    }
                )
            )

            val params = buildJsonObject {
                put("name", name)
                arguments?.let { put("arguments", it) }
            }
            val resp = session.request(
                JsonRpcRequest(
                    id = 2,
                    method = "tools/call",
                    params = params
                )
            )
            val resultMap = resp?.result ?: return null
            val resultElement = JsonObject(resultMap)
            try {
                json.decodeFromJsonElement(McpToolCallResult.serializer(), resultElement)
            } catch (_: Exception) {
                null
            }
        } finally {
            session.close()
        }
    }
}

private class RpcWebSocketSession(wsUrl: String, private val json: Json) : WebSocketListener() {
    private val okClient = OkHttpClient.Builder().build()
    private val requests = ConcurrentHashMap<Int, CompletableFuture<JsonRpcResponse>>()
    private val ws: WebSocket

    init {
        val req = Request.Builder().url(wsUrl).build()
        ws = okClient.newWebSocket(req, this)
    }

    fun request(request: JsonRpcRequest): JsonRpcResponse? {
        val future = CompletableFuture<JsonRpcResponse>()
        requests[request.id] = future
        val payload = json.encodeToString(JsonRpcRequest.serializer(), request)
        ws.send(payload)
        return try {
            future.get(15, TimeUnit.SECONDS)
        } catch (_: Exception) {
            null
        } finally {
            requests.remove(request.id)
        }
    }

    fun close() {
        try {
            ws.close(1000, null)
        } catch (_: Exception) {
        }
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            val msg = json.decodeFromString(JsonRpcResponse.serializer(), text)
            val id = msg.id
            if (id != null) {
                requests[id]?.complete(msg)
            }
        } catch (_: Exception) {
            // ignore
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        onMessage(webSocket, bytes.utf8())
    }
}

@Serializable
private data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val id: Int,
    val method: String,
    val params: JsonElement? = null
)

@Serializable
private data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val id: Int? = null,
    val result: Map<String, JsonElement>? = null,
    val error: JsonRpcError? = null
)

@Serializable
private data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

@Serializable
data class McpToolsWrapper(
    val tools: List<McpTool> = emptyList()
)

/** Public view of an MCP tool. */
@Serializable
data class McpTool(
    val name: String,
    val description: String? = null,
    @SerialName("input_schema")
    val inputSchema: JsonElement? = null
)

/** Result content from an MCP tool call. */
@Serializable
data class McpToolCallResult(
    val content: List<McpContent> = emptyList()
)

@Serializable
data class McpContent(
    val type: String,
    val text: String? = null
)
