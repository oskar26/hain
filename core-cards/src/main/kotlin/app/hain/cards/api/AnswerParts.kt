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
