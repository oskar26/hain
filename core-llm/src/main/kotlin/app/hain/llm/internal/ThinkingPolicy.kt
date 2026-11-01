package app.hain.llm.internal

/** Setting `answer_mode` (LLM.md §7, SETTINGS.md). */
enum class AnswerMode(
    val key: String,
) {
    FAST("fast"),
    THOROUGH("thorough"),
    AUTO("auto"),
    ;

    companion object {
        /** Unknown or missing keys fall back to the default [AUTO]. */
        fun fromKey(key: String?): AnswerMode = entries.firstOrNull { it.key == key } ?: AUTO
    }
}

/** Decides whether the model thinks before answering (LLM.md §7). Pure function, no state. */
object ThinkingPolicy {
    /** Messages longer than this always get thinking in [AnswerMode.AUTO]. */
    const val LONG_MESSAGE_CHARS = 200

    private val ARITHMETIC = Regex("""\d\s*[-+*/×÷^=]\s*\(?\s*-?\d|\d\s*%|√\s*\d|\d\s*²""")
    private val WORD = Regex("""\p{L}+""")
    private val TRIGGER_WORDS =
        setOf(
            // German
            "warum",
            "wieso",
            "weshalb",
            "erkläre",
            "erklär",
            "erklären",
            "erklärung",
            "vergleiche",
            "vergleich",
            "vergleichen",
            "plan",
            "plane",
            "planen",
            "planung",
            // English
            "why",
            "explain",
            "explains",
            "explanation",
            "compare",
            "comparison",
            "plan",
            "planning",
        )

    fun decide(
        message: String,
        mode: AnswerMode,
    ): Boolean =
        when (mode) {
            AnswerMode.FAST -> false
            AnswerMode.THOROUGH -> true
            AnswerMode.AUTO -> needsThinking(message)
        }

    internal fun needsThinking(message: String): Boolean =
        message.length > LONG_MESSAGE_CHARS ||
            ARITHMETIC.containsMatchIn(message) ||
            WORD.findAll(message).any { it.value.lowercase() in TRIGGER_WORDS }
}
