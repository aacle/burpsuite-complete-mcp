package net.portswigger.mcp.tools

import burp.api.montoya.MontoyaApi
import burp.api.montoya.http.message.HttpRequestResponse
import burp.api.montoya.sitemap.SiteMapFilter
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.portswigger.mcp.config.McpConfig
import net.portswigger.mcp.security.DataAccessSecurity
import net.portswigger.mcp.security.DataAccessType

fun Server.registerSitemapVerificationTools(api: MontoyaApi, config: McpConfig) {

    mcpPaginatedTool<GetSitemap>("Displays a compact summary of the site map (method, url, status code). Optionally filter by a URL prefix. Use this for passive recon of what Burp has discovered.") {
        val items = if (prefix.isNullOrBlank()) {
            api.siteMap().requestResponses()
        } else {
            api.siteMap().requestResponses(SiteMapFilter.prefixFilter(prefix))
        }

        items.asSequence().map { rr ->
            val method = rr.request()?.method() ?: "?"
            "${method} ${rr.url()} -> ${rr.statusCode()}"
        }
    }

    mcpTool<SendToOrganizerById>("Sends a proxy HTTP history item (by id) to Burp's Organizer tab for later follow-up.") {
        val allowed = runBlocking {
            DataAccessSecurity.checkDataAccessPermission(DataAccessType.HTTP_HISTORY, config)
        }
        if (!allowed) {
            return@mcpTool "HTTP history access denied by Burp Suite"
        }

        val item = api.proxy().history { it.id() == id }.firstOrNull()
            ?: return@mcpTool "<No history item with id $id>"
        val request = item.request()
            ?: return@mcpTool "<History item $id has no request>"
        val response = item.response()
            ?: return@mcpTool "<History item $id has no response>"

        api.organizer().sendToOrganizer(HttpRequestResponse.httpRequestResponse(request, response))
        "History item $id sent to Organizer"
    }

    mcpTool<SendToComparerById>("Sends a proxy HTTP history item's request and response (by id) to Burp's Comparer tool for diffing.") {
        val allowed = runBlocking {
            DataAccessSecurity.checkDataAccessPermission(DataAccessType.HTTP_HISTORY, config)
        }
        if (!allowed) {
            return@mcpTool "HTTP history access denied by Burp Suite"
        }

        val item = api.proxy().history { it.id() == id }.firstOrNull()
            ?: return@mcpTool "<No history item with id $id>"
        val request = item.request()
            ?: return@mcpTool "<History item $id has no request>"
        val response = item.response()
            ?: return@mcpTool "<History item $id has no response>"

        api.comparer().sendToComparer(request.toByteArray(), response.toByteArray())
        "History item $id request and response sent to Comparer"
    }
}

@Serializable
data class GetSitemap(
    val prefix: String? = null,
    override val count: Int,
    override val offset: Int
) : Paginated

@Serializable
data class SendToOrganizerById(val id: Int)

@Serializable
data class SendToComparerById(val id: Int)