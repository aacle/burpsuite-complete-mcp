package net.portswigger.mcp

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActivityLogTest {

    @Test
    fun `entries are returned newest first`() {
        ActivityLog.clear()
        ActivityLog.add("tool_a", "result a", isError = false)
        ActivityLog.add("tool_b", "result b", isError = true)

        val list = ActivityLog.list()
        assertEquals("tool_b", list.first().tool)
        assertTrue(list.first().isError)
        assertEquals("tool_a", list.last().tool)
        assertTrue(!list.last().isError)
    }

    @Test
    fun `summarize takes first non-blank line and truncates`() {
        assertEquals(
            "HTTP/1.1 200 OK",
            ActivityLog.summarize("HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\nbody")
        )
        assertTrue(ActivityLog.summarize("x".repeat(300)).length <= 123)
    }
}

class ToolCatalogTest {

    @Test
    fun `register count and sorted list`() {
        ToolCatalog.clear()
        ToolCatalog.register("b_tool", "desc b")
        ToolCatalog.register("a_tool", "desc a")

        assertEquals(2, ToolCatalog.count())
        assertEquals(listOf("a_tool", "b_tool"), ToolCatalog.list().map { it.name })
    }
}