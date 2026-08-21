package net.portswigger.mcp.shadow

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ExchangeShadowStoreTruncationTest {

    @Test
    fun `short bodies pass through untouched`() {
        val body = "GET / HTTP/1.1\r\nHost: example.com\r\n\r\n"
        assertEquals(body, truncateCapturedBody(body))
    }

    @Test
    fun `huge bodies are capped with a marker`() {
        val huge = "A".repeat(50_000)
        val truncated = truncateCapturedBody(huge)

        assertTrue(truncated.length <= MAX_CAPTURED_BODY_CHARS + "...(truncated)".length)
        assertTrue(truncated.endsWith("...(truncated)"))
        assertEquals(MAX_CAPTURED_BODY_CHARS, truncated.length - "...(truncated)".length)
    }
}