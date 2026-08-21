package net.portswigger.mcp.tools

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.Range
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.intruder.HttpRequestTemplate
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.portswigger.mcp.config.McpConfig
import net.portswigger.mcp.intruder.IntruderPayloadStore
import net.portswigger.mcp.security.HttpRequestSecurity
import net.portswigger.mcp.shadow.ExchangeShadowStore

fun Server.registerIntruderTools(api: MontoyaApi, config: McpConfig) {

    mcpTool<CreateIntruderAttack>("Stages an HTTP request in Intruder with optional payload positions and a payload list. The payloads are registered with the 'MCP Payloads' payload generator, which you then select in Intruder before clicking Start Attack. Note: the Montoya API cannot set the attack type or start the attack programmatically - choose the attack type (Sniper/Battering ram/Pitchfork/Cluster bomb) in Intruder's UI.") {
        val fixedContent = normalizeHttpContent(content)
        val requestDisplay = fixedContent

        val allowed = runBlocking {
            HttpRequestSecurity.checkHttpRequestPermission(targetHostname, targetPort, config, requestDisplay, api)
        }
        if (!allowed) {
            api.logging().logToOutput("MCP Intruder request denied: $targetHostname:$targetPort")
            return@mcpTool "Send Intruder request denied by Burp Suite"
        }

        IntruderPayloadStore.set(payloads)

        val request = HttpRequest.httpRequest(toMontoyaService(), fixedContent)
        val resolvedTabName = tabName ?: targetHostname

        if (positions.isEmpty()) {
            api.intruder().sendToIntruder(request, resolvedTabName)
        } else {
            val ranges = positions.map { Range.range(it.start, it.end) }
            val template = HttpRequestTemplate.httpRequestTemplate(request, ranges)
            api.intruder().sendToIntruder(toMontoyaService(), template, resolvedTabName)
        }

        buildString {
            appendLine("Staged in Intruder with ${payloads.size} payload(s) and ${positions.size} payload position(s).")
            appendLine("In Intruder: choose the attack type, set payload type to 'MCP Payloads', then click Start Attack.")
            appendLine("After the attack runs, call get_intruder_attack_results to read the results.")
        }.trimEnd()
    }

    mcpTool<SetIntruderPayloads>("Sets the payload list used by the 'MCP Payloads' Intruder payload generator. Select 'MCP Payloads' as the payload type in Intruder to use these payloads.") {
        IntruderPayloadStore.set(payloads)
        "Set ${payloads.size} payload(s) for the MCP Payloads generator"
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
    val payloads: List<String> = emptyList(),
    val positions: List<IntruderPosition> = emptyList(),
    override val targetHostname: String,
    override val targetPort: Int,
    override val usesHttps: Boolean
) : HttpServiceParams

@Serializable
data class SetIntruderPayloads(val payloads: List<String>)

@Serializable
data class GetIntruderAttackResults(val limit: Int = 100)