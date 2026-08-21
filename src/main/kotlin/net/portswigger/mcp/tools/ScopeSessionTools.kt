package net.portswigger.mcp.tools

import burp.api.montoya.MontoyaApi
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.serialization.Serializable
import net.portswigger.mcp.config.McpConfig
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

    mcpTool<GetCookies>("Returns the cookies currently in Burp's cookie jar. Use this to understand the session state for authenticated testing. Note: cookie values may be sensitive.") {
        val cookies = api.http().cookieJar().cookies()
        if (cookies.isEmpty()) {
            "<No cookies in the jar>"
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