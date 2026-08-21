package net.portswigger.mcp.tools

import io.modelcontextprotocol.kotlin.sdk.server.ClientConnection
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolRequest
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.ContentBlock
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.serializer
import net.portswigger.mcp.ActivityLog
import net.portswigger.mcp.ToolCatalog
import net.portswigger.mcp.schema.asInputSchema
import kotlin.experimental.ExperimentalTypeInference

@OptIn(InternalSerializationApi::class)
inline fun <reified I : Any> Server.mcpTool(
    description: String,
    crossinline execute: I.() -> List<ContentBlock>
) {
    val toolName = I::class.simpleName?.toLowerSnakeCase() ?: error("Couldn't find name for ${I::class}")
    val serializer = I::class.serializer()
    val inputSchema = I::class.asInputSchema()

    ToolCatalog.register(toolName, description)

    val handler: suspend (ClientConnection, CallToolRequest) -> CallToolResult = { _, request ->
        try {
            val result = execute(
                lenientJson.decodeFromJsonElement(
                    serializer,
                    request.params.arguments ?: JsonObject(emptyMap())
                )
            )
            val summary = result.filterIsInstance<TextContent>()
                .joinToString(" ") { it.text }
                .let { ActivityLog.summarize(it) }
            ActivityLog.add(toolName, summary, isError = false)
            CallToolResult(content = result, isError = false)
        } catch (e: Exception) {
            ActivityLog.add(toolName, "Error: ${e.message}", isError = true)
            CallToolResult(
                content = listOf(TextContent("Error: ${e.message}")),
                isError = true
            )
        }
    }

    addTool(name = toolName, description = description, inputSchema = inputSchema, handler = handler)
}

@PublishedApi
internal val lenientJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("mcpToolString")
inline fun <reified I : Any> Server.mcpTool(
    description: String,
    crossinline execute: I.() -> String
) {
    mcpTool<I>(description, execute = {
        listOf(TextContent(execute(this)))
    })
}

inline fun <reified I : Any> Server.mcpUnitTool(
    description: String,
    crossinline execute: I.() -> Unit
) {
    mcpTool<I>(description, execute = {
        execute(this)

        listOf(TextContent("Executed tool"))
    })
}

inline fun <reified I : Paginated, J : Any> Server.mcpPaginatedTool(
    description: String,
    noinline mapper: (J) -> CharSequence = { it.toString() },
    crossinline execute: I.() -> List<J>
) {
    mcpTool<I>(description, execute = {

        val items = execute(this)

        when {
            offset >= items.size -> {
                "Reached end of items"
            }

            else -> {
                val upperLimit = (offset + count).coerceAtMost(items.size)

                items.subList(offset, upperLimit)
                    .joinToString(separator = "\n\n", transform = mapper)
            }
        }
    })
}

inline fun <reified I : Paginated> Server.mcpPaginatedTool(
    description: String,
    crossinline execute: I.() -> Sequence<String>
) {
    mcpTool<I>(description, execute = {
        val seq = execute(this)
        val paginated = seq.drop(offset).take(count).toList()

        if (paginated.isEmpty()) {
            listOf(TextContent("Reached end of items"))
        } else {
            listOf(TextContent(paginated.joinToString(separator = "\n\n")))
        }
    })
}

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("mcpNamedToolString")
inline fun Server.mcpTool(
    name: String,
    description: String,
    crossinline execute: () -> List<ContentBlock>
) {
    ToolCatalog.register(name, description)

    val handler: suspend (ClientConnection, CallToolRequest) -> CallToolResult = { _, _ ->
        try {
            val result = execute()
            val summary = result.filterIsInstance<TextContent>()
                .joinToString(" ") { it.text }
                .let { ActivityLog.summarize(it) }
            ActivityLog.add(name, summary, isError = false)
            CallToolResult(content = result, isError = false)
        } catch (e: Exception) {
            ActivityLog.add(name, "Error: ${e.message}", isError = true)
            CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
        }
    }
    addTool(name = name, description = description, inputSchema = ToolSchema(), handler = handler)
}

inline fun Server.mcpTool(
    name: String,
    description: String,
    crossinline execute: () -> String
) {
    ToolCatalog.register(name, description)

    val handler: suspend (ClientConnection, CallToolRequest) -> CallToolResult = { _, _ ->
        try {
            val result = execute()
            ActivityLog.add(name, ActivityLog.summarize(result), isError = false)
            CallToolResult(content = listOf(TextContent(result)), isError = false)
        } catch (e: Exception) {
            ActivityLog.add(name, "Error: ${e.message}", isError = true)
            CallToolResult(content = listOf(TextContent("Error: ${e.message}")), isError = true)
        }
    }
    addTool(name = name, description = description, inputSchema = ToolSchema(), handler = handler)
}

fun String.toLowerSnakeCase(): String {
    return this
        .replace(Regex("([a-z0-9])([A-Z])"), "$1_$2")
        .replace(Regex("([A-Z])([A-Z][a-z])"), "$1_$2")
        .replace(Regex("[\\s-]+"), "_")
        .lowercase()
}

interface Paginated {
    val count: Int
    val offset: Int
}