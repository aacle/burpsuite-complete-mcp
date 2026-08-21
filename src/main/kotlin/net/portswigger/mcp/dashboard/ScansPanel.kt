package net.portswigger.mcp.dashboard

import net.portswigger.mcp.config.Design
import net.portswigger.mcp.tools.ScanTaskRegistry
import java.awt.FlowLayout
import javax.swing.JPanel

class ScansPanel : TablePanel("Scans", arrayOf("Task", "Status", "Requests", "Errors", "Issues")) {

    init {
        table.getColumnModel().getColumn(0).preferredWidth = 60
        table.getColumnModel().getColumn(1).preferredWidth = 200
        table.getColumnModel().getColumn(2).preferredWidth = 70
        table.getColumnModel().getColumn(3).preferredWidth = 60
        table.getColumnModel().getColumn(4).preferredWidth = 60

        val buttonBar = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
            isOpaque = false
            border = javax.swing.BorderFactory.createEmptyBorder(Design.Spacing.MD, 0, 0, 0)
        }

        val stopButton = Design.createOutlinedButton("Stop selected scan").apply {
            addActionListener {
                val row = table.selectedRow
                if (row >= 0) {
                    val taskId = (table.getValueAt(row, 0) as? Number)?.toLong()
                    if (taskId != null) {
                        ScanTaskRegistry.remove(taskId)?.delete()
                    }
                }
            }
        }
        buttonBar.add(stopButton)
        add(buttonBar, java.awt.BorderLayout.SOUTH)
    }

    override fun refresh() {
        val rows = ScanTaskRegistry.list().map { t ->
            arrayOf<Any?>(
                t.id,
                t.status,
                t.requests,
                t.errors,
                t.issues ?: ""
            )
        }
        setRows(rows)
    }
}