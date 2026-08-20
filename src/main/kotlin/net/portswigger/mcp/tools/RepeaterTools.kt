package net.portswigger.mcp.tools

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.message.requests.HttpRequest
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.portswigger.mcp.config.McpConfig
import net.portswigger.mcp.security.HttpRequestSecurity
import net.portswigger.mcp.shadow.ExchangeShadowStore
import net.portswigger.mcp.shadow.RepeaterUiBridge

fun Server.registerRepeaterTools(api: MontoyaApi, config: McpConfig) {

    val uiBridge = RepeaterUiBridge(api)

    mcpTool<ListRepeaterTabs>("Lists the Repeater tabs currently open in Burp, including the current request and response text of each tab when it can be read from the UI. Also includes recent Repeater exchanges recorded by the shadow store.") {
        val uiTabs = uiBridge.listTabs()
        val exchanges = ExchangeShadowStore.list(toolType = "REPEATER", limit = limit)

        val uiSection = if (uiTabs.isEmpty()) {
            "(No tabs readable via the UI — falling back to shadow-store exchanges only.)"
        } else {
            uiTabs.joinToString("\n\n") { tab ->
                buildString {
                    appendLine("Tab: ${tab.name}")
                    tab.request?.let { appendLine("Request:\n$it") }
                    tab.response?.let { appendLine("Response:\n$it") }
                }.trimEnd()
            }
        }

        val shadowSection = if (exchanges.isEmpty()) {
            "(No Repeater exchanges recorded yet.)"
        } else {
            exchanges.joinToString("\n\n") { e ->
                buildString {
                    appendLine("id=${e.id} ${e.method} ${e.url} -> ${e.statusCode}")
                    appendLine("Request:\n${e.request}")
                    appendLine("Response:\n${e.response}")
                }.trimEnd()
            }
        }

        "=== Repeater tabs (UI) ===\n$uiSection\n\n=== Recent Repeater exchanges (shadow store) ===\n$shadowSection"
    }

    mcpTool<ReadRepeaterTabRequest>("Reads the current request in a Repeater tab by name. Falls back to the most recent Repeater exchange recorded by the shadow store if the tab text cannot be read from the UI.") {
        val ui = uiBridge.findTab(tabName)
        if (ui?.request != null) {
            ui.request
        } else {
            ExchangeShadowStore.latest(toolType = "REPEATER")?.request
                ?: "<No request found for Repeater tab '$tabName'>"
        }
    }

    mcpTool<ReadRepeaterTabResponse>("Reads the current response in a Repeater tab by name. Falls back to the most recent Repeater exchange recorded by the shadow store if the tab text cannot be read from the UI.") {
        val ui = uiBridge.findTab(tabName)
        if (ui?.response != null) {
            ui.response
        } else {
            ExchangeShadowStore.latest(toolType = "REPEATER")?.response
                ?: "<No response found for Repeater tab '$tabName'>"
        }
    }

    mcpTool<SendRepeaterRequest>("Sends a raw HTTP/1.1 request and returns the response, and also stages the request in a Repeater tab under the given name (defaults to the target host). Use this after read_repeater_tab_request to mutate and resend a tester's request.") {
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

        response?.toString() ?: "<no response>"
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