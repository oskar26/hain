package app.hain.tools.api

import app.hain.common.api.WebSearch
import app.hain.common.api.net.OfflineException
import app.hain.llm.api.ToolCall
import app.hain.llm.api.ToolSchema
import app.hain.tools.internal.search.PageReader
import app.hain.tools.internal.search.ReadPage
import app.hain.tools.internal.search.ReadPageTool
import app.hain.tools.internal.search.SearchTool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException

/**
 * What runs instead when a tool has nothing (4c): [tool] with the arguments [args] makes from the
 * first call's, and [note] tells the brain why ("Wikipedia had nothing, here is a web search").
 */
class Fallback(
    val tool: String,
    val note: String,
    val args: (Map<String, Any?>) -> Map<String, Any?>,
)

/**
 * Registry of the tools (T-062): which schemas the brain sees and how one call runs. Every call
 * ends in a [ToolResult]; failures become a short error the brain can explain (TOOLS.md §7).
 *
 * Fallbacks (T-069) are decided here, not by the brain: when a tool in [fallbacks] finds nothing,
 * times out or fails, its fallback runs right away with the same question, and the result says so
 * ([ToolResult.via]).
 */
class ToolRouter(
    private val tools: List<Tool>,
    private val timeoutMs: Long = TIMEOUT_MS,
    private val pages: PageReader? = null,
    private val fallbacks: Map<String, Fallback> = emptyMap(),
) {
    private val byName = tools.associateBy { it.schema.name }

    /**
     * Schemas for the prompt; online tools are left out while offline (shorter prompt, no dead ends),
     * the web search and read_page also when the setting `web_search` is off (3b-2).
     */
    fun schemas(
        offline: Boolean,
        webSearch: WebSearch = WebSearch.ON,
    ): List<ToolSchema> =
        tools
            .filter { !offline || !it.online }
            .filter { webSearch != WebSearch.OFF || it.schema.name !in SEARCH_TOOLS }
            .map { it.schema }

    fun isOnline(name: String): Boolean = byName[name]?.online == true

    /**
     * Runs [call]; when it had nothing and has a [Fallback], the fallback runs with the same
     * question. The fallback's answer carries the note and [ToolResult.via]; when the fallback has
     * nothing either, the first error stays.
     */
    @Suppress("ReturnCount")
    suspend fun run(
        call: ToolCall,
        ctx: ToolContext,
    ): ToolResult {
        val first = runOne(call, ctx)
        if (first.error !in RECOVERABLE) return first
        val fallback = fallbacks[call.name] ?: return first
        val second = runOne(ToolCall(call.id, fallback.tool, fallback.args(call.arguments)), ctx)
        if (second.error != null) return first
        val json = JsonObject(mapOf("note" to JsonPrimitive(fallback.note), "result" to parse(second.forModel)))
        return second.copy(forModel = json.toString(), via = fallback.tool)
    }

    // Any tool problem must become a short answer for the brain, never a crash; the code is the message.
    @Suppress("TooGenericExceptionCaught", "SwallowedException", "ReturnCount")
    private suspend fun runOne(
        call: ToolCall,
        ctx: ToolContext,
    ): ToolResult {
        val tool = byName[call.name] ?: return failure("unknown_tool", "There is no tool called ${call.name}.")
        if (tool.online && ctx.offline) return failure("offline", OFFLINE_HINT)
        searchSwitch(call.name, ctx)?.let { return it }
        return try {
            withTimeout(timeoutMs) { tool.run(call.arguments, ctx) }
        } catch (e: TimeoutCancellationException) {
            failure("timeout", "The tool took too long.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: OfflineException) {
            failure("offline", OFFLINE_HINT)
        } catch (e: IllegalArgumentException) {
            failure("bad_arguments", e.message ?: "The arguments do not fit.")
        } catch (e: IOException) {
            failure("network", "The service could not be reached.")
        } catch (e: Exception) {
            failure("failed", "The tool did not work.")
        }
    }

    /** 3b-2: the web search follows its own switch; a Wikipedia fallback to the web as well. */
    private fun searchSwitch(
        name: String,
        ctx: ToolContext,
    ): ToolResult? =
        when {
            name !in SEARCH_TOOLS -> null
            ctx.webSearch == WebSearch.OFF -> failure(SEARCH_OFF, SEARCH_OFF_HINT)
            ctx.webSearch == WebSearch.ASK && !ctx.searchApproved -> failure(ASK_FIRST, ASK_FIRST_HINT)
            else -> null
        }

    /**
     * Reads the pages a web search picked (T-067, T-069), all at once (each page has its own
     * 8-second limit, all together at most [timeoutMs]). With a [focus] each page keeps only its
     * [maxChars] most relevant characters. Pages that fail are left out.
     */
    @Suppress("SwallowedException", "ReturnCount")
    suspend fun readExcerpts(
        urls: List<String>,
        ctx: ToolContext,
        maxChars: Int = PAGE_CHARS,
        focus: String? = null,
        timeoutMs: Long = READ_TIMEOUT_MS,
    ): List<ReadPage> {
        val reader = pages ?: return emptyList()
        if (urls.isEmpty() || ctx.offline || timeoutMs <= 0) return emptyList()
        return try {
            withTimeout(timeoutMs) { reader.readAll(urls, maxChars, focus) }
        } catch (e: TimeoutCancellationException) {
            emptyList()
        } catch (e: OfflineException) {
            emptyList()
        }
    }

    /**
     * Reads the pages a web search picked (T-067), all at once, each cut to [maxChars]. The result
     * lists only the pages that worked; `null` when nothing could be read.
     */
    suspend fun readPages(
        urls: List<String>,
        ctx: ToolContext,
        maxChars: Int = PAGE_CHARS,
    ): ToolResult? {
        val read = readExcerpts(urls, ctx, maxChars)
        if (read.isEmpty()) return null
        return ToolResult(
            forModel = PageReader.forModel(read),
            label = read.size.toString(),
            sources = read.map { ToolSource(it.title, it.url) },
        )
    }

    companion object {
        const val TIMEOUT_MS = 12_000L

        /** All pages of one search together (they load in parallel). */
        const val READ_TIMEOUT_MS = 15_000L

        /** Characters per page the brain gets after a search (3 pages ≈ 1000 tokens). */
        const val PAGE_CHARS = 1200
        private const val OFFLINE_HINT = "Offline mode is on. Tell the user and offer to answer from memory."

        /** Tools that search or read the open web; they follow the setting `web_search` (3b-2). */
        val SEARCH_TOOLS = setOf(SearchTool.NAME, ReadPageTool.NAME)
        const val SEARCH_OFF = "search_off"
        const val ASK_FIRST = "ask_first"
        private const val SEARCH_OFF_HINT =
            "Web search is switched off in the settings. Answer from what you know and say so in one sentence."
        private const val ASK_FIRST_HINT =
            "The user wants to be asked before you search the web. Ask in one short question whether you may " +
                "search for this now. Do not answer the question yet."

        /** Errors after which a fallback may help: nothing found, nothing readable, slow or broken. */
        val RECOVERABLE = setOf("not_found", "empty", "disambiguation", "timeout", "network", "failed")

        private fun parse(json: String): JsonElement {
            val parsed = runCatching { Json.parseToJsonElement(json) }
            return parsed.getOrElse { JsonPrimitive(json) }
        }

        /** `{"error":"<code>","hint":"<one sentence>"}` (TOOLS.md §7). */
        fun failure(
            code: String,
            hint: String,
        ): ToolResult {
            val json = JsonObject(mapOf("error" to JsonPrimitive(code), "hint" to JsonPrimitive(hint)))
            return ToolResult(forModel = json.toString(), error = code)
        }
    }
}

/** Helpers for reading the brain's arguments, which often arrive as text. */
internal object Args {
    fun string(
        args: Map<String, Any?>,
        key: String,
    ): String? = args[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

    fun required(
        args: Map<String, Any?>,
        key: String,
    ): String = string(args, key) ?: throw BadArgumentsException("Missing '$key'.")

    fun int(
        args: Map<String, Any?>,
        key: String,
    ): Int? =
        when (val v = args[key]) {
            is Number -> v.toInt()
            is String -> v.trim().toDoubleOrNull()?.toInt()
            else -> null
        }
}
