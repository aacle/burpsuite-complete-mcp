package net.portswigger.mcp.tools

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.HttpService
import burp.api.montoya.http.message.requests.HttpRequest
import burp.api.montoya.scanner.AuditConfiguration
import burp.api.montoya.scanner.BuiltInAuditConfiguration
import burp.api.montoya.scanner.CrawlConfiguration
import burp.api.montoya.scanner.ScanTask
import burp.api.montoya.scanner.audit.Audit
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.portswigger.mcp.config.McpConfig
import net.portswigger.mcp.security.HttpRequestSecurity
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Tracks in-flight crawl/audit tasks so MCP clients can poll status or stop them later.
 */
object ScanTaskRegistry {

    data class ScanTaskInfo(
        val id: Long,
        val status: String,
        val requests: Int,
        val errors: Int,
        val issues: Int?
    )

    private val tasks = ConcurrentHashMap<Long, ScanTask>()
    private val nextId = AtomicLong(0)

    fun register(task: ScanTask): Long {
        val id = nextId.incrementAndGet()
        tasks[id] = task
        return id
    }

    fun get(id: Long): ScanTask? = tasks[id]

    fun remove(id: Long): ScanTask? = tasks.remove(id)

    /** Stops and forgets a task; returns false if it was unknown or already dead. */
    fun stop(id: Long): Boolean {
        val task = tasks.remove(id) ?: return false
        return try {
            task.delete()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Snapshot of tracked tasks. Montoya calls can throw for tasks that were stopped/deleted
     * out from under us, so each task is read defensively and broken entries are skipped rather
     * than failing the whole listing (this is polled by the UI every couple of seconds).
     */
    fun list(): List<ScanTaskInfo> = tasks.entries.mapNotNull { (id, task) ->
        try {
            ScanTaskInfo(
                id = id,
                status = task.statusMessage(),
                requests = task.requestCount(),
                errors = task.errorCount(),
                issues = (task as? Audit)?.issues()?.size
            )
        } catch (e: Exception) {
            null
        }
    }.sortedBy { it.id }
}

private fun buildGetRequest(url: String): HttpRequest {
    val uri = URI(url)
    val scheme = uri.scheme?.lowercase()
    val host = uri.host ?: throw IllegalArgumentException("Invalid URL (no host): $url")
    val secure = scheme == "https"
    val port = if (uri.port > 0) uri.port else if (secure) 443 else 80
    val path = uri.rawPath.ifBlank { "/" } + (uri.rawQuery?.let { "?$it" } ?: "")
    val service = HttpService.httpService(host, port, secure)
    return HttpRequest.httpRequest(service, "GET $path HTTP/1.1\r\nHost: $host\r\n\r\n")
}

private fun parseSeedTarget(url: String): Triple<String, Int, Boolean> {
    val uri = URI(url)
    val host = uri.host ?: throw IllegalArgumentException("Invalid URL (no host): $url")
    val secure = uri.scheme.equals("https", ignoreCase = true)
    val port = if (uri.port > 0) uri.port else if (secure) 443 else 80
    return Triple(host, port, secure)
}

/**
 * Active scans hit many endpoints automatically, so they must pass the same per-host approval
 * gate as single request sends - otherwise one mistyped seed URL scans someone else's site.
 */
private suspend fun approveSeedUrls(
    seedUrls: List<String>, config: McpConfig, api: MontoyaApi
): Boolean {
    for (url in seedUrls) {
        val target = try {
            parseSeedTarget(url)
        } catch (e: Exception) {
            return false
        }
        val approved = HttpRequestSecurity.checkHttpRequestPermission(
            target.first, target.second, config, url, api
        )
        if (!approved) return false
    }
    return true
}

fun Server.registerScanTools(api: MontoyaApi, config: McpConfig) {

    mcpTool<StartCrawl>("Starts a crawl from the given seed URLs. Every seed host must be approved (same approval flow as sending requests). Returns a task id to poll with get_scan_status and stop with stop_scan.") {
        runCatching { seedUrls.forEach { buildGetRequest(it) } }.onFailure { e ->
            return@mcpTool "Invalid seed URL: ${e.message}"
        }

        val allowed = runBlocking { approveSeedUrls(seedUrls, config, api) }
        if (!allowed) {
            api.logging().logToOutput("MCP crawl denied: $seedUrls")
            return@mcpTool "Crawl denied by Burp Suite"
        }

        val crawl = api.scanner().startCrawl(CrawlConfiguration.crawlConfiguration(*seedUrls.toTypedArray()))
        val taskId = ScanTaskRegistry.register(crawl)
        "Crawl started (task id $taskId) from ${seedUrls.size} seed URL(s)"
    }

    mcpTool<StartAudit>("Starts an active audit of the given seed URLs using the legacy active audit checks. Every seed host must be approved (same approval flow as sending requests). Returns a task id to poll with get_scan_status and stop with stop_scan.") {
        val requests = runCatching { seedUrls.map { buildGetRequest(it) } }.getOrElse { e ->
            return@mcpTool "Invalid seed URL: ${e.message}"
        }

        val allowed = runBlocking { approveSeedUrls(seedUrls, config, api) }
        if (!allowed) {
            api.logging().logToOutput("MCP audit denied: $seedUrls")
            return@mcpTool "Audit denied by Burp Suite"
        }

        val audit = api.scanner().startAudit(
            AuditConfiguration.auditConfiguration(BuiltInAuditConfiguration.LEGACY_ACTIVE_AUDIT_CHECKS)
        )
        requests.forEach { audit.addRequest(it) }
        val taskId = ScanTaskRegistry.register(audit)
        "Audit started (task id $taskId) for ${seedUrls.size} seed URL(s)"
    }

    mcpTool<GetScanStatus>("Returns the status of a crawl or audit task started via start_crawl / start_audit.") {
        val task = ScanTaskRegistry.get(taskId)
            ?: return@mcpTool "<No scan task with id $taskId>"

        val issues = try {
            (task as? Audit)?.issues()?.size
        } catch (e: Exception) {
            null
        }

        buildString {
            appendLine("Task $taskId")
            appendLine("Status: ${task.statusMessage()}")
            appendLine("Requests: ${task.requestCount()}")
            appendLine("Errors: ${task.errorCount()}")
            if (issues != null) appendLine("Issues: $issues")
        }.trimEnd()
    }

    mcpTool<StopScan>("Stops and deletes a crawl or audit task started via start_crawl / start_audit.") {
        if (ScanTaskRegistry.stop(taskId)) {
            "Scan task $taskId stopped"
        } else {
            "<No stoppable scan task with id $taskId>"
        }
    }
}

@Serializable
data class StartCrawl(val seedUrls: List<String>)

@Serializable
data class StartAudit(val seedUrls: List<String>)

@Serializable
data class GetScanStatus(val taskId: Long)

@Serializable
data class StopScan(val taskId: Long)