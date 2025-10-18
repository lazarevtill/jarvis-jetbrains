package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message
import com.github.fmueller.jarvis.mcp.McpService

/**
 * Call an MCP tool by name with JSON arguments.
 */
class McpCallCommand(private val toolName: String, private val argsJson: String) : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        val mcp = McpService
        val out = mcp.callToolMarkdown(toolName, argsJson)
        conversation.addMessage(Message.fromAssistant(out))
        return conversation
    }
}
