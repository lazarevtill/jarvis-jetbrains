package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message
import com.github.fmueller.jarvis.mcp.McpService

/**
 * Set the MCP server WebSocket host (e.g., ws://localhost:5173/mcp).
 */
class McpHostCommand(private val host: String) : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        val service = McpService
        return if (host.isBlank()) {
            conversation.addMessage(Message.info("Usage: /mcp host ws://host:port/path"))
            conversation
        } else {
            service.setHost(host)
            conversation.addMessage(Message.info("MCP host set to: $host"))
            conversation
        }
    }
}
