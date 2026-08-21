package net.portswigger.mcp.intruder

import burp.api.montoya.core.ByteArray
import burp.api.montoya.intruder.GeneratedPayload
import burp.api.montoya.intruder.IntruderInsertionPoint
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class McpPayloadGeneratorTest {

    @AfterEach
    fun tearDown() {
        unmockkStatic(GeneratedPayload::class)
    }

    @Test
    fun `payload store set and snapshot`() {
        IntruderPayloadStore.set(listOf("a", "b"))
        assertEquals(listOf("a", "b"), IntruderPayloadStore.snapshot())
        IntruderPayloadStore.set(emptyList())
        assertEquals(emptyList<String>(), IntruderPayloadStore.snapshot())
    }

    @Test
    fun `generator yields payloads in order per insertion point`() {
        mockkStatic(GeneratedPayload::class)
        val emitted = mutableListOf<String>()
        every { GeneratedPayload.payload(capture(emitted)) } returns mockk(relaxed = true)

        val generator = McpPayloadGenerator(listOf("alpha", "beta"))
        val insertionPoint = mockk<IntruderInsertionPoint>()
        val baseValue = mockk<ByteArray>()
        every { insertionPoint.baseValue() } returns baseValue
        every { baseValue.getBytes() } returns byteArrayOf(1, 2, 3)

        generator.generatePayloadFor(insertionPoint)
        generator.generatePayloadFor(insertionPoint)

        assertEquals(listOf("alpha", "beta"), emitted)
    }
}