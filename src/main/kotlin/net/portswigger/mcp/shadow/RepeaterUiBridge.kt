package net.portswigger.mcp.shadow

import burp.api.montoya.MontoyaApi
import java.awt.Component
import java.awt.Container
import javax.swing.JTabbedPane
import javax.swing.text.JTextComponent

/**
 * Best-effort bridge to read the names and current editor text of Repeater tabs by walking
 * Burp's Swing window tree. The Montoya API exposes no read access to Repeater, so this is the
 * only way to see a tab's *current* contents (including edits a tester has not sent yet).
 *
 * This is inherently fragile across Burp versions. All callers must treat the result as
 * best-effort and fall back to [ExchangeShadowStore] (which reliably records sent exchanges).
 */
data class RepeaterTab(
    val name: String,
    val request: String?,
    val response: String?,
)

class RepeaterUiBridge(private val api: MontoyaApi) {

    fun listTabs(): List<RepeaterTab> {
        return try {
            val frame = api.userInterface().swingUtils().suiteFrame()
            val tabs = mutableListOf<RepeaterTab>()
            walk(frame, tabs)
            tabs
        } catch (e: Exception) {
            api.logging().logToError("MCP Repeater UI bridge failed: ${e.message}")
            emptyList()
        }
    }

    fun findTab(name: String): RepeaterTab? = listTabs().firstOrNull { it.name == name }

    private fun walk(component: Component, out: MutableList<RepeaterTab>) {
        if (component is JTabbedPane) {
            for (i in 0 until component.tabCount) {
                val title = component.getTitleAt(i) ?: continue
                val texts = mutableListOf<String>()
                collectText(component.getComponentAt(i), texts)
                out.add(RepeaterTab(name = title, request = texts.getOrNull(0), response = texts.getOrNull(1)))
            }
        }
        if (component is Container) {
            for (child in component.components) {
                walk(child, out)
            }
        }
    }

    private fun collectText(component: Component, out: MutableList<String>) {
        if (component is JTextComponent) {
            out.add(component.text)
        }
        if (component is Container) {
            for (child in component.components) {
                collectText(child, out)
            }
        }
    }
}