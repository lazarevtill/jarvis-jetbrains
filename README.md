# Jarvis

[![Build](https://github.com/fmueller/jarvis/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/fmueller/jarvis/actions/workflows/build.yml)
[![Version](https://img.shields.io/jetbrains/plugin/v/24755-jarvis.svg)](https://plugins.jetbrains.com/plugin/24755-jarvis)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/24755-jarvis.svg)](https://plugins.jetbrains.com/plugin/24755-jarvis)

## About

<!-- Plugin description -->
Jarvis is an LLM-powered developer plugin for the JetBrains IDE platform. It aims to support developers by leveraging
local LLMs only. To achieve this, it is integrating with Ollama. Jarvis keeps the currently used model in memory for
five minutes to reduce loading times.
<!-- Plugin description end -->

## Installation

1. Install and run [Ollama](https://ollama.com)
2. Install Jarvis plugin in your JetBrains IDE:
   - Using the IDE built-in plugin system: <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "jarvis"</kbd> >
     <kbd>Install</kbd>
   - Manually: Download the [latest release](https://github.com/fmueller/jarvis/releases/latest) and install it manually using
     <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## Usage

Jarvis can be controlled via chat messages and commands. To start a conversation, simply type ```/new``` in the chat window.

Available commands:

- ```/help``` or ```/?``` - Shows this help message
- ```/new``` - Starts a new conversation
- ```/plain``` - Sends a chat message without code context
- ```/copy``` - Copies the conversation to the clipboard
- ```/model <modelName>``` - Changes the model to use (model name `default` is `qwen3:1.7b`)
- ```/model``` or ```/model-info``` - Shows the info card of the current model
- ```/model set -<parameter> <value>``` - Configures model inference parameters
- ```/host <host>``` - Sets the Ollama host (host `default` is `http://localhost:11434`)

MCP integration (optional):

- When an MCP server is configured, Jarvis exposes its tools to the model. The model can request a tool call when needed and Jarvis will execute it and incorporate the result into the reply.
- Configure the MCP endpoint with: `/mcp host <wsUrl>` (e.g., `ws://localhost:5173/mcp`).
- Power users can still use `/mcp tools` and `/mcp call` explicitly if desired.

When using reasoning models with Ollama, Jarvis shows their internal thoughts in an expandable section at the top of each answer.

Auto-detecting JetBrains MCP Server:

- If the JetBrains "MCP Server" plugin is installed and running, Jarvis attempts to auto-detect the server at common endpoints (e.g., `ws://127.0.0.1:5173/mcp`).
- You can also set an environment variable `JETBRAINS_MCP_WS_URL` to the WebSocket endpoint, and Jarvis will use it automatically.
- If auto-detection fails, configure it explicitly via `/mcp host <wsUrl>`.

## License

This project is licensed under the [MIT](LICENSE).
