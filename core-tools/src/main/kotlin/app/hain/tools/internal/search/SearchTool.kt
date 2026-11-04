package app.hain.tools.internal.search

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import app.hain.tools.api.Args
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolHit
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import app.hain.tools.api.ToolSource
import app.hain.tools.api.ToolSpecs
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.IOException
import java.net.URLEncoder

/**
 * `web_search` (T-066): web search without an API key. Tries the SearXNG instances, then
 * DuckDuckGo's HTML page, DuckDuckGo Lite; the first that gives hits wins. When the whole web search
 * has nothing, Wikipedia's full-text search (German and English) answers instead (4c fallback,
 * [ToolResult.via]). Up to 8 hits, one per site; the best [ToolContext.pagesToRead] of them are in
 * [ToolResult.readUrls], which the chat engine reads next (T-067, T-069), so Pim answers from
 * several different pages and not only from the snippets.
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
    override val schema = ToolSpecs.of(NAME).schema

    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val query = Args.required(args, "query").take(MAX_QUERY)
        val found = search(query, ctx.lang, ctx.searxUrl)
        // 4c: one hit per site, so the pages read come from different sources (Wikipedia hits are all one site)
        val usable = found.hits.filterNot { blocked(it.url) }
        val hits =
            (if (found.fromWikipedia) usable.distinctBy { it.url } else onePerSite(usable))
                .take(ctx.hits.coerceIn(1, MAX_HITS))
        if (hits.isEmpty()) {
            return ToolRouter.failure(
                "not_found",
                "The search found nothing for '$query'. Try 2 to 4 other key words once, else say you found nothing.",
            )
        }
        val json =
            buildJsonObject {
                put("query", query)
                if (found.fromWikipedia) put("note", WIKI_NOTE)
                putJsonArray("results") {
                    hits.forEach { h ->
                        addJsonObject {
                            put("title", h.title)
                            put("url", h.url)
                            put("snippet", h.snippet)
                        }
                    }
                }
            }
        val read = hits.take(ctx.pagesToRead.coerceAtLeast(0))
        return ToolResult(
            forModel = json.toString(),
            label = query,
            sources = hits.take(ctx.pagesToRead.coerceAtLeast(1)).map { ToolSource(it.title, it.url) },
            readUrls = read.map { it.url },
            via = if (found.fromWikipedia) WIKIPEDIA else null,
            hits = hits.map { ToolHit(it.title, it.url, it.snippet) },
        )
    }

    private class Found(
        val hits: List<SearchHit>,
        val fromWikipedia: Boolean,
    )

    /**
     * Each web service in turn; a failing one is skipped. When none has anything, Wikipedia's
     * search answers instead, first in the chat's language, then in the other (4c fallback).
     * Offline mode stops at once.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun search(
        query: String,
        lang: String,
        ownSearx: String?,
    ): Found {
        // the user's own SearXNG (settings) comes first, then the public ones
        val searx = (listOfNotNull(ownSearx) + searxInstances).distinct()
        val web =
            searx.map { base -> suspend { SearchParsers.searxng(get(searxUrl(base, query, lang))) } } +
                listOf(
                    suspend { ddg(DDG, ddgUrl(query, lang), SearchParsers::duckDuckGo) },
                    suspend { ddg(DDG_LITE, ddgLiteUrl(query, lang), SearchParsers::duckDuckGoLite) },
                )
        val first = wikiLang(lang)
        val wiki =
            listOf(first, if (first == "de") "en" else "de").map { l ->
                suspend { SearchParsers.wikipedia(get(wikiUrl(query, l)), l) }
            }
        firstHits(web)?.let { return Found(it, fromWikipedia = false) }
        return Found(firstHits(wiki).orEmpty(), fromWikipedia = true)
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private suspend fun firstHits(attempts: List<suspend () -> List<SearchHit>>): List<SearchHit>? {
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
        return null
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
        const val WIKIPEDIA = "wikipedia"
        private const val MAX_QUERY = 120

        /** Hits kept per search, one per site (4c). */
        const val MAX_HITS = 8
        private const val WIKI_NOTE = "The web search had nothing. These hits are from Wikipedia."

        /** Public SearXNG instances that answer JSON; tried first, in order (searx.space). */
        val SEARX_INSTANCES = listOf("https://searx.be", "https://search.inetol.net")

        private val HEADERS = mapOf("Accept-Language" to "de,en;q=0.8")

        /** Hosts whose pages are no use to read (video, social, shops). */
        private val BLOCKED = listOf("youtube.com", "tiktok.com", "instagram.com", "facebook.com", "x.com/", "amazon.")

        internal fun blocked(url: String) = BLOCKED.any { url.contains(it) }

        private const val LONG_SITE = 3
        private val SECOND_LEVEL = setOf("co", "com", "org", "net", "gov", "ac", "edu")

        /** The site behind [url]: `de.wikipedia.org` → `wikipedia.org`, `www.bbc.co.uk` → `bbc.co.uk`. */
        fun siteOf(url: String): String {
            val parts = PageReader.hostOf(url).lowercase().split('.')
            val keep = if (parts.size > 2 && parts[parts.size - 2] in SECOND_LEVEL) LONG_SITE else 2
            return parts.takeLast(keep).joinToString(".")
        }

        /** Keeps the first hit of each site, in order, so the pages read are independent sources. */
        fun onePerSite(hits: List<SearchHit>): List<SearchHit> {
            val unique = hits.distinctBy { it.url }
            return unique.distinctBy { siteOf(it.url) }
        }

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
