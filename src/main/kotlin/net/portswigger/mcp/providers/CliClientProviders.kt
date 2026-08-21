package net.portswigger.mcp.providers

import burp.api.montoya.logging.Logging
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import net.portswigger.mcp.config.McpConfig
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

internal fun javaExecutablePath(): String {
    val javaHome = System.getProperty("java.home")
    val os = System.getProperty("os.name").lowercase()
    return if (os.contains("win")) "$javaHome\\bin\\java.exe" else "$javaHome/bin/java"
}

internal fun stdioProxyArgs(config: McpConfig, proxyJar: Path): List<String> =
    listOf("-jar", proxyJar.toString(), "--sse-url", "http://${config.host}:${config.port}")

private fun writeJson(path: Path, content: Map<String, JsonElement>): String {
    Files.createDirectories(path.parent)
    val json = Json { prettyPrint = true; encodeDefaults = true }
    path.writeText(json.encodeToString(JsonObject.serializer(), JsonObject(content)))
    return path.toString()
}

/**
 * Installs a stdio MCP server entry (java -jar proxy --sse-url) into OpenCode's config.
 */
class OpenCodeProvider(private val logging: Logging, private val proxyJarManager: ProxyJarManager) : Provider {

    override val name = "OpenCode"
    override val installButtonText = "Install to $name"
    override val confirmationText =
        "Install to $name?\nThis will add a 'burp' MCP server to ~/.config/opencode/opencode.json"

    override fun install(config: McpConfig): String {
        val proxyJar = proxyJarManager.getProxyJar()
        val path = Path.of(System.getProperty("user.home"), ".config", "opencode", "opencode.json")

        val existing = if (path.exists()) {
            Json.parseToJsonElement(path.readText()).jsonObject.toMutableMap()
        } else {
            mutableMapOf()
        }

        val mcp = (existing["mcp"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        mcp["burp"] = buildJsonObject {
            put("type", JsonPrimitive("stdio"))
            put("command", JsonPrimitive(javaExecutablePath()))
            put("args", JsonArray(stdioProxyArgs(config, proxyJar).map { JsonPrimitive(it) }))
        }
        existing["mcp"] = JsonObject(mcp)

        val written = writeJson(path, existing)
        logging.logToOutput("Installed Burp MCP Server to OpenCode config: $written")
        return "Installation successful. Please restart OpenCode if it is running."
    }
}

/**
 * Installs a stdio MCP server entry into GitHub Copilot CLI's config.
 */
class CopilotCliProvider(private val logging: Logging, private val proxyJarManager: ProxyJarManager) : Provider {

    override val name = "GitHub Copilot CLI"
    override val installButtonText = "Install to $name"
    override val confirmationText =
        "Install to $name?\nThis will add a 'burp' MCP server to ~/.copilot/mcp-config.json"

    override fun install(config: McpConfig): String {
        val proxyJar = proxyJarManager.getProxyJar()
        val path = Path.of(System.getProperty("user.home"), ".copilot", "mcp-config.json")

        val existing = if (path.exists()) {
            Json.parseToJsonElement(path.readText()).jsonObject.toMutableMap()
        } else {
            mutableMapOf()
        }

        val servers = (existing["servers"] as? JsonObject)?.toMutableMap() ?: mutableMapOf()
        servers["burp"] = buildJsonObject {
            put("command", JsonPrimitive(javaExecutablePath()))
            put("args", JsonArray(stdioProxyArgs(config, proxyJar).map { JsonPrimitive(it) }))
        }
        existing["servers"] = JsonObject(servers)

        val written = writeJson(path, existing)
        logging.logToOutput("Installed Burp MCP Server to Copilot CLI config: $written")
        return "Installation successful. Please restart the Copilot CLI if it is running."
    }
}

/**
 * Installs a stdio MCP server entry into OpenAI Codex CLI's TOML config.
 */
class CodexCliProvider(private val logging: Logging, private val proxyJarManager: ProxyJarManager) : Provider {

    override val name = "Codex CLI"
    override val installButtonText = "Install to $name"
    override val confirmationText =
        "Install to $name?\nThis will add a 'burp' MCP server to ~/.codex/config.toml"

    override fun install(config: McpConfig): String {
        val proxyJar = proxyJarManager.getProxyJar()
        val path = Path.of(System.getProperty("user.home"), ".codex", "config.toml")

        Files.createDirectories(path.parent)
        val existing = if (path.exists()) path.readText() else ""

        val entry = buildString {
            appendLine()
            appendLine("[mcp_servers.burp]")
            append("command = \"${javaExecutablePath()}\"")
            appendLine()
            append("args = [\"-jar\", \"$proxyJar\", \"--sse-url\", \"http://${config.host}:${config.port}\"]")
            appendLine()
        }

        val updated = if (existing.contains("[mcp_servers.burp]")) {
            // Replace the existing block (up to the next section header).
            existing.replace(
                Regex("(?s)\\[mcp_servers\\.burp\\].*?(?=\\[|\\z)"),
                entry.trimEnd()
            )
        } else {
            existing.trimEnd() + "\n" + entry.trimEnd() + "\n"
        }

        path.writeText(updated)
        logging.logToOutput("Installed Burp MCP Server to Codex CLI config: $path")
        return "Installation successful. Please restart the Codex CLI if it is running."
    }
}