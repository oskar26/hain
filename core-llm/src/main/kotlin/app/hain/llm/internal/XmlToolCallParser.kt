package app.hain.llm.internal

import app.hain.llm.api.ParsedOutput
import app.hain.llm.api.ToolCall
import app.hain.llm.api.ToolCallParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Strict parser for LFM3 output (LLM.md §3, §5).
 *
 * Model output is untrusted: on any malformed `<tool_call>` block the result has no tool calls,
 * an error, and the whole visible answer as [ParsedOutput.text], so nothing gets executed.
 */
class XmlToolCallParser : ToolCallParser {
    override fun parse(output: String): ParsedOutput {
        val (reasoning, answer) = splitThinking(output)
        val errors = mutableListOf<String>()
        val calls = mutableListOf<ToolCall>()
        val text = StringBuilder()
        var cursor = 0
        while (cursor < answer.length && errors.isEmpty()) {
            val open = answer.indexOf(CALL_OPEN, cursor).let { if (it < 0) answer.length else it }
            text.append(answer, cursor, open)
            cursor = open
            if (open < answer.length) {
                val close = answer.indexOf(CALL_CLOSE, open + CALL_OPEN.length)
                if (close < 0) {
                    errors += "missing $CALL_CLOSE"
                } else {
                    parseCall(answer.substring(open + CALL_OPEN.length, close), calls.size)
                        .onSuccess { calls += it }
                        .onFailure { errors += it.message.orEmpty() }
                    cursor = close + CALL_CLOSE.length
                }
            }
        }
        if (CALL_CLOSE in text) errors += "stray $CALL_CLOSE"
        return if (errors.isEmpty()) {
            ParsedOutput(reasoning, text.toString().trim(), calls)
        } else {
            ParsedOutput(reasoning, answer.trim(), emptyList(), errors)
        }
    }

    private fun splitThinking(output: String): Pair<String?, String> {
        val end = output.indexOf(THINK_CLOSE)
        if (end < 0) return null to output
        val head = output.substring(0, end)
        val reasoning = if (head.trimStart().startsWith(THINK_OPEN)) head.trimStart().removePrefix(THINK_OPEN) else head
        return reasoning.trim() to output.substring(end + THINK_CLOSE.length)
    }

    private fun parseCall(
        body: String,
        index: Int,
    ): Result<ToolCall> =
        runCatching {
            val inner = body.trim()
            val header =
                requireNotNull(FUNCTION_HEADER.matchAt(inner, 0)) { "call ${index + 1}: missing <function=name>" }
            require(inner.endsWith(FUNCTION_CLOSE)) { "call ${index + 1}: missing $FUNCTION_CLOSE" }
            val params = inner.substring(header.range.last + 1, inner.length - FUNCTION_CLOSE.length)
            ToolCall(id = "call_$index", name = header.groupValues[1], arguments = parseParams(params, index))
        }

    private fun parseParams(
        params: String,
        index: Int,
    ): Map<String, Any?> {
        val arguments = linkedMapOf<String, Any?>()
        var rest = params.trimStart()
        while (rest.isNotEmpty()) {
            val param = requireNotNull(PARAM_HEADER.matchAt(rest, 0)) { "call ${index + 1}: unexpected text" }
            val valueStart = param.range.last + 1
            val valueEnd = rest.indexOf(PARAM_CLOSE, valueStart)
            require(valueEnd >= 0) { "call ${index + 1}: missing $PARAM_CLOSE" }
            val key = param.groupValues[1]
            require(key !in arguments) { "call ${index + 1}: duplicate parameter $key" }
            arguments[key] = toValue(rest.substring(valueStart, valueEnd).removePrefix("\n").removeSuffix("\n"))
            rest = rest.substring(valueEnd + PARAM_CLOSE.length).trimStart()
        }
        return arguments
    }

    companion object {
        private const val THINK_OPEN = "<think>"
        private const val THINK_CLOSE = "</think>"
        private const val CALL_OPEN = "<tool_call>"
        private const val CALL_CLOSE = "</tool_call>"
        private const val FUNCTION_CLOSE = "</function>"
        private const val PARAM_CLOSE = "</parameter>"
        private val FUNCTION_HEADER = Regex("""<function=([A-Za-z0-9_.\-]{1,64})>""")
        private val PARAM_HEADER = Regex("""<parameter=([A-Za-z0-9_.\-]{1,64})>""")
        private val JSON_NUMBER = Regex("""-?(0|[1-9]\d*)(\.\d+)?([eE][+-]?\d+)?""")
        private val JSON_INTEGER = Regex("""-?(0|[1-9]\d*)""")
        private val json = Json { isLenient = false }

        /**
         * Parameter text → value. Only real JSON (number, true/false/null, object, array, quoted
         * string) is converted; everything else, e.g. `Köln` or `01234`, stays the raw String.
         */
        internal fun toValue(raw: String): Any? {
            val text = raw.trim()
            val element = if (looksLikeJson(text)) runCatching { json.parseToJsonElement(text) }.getOrNull() else null
            return if (element == null) raw else element.toKotlin()
        }

        private fun looksLikeJson(text: String): Boolean =
            text == "true" ||
                text == "false" ||
                text == "null" ||
                JSON_NUMBER.matches(text) ||
                (text.length >= 2 && text.first() to text.last() in JSON_BRACKETS)

        private val JSON_BRACKETS = setOf('{' to '}', '[' to ']', '"' to '"')

        private fun JsonElement.toKotlin(): Any? =
            when (this) {
                is JsonNull -> null
                is JsonObject -> entries.associateTo(linkedMapOf()) { it.key to it.value.toKotlin() }
                is JsonArray -> map { it.toKotlin() }
                is JsonPrimitive -> primitiveValue()
            }

        private fun JsonPrimitive.primitiveValue(): Any? {
            val long = if (!isString && JSON_INTEGER.matches(content)) longOrNull else null
            return when {
                isString -> content
                booleanOrNull != null -> booleanOrNull
                long != null -> if (long in Int.MIN_VALUE..Int.MAX_VALUE) long.toInt() else long
                else -> doubleOrNull ?: content
            }
        }
    }
}
