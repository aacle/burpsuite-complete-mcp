package net.portswigger.mcp.tools

import burp.api.montoya.MontoyaApi
import burp.api.montoya.core.HighlightColor
import io.modelcontextprotocol.kotlin.sdk.server.Server
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.portswigger.mcp.config.McpConfig
import net.portswigger.mcp.security.DataAccessSecurity
import net.portswigger.mcp.security.DataAccessType

fun Server.registerAnnotationTools(api: MontoyaApi, config: McpConfig) {

    mcpTool<SetHistoryItemNotes>("Sets the Notes field on a proxy HTTP history item identified by its request id. Use this to attach a finding to its evidence for one-glance verification.") {
        val allowed = runBlocking {
            DataAccessSecurity.checkDataAccessPermission(DataAccessType.HTTP_HISTORY, config)
        }
        if (!allowed) {
            return@mcpTool "HTTP history access denied by Burp Suite"
        }

        val item = api.proxy().history { it.id() == id }.firstOrNull()
            ?: return@mcpTool "<No history item with id $id>"

        item.annotations().setNotes(notes)
        "Notes set on history item $id"
    }

    mcpTool<SetHistoryItemHighlight>("Sets the highlight color on a proxy HTTP history item identified by its request id. Color must be one of RED, ORANGE, YELLOW, GREEN, CYAN, BLUE, PINK, MAGENTA, GRAY, NONE. Use this to color-code findings.") {
        val allowed = runBlocking {
            DataAccessSecurity.checkDataAccessPermission(DataAccessType.HTTP_HISTORY, config)
        }
        if (!allowed) {
            return@mcpTool "HTTP history access denied by Burp Suite"
        }

        val item = api.proxy().history { it.id() == id }.firstOrNull()
            ?: return@mcpTool "<No history item with id $id>"

        val color = try {
            HighlightColor.valueOf(colorName.uppercase())
        } catch (e: IllegalArgumentException) {
            return@mcpTool "Invalid color '$colorName'. Use RED, ORANGE, YELLOW, GREEN, CYAN, BLUE, PINK, MAGENTA, GRAY, or NONE."
        }

        item.annotations().setHighlightColor(color)
        "Highlight set to ${color.name} on history item $id"
    }
}

@Serializable
data class SetHistoryItemNotes(val id: Int, val notes: String)

@Serializable
data class SetHistoryItemHighlight(val id: Int, val colorName: String)