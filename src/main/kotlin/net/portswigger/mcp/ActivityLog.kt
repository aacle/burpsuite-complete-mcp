package net.portswigger.mcp

import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A bounded, thread-safe log of every MCP tool invocation, so the MCP tab can show a live feed of
 * what the AI agent is doing (requests sent, tabs created, scans started, findings written, ...).
 */
object ActivityLog {

    data class Entry(
        val timestamp: Long,
        val tool: String,
        val result: String,
        val isError: Boolean
    )

    private const val MAX_ENTRIES = 1000

    private val entries = CopyOnWriteArrayList<Entry>()

    fun add(tool: String, result: String, isError: Boolean) {
        entries.add(Entry(System.currentTimeMillis(), tool, result, isError))
        while (entries.size > MAX_ENTRIES) {
            entries.removeAt(0)
        }
    }

    /** Newest-first view of the most recent [limit] entries. */
    fun list(limit: Int = 200): List<Entry> {
        val size = entries.size
        if (size == 0) return emptyList()
        val from = (size - limit).coerceAtLeast(0)
        return entries.subList(from, size).asReversed()
    }

    fun clear() {
        entries.clear()
    }

    fun size(): Int = entries.size

    /** Reduces a tool result to a single short line for the activity feed. */
    fun summarize(text: String): String {
        val line = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim() ?: ""
        return if (line.length > 120) line.take(120) + "..." else line
    }

    fun formatTime(timestamp: Long): String =
        SimpleDateFormat("HH:mm:ss").format(Date(timestamp))
}