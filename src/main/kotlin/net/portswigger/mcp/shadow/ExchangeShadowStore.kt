package net.portswigger.mcp.shadow

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.Registration
import burp.api.montoya.http.handler.HttpHandler
import burp.api.montoya.http.handler.HttpRequestToBeSent
import burp.api.montoya.http.handler.HttpResponseReceived
import burp.api.montoya.http.handler.RequestToBeSentAction
import burp.api.montoya.http.handler.ResponseReceivedAction
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Records every HTTP exchange that flows through Burp's HTTP engine — Proxy, Repeater,
 * Intruder, Scanner, and this extension's own MCP sends — into a bounded in-memory ring.
 *
 * This is the only reliable way to make a human tester's live Repeater/Intruder work visible
 * to an AI client, because the Montoya API exposes no read access to those tools.
 *
 * Source is tagged via [HttpRequestToBeSent.toolSource] ([burp.api.montoya.core.ToolType]),
 * and request/response pairs are correlated by their shared message id.
 */
data class ExchangeRecord(
    val id: Long,
    val messageId: Int,
    val toolType: String,
    val url: String,
    val method: String,
    val statusCode: Int,
    val request: String?,
    val response: String?,
    val timestamp: Long,
)

object ExchangeShadowStore {

    private var montoyaApi: MontoyaApi? = null
    private var registration: Registration? = null
    private var maxEntries = 2000

    private val nextId = AtomicLong(0)
    private val pending = ConcurrentHashMap<Int, PendingRequest>()
    private val exchanges = ArrayDeque<ExchangeRecord>()
    private val lock = Any()

    private class PendingRequest(
        val messageId: Int,
        val toolType: String,
        val url: String,
        val method: String,
        val request: String,
        val timestamp: Long,
    )

    @Synchronized
    fun start(api: MontoyaApi, maxEntries: Int = 2000) {
        if (registration?.isRegistered() == true) return
        this.montoyaApi = api
        this.maxEntries = maxEntries
        registration = api.http().registerHttpHandler(object : HttpHandler {
            override fun handleHttpRequestToBeSent(requestToBeSent: HttpRequestToBeSent): RequestToBeSentAction {
                try {
                    val toolType = requestToBeSent.toolSource()?.toolType()?.name ?: "UNKNOWN"
                    pending[requestToBeSent.messageId()] = PendingRequest(
                        messageId = requestToBeSent.messageId(),
                        toolType = toolType,
                        url = requestToBeSent.url(),
                        method = requestToBeSent.method(),
                        request = requestToBeSent.toString(),
                        timestamp = System.currentTimeMillis(),
                    )
                } catch (e: Exception) {
                    api.logging().logToError("MCP shadow store: failed to capture request: ${e.message}")
                }
                return RequestToBeSentAction.continueWith(requestToBeSent)
            }

            override fun handleHttpResponseReceived(responseReceived: HttpResponseReceived): ResponseReceivedAction {
                try {
                    val request = pending.remove(responseReceived.messageId())
                    if (request != null) {
                        add(
                            ExchangeRecord(
                                id = nextId.incrementAndGet(),
                                messageId = responseReceived.messageId(),
                                toolType = request.toolType,
                                url = request.url,
                                method = request.method,
                                statusCode = responseReceived.statusCode().toInt(),
                                request = request.request,
                                response = responseReceived.toString(),
                                timestamp = request.timestamp,
                            )
                        )
                    }
                } catch (e: Exception) {
                    api.logging().logToError("MCP shadow store: failed to capture response: ${e.message}")
                }
                return ResponseReceivedAction.continueWith(responseReceived)
            }
        })
        api.logging().logToOutput("MCP shadow store started (max $maxEntries entries)")
    }

    @Synchronized
    fun stop() {
        registration?.deregister()
        registration = null
        montoyaApi = null
        pending.clear()
        synchronized(lock) { exchanges.clear() }
    }

    private fun add(record: ExchangeRecord) {
        synchronized(lock) {
            exchanges.addLast(record)
            while (exchanges.size > maxEntries) exchanges.removeFirst()
        }
    }

    fun list(toolType: String? = null, limit: Int = 100): List<ExchangeRecord> = synchronized(lock) {
        exchanges
            .asReversed()
            .asSequence()
            .filter { toolType == null || it.toolType == toolType }
            .take(limit)
            .toList()
    }

    fun latest(toolType: String? = null): ExchangeRecord? = synchronized(lock) {
        exchanges.lastOrNull { toolType == null || it.toolType == toolType }
    }

    fun findById(id: Long): ExchangeRecord? = synchronized(lock) {
        exchanges.lastOrNull { it.id == id }
    }

    fun size(): Int = synchronized(lock) { exchanges.size }
}