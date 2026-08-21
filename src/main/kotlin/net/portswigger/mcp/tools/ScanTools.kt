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
import kotlinx.serialization.Serializable
import net.portswigger.mcp.config.McpConfig
import java.net.URI
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Tracks in-flight crawl/audit tasks so MCP clients can poll status or stop them later.
 */
object ScanTaskRegistry {

    private val tasks = ConcurrentHashMap<Long, ScanTask>()
    private val nextId = AtomicLong(0)

    fun register(task: ScanTask): Long {
        val id = nextId.incrementAndGet()
        tasks[id] = task
        return id
    }

    fun get(id: Long): ScanTask? = tasks[id]

    fun remove(id: Long): ScanTask? = tasks.remove(id)
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

fun Server.registerScanTools(api: MontoyaApi, config: McpConfig) {

    mcpTool<StartCrawl>("Starts a crawl from the given seed URLs. Returns a task id to poll with get_scan_status and stop with stop_scan.") {
        val crawl = api.scanner().startCrawl(CrawlConfiguration.crawlConfiguration(*seedUrls.toTypedArray()))
        val taskId = ScanTaskRegistry.register(crawl)
        "Crawl started (task id $taskId) from ${seedUrls.size} seed URL(s)"
    }

    mcpTool<StartAudit>("Starts an active audit of the given seed URLs using the legacy active audit checks. Returns a task id to poll with get_scan_status and stop with stop_scan.") {
        val audit = api.scanner().startAudit(
            AuditConfiguration.auditConfiguration(BuiltInAuditConfiguration.LEGACY_ACTIVE_AUDIT_CHECKS)
        )
        seedUrls.forEach { url ->
            audit.addRequest(buildGetRequest(url))
        }
        val taskId = ScanTaskRegistry.register(audit)
        "Audit started (task id $taskId) for ${seedUrls.size} seed URL(s)"
    }

    mcpTool<GetScanStatus>("Returns the status of a crawl or audit task started via start_crawl / start_audit.") {
        val task = ScanTaskRegistry.get(taskId)
            ?: return@mcpTool "<No scan task with id $taskId>"

        val issues = (task as? Audit)?.issues()?.size
        buildString {
            appendLine("Task $taskId")
            appendLine("Status: ${task.statusMessage()}")
            appendLine("Requests: ${task.requestCount()}")
            appendLine("Errors: ${task.errorCount()}")
            if (issues != null) appendLine("Issues: $issues")
        }.trimEnd()
    }

    mcpTool<StopScan>("Stops and deletes a crawl or audit task started via start_crawl / start_audit.") {
        val task = ScanTaskRegistry.remove(taskId)
            ?: return@mcpTool "<No scan task with id $taskId>"
        task.delete()
        "Scan task $taskId stopped"
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