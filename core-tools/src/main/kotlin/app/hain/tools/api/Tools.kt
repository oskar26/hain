package app.hain.tools.api

import app.hain.llm.api.ToolSchema
import java.time.ZonedDateTime

/** What a tool knows about the turn it runs in (TOOLS.md §6). [lang] is `de` or `en`. */
data class ToolContext(
    val lang: String,
    val now: ZonedDateTime,
    val offline: Boolean,
    /** How many search hits are read in full after a web search: 3, or 1 in the "Schnell" mode. */
    val pagesToRead: Int = DEFAULT_PAGES,
    /** The user's own SearXNG address from the settings (asked first), or `null` (4b). */
    val searxUrl: String? = null,
    /** Search hits a web search keeps, one per site (4c: up to 8). */
    val hits: Int = MAX_HITS,
) {
    companion object {
        const val DEFAULT_PAGES = 3
        const val FAST_PAGES = 1
        const val MAX_HITS = 8
    }
}

/** One search hit: a page the brain may want, with the search engine's short text about it. */
data class ToolHit(
    val title: String,
    val url: String,
    val snippet: String,
)

/** A page a tool used; shown as a source chip under the answer. */
data class ToolSource(
    val title: String,
    val url: String,
)

/**
 * Result of one tool call (TOOLS.md §7).
 *
 * @property forModel compact JSON the brain reads in `<tool_response>`.
 * @property label a few words for the tool line in the chat ("Köln", "12 × 7"); `null` = none.
 * @property error short English code (`offline`, `timeout`, `not_found` …); `null` = it worked.
 * @property readUrls pages the chat engine should read next with [ToolRouter.readPages] (web search).
 * @property action a phone action the user still has to confirm on a card (T-068); `null` = none.
 * @property via the tool that really answered when the one called had nothing (4c fallback):
 *   `web_search` after an empty Wikipedia, `wikipedia` after an empty web search; `null` = no fallback.
 * @property hits the search hits behind [readUrls], one per site (web search only).
 */
data class ToolResult(
    val forModel: String,
    val label: String? = null,
    val sources: List<ToolSource> = emptyList(),
    val error: String? = null,
    val readUrls: List<String> = emptyList(),
    val action: PhoneAction? = null,
    val via: String? = null,
    val hits: List<ToolHit> = emptyList(),
)

/** One tool the brain may call. [online] tools go through the NetworkGate. */
interface Tool {
    val schema: ToolSchema
    val online: Boolean

    suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult
}

/** Thrown by a tool when the brain sent arguments it cannot use. */
class BadArgumentsException(
    message: String,
) : IllegalArgumentException(message)
