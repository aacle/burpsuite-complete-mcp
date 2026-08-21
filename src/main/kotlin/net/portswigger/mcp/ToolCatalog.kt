package net.portswigger.mcp

import java.util.concurrent.ConcurrentHashMap

/**
 * Registry of every MCP tool name + description, populated as tools are registered. Used by the
 * MCP tab to show a searchable catalog of what the AI agent can do.
 */
object ToolCatalog {

    data class Tool(val name: String, val description: String)

    private val tools = ConcurrentHashMap<String, String>()

    fun register(name: String, description: String) {
        tools[name] = description
    }

    fun list(): List<Tool> =
        tools.entries.map { Tool(it.key, it.value) }.sortedBy { it.name }

    fun count(): Int = tools.size

    fun clear() {
        tools.clear()
    }
}