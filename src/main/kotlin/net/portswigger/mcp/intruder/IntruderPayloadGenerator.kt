package net.portswigger.mcp.intruder

import burp.api.montoya.intruder.AttackConfiguration
import burp.api.montoya.intruder.GeneratedPayload
import burp.api.montoya.intruder.IntruderInsertionPoint
import burp.api.montoya.intruder.PayloadGenerator
import burp.api.montoya.intruder.PayloadGeneratorProvider

/**
 * Holds the payload list the AI has supplied for the next Intruder attack. The list is set via the
 * `create_intruder_attack` / `set_intruder_payloads` tools and consumed by [McpPayloadGeneratorProvider]
 * when the user starts the attack in Intruder.
 */
object IntruderPayloadStore {

    @Volatile
    var payloads: List<String> = emptyList()
        private set

    fun set(newPayloads: List<String>) {
        payloads = newPayloads
    }

    fun snapshot(): List<String> = payloads
}

/**
 * A custom Intruder payload source named "MCP Payloads". When the user selects it in Intruder's
 * payload settings and starts an attack, Burp calls [providePayloadGenerator] and our generator
 * serves the payloads the AI set via the MCP tools.
 */
class McpPayloadGeneratorProvider : PayloadGeneratorProvider {

    override fun displayName(): String = "MCP Payloads"

    override fun providePayloadGenerator(attackConfiguration: AttackConfiguration): PayloadGenerator =
        McpPayloadGenerator(IntruderPayloadStore.snapshot())
}

/**
 * Yields each payload once per insertion point, then signals [GeneratedPayload.end].
 */
class McpPayloadGenerator(private val payloads: List<String>) : PayloadGenerator {

    private val positionIndexes = mutableMapOf<String, Int>()

    override fun generatePayloadFor(insertionPoint: IntruderInsertionPoint): GeneratedPayload {
        val key = insertionPoint.baseValue().getBytes().joinToString(",") { it.toString() }
        val index = positionIndexes.getOrDefault(key, 0)

        return if (index < payloads.size) {
            positionIndexes[key] = index + 1
            GeneratedPayload.payload(payloads[index])
        } else {
            GeneratedPayload.end()
        }
    }
}