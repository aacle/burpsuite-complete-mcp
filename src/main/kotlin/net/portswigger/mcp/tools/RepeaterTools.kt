package net.portswigger.mcp.tools

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.message.requests.HttpRequest
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.portswigger.mcp.config.McpConfig
import net.portswigger.mcp.security.HttpRequestSecurity
import net.portswigger.mcp.shadow.ExchangeShadowStore
import net.portswigger.mcp.shadow.RepeaterTabStore

fun Server.registerRepeaterTools(api: MontoyaApi, config: McpConfig) {

    mcpTool<ListRepeaterTabs>("Lists the Repeater tabs staged by this MCP server, each with the exact request and (if it was sent) its response. Also lists Repeater traffic sent from Burp's UI, which is captured without tab names because Burp exposes no Repeater read API.") {
        val staged = RepeaterTabStore.list().take(limit)
        val uiExchanges = ExchangeShadowStore.list(toolType = "REPEATER", limit = limit)

        val stagedSection = if (staged.isEmpty()) {
            "(No tabs staged by this MCP server yet.)"
        } else {
            staged.joinToString("\n\n") { t ->
                buildString {
                    appendLine("Tab: ${t.name} [HTTP/${t.httpVersion}]")
                    appendLine("Request:\n${t.request}")
                    appendLine(t.response?.let { "Response:\n$it" } ?: "Response: (not sent)")
                }.trimEnd()
            }
        }

        val uiSection = if (uiExchanges.isEmpty()) {
            "(No Repeater traffic sent from the UI recorded.)"
        } else {
            uiExchanges.joinToString("\n\n") { e ->
                buildString {
                    appendLine("UI Repeater: ${e.method} ${e.url} -> ${e.statusCode}")
                    appendLine("Request:\n${e.request}")
                    appendLine("Response:\n${e.response}")
                }.trimEnd()
            }
        }

        "=== Repeater tabs staged by MCP ===\n$stagedSection\n\n=== Repeater traffic sent from Burp's UI ===\n$uiSection"
    }

    mcpTool<ReadRepeaterTabRequest>("Reads back the request staged in a Repeater tab by this MCP server, by exact tab name. Burp exposes no API to read tabs a human typed manually, so only tabs this server staged (and sent traffic captured in the shadow store) are readable.") {
        RepeaterTabStore.find(tabName)?.request
            ?: "<No tab '$tabName' staged by this MCP server. Use list_repeater_tabs to see staged tabs, or check the shadow store for traffic sent from the UI.>"
    }

    mcpTool<ReadRepeaterTabResponse>("Reads back the response of a Repeater tab staged and sent by this MCP server, by exact tab name.") {
        val tab = RepeaterTabStore.find(tabName)
            ?: return@mcpTool "<No tab '$tabName' staged by this MCP server.>"
        tab.response ?: "<Tab '$tabName' has been staged but not sent - no response yet.>"
    }

    mcpTool<SendRepeaterRequest>("Sends a raw HTTP/1.1 request, returns the response, and stages the request in a Repeater tab under the given name (defaults to the target host). The staged request and response are recorded so read_repeater_tab_request/response can read them back by name.") {
        val fixedContent = normalizeHttpContent(content)
        val requestDisplay = fixedContent

        val allowed = runBlocking {
            HttpRequestSecurity.checkHttpRequestPermission(targetHostname, targetPort, config, requestDisplay, api)
        }
        if (!allowed) {
            api.logging().logToOutput("MCP Repeater request denied: $targetHostname:$targetPort")
            return@mcpTool "Send Repeater request denied by Burp Suite"
        }

        val request = HttpRequest.httpRequest(toMontoyaService(), fixedContent)
        val response = api.http().sendRequest(request)

        val resolvedTabName = tabName ?: targetHostname
        api.repeater().sendToRepeater(request, resolvedTabName)

        val responseText = response?.toString()
        RepeaterTabStore.record(resolvedTabName, fixedContent, responseText, "1.1")

        responseText ?: "<no response>"
    }
}

@Serializable
data class ListRepeaterTabs(val limit: Int = 100)

@Serializable
data class ReadRepeaterTabRequest(val tabName: String)

@Serializable
data class ReadRepeaterTabResponse(val tabName: String)

@Serializable
data class SendRepeaterRequest(
    val tabName: String? = null,
    val content: String,
    override val targetHostname: String,
    override val targetPort: Int,
    override val usesHttps: Boolean
) : HttpServiceParams