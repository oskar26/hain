package app.hain.feature.chat.internal

import app.hain.tools.api.NewsHeadline
import app.hain.tools.api.PhoneAction
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * Tool steps, sources and phone-action cards of an answer as one JSON text in `message.sources_json`
 * (no schema change).
 */
internal object ToolStepsJson {
    fun encode(
        steps: List<ToolStep>,
        sources: List<SourceRef>,
        actions: List<ActionCard> = emptyList(),
        news: List<NewsHeadline> = emptyList(),
    ): String? {
        if (listOf(steps, sources, actions, news).all { it.isEmpty() }) return null
        return buildJsonObject {
            putJsonArray("steps") {
                steps.forEach { s ->
                    add(
                        buildJsonObject {
                            put("name", s.name)
                            s.detail?.let { put("detail", it) }
                            put("state", s.state.name)
                            put("online", s.online)
                            s.error?.let { put("error", it) }
                            s.via?.let { put("via", it) }
                        },
                    )
                }
            }
            putJsonArray("sources") {
                sources.forEach { s ->
                    add(
                        buildJsonObject {
                            put("title", s.title)
                            put("url", s.url)
                        },
                    )
                }
            }
            if (actions.isNotEmpty()) {
                putJsonArray("actions") {
                    actions.forEach { a ->
                        add(JsonObject(PhoneAction.toJson(a.action) + ("state" to JsonPrimitive(a.state.name))))
                    }
                }
            }
            if (news.isNotEmpty()) putJsonArray("news") { news.forEach { add(newsJson(it)) } }
        }.toString()
    }

    private fun newsJson(h: NewsHeadline): JsonObject =
        buildJsonObject {
            put("source", h.source)
            put("title", h.title)
            h.url?.let { put("url", it) }
            h.publishedAt?.let { put("at", it) }
            if (h.paywall) put("paywall", true)
        }

    /** The headlines of a NewsCard in [json] (T-072). */
    fun decodeNews(json: String?): List<NewsHeadline> {
        val root =
            json
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() }
                ?: return emptyList()
        return (root["news"] as? JsonArray).orEmpty().mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val title = o.text("title") ?: return@mapNotNull null
            val at = (o["at"] as? JsonPrimitive)?.longOrNull
            val paywall = (o["paywall"] as? JsonPrimitive)?.booleanOrNull == true
            NewsHeadline(o.text("source").orEmpty(), title, o.text("url"), at, paywall)
        }
    }

    /** The phone-action cards in [json] (T-068); unknown ones are skipped. */
    fun decodeActions(json: String?): List<ActionCard> {
        val root =
            json
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() }
                ?: return emptyList()
        return (root["actions"] as? JsonArray).orEmpty().mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val action = PhoneAction.fromJson(o) ?: return@mapNotNull null
            val state = ActionState.entries.firstOrNull { it.name == o.text("state") } ?: ActionState.PENDING
            ActionCard(action, state)
        }
    }

    fun decode(json: String?): Pair<List<ToolStep>, List<SourceRef>> {
        val root =
            json
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { Json.parseToJsonElement(it).jsonObject }.getOrNull() }
                ?: return emptyList<ToolStep>() to emptyList()
        val steps =
            (root["steps"] as? JsonArray).orEmpty().mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                val name = o.text("name") ?: return@mapNotNull null
                val state = StepState.entries.firstOrNull { it.name == o.text("state") } ?: StepState.DONE
                val online = (o["online"] as? JsonPrimitive)?.booleanOrNull == true
                ToolStep(name, o.text("detail"), state, online, o.text("error"), o.text("via"))
            }
        val sources =
            (root["sources"] as? JsonArray).orEmpty().mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                SourceRef(o.text("title") ?: return@mapNotNull null, o.text("url") ?: return@mapNotNull null)
            }
        return steps to sources
    }

    /** Tool calls of an answer with short results, in `message.tool_calls_json` (T-062 fix). */
    fun encodeTrace(trace: List<ToolTrace>): String? {
        if (trace.isEmpty()) return null
        return JsonArray(
            trace.map { t ->
                buildJsonObject {
                    put("name", t.name)
                    put("args", JsonObject(t.args.mapValues { JsonPrimitive(it.value) }))
                    put("result", PromptHistory.cut(t.result, PromptHistory.STORED_RESULT_CHARS))
                }
            },
        ).toString()
    }

    fun decodeTrace(json: String?): List<ToolTrace> {
        val root =
            json
                ?.takeIf { it.isNotBlank() }
                ?.let { runCatching { Json.parseToJsonElement(it) }.getOrNull() } as? JsonArray
                ?: return emptyList()
        return root.mapNotNull { el ->
            val o = el as? JsonObject ?: return@mapNotNull null
            val name = o.text("name") ?: return@mapNotNull null
            val args =
                (o["args"] as? JsonObject)
                    .orEmpty()
                    .mapNotNull { (k, v) -> (v as? JsonPrimitive)?.content?.let { k to it } }
                    .toMap()
            ToolTrace(name, args, o.text("result").orEmpty())
        }
    }

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
}
