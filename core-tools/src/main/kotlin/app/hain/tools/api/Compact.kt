package app.hain.tools.api

/**
 * Short tool results for the small brain (4d, T-182): a title, 2–4 facts and the source. Long
 * encyclopedia text is cut after whole sentences so nothing ends mid-word.
 */
object Compact {
    const val MAX_SENTENCES = 4
    const val MAX_CHARS = 600

    private val SENTENCE_END = Regex("""(?<=[.!?])\s+(?=[A-ZÄÖÜ0-9„"(])""")

    /** The first [maxSentences] sentences of [text], at most [maxChars] characters (whole sentences). */
    fun facts(
        text: String,
        maxSentences: Int = MAX_SENTENCES,
        maxChars: Int = MAX_CHARS,
    ): String {
        val clean = text.replace(Regex("\\s+"), " ").trim()
        if (clean.length <= maxChars && SENTENCE_END.findAll(clean).count() < maxSentences) return clean
        val out = StringBuilder()
        for (sentence in clean.split(SENTENCE_END).take(maxSentences)) {
            if (out.isNotEmpty() && out.length + sentence.length + 1 > maxChars) break
            if (out.isNotEmpty()) out.append(' ')
            out.append(sentence)
        }
        return if (out.length > maxChars) out.substring(0, maxChars).substringBeforeLast(' ') + "…" else out.toString()
    }
}
