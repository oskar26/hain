package app.hain.feature.chat.internal

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/** Tool steps and sources of an answer as one JSON text in `message.sources_json` (no schema change). */
internal object ToolStepsJson {
    fun encode(
        steps: List<ToolStep>,
        sources: List<SourceRef>,
    ): String? {
        if (steps.isEmpty() && sources.isEmpty()) return null
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
        }.toString()
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
                ToolStep(name, o.text("detail"), state, online, o.text("error"))
            }
        val sources =
            (root["sources"] as? JsonArray).orEmpty().mapNotNull { el ->
                val o = el as? JsonObject ?: return@mapNotNull null
                SourceRef(o.text("title") ?: return@mapNotNull null, o.text("url") ?: return@mapNotNull null)
            }
        return steps to sources
    }

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
}
