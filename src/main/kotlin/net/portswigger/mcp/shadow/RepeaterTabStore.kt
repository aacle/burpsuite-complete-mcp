package net.portswigger.mcp.shadow

import java.util.concurrent.ConcurrentHashMap

/**
 * The staged Repeater tab the MCP server knows about. Burp's Montoya API exposes no way to read a
 * Repeater tab's contents, so we record every tab we stage (request + response) and read it back
 * from here. This is the reliable, headless-safe source of truth for "what did the AI stage".
 */
data class RepeaterTab(
    val name: String,
    val request: String,
    val response: String?,
    val httpVersion: String,
    val createdAt: Long,
    val updatedAt: Long,
)

/**
 * In-memory, thread-safe, bounded registry of Repeater tabs staged by this extension.
 */
object RepeaterTabStore {

    private const val MAX_TABS = 500

    private val tabs = ConcurrentHashMap<String, RepeaterTab>()

    fun record(name: String, request: String, response: String?, httpVersion: String) {
        val now = System.currentTimeMillis()
        val existing = tabs[name]
        val createdAt = existing?.createdAt ?: now
        tabs[name] = RepeaterTab(
            name = name,
            request = request,
            response = response,
            httpVersion = httpVersion,
            createdAt = createdAt,
            updatedAt = now
        )

        while (tabs.size > MAX_TABS) {
            val oldest = tabs.values.minByOrNull { it.updatedAt }
            oldest?.let { tabs.remove(it.name) } ?: break
        }
    }

    fun find(name: String): RepeaterTab? = tabs[name]

    /** Most recently staged/updated first. */
    fun list(): List<RepeaterTab> = tabs.values.sortedByDescending { it.updatedAt }

    fun remove(name: String): RepeaterTab? = tabs.remove(name)

    fun clear() {
        tabs.clear()
    }

    fun size(): Int = tabs.size
}