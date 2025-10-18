package com.github.fmueller.jarvis.commands

import com.github.fmueller.jarvis.conversation.Conversation
import com.github.fmueller.jarvis.conversation.Message
import com.github.fmueller.jarvis.mcp.McpService

/**
 * List available MCP tools from the configured server.
 */
class McpToolsCommand : SlashCommand {

    override suspend fun run(conversation: Conversation): Conversation {
        val mcp = McpService
        val md = mcp.listToolsMarkdown()
        conversation.addMessage(Message.fromAssistant(md))
        return conversation
    }
}
