package net.portswigger.mcp.dashboard

import burp.api.montoya.MontoyaApi
import net.portswigger.mcp.ServerState
import net.portswigger.mcp.ToolCatalog
import net.portswigger.mcp.config.Design
import net.portswigger.mcp.config.McpConfig
import net.portswigger.mcp.shadow.ExchangeShadowStore
import net.portswigger.mcp.tools.ScanTaskRegistry
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class DashboardPanel(private val api: MontoyaApi, private val config: McpConfig) : JPanel(BorderLayout()) {

    private val statusLabel = JLabel("Stopped").apply {
        font = Design.Typography.headlineMedium
        foreground = Design.Colors.onSurfaceVariant
    }

    private val statsLabel = JLabel().apply {
        font = Design.Typography.bodyLarge
        foreground = Design.Colors.onSurface
        verticalAlignment = SwingConstants.TOP
    }

    init {
        background = Design.Colors.surface
        border = BorderFactory.createEmptyBorder(
            Design.Spacing.LG, Design.Spacing.LG, Design.Spacing.LG, Design.Spacing.LG
        )

        val titlePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            alignmentX = LEFT_ALIGNMENT

            add(JLabel("Burp Suite Complete MCP").apply {
                font = Design.Typography.headlineMedium
                foreground = Design.Colors.onSurface
                alignmentX = LEFT_ALIGNMENT
            })
            add(Box.createVerticalStrut(Design.Spacing.SM))
            add(statusLabel)
            add(Box.createVerticalStrut(Design.Spacing.MD))
            add(JLabel("Live view of the MCP server and the AI agent's activity.").apply {
                font = Design.Typography.bodyMedium
                foreground = Design.Colors.onSurfaceVariant
                alignmentX = LEFT_ALIGNMENT
            })
        }

        add(titlePanel, BorderLayout.NORTH)
        add(statsLabel, BorderLayout.CENTER)
    }

    fun updateServerState(state: ServerState) {
        val (text, color) = when (state) {
            ServerState.Starting -> "Starting..." to Design.Colors.warning
            ServerState.Running -> "Running" to Design.Colors.primary
            ServerState.Stopping -> "Stopping..." to Design.Colors.warning
            ServerState.Stopped -> "Stopped" to Design.Colors.onSurfaceVariant
            is ServerState.Failed -> "Failed" to Design.Colors.error
        }
        statusLabel.text = text
        statusLabel.foreground = color
        statusLabel.font = statusLabel.font.deriveFont(Font.BOLD)
    }

    fun refresh() {
        val edition = api.burpSuite().version().edition().name
        val html = """
            <html>
            <table cellpadding="4" style="border-spacing: 0 6px;">
              <tr><td><b>Server</b></td><td>${config.host}:${config.port}</td></tr>
              <tr><td><b>Edition</b></td><td>$edition</td></tr>
              <tr><td><b>MCP tools exposed</b></td><td>${ToolCatalog.count()}</td></tr>
              <tr><td><b>Traffic captured (shadow store)</b></td><td>${ExchangeShadowStore.size()}</td></tr>
              <tr><td><b>Active scans</b></td><td>${ScanTaskRegistry.list().size}</td></tr>
            </table>
            </html>
        """.trimIndent()
        statsLabel.text = html
    }
}