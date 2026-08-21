package net.portswigger.mcp.dashboard

import net.portswigger.mcp.ToolCatalog
import net.portswigger.mcp.config.Design
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class ToolsPanel : TablePanel("Tools", arrayOf("Tool", "Description")) {

    private var allTools: List<ToolCatalog.Tool> = emptyList()

    private val searchField = JTextField().apply {
        preferredSize = Dimension(220, 28)
        font = Design.Typography.bodyMedium
        putClientProperty("JTextField.placeholderText", "Filter tools...")
        document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = applyFilter()
            override fun removeUpdate(e: DocumentEvent?) = applyFilter()
            override fun changedUpdate(e: DocumentEvent?) = applyFilter()
        })
    }

    init {
        table.getColumnModel().getColumn(0).preferredWidth = 260
        table.getColumnModel().getColumn(1).preferredWidth = 620
        headerPanel.add(searchField, BorderLayout.EAST)
    }

    override fun refresh() {
        allTools = ToolCatalog.list()
        applyFilter()
    }

    private fun applyFilter() {
        val query = searchField.text.trim().lowercase()
        val rows = allTools
            .filter { tool ->
                query.isEmpty() || tool.name.contains(query) || tool.description.lowercase().contains(query)
            }
            .map { arrayOf<Any?>(it.name, it.description) }
        setRows(rows)
    }
}