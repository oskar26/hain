package app.hain.ui.components

/**
 * T-274: the "[1]" markers of an answer. In the chat they become small raised chips (the markdown gets
 * a link `[1](hain-source:1)` per number, drawn by [AssistantBlock]); for copy, share and read aloud
 * they are removed. Code blocks, inline code and existing markdown links are never touched. Pure, tested.
 */
object CitationMarks {
    /** Link scheme of a chip; the number follows the colon. */
    const val SCHEME = "hain-source:"

    /** Every marker whose numbers are 1..[count] becomes one link per number; others stay as they are. */
    fun toLinks(
        markdown: String,
        count: Int,
    ): String {
        if (count <= 0 || !markdown.contains('[')) return markdown
        return mapProse(markdown) { chunk ->
            MARKER.replace(chunk) { run ->
                val numbers = BRACKET.findAll(run.value).flatMap { numbersOf(it.groupValues[1]) }.toList()
                if (numbers.isEmpty() || numbers.any { it !in 1..count }) {
                    run.value
                } else {
                    numbers.distinct().joinToString("") { "[$it]($SCHEME$it)" }
                }
            }
        }
    }

    /** The text without any "[1]" / "[2, 3]" marker (and without the space before it). */
    fun strip(text: String): String {
        if (!text.contains('[')) return text
        return mapProse(text) { chunk -> MARKER.replace(chunk, "") }
    }

    /** The number of a chip link, or `null` for any other link. */
    fun numberOf(destination: String): Int? {
        if (!destination.startsWith(SCHEME)) return null
        return destination.removePrefix(SCHEME).toIntOrNull()
    }

    private fun numbersOf(inside: String): List<Int> =
        inside.split(',', ';').flatMap { part ->
            val ends = part.split('-', '–').mapNotNull { it.trim().toIntOrNull() }
            if (ends.size == 2 && ends[1] >= ends[0] && ends[1] - ends[0] < MAX_RANGE) {
                (ends[0]..ends[1]).toList()
            } else {
                ends
            }
        }

    /** Applies [block] to the prose of [text]: not inside ``` blocks, inline code or markdown links. */
    private fun mapProse(
        text: String,
        block: (String) -> String,
    ): String {
        var inCode = false
        return text.split('\n').joinToString("\n") { line ->
            when {
                FENCE.containsMatchIn(line) -> line.also { inCode = !inCode }
                inCode -> line
                else -> mapUnprotected(line, block)
            }
        }
    }

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

    private const val MAX_RANGE = 20
    private val FENCE = Regex("""^\s{0,3}(```|~~~)""")
    private val PROTECTED = Regex("""`[^`\n]*`|!?\[[^\]\n]*]\([^)\n]*\)""")

    /** A run of markers with the spaces before it: "[1]", " [2, 3][4]" (never "[x](…)" or "[[1]]"). */
    private val MARKER = Regex("""(?<![\]\[])[ \t]*(?:\[\d{1,2}(?:\s*[,;–-]\s*\d{1,2})*])+(?![(\]])""")
    private val BRACKET = Regex("""\[(\d{1,2}(?:\s*[,;–-]\s*\d{1,2})*)]""")
}
