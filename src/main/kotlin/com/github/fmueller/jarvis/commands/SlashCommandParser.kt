package com.github.fmueller.jarvis.commands

object SlashCommandParser {

    fun parse(message: String): SlashCommand {
        val trimmed = message.trim()
        val trimmedMessage = trimmed.lowercase()
        if (trimmedMessage == "/help" || trimmedMessage == "/?") {
            return HelpCommand()
        }

        if (trimmedMessage == "/new") {
            return NewConversationCommand()
        }

        if (trimmedMessage.startsWith("/plain ")) {
            return PlainChatCommand()
        }

        if (trimmedMessage == "/model" || trimmedMessage == "/model-info") {
            return ModelCommand()
        }

        if (trimmedMessage.startsWith("/model set")) {
            val paramsPart = trimmed.removePrefix("/model set").trim()
            val tokens = paramsPart.split(" ")
            val params = mutableMapOf<String, String>()
            var i = 0
            while (i < tokens.size) {
                val token = tokens[i]
                if (token.startsWith("-")) {
                    val value = tokens.getOrNull(i + 1)
                    if (value != null && !value.startsWith("-")) {
                        params[token.removePrefix("-")] = value
                        i += 2
                        continue
                    }
                }
                i++
            }
            return ModelSetCommand(params)
        }

        if (trimmedMessage.startsWith("/model ")) {
            val modelName = trimmedMessage.removePrefix("/model ").trim()
            return ModelCommand(modelName)
        }

        if (trimmedMessage.startsWith("/host ")) {
            val host = trimmedMessage.removePrefix("/host ").trim()
            return HostCommand(host)
        }

        if (trimmedMessage == "/copy") {
            return CopyCommand()
        }

        // MCP subcommands
        if (trimmedMessage.startsWith("/mcp ")) {
            val withoutPrefix = trimmed.removePrefix("/mcp ")
            val parts = withoutPrefix.trim().split(" ", limit = 2)
            val sub = parts.firstOrNull()?.lowercase() ?: ""
            return when (sub) {
                "host" -> {
                    val host = parts.getOrNull(1)?.trim() ?: ""
                    McpHostCommand(host)
                }
                "tools" -> McpToolsCommand()
                "call" -> {
                    val rest = parts.getOrNull(1)?.trim() ?: ""
                    val callParts = rest.split(" ", limit = 2)
                    val toolName = callParts.firstOrNull()?.trim().orEmpty()
                    val argsJson = callParts.getOrNull(1)?.trim().orEmpty()
                    McpCallCommand(toolName, argsJson)
                }
                else -> ChatCommand()
            }
        }

        return ChatCommand()
    }
}
