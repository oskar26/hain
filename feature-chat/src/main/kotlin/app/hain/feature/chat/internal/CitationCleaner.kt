package app.hain.feature.chat.internal

/**
 * Cleans the citations of an answer (T-220). The small brain sometimes writes "[1]" and a source list
 * although no tool ran in this answer, talks about tools ("hier kein Werkzeug nötig") or starts with a
 * label like "**Fakt:**". This runs on the live text and on the finished answer, so it works even when
 * the brain ignores the prompt.
 *
 * - No sources ([sourceCount] = 0): drops every marker ("[1]", "[2, 3]", "【1】"), source lines
 *   ("[1] Wikipedia …") and blocks ("Quellen:" + list), and sentences saying no tool was needed.
 * - With sources: keeps the markers whose number exists, drops the others (and source lines whose
 *   number does not exist).
 * - Always: drops a "Fakt:" / "Fact:" label at the start of a line.
 *
 * Code blocks, inline code and markdown links are never touched. Text without any change comes back
 * as it was.
 */
@Suppress("TooManyFunctions") // one small function per kind of cleanup
object CitationCleaner {
    fun clean(
        text: String,
        sourceCount: Int,
    ): String {
        if (!NEEDS_WORK.containsMatchIn(text)) return text
        val state = State(sourceCount.coerceAtLeast(0))
        val out = mutableListOf<Line>()
        text.split('\n').forEach { line -> state.process(line)?.let { out += it } }
        return if (state.changed) join(out) else text
    }

    /** One line of the result; [prose] = outside a code block (blank prose lines may be merged). */
    private class Line(
        val text: String,
        val prose: Boolean,
    )

    private class State(
        val sources: Int,
    ) {
        var inCode = false
        var inSourceBlock = false
        var changed = false

        /** The cleaned line, or `null` when it is dropped. */
        fun process(line: String): Line? =
            when {
                FENCE.containsMatchIn(line) -> {
                    inCode = !inCode
                    inSourceBlock = false
                    Line(line, prose = false)
                }
                inCode -> Line(line, prose = false)
                else -> prose(line)
            }

        private fun prose(line: String): Line? {
            val cleaned = if (dropLine(line)) null else cleanProse(line)
            if (cleaned != line) changed = true
            return cleaned?.let { Line(it, prose = true) }
        }

        private fun dropLine(line: String): Boolean {
            if (sources == 0 && inSourceBlock && line.isNotBlank()) {
                if (LIST_ITEM.containsMatchIn(line) || REF_LINE.containsMatchIn(line)) return true
                inSourceBlock = false
            }
            val ref = REF_LINE.find(line)
            return when {
                sources == 0 && SOURCE_HEADING.matches(line) -> true.also { inSourceBlock = true }
                ref == null -> false
                else -> numbersOf(ref.groupValues[1].ifEmpty { ref.groupValues[2] }).none { it in 1..sources }
            }
        }

        private fun cleanProse(line: String): String? {
            var result = stripLabel(line)
            result = mapUnprotected(result) { chunk -> MARKER.replace(chunk) { keepValid(it.value) } }
            if (sources == 0) result = dropToolTalk(result)
            if (result != line) result = tidy(result)
            return if (result.isBlank() && line.isNotBlank()) null else result
        }

        /** A run of markers like " [1][2, 7]" → only the numbers that exist, or nothing. */
        private fun keepValid(run: String): String {
            val kept =
                BRACKET.findAll(run).mapNotNull { m ->
                    val numbers = numbersOf(m.groupValues[1].ifEmpty { m.groupValues[2] })
                    val valid = numbers.filter { it in 1..sources }
                    when {
                        valid.isEmpty() -> null
                        valid.size == numbers.size -> m.value
                        else -> valid.joinToString(", ", "[", "]")
                    }
                }
            val joined = kept.joinToString("")
            return if (joined.isEmpty()) "" else run.takeWhile { it == ' ' || it == '\t' } + joined
        }
    }

    /** "1", "2, 3", "1-3" → the numbers in it (a range is spread out, at most [MAX_RANGE] long). */
    internal fun numbersOf(inside: String): List<Int> =
        inside.split(',', ';').flatMap { part ->
            val ends = part.split('-', '–').mapNotNull { it.trim().toIntOrNull() }
            when {
                ends.size == 2 && ends[1] >= ends[0] && ends[1] - ends[0] < MAX_RANGE -> (ends[0]..ends[1]).toList()
                ends.size == 2 -> ends
                else -> ends.take(1)
            }
        }

    private fun stripLabel(line: String): String = LABEL.replaceFirst(line, "$1")

    /** Applies [block] only to the parts of [line] that are not inline code or a markdown link. */
    private fun mapUnprotected(
        line: String,
        block: (String) -> String,
    ): String {
        val out = StringBuilder()
        var at = 0
        PROTECTED.findAll(line).forEach { m ->
            out.append(block(line.substring(at, m.range.first))).append(m.value)
            at = m.range.last + 1
        }
        return out.append(block(line.substring(at))).toString()
    }

