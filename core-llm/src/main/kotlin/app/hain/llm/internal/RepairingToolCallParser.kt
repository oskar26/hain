package app.hain.llm.internal

import app.hain.llm.api.ParsedOutput
import app.hain.llm.api.ToolCall
import app.hain.llm.api.ToolCallParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

/**
 * The strict [XmlToolCallParser] first; only when it rejects the output, a careful repair of what
 * the small brain typically breaks (4d, T-183). Before 4d one broken block threw away every call
 * and showed the raw `<tool_call>` text in the chat.
 *
 * Repaired: a missing `</tool_call>` at the end, a missing `</function>` or `</parameter>`, a JSON
 * body (`{"name": "weather", "arguments": {"place": "Köln"}}`), a Python-like call
 * (`weather(place="Köln")`) and LFM's own `<|tool_call_start|>[…]<|tool_call_end|>` tags. A block
 * that still cannot be read is dropped; the others run. Names are checked like the strict parser
 * does (`[A-Za-z0-9_.-]`), so nothing odd reaches the tools, and they still go through the
 * ToolRouter and, for phone actions, the confirm card.
 */
class RepairingToolCallParser(
    private val strict: XmlToolCallParser = XmlToolCallParser(),
) : ToolCallParser {
    override fun parse(output: String): ParsedOutput {
        val first = strict.parse(output)
        if (first.errors.isEmpty()) return first
        val answer = output.substringAfterLast(THINK_CLOSE, output)
        val repaired = repair(answer)
        if (repaired.calls.isEmpty()) return first
        return ParsedOutput(first.reasoning, repaired.text, repaired.calls)
    }

    /** Calls found in [answer] and the text around them. */
    data class Repaired(
        val text: String,
        val calls: List<ToolCall>,
    )

    companion object {
        private const val THINK_CLOSE = "</think>"
        private const val CALL_OPEN = "<tool_call>"
        private const val CALL_CLOSE = "</tool_call>"
        private const val LFM_OPEN = "<|tool_call_start|>"
        private const val LFM_CLOSE = "<|tool_call_end|>"
        private val NAME = Regex("""[A-Za-z0-9_.\-]{1,64}""")
        private val BLOCK = Regex("""<tool_call>(.*?)</tool_call>""", RegexOption.DOT_MATCHES_ALL)
        private val FUNCTION = Regex("""^<function=([A-Za-z0-9_.\-]{1,64})>""")
        private val PARAM =
            Regex("""<parameter=([A-Za-z0-9_.\-]{1,64})>(.*?)(?=</parameter>|<parameter=|$)""", RegexOption.DOT_MATCHES_ALL)
        private val PY_CALL = Regex("""^([A-Za-z_][A-Za-z0-9_.\-]{0,63})\((.*)\)$""", RegexOption.DOT_MATCHES_ALL)
        private val PY_ARG = Regex("""([A-Za-z_]\w*)\s*=\s*("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'|[^,]+)""")
        private val lenientJson = Json { isLenient = true }

        fun repair(answer: String): Repaired {
            var text = answer.replace(LFM_OPEN, CALL_OPEN).replace(LFM_CLOSE, CALL_CLOSE)
            val opens = Regex(Regex.escape(CALL_OPEN)).findAll(text).count()
            val closes = Regex(Regex.escape(CALL_CLOSE)).findAll(text).count()
            if (opens == closes + 1) text = text.trimEnd() + "\n" + CALL_CLOSE
            val calls = mutableListOf<ToolCall>()
            BLOCK.findAll(text).forEach { m -> block(m.groupValues[1].trim(), calls.size)?.let { calls += it } }
            val rest = BLOCK.replace(text, "").replace(CALL_CLOSE, "").trim()
            return Repaired(rest, calls)
        }

        private fun block(
            body: String,
            index: Int,
        ): ToolCall? {
            val id = "call_$index"
            FUNCTION.find(body)?.let { header ->
                val inner = body.substring(header.range.last + 1).substringBefore("</function>")
                val args = linkedMapOf<String, Any?>()
                PARAM.findAll(inner).forEach { p ->
                    val key = p.groupValues[1]
                    if (key !in args) args[key] = XmlToolCallParser.toValue(p.groupValues[2].trim('\n', ' '))
                }
                return ToolCall(id, header.groupValues[1], args)
            }
            if (body.startsWith("{")) return json(body, id)
            val py =
                PY_CALL.find(
                    body
                        .trim()
                        .removePrefix("[")
                        .removeSuffix("]")
                        .trim(),
                ) ?: return null
            val args = linkedMapOf<String, Any?>()
            PY_ARG.findAll(py.groupValues[2]).forEach { a ->
                val raw = a.groupValues[2].trim()
                val unquoted = raw.removeSurrounding("\"").removeSurrounding("'")
                args[a.groupValues[1]] = if (unquoted != raw) unquoted else XmlToolCallParser.toValue(raw)
            }
            return ToolCall(id, py.groupValues[1], args)
        }

        @Suppress("ReturnCount")
        private fun json(
            body: String,
            id: String,
        ): ToolCall? {
            val root = runCatching { lenientJson.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
            val name =
                listOf("name", "function", "tool")
                    .firstNotNullOfOrNull { (root[it] as? JsonPrimitive)?.content }
                    ?.takeIf { NAME.matches(it) } ?: return null
            val rawArgs = listOf("arguments", "parameters", "args").firstNotNullOfOrNull { root[it] }
            val args =
                when (rawArgs) {
                    is JsonObject -> rawArgs
                    is JsonPrimitive -> runCatching { lenientJson.parseToJsonElement(rawArgs.content).jsonObject }.getOrNull()
                    else -> null
                } ?: JsonObject(emptyMap())
            val values = args.mapValuesTo(linkedMapOf()) { (_, v) -> XmlToolCallParser.toValue(plain(v.toString())) }
            return ToolCall(id, name, values)
        }

        /** A JSON string value without its quotes; numbers and objects stay as they are. */
        private fun plain(json: String): String =
            if (json.length >= 2 && json.first() == '"' && json.last() == '"') {
                runCatching { Json.parseToJsonElement(json) }
                    .map { (it as JsonPrimitive).content }
                    .getOrDefault(json.substring(1, json.length - 1))
            } else {
                json
            }
    }
}
