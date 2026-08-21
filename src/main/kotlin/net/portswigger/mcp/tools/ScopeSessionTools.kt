package net.portswigger.mcp.tools

import burp.api.montoya.MontoyaApi
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.portswigger.mcp.config.McpConfig
import net.portswigger.mcp.security.DataAccessSecurity
import net.portswigger.mcp.security.DataAccessType
import java.time.ZonedDateTime

fun Server.registerScopeSessionTools(api: MontoyaApi, config: McpConfig) {

    mcpTool<IsInScope>("Checks whether a URL is within Burp's suite-wide target scope.") {
        if (api.scope().isInScope(url)) {
            "$url is in scope"
        } else {
            "$url is NOT in scope"
        }
    }

    mcpTool<AddToScope>("Adds a URL to Burp's suite-wide target scope.") {
        api.scope().includeInScope(url)
        "Added $url to scope"
    }

    mcpTool<RemoveFromScope>("Removes a URL from Burp's suite-wide target scope.") {
        api.scope().excludeFromScope(url)
        "Removed $url from scope"
    }

    mcpTool<GetCookies>("Returns the cookies currently in Burp's cookie jar, optionally filtered by an exact domain match. Use this to understand session state for authenticated testing. Requires Cookie jar access approval; values may be sensitive.") {
        val allowed = runBlocking {
            DataAccessSecurity.checkDataAccessPermission(DataAccessType.COOKIES, config)
        }
        if (!allowed) {
            return@mcpTool "Cookie jar access denied by Burp Suite"
        }

        val cookies = api.http().cookieJar().cookies().filter { cookie ->
            domain == null || cookie.domain().equals(domain, ignoreCase = true)
        }

        if (cookies.isEmpty()) {
            if (domain == null) "<No cookies in the jar>" else "<No cookies for domain '$domain'>"
        } else {
            cookies.joinToString("\n") { c ->
                "${c.name()}=${c.value()}  domain=${c.domain()} path=${c.path()}"
            }
        }
    }

    mcpTool<SetCookie>("Adds a cookie to Burp's cookie jar. Provide domain (required), path (optional), and an ISO-8601 expiration (optional; omit for a session cookie).") {
        val expiration = expiration?.let { ZonedDateTime.parse(it) }
        api.http().cookieJar().setCookie(name, value, path, domain, expiration)
        "Cookie '$name' set for domain $domain"
    }
}

@Serializable
data class IsInScope(val url: String)

@Serializable
data class AddToScope(val url: String)

@Serializable
data class RemoveFromScope(val url: String)

@Serializable
data class GetCookies(val domain: String? = null)

@Serializable
data class SetCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String? = null,
    val expiration: String? = null
)