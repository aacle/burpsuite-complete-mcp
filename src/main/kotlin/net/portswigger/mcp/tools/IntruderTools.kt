package net.portswigger.mcp.tools

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.Range
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.intruder.HttpRequestTemplate
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.portswigger.mcp.config.McpConfig
import net.portswigger.mcp.security.HttpRequestSecurity
import net.portswigger.mcp.shadow.ExchangeShadowStore

fun Server.registerIntruderTools(api: MontoyaApi, config: McpConfig) {

    mcpTool<CreateIntruderAttack>("Stages an HTTP request in Intruder with optional payload positions (insertion point offsets), so the request keeps its payload markers. Note: the Montoya API cannot set the attack type, payloads, or start the attack - configure those in Intruder's UI after staging.") {
        val fixedContent = normalizeHttpContent(content)
        val requestDisplay = fixedContent

        val allowed = runBlocking {
            HttpRequestSecurity.checkHttpRequestPermission(targetHostname, targetPort, config, requestDisplay, api)
        }
        if (!allowed) {
            api.logging().logToOutput("MCP Intruder request denied: $targetHostname:$targetPort")
            return@mcpTool "Send Intruder request denied by Burp Suite"
        }

        val request = HttpRequest.httpRequest(toMontoyaService(), fixedContent)
        val resolvedTabName = tabName ?: targetHostname

        if (positions.isEmpty()) {
            api.intruder().sendToIntruder(request, resolvedTabName)
        } else {
            val ranges = positions.map { Range.range(it.start, it.end) }
            val template = HttpRequestTemplate.httpRequestTemplate(request, ranges)
            api.intruder().sendToIntruder(toMontoyaService(), template, resolvedTabName)
        }

        if (positions.isEmpty()) {
            "Staged in Intruder. Configure attack type, payloads, and payload positions in Intruder's UI."
        } else {
            "Staged in Intruder with ${positions.size} payload position(s). Configure attack type and payloads in Intruder's UI, then click Start Attack."
        }
    }

    mcpTool<GetIntruderAttackResults>("Displays recent Intruder requests and responses recorded by the shadow store. Use this to read the results of an Intruder attack (the Montoya API has no direct Intruder read access).") {
        val exchanges = ExchangeShadowStore.list(toolType = "INTRUDER", limit = limit)
        if (exchanges.isEmpty()) {
            "<No Intruder traffic recorded yet>"
        } else {
            exchanges.joinToString("\n\n") { e ->
                buildString {
                    appendLine("id=${e.id} ${e.method} ${e.url} -> ${e.statusCode}")
                    appendLine("Request:\n${e.request}")
                    appendLine("Response:\n${e.response}")
                }.trimEnd()
            }
        }
    }
}

@Serializable
data class IntruderPosition(val start: Int, val end: Int)

@Serializable
data class CreateIntruderAttack(
    val tabName: String? = null,
    val content: String,
    val positions: List<IntruderPosition> = emptyList(),
    override val targetHostname: String,
    override val targetPort: Int,
    override val usesHttps: Boolean
) : HttpServiceParams

@Serializable
data class GetIntruderAttackResults(val limit: Int = 100)