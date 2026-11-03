package app.hain.tools.internal.search

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.net.URI
import java.net.URLDecoder

/** One search hit. */
data class SearchHit(
    val title: String,
    val url: String,
    val snippet: String,
)

/** Reads the answers of the key-free search services (T-066), unit tested with fixtures. */
object SearchParsers {
    private val DDG_LINK =
        Regex(
            "class=\"result__a\"[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    private val DDG_SNIPPET =
        Regex(
            "class=\"result__snippet\"[^>]*>(.*?)</a>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    private val LITE_LINK =
        Regex(
            "<a[^>]*href=\"([^\"]+)\"[^>]*class=['\"]result-link['\"][^>]*>(.*?)</a>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    private val LITE_SNIPPET =
        Regex(
            "class=['\"]result-snippet['\"][^>]*>(.*?)</td>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    private val TAG = Regex("<[^>]+>")
    private val SPACES = Regex("\\s+")
    private const val MAX_SNIPPET = 240

    /** DuckDuckGo's HTML page (`html.duckduckgo.com/html/?q=`): result links with their snippets. */
    fun duckDuckGo(html: String): List<SearchHit> = ddg(html, DDG_LINK, DDG_SNIPPET)

    /** DuckDuckGo Lite (`lite.duckduckgo.com/lite/?q=`), a table; the fallback when the HTML page asks for a check. */
    fun duckDuckGoLite(html: String): List<SearchHit> = ddg(html, LITE_LINK, LITE_SNIPPET)

    private fun ddg(
        html: String,
        link: Regex,
        snippet: Regex,
    ): List<SearchHit> {
        val snippets = snippet.findAll(html).map { clean(it.groupValues[1]) }.toList()
        return link
            .findAll(html)
            .mapIndexedNotNull { i, m ->
                val url = ddgTarget(HtmlText.decode(m.groupValues[1])) ?: return@mapIndexedNotNull null
                if (url.contains("duckduckgo.com/y.js")) return@mapIndexedNotNull null // ads
                SearchHit(clean(m.groupValues[2]), url, snippets.getOrElse(i) { "" })
            }.toList()
    }

    /** `//duckduckgo.com/l/?uddg=https%3A…&rut=…` → the real address. */
    internal fun ddgTarget(href: String): String? {
        val full = if (href.startsWith("//")) "https:$href" else href
        val query = runCatching { URI(full).rawQuery }.getOrNull()
        val uddg =
            query
                ?.split('&')
                ?.firstOrNull { it.startsWith("uddg=") }
                ?.removePrefix("uddg=")
                ?.let { URLDecoder.decode(it, "UTF-8") }
        val url = uddg ?: full
        return url.takeIf { it.startsWith("https://") || it.startsWith("http://") }
    }

    /** SearXNG JSON (`/search?format=json`): `{"results":[{"title","url","content"}]}`. */
    fun searxng(body: String): List<SearchHit> {
        val root = parse(body) ?: return emptyList()
        return (root["results"] as? JsonArray).orEmpty().mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val url = o.text("url")?.takeIf { it.startsWith("http") } ?: return@mapNotNull null
            SearchHit(clean(o.text("title") ?: url), url, clean(o.text("content").orEmpty()))
        }
    }

    /** Wikipedia full-text search (`list=search`), the last fallback. */
    @Suppress("ReturnCount")
    fun wikipedia(
        body: String,
        lang: String,
    ): List<SearchHit> {
        val root = parse(body) ?: return emptyList()
        val list = (root["query"] as? JsonObject)?.get("search") as? JsonArray ?: return emptyList()
        return list.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val title = o.text("title") ?: return@mapNotNull null
            val path =
                java.net.URLEncoder
                    .encode(title.replace(' ', '_'), "UTF-8")
                    .replace("+", "%20")
            SearchHit(title, "https://$lang.wikipedia.org/wiki/$path", clean(o.text("snippet").orEmpty()))
        }
    }

    private fun parse(body: String): JsonObject? = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull()

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun clean(html: String): String =
        HtmlText
            .decode(html.replace(TAG, ""))
            .replace(SPACES, " ")
            .trim()
            .take(MAX_SNIPPET)
}
