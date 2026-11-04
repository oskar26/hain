package app.hain.docs.api

import app.hain.docs.internal.search.DocSearch
import app.hain.docs.internal.search.SearchChunk
import app.hain.llm.api.ToolSchema
import app.hain.tools.api.BadArgumentsException
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * `search_document` (T-123): the best 4 parts of the long documents attached in this chat, with
 * their page. Offline; the brain only sees it while a long document is attached
 * ([ToolContext.documents]).
 */
class SearchDocumentTool(
    private val chunks: suspend (ids: List<Long>) -> List<SearchChunk>,
) : Tool {
    override val online = false
    override val schema =
        ToolSchema(
            name = NAME,
            description =
                "Search the documents the user attached in this chat. Returns the best passages with " +
                    "their page; cite them like [S. 2].",
            parameters =
                buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("query") {
                            put("type", "string")
                            put("description", "What to look for, in a few words")
                        }
                        putJsonObject("document") {
                            put("type", "string")
                            put("description", "Optional: the file name, when several are attached")
                        }
                    }
                    putJsonArray("required") { add("query") }
                },
        )

    @Suppress("ReturnCount")
    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val query =
            args["query"]?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
                ?: throw BadArgumentsException("Missing 'query'.")
        if (ctx.documents.isEmpty()) {
            return ToolResult(note("No document is attached in this chat."), error = "no_document")
        }
        val wanted =
            args["document"]
                ?.toString()
                ?.trim()
                ?.lowercase()
                ?.takeIf { it.isNotEmpty() && it != "null" }
        val all = chunks(ctx.documents)
        val pool = wanted?.let { w -> all.filter { w in it.document.lowercase() }.ifEmpty { all } } ?: all
        val best = DocSearch.best(pool, query)
        if (best.isEmpty()) {
            return ToolResult(note("Nothing in the document fits '$query'. Try other words."), label = query)
        }
        val json =
            JsonObject(
                mapOf(
                    "query" to JsonPrimitive(query),
                    "results" to
                        JsonArray(
                            best.map {
                                JsonObject(
                                    mapOf(
                                        "document" to JsonPrimitive(it.document),
                                        "page" to JsonPrimitive(it.page),
                                        "text" to JsonPrimitive(it.text),
                                    ),
                                )
                            },
                        ),
                    "note" to JsonPrimitive("Cite pages like [S. 2]."),
                ),
            )
        return ToolResult(forModel = json.toString(), label = query)
    }

    private fun note(text: String) = JsonObject(mapOf("note" to JsonPrimitive(text))).toString()

    companion object {
        const val NAME = "search_document"
    }
}
