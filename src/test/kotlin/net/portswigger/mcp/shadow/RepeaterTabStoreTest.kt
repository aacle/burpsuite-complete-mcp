package net.portswigger.mcp.shadow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RepeaterTabStoreTest {

    @Test
    fun `record find and list`() {
        RepeaterTabStore.clear()
        RepeaterTabStore.record("tab-a", "GET /a HTTP/1.1", null, "1.1")
        RepeaterTabStore.record("tab-b", "POST /b HTTP/2", "HTTP/2 200", "2")

        assertEquals("GET /a HTTP/1.1", RepeaterTabStore.find("tab-a")?.request)
        assertNull(RepeaterTabStore.find("tab-a")?.response)
        assertEquals("HTTP/2 200", RepeaterTabStore.find("tab-b")?.response)
        assertNull(RepeaterTabStore.find("missing"))

        val names = RepeaterTabStore.list().map { it.name }
        assertTrue(names.contains("tab-a"))
        assertTrue(names.contains("tab-b"))
    }

    @Test
    fun `record updates existing tab and preserves createdAt`() {
        RepeaterTabStore.clear()
        RepeaterTabStore.record("tab-x", "req1", null, "1.1")
        val first = RepeaterTabStore.find("tab-x")!!

        RepeaterTabStore.record("tab-x", "req2", "resp2", "1.1")
        val second = RepeaterTabStore.find("tab-x")!!

        assertEquals(first.createdAt, second.createdAt)
        assertEquals("req2", second.request)
        assertEquals("resp2", second.response)
    }

    @Test
    fun `remove clears a single entry`() {
        RepeaterTabStore.clear()
        RepeaterTabStore.record("tab-y", "req", null, "1.1")
        RepeaterTabStore.remove("tab-y")
        assertNull(RepeaterTabStore.find("tab-y"))
    }
}