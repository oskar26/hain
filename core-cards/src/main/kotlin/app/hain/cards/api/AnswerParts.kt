package app.hain.cards.api

/** One piece of an answer: markdown text, or a card the brain wrote in a "```card" block. */
sealed interface AnswerPart {
    data class Text(
        val markdown: String,
    ) : AnswerPart

    /** [index] counts the cards of one answer from 0 (key for the saved checklist ticks). */
    data class Card(
        val card: ModelCard,
        val index: Int,
        val complete: Boolean,
    ) : AnswerPart
}

/**
 * Splits an answer into text and cards (CARDS.md §3, T-130). Works on a streaming text: a card
 * block that is still open is parsed as far as it goes (`complete = false`), and while
 * [split]'s `streaming` is on, an open block with nothing usable yet is held back instead of
 * flashing up as code. A block that cannot be parsed stays in the text as it was (a code block).
 * Card fences inside another code block are left alone.
 */
object AnswerParts {
    private val cardFence = Regex("""^ {0,3}(```|~~~)\s*card\s*$""", RegexOption.IGNORE_CASE)
    private val anyFence = Regex("""^ {0,3}(```|~~~)""")

    fun split(
        markdown: String,
        streaming: Boolean = false,
    ): List<AnswerPart> {
        if (!markdown.contains("```") && !markdown.contains("~~~")) return textOnly(markdown)
        val splitter = Splitter(markdown.lines(), streaming)
        var i = 0
        while (i < splitter.lines.size) i = splitter.step(i)
        splitter.flushText()
        return splitter.parts
    }

    private class Splitter(
        val lines: List<String>,
        val streaming: Boolean,
    ) {
        val parts = mutableListOf<AnswerPart>()
        private val text = StringBuilder()
        private var cards = 0

        /** Handles the line at [i] (and the block it opens); returns the next line to look at. */
        fun step(i: Int): Int {
            val open = cardFence.find(lines[i])
            return when {
                open != null -> card(i, open.groupValues[1])
                streaming && i == lines.lastIndex && isFenceStart(lines[i]) -> i + 1
                else -> copyText(i)
            }
        }

        private fun card(
            start: Int,
            fence: String,
        ): Int {
            val end = (start + 1 until lines.size).firstOrNull { lines[it].trim() == fence }
            val card = CardParser.parse(lines.subList(start + 1, end ?: lines.size).joinToString("\n"))
            when {
                card != null -> {
                    flushText()
                    parts += AnswerPart.Card(card, cards++, complete = end != null)
                }
                end == null && streaming -> Unit
                else -> lines.subList(start, end?.plus(1) ?: lines.size).forEach { text.appendLine(it) }
            }
            return (end ?: lines.lastIndex) + 1
        }

        /** Copies a normal line, or a whole non-card code block, into the text; returns the next index. */
        private fun copyText(start: Int): Int {
            val fence = anyFence.find(lines[start])?.groupValues?.get(1)
            text.appendLine(lines[start])
            var i = start + 1
            var open = fence != null
            while (open && i < lines.size) {
                text.appendLine(lines[i])
                open = lines[i].trim() != fence
                i++
            }
            return i
        }

        fun flushText() {
            val chunk = text.toString().trim('\n').trimEnd()
            if (chunk.isNotBlank()) parts += AnswerPart.Text(chunk)
            text.clear()
        }
    }

    /** The answer as plain text for copy and share: cards become readable lines. */
    fun plainText(
        markdown: String,
        state: CardState = CardState(),
    ): String =
        split(markdown).joinToString("\n\n") { part ->
            when (part) {
                is AnswerPart.Text -> part.markdown
                is AnswerPart.Card -> CardText.plain(part.card, part.index, state)
            }
        }

    /**
     * T-271: a card the brain wrote without its fence (`Card title="…"` / `Chart type=…` as plain lines,
     * seen with LFM3-Omni) gets the "```card" fence, so it shows as a card instead of raw syntax. A
     * `Button label=…` glued to the end of a list line moves to its own line; "•" bullets become "-".
     */
    @Suppress("CyclomaticComplexMethod") // one pass over the lines: fences, the loose card, its lines
    fun fenceLooseCards(markdown: String): String {
        if (!LOOSE_START.containsMatchIn(markdown)) return markdown
        val lines = markdown.lines()
        val out = mutableListOf<String>()
        var inFence = false
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            if (anyFence.containsMatchIn(line)) inFence = !inFence
            if (inFence || !LOOSE_START.matches(trimmed)) {
                out += line
                i++
                continue
            }
            val body = mutableListOf<String>()
            var j = i
            while (j < lines.size) {
                val t = lines[j].trim()
                val next = lines.getOrNull(j + 1)?.trim().orEmpty()
                val keep = j == i || isCardLine(t) || (t.isEmpty() && isCardLine(next) && !LOOSE_START.matches(next))
                if (!keep) break
                if (t.isNotEmpty()) body += cardLines(t)
                j++
            }
            out += FENCE_FULL
            out += body
            out += "```"
            i = j
        }
        return out.joinToString("\n")
    }

    private fun isCardLine(t: String): Boolean {
        val bullet = t.isNotEmpty() && t.first() in BULLETS && t.getOrNull(1) == ' '
        return bullet || t.startsWith("|") || (t.isNotEmpty() && t.substringBefore(' ').lowercase() in CARD_WORDS)
    }

    /** One loose line in card syntax: bullets as "-", a glued `Button …` on a line of its own. */
    private fun cardLines(t: String): List<String> {
        val bullet = if (t.first() in BULLETS && t.first() != '-' && t.getOrNull(1) == ' ') "- " + t.drop(2) else t
        val glued = GLUED_BUTTON.find(bullet) ?: return listOf(bullet)
        return listOf(bullet.substring(0, glued.range.first).trimEnd(), bullet.substring(glued.range.first).trim())
    }

    private val LOOSE_START = Regex("""(?m)^\s*(?:Card|Karte)(?:\s+title=.*)?$|^\s*(?:Chart|Diagramm)\s+type=.*$""")
    private val GLUED_BUTTON = Regex("""\s(?:Button|Knopf)\s+label=""")
    private val BULLETS = setOf('-', '•', '*', '▪', '·')
    private val CARD_WORDS =
        setOf(
            "card",
            "karte",
            "list",
            "liste",
            "checklist",
            "checkliste",
            "todo",
            "todos",
            "table",
            "tabelle",
            "progress",
            "fortschritt",
            "button",
            "knopf",
            "chart",
            "diagramm",
            "grafik",
        )

    /** True when [markdown] holds at least one card the app can show. */
    fun hasCards(markdown: String): Boolean = split(markdown).any { it is AnswerPart.Card }

    /** "```c", "```car" … at the very end of a streaming text: the start of a card fence, not yet shown. */
    private fun isFenceStart(line: String): Boolean {
        val t = line.trim().lowercase()
        return t.length in FENCE_MIN..FENCE_FULL.length && FENCE_FULL.startsWith(t)
    }

    private fun textOnly(markdown: String): List<AnswerPart> {
        if (markdown.isBlank()) return emptyList()
        return listOf(AnswerPart.Text(markdown))
    }

    private const val FENCE_FULL = "```card"
    private const val FENCE_MIN = 3
}