    /** Drops sentences and brackets that say no tool was used or needed ("kein Werkzeug nötig …"). */
    private fun dropToolTalk(line: String): String {
        if (!TOOL_WORD.containsMatchIn(line)) return line
        val noBrackets = PAREN.replace(line) { if (isToolTalk(it.value)) "" else it.value }
        val indent = noBrackets.takeWhile { it == ' ' || it == '\t' }
        val sentences = SENTENCE_END.split(noBrackets.substring(indent.length))
        val kept = sentences.filterNot { isToolTalk(it) }
        return if (kept.size == sentences.size) noBrackets else indent + kept.joinToString(" ")
    }

    private fun isToolTalk(sentence: String): Boolean {
        val plain = !sentence.contains('`') && !sentence.contains("](")
        return plain && listOf(TOOL_WORD, NEGATION, NEED, META).all { it.containsMatchIn(sentence) }
    }

    /** Removes double spaces, spaces before punctuation and trailing spaces left by a cleanup. */
    private fun tidy(line: String): String {
        val indent = line.takeWhile { it == ' ' || it == '\t' }
        val body =
            mapUnprotected(line.substring(indent.length)) {
                it.replace(SPACES, " ").replace(SPACE_BEFORE_PUNCT, "$1")
            }.trimEnd()
        return indent + body
    }

    /** Joins the lines; blank lines left by dropped ones are merged, blank lines at both ends removed. */
    private fun join(lines: List<Line>): String {
        val out = mutableListOf<Line>()
        lines.forEach { line ->
            val blank = line.prose && line.text.isBlank()
            val lastBlank = out.lastOrNull()?.let { it.prose && it.text.isBlank() } ?: true
            if (!(blank && lastBlank)) out += line
        }
        while (out.lastOrNull()?.let { it.prose && it.text.isBlank() } == true) out.removeAt(out.lastIndex)
        return out.joinToString("\n") { it.text }
    }

    private const val MAX_RANGE = 20

    /** Quick check: nothing of what could be cleaned is in the text. */
    private val NEEDS_WORK =
        Regex("""[\[【]|Fakt|Fact|Werkzeug|Tool|Suche|Search|Quelle|Source|Referen""", RegexOption.IGNORE_CASE)
    private val FENCE = Regex("""^\s{0,3}(```|~~~)""")

    /** Inline code and markdown links / images: never changed. */
    private val PROTECTED = Regex("""`[^`\n]*`|!?\[[^\]\n]*]\([^)\n]*\)""")

    /** A run of citation markers with the spaces before it: "[1]", " [2, 3][4]", "【1†source】". */
    private val MARKER =
        Regex("""(?<!])[ \t]*(?:\[\d{1,2}(?:\s*[,;–-]\s*\d{1,2})*]|【\d{1,2}(?:†[^】\n]*)?】)+(?!\()""")
    private val BRACKET = Regex("""\[(\d{1,2}(?:\s*[,;–-]\s*\d{1,2})*)]|【(\d{1,2})(?:†[^】\n]*)?】""")

    /** A source line: "[1] Wikipedia …", "- [2]: …", "【3】 …". */
    private val REF_LINE = Regex("""^\s*(?:[-*•]\s+)?(?:\[(\d{1,2})]|【(\d{1,2})】)""")

    /** "Quellen:", "**Sources:**", "### Quellen", "Quelle: Wikipedia". */
    private val SOURCE_HEADING =
        Regex(
            """^\s*(?:#{1,6}\s*)?(?:\*\*|__)?\s*(?:Quellen(?:angaben)?|Quelle|Sources?|References?|Referenzen)""" +
                """\s*(?:(?:\*\*|__)?\s*:.*|(?:\*\*|__)?\s*)$""",
            RegexOption.IGNORE_CASE,
        )
    private val LIST_ITEM = Regex("""^\s*(?:[-*•]|\d{1,2}[.)])\s+""")

    /** "**Fakt:** ", "Fakt: ", "**Fact**: " at the start of a line (indentation and list mark kept). */
    private val LABEL =
        Regex("""^(\s*(?:[-*•]\s+)?)(?:\*\*|__)?(?:Fakt|Fakten|Fact|Facts)\s*(?:\*\*|__)?\s*:\s*(?:\*\*|__)?[ \t]*""")

    private val TOOL_WORD =
        Regex(
            """\b(?:Werkzeug\w*|Tools?|Websuche|Internetsuche|Web-Suche|web ?search|web_search)\b""",
            RegexOption.IGNORE_CASE,
        )
    private val NEGATION =
        Regex("""\b(?:kein\w*|nicht|ohne|no|not|without|never|nie)\b|n't\b""", RegexOption.IGNORE_CASE)

    /** "needed / used": the sentence says a tool was not needed or not used … */
    private val NEED =
        Regex(
            """\b(?:nötig|notwendig|erforderlich|benötigt|gebraucht|brauch\w*|benutzt|verwendet|genutzt|""" +
                """aufgerufen|need\w*|necessary|required|used|called)\b""",
            RegexOption.IGNORE_CASE,
        )

    /** … for this question or answer (not about a real tool like a hammer). */
    private val META =
        Regex(
            """\b(?:Frage|Antwort|aktuell\w*|allgemein\w*|question|answer|current|general)\b""",
            RegexOption.IGNORE_CASE,
        )
    private val PAREN = Regex("""\s*\([^()\n]*\)""")
    private val SENTENCE_END = Regex("""(?<=[.!?])\s+""")
    private val SPACES = Regex("""[ \t]{2,}""")
    private val SPACE_BEFORE_PUNCT = Regex("""[ \t]+([.,;:!?])""")
}
