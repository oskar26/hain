package app.hain.llm.internal

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * JSON text exactly like Python's `json.dumps(value, ensure_ascii=False)`, which is what the chat
 * template's `tojson` produces in llama.cpp / transformers: separators `", "` and `": "`,
 * key order kept, non-ASCII kept as-is, control characters escaped.
 */
internal object PyJson {
    fun encode(element: JsonElement): String = StringBuilder().also { write(element, it) }.toString()

    /** Encodes plain Kotlin values (String, Boolean, Number, null, Map, List, JsonElement). */
    fun encodeValue(value: Any?): String = StringBuilder().also { writeValue(value, it) }.toString()

    private fun write(
        element: JsonElement,
        out: StringBuilder,
    ) {
        when (element) {
            is JsonNull -> out.append("null")
            is JsonPrimitive -> if (element.isString) writeString(element.content, out) else out.append(element.content)
            is JsonObject -> writeEntries(element.entries.map { it.key to it.value }, out) { v, o -> write(v, o) }
            is JsonArray -> writeItems(element, out) { v, o -> write(v, o) }
        }
    }

    private fun writeValue(
        value: Any?,
        out: StringBuilder,
    ) {
        when (value) {
            null -> out.append("null")
            is JsonElement -> write(value, out)
            is String -> writeString(value, out)
            is Boolean -> out.append(if (value) "true" else "false")
            is Number -> out.append(value.toString())
            is Map<*, *> ->
                writeEntries(value.entries.map { it.key.toString() to it.value }, out) { v, o -> writeValue(v, o) }
            is Iterable<*> -> writeItems(value.toList(), out) { v, o -> writeValue(v, o) }
            else -> writeString(value.toString(), out)
        }
    }

    private fun <T> writeEntries(
        entries: List<Pair<String, T>>,
        out: StringBuilder,
        writeChild: (T, StringBuilder) -> Unit,
    ) {
        out.append('{')
        entries.forEachIndexed { index, (key, child) ->
            if (index > 0) out.append(", ")
            writeString(key, out)
            out.append(": ")
            writeChild(child, out)
        }
        out.append('}')
    }

    private fun <T> writeItems(
        items: List<T>,
        out: StringBuilder,
        writeChild: (T, StringBuilder) -> Unit,
    ) {
        out.append('[')
        items.forEachIndexed { index, child ->
            if (index > 0) out.append(", ")
            writeChild(child, out)
        }
        out.append(']')
    }

    private const val FIRST_PRINTABLE = 0x20

    private fun writeString(
        text: String,
        out: StringBuilder,
    ) {
        out.append('"')
        for (ch in text) {
            when (ch) {
                '"' -> out.append("\\\"")
                '\\' -> out.append("\\\\")
                '\n' -> out.append("\\n")
                '\r' -> out.append("\\r")
                '\t' -> out.append("\\t")
                '\b' -> out.append("\\b")
                '\u000C' -> out.append("\\f")
                else ->
                    if (ch.code < FIRST_PRINTABLE) {
                        out.append("\\u").append(ch.code.toString(HEX).padStart(UNICODE_DIGITS, '0'))
                    } else {
                        out.append(ch)
                    }
            }
        }
        out.append('"')
    }

    private const val HEX = 16
    private const val UNICODE_DIGITS = 4
}
