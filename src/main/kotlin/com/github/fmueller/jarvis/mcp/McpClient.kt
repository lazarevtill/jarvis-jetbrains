package com.github.fmueller.jarvis.mcp

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.*
import okio.ByteString
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
        val req = JsonRpcRequest(
            id = 1,
            method = "tools/list",
            params = null
        )
        val resp = sendAndAwait(wsUrl, req)
        val result = resp?.result ?: return emptyList()
        val tools = result["tools"]
        return tools?.let {
            json.decodeFromJsonElement(McpToolsWrapper.serializer(), result).tools
        } ?: emptyList()
    }

    /**
     * Call a specific tool via `tools/call` with provided arguments.
     */
    fun callTool(wsUrl: String, name: String, arguments: JsonElement?): McpToolCallResult? {
        val params = buildJsonObject {
            put("name", name)
            arguments?.let { put("arguments", it) }
        }
        val req = JsonRpcRequest(
            id = 1,
            method = "tools/call",
            params = params
        )
        val resp = sendAndAwait(wsUrl, req)
        val result = resp?.result ?: return null
        return try {
            json.decodeFromJsonElement(McpToolCallResponse.serializer(), result).result
        } catch (_: Exception) {
            null
        }
    }

    private fun sendAndAwait(wsUrl: String, request: JsonRpcRequest): JsonRpcResponse? {
        val okClient = OkHttpClient.Builder().build()
        val listener = SingleResponseWebSocketListener(json)
        val req = Request.Builder().url(wsUrl).build()
        val ws = okClient.newWebSocket(req, listener)
        try {
            val payload = json.encodeToString(JsonRpcRequest.serializer(), request)
            ws.send(payload)
            listener.await()
            return listener.response
        } finally {
            ws.close(1000, null)
        }
    }
}

private class SingleResponseWebSocketListener(private val json: Json) : WebSocketListener() {
    private val latch = CountDownLatch(1)
    @Volatile
    var response: JsonRpcResponse? = null
        private set

    fun await(timeoutSeconds: Long = 10): Boolean {
        return latch.await(timeoutSeconds, TimeUnit.SECONDS)
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        try {
            val msg = json.decodeFromString(JsonRpcResponse.serializer(), text)
            response = msg
        } catch (_: Exception) {
            // ignore non-JSON-RPC messages
        } finally {
            latch.countDown()
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        onMessage(webSocket, bytes.utf8())
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        latch.countDown()
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
data class McpToolCallResponse(
    val result: McpToolCallResult
)

@Serializable
data class McpContent(
    val type: String,
    val text: String? = null
)

