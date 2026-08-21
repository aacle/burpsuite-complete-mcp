package net.portswigger.mcp.schema

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

private const val TRUNCATION_MARKER = "... (truncated)"

/**
 * Configurable limits for history serialization. Values are synced from [net.portswigger.mcp.config.McpConfig]
 * when tools are registered, so the per-item and per-field budgets can be tuned from the Burp MCP tab.
 */
object HistoryLimits {
    @Volatile
    var maxItemChars: Int = 12_000

    @Volatile
    var maxFieldChars: Int = 8_000
}

internal inline fun <reified T> encodeHistoryItem(item: T): String =
    limitHistoryItemJson(Json.encodeToString(item))

@PublishedApi
internal fun limitHistoryItemJson(serialized: String): String {
    if (serialized.length <= HistoryLimits.maxItemChars) return serialized

    val item = Json.parseToJsonElement(serialized)
    var lowerBound = TRUNCATION_MARKER.length
    var upperBound = serialized.length
    var best: String? = null

    while (lowerBound <= upperBound) {
        val fieldLimit = (lowerBound + upperBound) / 2
        val candidate = Json.encodeToString(
            JsonElement.serializer(), item.truncateStringsTo(fieldLimit)
        )

        if (candidate.length <= HistoryLimits.maxItemChars) {
            best = candidate
            lowerBound = fieldLimit + 1
        } else {
            upperBound = fieldLimit - 1
        }
    }

    return checkNotNull(best) {
        "History item JSON structure exceeds the ${HistoryLimits.maxItemChars} character limit"
    }
}

/**
 * Truncates a single field (request, response, notes) to [HistoryLimits.maxFieldChars] so that a large
 * request body does not cause the response to be dropped entirely from the item.
 */
internal fun String.truncateField(maxChars: Int = HistoryLimits.maxFieldChars): String {
    if (length <= maxChars) return this

    var prefixLength = maxChars - TRUNCATION_MARKER.length
    if (prefixLength > 0 && this[prefixLength - 1].isHighSurrogate() && this[prefixLength].isLowSurrogate()) {
        prefixLength--
    }
    return take(prefixLength) + TRUNCATION_MARKER
}

private fun JsonElement.truncateStringsTo(maxLength: Int): JsonElement = when (this) {
    is JsonObject -> JsonObject(mapValues { (_, value) -> value.truncateStringsTo(maxLength) })
    is JsonArray -> JsonArray(map { it.truncateStringsTo(maxLength) })
    is JsonPrimitive -> if (isString) JsonPrimitive(content.truncateTo(maxLength)) else this
}

private fun String.truncateTo(maxLength: Int): String {
    if (length <= maxLength) return this

    var prefixLength = maxLength - TRUNCATION_MARKER.length
    if (prefixLength > 0 && this[prefixLength - 1].isHighSurrogate() && this[prefixLength].isLowSurrogate()) {
        prefixLength--
    }
    return take(prefixLength) + TRUNCATION_MARKER
}