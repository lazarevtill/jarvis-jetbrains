package com.github.fmueller.jarvis.mcp

import org.jetbrains.annotations.VisibleForTesting
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * Singleton to manage MCP configuration and simple operations.
 */
object McpService {

    @VisibleForTesting
    var wsUrl: String = ""

    private val client = McpClient()
    @Volatile
    private var triedAutoDiscovery: Boolean = false

    /**
     * Configure the MCP WebSocket URL.
     */
    fun setHost(url: String) {
        wsUrl = url.trim()
    }

    /**
     * Return a human-readable list of tools from the MCP server.
     */
    fun listToolsMarkdown(): String {
        ensureAutoConfigured()
        if (wsUrl.isEmpty()) {
            return "No MCP host configured. Use `/mcp host ws://host:port/path`."
        }
        return try {
            val tools = client.listTools(wsUrl)
            if (tools.isEmpty()) {
                "No tools reported by MCP server."
            } else buildString {
                appendLine("### MCP Tools")
                tools.forEach { t ->
                    append("- ")
                    append("**${t.name}**")
                    t.description?.let { d -> append(": ").append(d) }
                    appendLine()
                }
            }.trimEnd()
        } catch (e: Exception) {
            "Failed to list MCP tools: ${e.message}"
        }
    }

    /**
     * Call a tool and return a readable summary of its content.
     */
    fun callToolMarkdown(name: String, argsJson: String): String {
        ensureAutoConfigured()
        if (wsUrl.isEmpty()) {
            return "No MCP host configured. Use `/mcp host ws://host:port/path`."
        }
        if (name.isBlank()) {
            return "Usage: /mcp call <toolName> {jsonArgs}"
        }

        val jsonArgs: JsonElement? = try {
            if (argsJson.isBlank()) null else Json.parseToJsonElement(argsJson)
        } catch (e: Exception) {
            return "Invalid JSON arguments: ${e.message}"
        }

        return try {
            val result = client.callTool(wsUrl, name, jsonArgs ?: JsonNull)
            if (result == null || result.content.isEmpty()) {
                "No content returned by tool `$name`."
            } else buildString {
                appendLine("### MCP Tool Result: $name")
                result.content.forEach { c ->
                    when (c.type) {
                        "text" -> appendLine(c.text ?: "")
                        else -> appendLine("[${c.type}] ${c.text ?: ""}")
                    }
                }
            }.trimEnd()
        } catch (e: Exception) {
            "Tool call failed: ${e.message}"
        }
    }

    /**
     * Render available tools in a concise spec for the model prompt.
     * Returns null if no MCP host or tools are available.
     */
    fun toolsForModelSpec(): String? {
        ensureAutoConfigured()
        if (wsUrl.isEmpty()) {
            return null
        }
        return try {
            val tools = client.listTools(wsUrl)
            if (tools.isEmpty()) {
                null
            } else buildString {
                appendLine("You can request external tools via MCP.")
                appendLine("Available tools:")
                tools.forEach { t ->
                    append("- ")
                    append(t.name)
                    t.description?.let { d -> append(": ").append(d) }
                    appendLine()
                }
                appendLine()
                appendLine("When you need a tool, output exactly one line:")
                appendLine("TOOL_CALL: {\"name\":\"<toolName>\",\"arguments\":{...}}")
                appendLine("Do not include any other text on that line.")
            }.trimEnd()
        } catch (_: Exception) {
            null
        }
    }

    private fun ensureAutoConfigured() {
        if (wsUrl.isNotEmpty()) {
            return
        }
        if (triedAutoDiscovery) {
            return
        }
        synchronized(this) {
            if (triedAutoDiscovery || wsUrl.isNotEmpty()) {
                return
            }
            val envCandidates = sequenceOf(
                System.getenv("JETBRAINS_MCP_WS_URL"),
                System.getenv("JETBRAINS_MCP_SERVER_URL"),
                System.getenv("MCP_WS_URL"),
                System.getenv("MCP_SERVER_URL")
            ).filterNotNull().map { it.trim() }.filter { it.startsWith("ws://") || it.startsWith("wss://") }

            val defaultCandidates = sequenceOf(
                "ws://127.0.0.1:5173/mcp",
                "ws://localhost:5173/mcp",
                // JetBrains built-in webserver often uses 63342, include as a fallback
                "ws://127.0.0.1:63342/mcp",
                "ws://localhost:63342/mcp"
            )

            val candidates = (envCandidates + defaultCandidates).toList()
            for (candidate in candidates) {
                try {
                    // A successful round-trip is sufficient to accept this endpoint
                    client.listTools(candidate)
                    wsUrl = candidate
                    break
                } catch (_: Exception) {
                    // try next
                }
            }
            triedAutoDiscovery = true
        }
    }
}
