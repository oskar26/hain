package app.hain.tools.api

import app.hain.llm.api.ToolCall
import app.hain.llm.api.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

/**
 * T-271 (Oskar's device test of 0.1.10.7, 05.11.): the small brains sometimes write a tool call as
 * plain text instead of the `<tool_call>` block: `remember "Ich lerne Spanisch"`, a bare
 * `{"expression": "2.5 * 1609.34"}` or `calculate(expression="…")`. That text must never reach the
 * chat. [extract] turns such lines into real calls (only names and parameters of [ToolSchema]s the
 * round offers); [strip] removes whatever tool syntax is left, so the user never sees it. Pure, tested.
 */
@Suppress("TooManyFunctions") // one small function per call shape, plus the JSON helpers
object RawToolText {
    /** What [extract] found: the answer without the call lines, and the calls they stood for. */
    data class Result(
        val text: String,
        val calls: List<ToolCall>,
    )

    /**
     * Bare call lines of [text] with a tool name of [schemas] (anywhere), or a bare JSON object whose
     * keys fit exactly one tool (only when it opens the answer or is all of it). Code fences around a
     * call count as part of it. Without [schemas] nothing is parsed, but call lines are still removed.
     */
    fun extract(
        text: String,
        schemas: List<ToolSchema>,
    ): Result {
        if (text.isBlank()) return Result("", emptyList())
        val calls = mutableListOf<ToolCall>()
        val kept = mutableListOf<String>()
        // `<function=…>` blocks span lines: taken out of the whole text first
        val names = schemas.map { it.name }
        XML_ANY.findAll(text).forEach { m ->
            val name = m.groupValues[1]
            if (name in names) calls += ToolCall("call_${calls.size}", name, params(m.groupValues[2]))
        }
        val lines = XML_ANY.replace(text, "").lines()
        var i = 0
        var leading = true
        while (i < lines.size) {
            val block = codeBlockAt(lines, i)
            val chunk = block?.let { lines.subList(i + 1, it).joinToString("\n") } ?: lines[i]
            val next = block?.plus(1) ?: (i + 1)
            val call = callOf(chunk.trim(), schemas, calls.size, allowJson = leading || block != null)
            when {
                call != null -> calls += call
                chunk.isNotBlank() && isCallSyntax(chunk.trim(), schemas) -> Unit
                else -> {
                    lines.subList(i, next).forEach(kept::add)
                    if (chunk.isNotBlank()) leading = false
                }
            }
            i = next
        }
        return Result(kept.joinToString("\n").trim(), calls)
    }

    /**
     * Removes tool syntax that is left anywhere (tags, `<function=…>` lines, call lines with a known
     * name), for the final answer and the live text. [names] are the tools of the turn.
     */
    fun strip(
        text: String,
        names: Collection<String> = emptyList(),
    ): String {
        if (TAG.containsMatchIn(text).not() && names.none { text.contains(it) }) return text
        var t = BLOCK.replace(text, "")
        t = XML_ANY.replace(t, "")
        t = TAG_LINE.replace(t, "")
        t = TAG.replace(t, "")
        val lines =
            t.lines().filterNot { line ->
                val s = line.trim()
                s.isNotEmpty() && names.isNotEmpty() && bareName(s, names) != null
            }
        return lines.joinToString("\n").replace(BLANKS, "\n\n").trim()
    }

    /**
     * The live text while the answer streams: complete call lines at the start go, and an unfinished
     * first line that may still become one ("remem", "{\"expr", "<func") is held back until it is clear.
     */
    fun liveText(
        text: String,
        names: Collection<String>,
    ): String {
        val stripped = strip(text, names).trimStart()
        val opened = leadingCall(stripped, names)
        if (opened != null) return if (opened < 0) "" else liveText(stripped.substring(opened), names)
        val single = !stripped.contains('\n')
        return if (single && mayBecomeCall(stripped.trim(), names)) "" else stripped
    }

    /**
     * A JSON object or a code block with a call at the start of the live text: the index after it
     * (so it is skipped), -1 while it is still open, `null` when the text does not start with one.
     */
    @Suppress("ReturnCount") // one exit per shape
    private fun leadingCall(
        text: String,
        names: Collection<String>,
    ): Int? {
        if (text.startsWith("{")) {
            val end = text.indexOf('}')
            return if (end < 0) -1 else end + 1
        }
        if (!text.startsWith("```") || text.startsWith("```card")) return null
        val firstBreak = text.indexOf('\n')
        if (firstBreak < 0) return -1
        val inner = text.substring(firstBreak + 1).trimStart()
        if (inner.isEmpty()) return -1
        val looksLikeCall = inner.startsWith("{") || names.any { inner.startsWith(it) }
        if (!looksLikeCall) return null
        val close = text.indexOf("\n```", firstBreak)
        return if (close < 0) -1 else close + FENCE_CLOSE_CHARS
    }

