package app.hain.docs.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long

/**
 * A document attached to a chat message (T-123): shown as a file chip, kept in the message's
 * `attachments_json`. The text itself stays in "Meine Dateien" (hain_docs.db).
 */
data class DocAttachment(
    val id: Long,
    val name: String,
    val kind: DocKind,
    val pages: Int,
    val tokens: Int,
) {
    companion object {
        fun of(info: DocInfo) = DocAttachment(info.id, info.name, info.kind, info.pages, info.tokens)

        /** JSON for `message.attachments_json`; `null` for no attachments. */
        fun encode(list: List<DocAttachment>): String? {
            if (list.isEmpty()) return null
            return JsonArray(
                list.map { a ->
                    JsonObject(
                        mapOf(
                            "id" to JsonPrimitive(a.id),
                            "name" to JsonPrimitive(a.name),
                            "kind" to JsonPrimitive(a.kind.name.lowercase()),
                            "pages" to JsonPrimitive(a.pages),
                            "tokens" to JsonPrimitive(a.tokens),
                        ),
                    )
                },
            ).toString()
        }

        /** The attachments of [json]; broken or empty JSON gives none (never a crash). */
        fun decode(json: String?): List<DocAttachment> {
            if (json.isNullOrBlank()) return emptyList()
            return runCatching {
                (Json.parseToJsonElement(json) as JsonArray).mapNotNull { e ->
                    val o = e.jsonObject
                    val kind = DocKind.entries.firstOrNull { it.name.equals(o["kind"]?.jsonPrimitive?.content, true) }
                    DocAttachment(
                        id = o.getValue("id").jsonPrimitive.long,
                        name = o.getValue("name").jsonPrimitive.content,
                        kind = kind ?: DocKind.PDF,
                        pages = o["pages"]?.jsonPrimitive?.intOrNull ?: 1,
                        tokens = o["tokens"]?.jsonPrimitive?.int ?: 0,
                    )
                }
            }.getOrDefault(emptyList())
        }
    }
}
