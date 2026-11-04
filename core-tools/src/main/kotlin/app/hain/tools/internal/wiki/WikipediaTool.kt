package app.hain.tools.internal.wiki

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.Purpose
import app.hain.tools.api.Args
import app.hain.tools.api.Compact
import app.hain.tools.api.InfoCard
import app.hain.tools.api.OfflineWiki
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import app.hain.tools.api.ToolSource
import app.hain.tools.api.ToolSpecs
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.io.IOException
import java.net.URLEncoder

/**
 * `wikipedia` (T-065): opensearch for the best title, then the page summary, in German or English.
 * T-237: with an installed knowledge pack ([offline]) the article comes from the phone first —
 * without internet and also in offline mode; online only when the pack has nothing.
 */
class WikipediaTool(
    private val gate: NetworkGate,
    private val offline: OfflineWiki? = null,
) : Tool {
    /** Online only while no knowledge pack is installed (then it also works in offline mode). */
    override val online: Boolean get() = offline?.languages().isNullOrEmpty()
    override val schema = ToolSpecs.of("wikipedia").schema

    @Suppress("ReturnCount")
    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val topic = Args.required(args, "topic")
        val lang = (Args.string(args, "lang") ?: ctx.lang).lowercase().takeIf { it == "de" || it == "en" } ?: "en"
        val other = if (lang == "de") "en" else "de"
        // T-237: the knowledge pack first (no internet), the asked language before the other one
        val local = offlineLookUp(topic, lang) ?: offlineLookUp(topic, other)
        if (local != null) return result(local, fromPack = true)
        // 4c: the asked language first, then the other one (a German topic may only exist in English)
        val summary =
            lookUp(topic, lang)
                ?: lookUp(topic, other)
                ?: return ToolRouter.failure(
                    "not_found",
                    "Wikipedia has no page about '$topic'. Try web_search with other words.",
                )
        return result(summary, fromPack = false)
    }

    private fun result(
        summary: WikiSummary,
        fromPack: Boolean,
    ): ToolResult {
        if (summary.disambiguation) {
            return ToolRouter.failure(
                "disambiguation",
                "'${summary.title}' has several meanings on Wikipedia. Ask the user which one they mean.",
            )
        }
        val json =
            buildJsonObject {
                put("title", summary.title)
                put("extract", Compact.facts(summary.extract))
                put("url", summary.url)
            }
        val source = if (fromPack) "Wikipedia (offline): ${summary.title}" else "Wikipedia: ${summary.title}"
        return ToolResult(
            forModel = json.toString(),
            label = summary.title,
            sources = listOf(ToolSource(source, summary.url)),
            card = InfoCard(InfoCard.WIKI, json.toString()),
        )
    }

    /** T-237: the article from an installed pack; a broken pack file is simply skipped. */
    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun offlineLookUp(
        topic: String,
        lang: String,
    ): WikiSummary? =
        try {
            offline?.summary(topic, lang)
        } catch (e: Exception) {
            null
        }

    /** Best page about [topic] in [lang], or `null` when there is none or it has no text. */
    private suspend fun lookUp(
        topic: String,
        lang: String,
    ): WikiSummary? {
        val title = WikiParser.firstTitle(get(searchUrl(topic, lang))) ?: return null
        return WikiParser.summary(get(summaryUrl(title, lang)))?.takeIf { it.extract.isNotBlank() }
    }

    private suspend fun get(url: String): String {
        val response = gate.fetch(GateRequest(url, Purpose.WIKI))
        if (!response.ok) throw IOException("HTTP ${response.code}")
        return response.body
    }

    companion object {
        fun searchUrl(
            topic: String,
            lang: String,
        ) = "https://$lang.wikipedia.org/w/api.php?action=opensearch&search=${enc(topic)}" +
            "&limit=1&namespace=0&format=json"

        fun summaryUrl(
            title: String,
            lang: String,
        ) = "https://$lang.wikipedia.org/api/rest_v1/page/summary/" +
            enc(title.replace(' ', '_')).replace("+", "%20")

        private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
    }
}

/** The short summary of one page. */
data class WikiSummary(
    val title: String,
    val extract: String,
    val url: String,
    /** A page that only lists other pages ("Merkur may mean …"). */
    val disambiguation: Boolean = false,
)

/** Reads Wikipedia answers (unit tested with fixtures). */
object WikiParser {
    private const val MAX_EXTRACT = 1200

    /** Opensearch answer `["query", ["Title"], [""], ["https://…"]]` → first title. */
    @Suppress("ReturnCount")
    fun firstTitle(body: String): String? {
        val root = runCatching { Json.parseToJsonElement(body) }.getOrNull() as? JsonArray ?: return null
        val titles = root.getOrNull(1) as? JsonArray ?: return null
        return (titles.firstOrNull() as? JsonPrimitive)?.content?.takeIf { it.isNotBlank() }
    }

    @Suppress("ReturnCount")
    fun summary(body: String): WikiSummary? {
        val root = runCatching { Json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val title = root.text("title") ?: return null
        val extract = root.text("extract")?.take(MAX_EXTRACT) ?: return null
        val url =
            root["content_urls"]
                ?.jsonObject
                ?.get("desktop")
                ?.jsonObject
                ?.text("page")
                ?: return null
        return WikiSummary(title, extract, url, root.text("type") == "disambiguation")
    }

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.jsonPrimitive?.content
}
