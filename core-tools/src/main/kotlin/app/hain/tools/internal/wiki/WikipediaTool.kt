package app.hain.tools.internal.wiki

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.Purpose
import app.hain.llm.api.ToolSchema
import app.hain.tools.api.Args
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import app.hain.tools.api.ToolRouter
import app.hain.tools.api.ToolSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.URLEncoder

/** `wikipedia` (T-065): opensearch for the best title, then the page summary, in German or English. */
class WikipediaTool(
    private val gate: NetworkGate,
) : Tool {
    override val online = true
    override val schema =
        ToolSchema(
            name = "wikipedia",
            description = "Short encyclopedia summary about a person, place or thing.",
            parameters =
                buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("topic") {
                            put("type", "string")
                            put("description", "What to look up")
                        }
                        putJsonObject("lang") {
                            put("type", "string")
                            put("description", "de or en")
                        }
                    }
                    putJsonArray("required") { add(JsonPrimitive("topic")) }
                },
        )

    @Suppress("ReturnCount")
    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val topic = Args.required(args, "topic")
        val lang = (Args.string(args, "lang") ?: ctx.lang).lowercase().takeIf { it == "de" || it == "en" } ?: "en"
        // 4c: the asked language first, then the other one (a German topic may only exist in English)
        val summary =
            lookUp(topic, lang)
                ?: lookUp(topic, if (lang == "de") "en" else "de")
                ?: return ToolRouter.failure("not_found", "Wikipedia has no page about '$topic'.")
        if (summary.disambiguation) {
            return ToolRouter.failure("disambiguation", "'$topic' has several meanings on Wikipedia.")
        }
        val json =
            buildJsonObject {
                put("title", summary.title)
                put("extract", summary.extract)
                put("url", summary.url)
            }
        return ToolResult(
            forModel = json.toString(),
            label = summary.title,
            sources = listOf(ToolSource("Wikipedia: ${summary.title}", summary.url)),
        )
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