    /** An unfinished single line that may still turn out to be a call. */
    @Suppress("ReturnCount") // one early exit per shape that does not fit
    private fun mayBecomeCall(
        line: String,
        names: Collection<String>,
    ): Boolean {
        if (line.isEmpty()) return false
        if (line.first() in HELD_STARTS || line.startsWith("```")) return true
        val word = line.takeWhile { it.isLetter() || it == '_' }
        val prefixOfName = word.isNotEmpty() && line.length <= word.length + 1 && names.any { it.startsWith(word) }
        val opening =
            names.any { name ->
                line.startsWith(name) && OPEN_TAIL.matches(line.substring(name.length).take(OPEN_TAIL_CHARS))
            }
        return prefixOfName || opening || bareName(line, names) != null
    }

    /** True when [line] is tool syntax that has no business in an answer (a call we could not read). */
    private fun isCallSyntax(
        line: String,
        schemas: List<ToolSchema>,
    ): Boolean = TAG.containsMatchIn(line) || bareName(line, schemas.map { it.name }) != null

    @Suppress("ReturnCount") // one early return per call shape
    private fun callOf(
        chunk: String,
        schemas: List<ToolSchema>,
        index: Int,
        allowJson: Boolean,
    ): ToolCall? {
        if (chunk.isEmpty() || schemas.isEmpty()) return null
        val id = "call_$index"
        val names = schemas.map { it.name }
        val unwrapped = chunk.removePrefix("[").removeSuffix("]").trim()
        if (unwrapped.startsWith("{")) return if (allowJson) jsonCall(unwrapped, schemas, id) else null
        val name = bareName(unwrapped, names) ?: return null
        val schema = schemas.first { it.name == name }
        val rest =
            unwrapped
                .substring(name.length)
                .trim()
                .removePrefix(":")
                .trim()
        val args =
            when {
                rest.startsWith("(") && rest.endsWith(")") -> pyArgs(rest.substring(1, rest.length - 1), schema)
                rest.startsWith("{") -> jsonArgs(rest)
                else -> quoted(rest)?.let { v -> firstParam(schema)?.let { mapOf(it to v) } }
            } ?: return null
        return ToolCall(id, name, args)
    }

