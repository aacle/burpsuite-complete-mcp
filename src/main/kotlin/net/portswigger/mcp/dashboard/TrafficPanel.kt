package net.portswigger.mcp.dashboard

import net.portswigger.mcp.shadow.ExchangeShadowStore

class TrafficPanel : TablePanel("Traffic", arrayOf("Time", "Source", "Method", "URL", "Status")) {

    init {
        table.getColumnModel().getColumn(0).preferredWidth = 70
        table.getColumnModel().getColumn(1).preferredWidth = 90
        table.getColumnModel().getColumn(2).preferredWidth = 60
        table.getColumnModel().getColumn(3).preferredWidth = 420
        table.getColumnModel().getColumn(4).preferredWidth = 50
    }

    override fun refresh() {
        val rows = ExchangeShadowStore.listMetadata(limit = 300).map { e ->
            arrayOf<Any?>(
                formatTimestamp(e.timestamp),
                e.toolType,
                e.method,
                e.url,
                e.statusCode
            )
        }
        setRows(rows)
    }
}