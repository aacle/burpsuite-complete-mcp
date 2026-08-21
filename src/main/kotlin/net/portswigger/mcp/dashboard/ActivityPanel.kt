package net.portswigger.mcp.dashboard

import net.portswigger.mcp.ActivityLog
import net.portswigger.mcp.config.Design
import java.awt.Color
import java.awt.Component
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

class ActivityPanel : TablePanel("Activity", arrayOf("Time", "Tool", "Status", "Result")) {

    init {
        table.getColumnModel().getColumn(0).preferredWidth = 70
        table.getColumnModel().getColumn(1).preferredWidth = 210
        table.getColumnModel().getColumn(2).preferredWidth = 50
        table.getColumnModel().getColumn(3).preferredWidth = 520

        table.setDefaultRenderer(Any::class.java, object : DefaultTableCellRenderer() {
            override fun getTableCellRendererComponent(
                table: JTable,
                value: Any?,
                isSelected: Boolean,
                hasFocus: Boolean,
                row: Int,
                column: Int
            ): Component {
                val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
                if (!isSelected) {
                    val isError = table.getValueAt(row, 2) == "ERROR"
                    c.foreground = if (isError) Design.Colors.error else Design.Colors.onSurface
                }
                return c
            }
        })
    }

    override fun refresh() {
        val rows = ActivityLog.list(300).map { e ->
            arrayOf<Any?>(
                ActivityLog.formatTime(e.timestamp),
                e.tool,
                if (e.isError) "ERROR" else "OK",
                e.result
            )
        }
        setRows(rows)
    }
}