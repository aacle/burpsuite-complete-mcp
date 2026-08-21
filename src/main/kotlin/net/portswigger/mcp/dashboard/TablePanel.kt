package net.portswigger.mcp.dashboard

import net.portswigger.mcp.config.Design
import java.awt.BorderLayout
import java.awt.Dimension
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.BorderFactory
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.table.DefaultTableModel

fun formatTimestamp(ms: Long): String =
    SimpleDateFormat("HH:mm:ss").format(Date(ms))

/**
 * A styled, read-only table panel with a section title. Subclasses implement [refresh] to repopulate
 * the table from their backing data source.
 */
abstract class TablePanel(title: String, columns: Array<String>) : JPanel(BorderLayout()) {

    protected val model = object : DefaultTableModel(columns, 0) {
        override fun isCellEditable(row: Int, column: Int): Boolean = false
    }

    protected val headerPanel = JPanel(BorderLayout()).apply {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(0, 0, Design.Spacing.MD, 0)
    }

    protected val table = JTable(model).apply {
        font = Design.Typography.bodyMedium
        rowHeight = 24
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
        background = Design.Colors.listBackground
        foreground = Design.Colors.onSurface
        gridColor = Design.Colors.outlineVariant
        showVerticalLines = false
        tableHeader.font = Design.Typography.labelMedium
    }

    init {
        background = Design.Colors.surface
        border = BorderFactory.createEmptyBorder(
            Design.Spacing.LG, Design.Spacing.LG, Design.Spacing.LG, Design.Spacing.LG
        )

        headerPanel.add(
            JLabel(title).apply {
                font = Design.Typography.titleMedium
                foreground = Design.Colors.onSurface
            },
            BorderLayout.CENTER
        )
        add(headerPanel, BorderLayout.NORTH)

        add(
            JScrollPane(table).apply {
                border = BorderFactory.createLineBorder(Design.Colors.outlineVariant, 1)
                background = Design.Colors.listBackground
                viewport.background = Design.Colors.listBackground
                preferredSize = Dimension(400, 300)
            },
            BorderLayout.CENTER
        )
    }

    protected fun setRows(rows: List<Array<Any?>>) {
        model.setRowCount(0)
        rows.forEach { model.addRow(it) }
    }

    abstract fun refresh()
}