    /** `{"name": "x", "arguments": {…}}` or bare arguments whose keys fit exactly one tool. */
    @Suppress("ReturnCount")
    private fun jsonCall(
        body: String,
        schemas: List<ToolSchema>,
        id: String,
    ): ToolCall? {
        val root = runCatching { lenient.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        val named = listOf("name", "function", "tool").firstNotNullOfOrNull { (root[it] as? JsonPrimitive)?.content }
        if (named != null) {
            val schema = schemas.firstOrNull { it.name == named } ?: return null
            val raw = listOf("arguments", "parameters", "args").firstNotNullOfOrNull { root[it] } as? JsonObject
            return ToolCall(id, schema.name, raw?.toArgs().orEmpty())
        }
        val keys = root.keys
        if (keys.isEmpty()) return null
        val fitting =
            schemas.filter { s ->
                val props = properties(s)
                props.isNotEmpty() && props.containsAll(keys) && required(s).all { it in keys }
            }
        val schema = fitting.singleOrNull() ?: return null
        return ToolCall(id, schema.name, root.toArgs())
    }

    private fun params(body: String): Map<String, Any?> {
        val args = linkedMapOf<String, Any?>()
        PARAM.findAll(body).forEach { p -> args.putIfAbsent(p.groupValues[1], value(p.groupValues[2].trim())) }
        return args
    }

    private fun jsonArgs(body: String): Map<String, Any?>? =
        runCatching { lenient.parseToJsonElement(body).jsonObject.toArgs() }.getOrNull()

    @Suppress("ReturnCount") // one early exit per shape that does not fit
    private fun pyArgs(
        inner: String,
        schema: ToolSchema,
    ): Map<String, Any?>? {
        val trimmed = inner.trim()
        if (trimmed.isEmpty()) return emptyMap()
        val named = PY_ARG.findAll(trimmed).toList()
        if (named.isNotEmpty()) {
            return named.associateTo(linkedMapOf()) { m ->
                val raw = m.groupValues[2].trim()
                m.groupValues[1] to (quoted(raw) ?: value(raw))
            }
        }
        val single = quoted(trimmed) ?: return null
        return firstParam(schema)?.let { mapOf(it to single) }
    }

    /** `"…"`, `'…'`, `„…“` or `“…”` → its content; `null` when [s] is not one quoted string. */
    @Suppress("ReturnCount") // one early exit per shape that does not fit
    private fun quoted(s: String): String? {
        val t = s.trim()
        if (t.length < 2) return null
        val pair = QUOTES.firstOrNull { (open, close) -> t.first() == open && t.last() == close } ?: return null
        val inner = t.substring(1, t.length - 1).replace("\\\"", "")
        if (pair.second in inner) return null
        return t
            .substring(1, t.length - 1)
            .replace("\\\"", "\"")
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    /** A known tool name that opens [line] as a call: `name "…"`, `name(…)`, `name: "…"`, `name {…}`. */
    private fun bareName(
        line: String,
        names: Collection<String>,
    ): String? =
        names.sortedByDescending { it.length }.firstOrNull { name ->
            line.startsWith(name) && CALL_TAIL.matches(line.substring(name.length))
        }

    private fun firstParam(schema: ToolSchema): String? {
        val required = required(schema)
        return required.firstOrNull() ?: properties(schema).firstOrNull()
    }

    private fun properties(schema: ToolSchema): Set<String> {
        val props = schema.parameters["properties"] as? JsonObject
        return props?.keys.orEmpty()
    }

    private fun required(schema: ToolSchema): List<String> =
        (schema.parameters["required"] as? JsonArray)?.mapNotNull { (it as? JsonPrimitive)?.content }.orEmpty()

    /** Index of the closing fence when [lines] opens a fenced block at [start]; `null` otherwise. */
    @Suppress("ReturnCount") // one early exit per shape that does not fit
    private fun codeBlockAt(
        lines: List<String>,
        start: Int,
    ): Int? {
        if (!FENCE.matches(lines[start].trim())) return null
        if (lines[start]
                .trim()
                .lowercase()
                .removePrefix("```")
                .trim() == "card"
        ) {
            return null
        }
        return (start + 1 until lines.size).firstOrNull { lines[it].trim() == "```" }
    }

    private fun JsonObject.toArgs(): Map<String, Any?> {
        val out = linkedMapOf<String, Any?>()
        entries.forEach { out[it.key] = it.value.plain() }
        return out
    }

    private fun JsonElement.plain(): Any? =
        when (this) {
            is JsonNull -> null
            is JsonObject -> toArgs()
            is JsonArray -> map { it.plain() }
            is JsonPrimitive -> primitive()
        }

    private fun JsonPrimitive.primitive(): Any? =
        when {
            isString -> content
            booleanOrNull != null -> booleanOrNull
            longOrNull != null -> longOrNull?.let(::narrow)
            else -> doubleOrNull ?: content
        }

    private fun narrow(n: Long): Any = if (n in Int.MIN_VALUE..Int.MAX_VALUE) n.toInt() else n

    private fun value(raw: String): Any? =
        when {
            raw == "true" -> true
            raw == "false" -> false
            raw == "null" -> null
            NUMBER.matches(raw) -> raw.toLongOrNull()?.let(::narrow) ?: raw.toDouble()
            else -> raw
        }

    private val lenient = Json { isLenient = true }
    private val HELD_STARTS = setOf('{', '<', '[')
    private val OPEN_TAIL = Regex("""\s*(:\s*)?([("'{„“»«].*)?""", RegexOption.DOT_MATCHES_ALL)
    private const val OPEN_TAIL_CHARS = 400
    private const val FENCE_CLOSE_CHARS = 4
    private val QUOTES = listOf('"' to '"', '\'' to '\'', '„' to '“', '“' to '”', '»' to '«', '«' to '»')
    private val CALL_TAIL = Regex("""\s*(:\s*)?(\(.*\)|\{.*\}|["'„“»«].*["'“”«»])\s*""", RegexOption.DOT_MATCHES_ALL)
    private val PY_ARG = Regex("""([A-Za-z_]\w*)\s*=\s*("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*'|[^,]+)""")
    private val XML_ANY =
        Regex("""<function=([A-Za-z0-9_.\-]{1,64})>(.*?)(?:</function>|$)""", RegexOption.DOT_MATCHES_ALL)
    private val PARAM =
        Regex(
            """<parameter=([A-Za-z0-9_.\-]{1,64})>(.*?)(?=</parameter>|<parameter=|</function>|$)""",
            RegexOption.DOT_MATCHES_ALL,
        )
    private val FENCE = Regex("""```[A-Za-z]*""")
    private val NUMBER = Regex("""-?\d+(\.\d+)?""")
    private val BLOCK =
        Regex(
            """(<tool_call>|<\|tool_call_start\|>).*?(</tool_call>|<\|tool_call_end\|>|$)""",
            RegexOption.DOT_MATCHES_ALL,
        )
    private val TAG_LINE = Regex("""(?m)^\s*</?(function|parameter)(=[^>]*)?>.*$""")
    private val TAG = Regex("""</?(tool_call|tool_response|function|parameter)(=[^>]*)?>|<\|tool_call_(start|end)\|>""")
    private val BLANKS = Regex("""\n{3,}""")
}
