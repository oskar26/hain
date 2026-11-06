package app.hain.tools.api

/**
 * T-278: thinking that ended up in the answer without its tags (LFM3 and LFM3-Omni, Oskar's device
 * test: "We need to answer … The user asks …"). Leading paragraphs that talk about the task instead
 * of answering it go; a stray `</think>` keeps only what follows it. An answer that is only such
 * thinking becomes empty, so the chat asks the brain once more instead of showing it.
 *
 * Strict on purpose: a strong opening ("We need to", "The user asks", "Okay, so the user") is
 * enough; a soft one ("Let me", "I should") only together with words about the task (the user,
 * the question, a search, a tool). "Let me explain:" in a real English answer stays.
 */
object ThinkLeak {
    /** [text] without leaked thinking at its start. */
    fun strip(text: String): String {
        val t = untagged(text)
        val paragraphs = PARAGRAPH.split(t)
        val first = paragraphs.indexOfFirst { !isThinking(it) }
        return when {
            first == 0 -> t.trim()
            first < 0 -> ""
            else -> paragraphs.drop(first).joinToString("\n\n").trim()
        }
    }

    /**
     * The live text: thinking paragraphs at the start are held back; a first paragraph that may
     * still turn out to be thinking ("We need", "The user") is held until it is clear.
     */
    fun live(text: String): String {
        val t = untagged(text)
        val paragraphs = PARAGRAPH.split(t)
        val first = paragraphs.indexOfFirst { !isThinking(it) }
        val rest = if (first < 0) emptyList() else paragraphs.drop(first)
        return when {
            rest.isEmpty() || (rest.size == 1 && mayBecome(rest[0])) -> ""
            first == 0 -> t
            else -> rest.joinToString("\n\n")
        }
    }

    /** Whether [paragraph] is the brain talking about its task instead of answering. */
    fun isThinking(paragraph: String): Boolean {
        val p = paragraph.trim()
        return p.isEmpty() || STRONG.containsMatchIn(p) || (SOFT.containsMatchIn(p) && TASK.containsMatchIn(p))
    }

    private fun untagged(text: String): String {
        val after = if (text.contains(CLOSE)) text.substringAfterLast(CLOSE) else text
        return after.replace(OPEN, "")
    }

    private fun mayBecome(paragraph: String): Boolean {
        val p = paragraph.trimStart()
        val low = p.lowercase()
        if (OPENINGS.any { it.startsWith(low) }) return true
        return SOFT.containsMatchIn(p) && p.length <= HOLD_CHARS
    }

    private const val OPEN = "<think>"
    private const val CLOSE = "</think>"

    /** A soft opening is held at most this long while it streams (then it is clear). */
    private const val HOLD_CHARS = 240
    private val PARAGRAPH = Regex("\\n\\s*\\n")
    private val STRONG =
        Regex(
            "^(?:we need to|we have to|we must|we should (?:answer|respond|reply|call|use|search|provide)|" +
                "the user (?:asks|asked|is asking|wants|wanted|says|said|writes|wrote|requests|is requesting)|" +
                "(?:okay|ok|alright|so),? (?:so )?the user|i need to (?:answer|respond|reply)|" +
                "der nutzer (?:fragt|möchte|will|schreibt|bittet))\\b",
            RegexOption.IGNORE_CASE,
        )
    private val SOFT =
        Regex(
            "^(?:let me|let's|let us|i need to|i should|i will|i'll|okay,? |ok,? |alright,? |first,? |now,? )",
            RegexOption.IGNORE_CASE,
        )
    private val TASK =
        Regex(
            "\\b(?:the user|user's|the question|the query|answer (?:in|the|it)|respond|search|tool|" +
                "in german|auf deutsch|the prompt|the instructions)\\b",
            RegexOption.IGNORE_CASE,
        )
    private val OPENINGS =
        listOf(
            "we need",
            "we have to",
            "we must",
            "we should",
            "the user",
            "okay, so",
            "ok, so",
            "so the user",
            "der nutzer",
        )
}
