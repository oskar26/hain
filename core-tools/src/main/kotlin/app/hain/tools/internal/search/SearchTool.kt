package app.hain.tools.internal.search

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import app.hain.llm.api.ToolSchema
import app.hain.tools.api.Args
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import app.hain.tools.api.ToolSource
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.URLEncoder

/**
 * `web_search` (T-066): web search without an API key. Tries the SearXNG instances, then
 * DuckDuckGo's HTML page, DuckDuckGo Lite, then Wikipedia's full-text search; the first that gives hits wins. The
 * result names the best [ToolContext.pagesToRead] pages in [ToolResult.readUrls], which the chat
 * engine reads next (T-067), so Pim answers from the pages and not only from the snippets.
 */
class SearchTool(
    private val gate: NetworkGate,
    private val searxInstances: List<String> = SEARX_INSTANCES,
    private val clock: () -> Long = System::currentTimeMillis,
) : Tool {
    /**
     * DuckDuckGo back-off (4b): when a DuckDuckGo page answers with its bot check, that page is
     * left out for [BACKOFF_MS] and the next service answers instead, so we stop knocking.
     */
    private val pausedUntil = java.util.concurrent.ConcurrentHashMap<String, Long>()

    /** `true` while [service] (`ddg` or `ddg_lite`) is paused after a bot check. */
    fun isPaused(service: String): Boolean = (pausedUntil[service] ?: 0L) > clock()

    override val online = true
    override val schema =
        ToolSchema(
            name = NAME,
            description =
                "Search the web for current or detailed information. " +
                    "Hain then reads the best pages for you.",
            parameters =
                buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("query") {
                            put("type", "string")
                            put("description", "A few search words")
                        }
                    }
                    putJsonArray("required") { add(JsonPrimitive("query")) }
                },
        )

    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val query = Args.required(args, "query").take(MAX_QUERY)
        val hits =
            search(query, ctx.lang, ctx.searxUrl)
                .distinctBy { it.url }
                .filterNot { blocked(it.url) }
                .take(MAX_HITS)
        if (hits.isEmpty()) return ToolRouter.failure("not_found", "The search found nothing for '$query'.")
        val json =
            buildJsonObject {
                put("query", query)
                putJsonArray("results") {
                    hits.forEachIndexed { i, h ->
                        addJsonObject {
                            put("n", i + 1)
                            put("title", h.title)
                            put("url", h.url)
                            put("snippet", h.snippet)
                        }
                    }
                }
            }
        return ToolResult(
            forModel = json.toString(),
            label = query,
            sources = hits.take(ctx.pagesToRead.coerceAtLeast(1)).map { ToolSource(it.title, it.url) },
            readUrls = hits.take(ctx.pagesToRead).map { it.url },
        )
    }

    /** Each service in turn; a failing one is skipped. Offline mode stops at once. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun search(
        query: String,
        lang: String,
        ownSearx: String?,
    ): List<SearchHit> {
        // the user's own SearXNG (settings) comes first, then the public ones
        val searx = (listOfNotNull(ownSearx) + searxInstances).distinct()
        val attempts =
            searx.map { base -> suspend { SearchParsers.searxng(get(searxUrl(base, query, lang))) } } +
                listOf(
                    suspend { ddg(DDG, ddgUrl(query, lang), SearchParsers::duckDuckGo) },
                    suspend { ddg(DDG_LITE, ddgLiteUrl(query, lang), SearchParsers::duckDuckGoLite) },
                    suspend { SearchParsers.wikipedia(get(wikiUrl(query, lang)), wikiLang(lang)) },
                )
        for (attempt in attempts) {
            val hits =
                try {
                    attempt()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: OfflineException) {
                    throw e
                } catch (e: Exception) {
                    emptyList()
                }
            if (hits.isNotEmpty()) return hits
        }
        return emptyList()
    }

    /** One DuckDuckGo page, skipped while paused; a bot check pauses it for [BACKOFF_MS]. */
    @Suppress("ReturnCount")
    private suspend fun ddg(
        service: String,
        url: String,
        parse: (String) -> List<SearchHit>,
    ): List<SearchHit> {
        if (isPaused(service)) return emptyList()
        val response = gate.fetch(GateRequest(url, Purpose.SEARCH, headers = HEADERS))
        if (SearchParsers.isBotCheck(response.code, response.body)) {
            pausedUntil[service] = clock() + BACKOFF_MS
            return emptyList()
        }
        if (!response.ok) throw IOException("HTTP ${response.code}")
        return parse(response.body)
    }

    private suspend fun get(url: String): String {
        val response = gate.fetch(GateRequest(url, Purpose.SEARCH, headers = HEADERS))
        if (!response.ok) throw IOException("HTTP ${response.code}")
        return response.body
    }

    companion object {
        const val NAME = "web_search"

        /** How long a DuckDuckGo page rests after its bot check (4b): 10 minutes. */
        const val BACKOFF_MS = 10 * 60 * 1000L
        const val DDG = "ddg"
        const val DDG_LITE = "ddg_lite"
        private const val MAX_QUERY = 120
        private const val MAX_HITS = 6

        /** Public SearXNG instances that answer JSON; tried first, in order (searx.space). */
        val SEARX_INSTANCES = listOf("https://searx.be", "https://search.inetol.net")

        private val HEADERS = mapOf("Accept-Language" to "de,en;q=0.8")

        /** Hosts whose pages are no use to read (video, social, shops). */
        private val BLOCKED = listOf("youtube.com", "tiktok.com", "instagram.com", "facebook.com", "x.com/", "amazon.")

        internal fun blocked(url: String) = BLOCKED.any { url.contains(it) }

        private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

        private fun wikiLang(lang: String) = if (lang == "de") "de" else "en"

        fun searxUrl(
            base: String,
            query: String,
            lang: String,
        ) = "$base/search?q=${enc(query)}&format=json&language=${wikiLang(lang)}&safesearch=1"

        fun ddgUrl(
            query: String,
            lang: String,
        ) = "https://html.duckduckgo.com/html/?q=${enc(query)}&kl=${if (lang == "de") "de-de" else "us-en"}"

        fun ddgLiteUrl(
            query: String,
            lang: String,
        ) = "https://lite.duckduckgo.com/lite/?q=${enc(query)}&kl=${if (lang == "de") "de-de" else "us-en"}"

        fun wikiUrl(
            query: String,
            lang: String,
        ) = "https://${wikiLang(lang)}.wikipedia.org/w/api.php?action=query&list=search&format=json" +
            "&srlimit=5&srsearch=${enc(query)}"
    }
}
