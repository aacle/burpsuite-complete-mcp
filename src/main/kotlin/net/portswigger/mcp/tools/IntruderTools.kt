package net.portswigger.mcp.tools

import burp.api.montoya.MontoyaApi
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.Serializable
import net.portswigger.mcp.config.McpConfig
import net.portswigger.mcp.shadow.ExchangeShadowStore

fun Server.registerIntruderTools(api: MontoyaApi, config: McpConfig) {

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
data class GetIntruderAttackResults(val limit: Int = 100)