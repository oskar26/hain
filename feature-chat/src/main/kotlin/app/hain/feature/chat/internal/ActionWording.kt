package app.hain.feature.chat.internal

/**
 * Sharper words after a phone action (4b-2). The brain often writes "Ich habe den Wecker
 * eingestellt" or "Your alarm is set" although nothing happens before the tap on the ConfirmCard.
 * For answers with a prepared action this turns such claims into "vorbereitet" / "prepared"; the card
 * itself says "Vorbereitet – tipp auf „Stellen“". Code blocks and inline code stay as they are.
 */
object ActionWording {
    private const val DE_PREPARED = "vorbereitet"
    private const val EN_PREPARED = "prepared"

    // German participles that claim the action is already done
    private val DE_WORDS =
        listOf(
            "eingestellt",
            "gestellt",
            "gesetzt",
            "eingetragen",
            "gestartet",
            "angelegt",
            "erstellt",
            "hinzugefügt",
            "programmiert",
        )
    private val DE_DONE = Regex("""(?iu)(?<!\p{L})(""" + DE_WORDS.joinToString("|") + """)(?!\p{L})""")

    // English: only after a helper verb, so "a set of" or "set off" stay
    private const val EN_HELPERS = "is|are|was|were|been|has|have|i've|i have|it's|we've"
    private const val EN_WORDS = "set|added|started|scheduled|created"
    private val EN_DONE =
        Regex("""(?i)\b($EN_HELPERS)(\s+(?:now|already|all|just)?\s*)($EN_WORDS)\b""")

    private val FENCE = Regex("^\\s*```")
    private val INLINE_CODE = Regex("`[^`]*`")

    /** [text] with "done" claims replaced; unchanged when there is nothing to fix. */
    fun clean(text: String): String {
        var inCode = false
        return text.split('\n').joinToString("\n") { line ->
            when {
                FENCE.containsMatchIn(line) -> {
                    inCode = !inCode
                    line
                }
                inCode -> line
                else -> cleanLine(line)
            }
        }
    }

    private fun cleanLine(line: String): String {
        val codes = INLINE_CODE.findAll(line).map { it.range }.toList()

        fun inCode(range: IntRange) = codes.any { range.first >= it.first && range.last <= it.last }
        val de =
            DE_DONE.replace(line) { m ->
                if (inCode(m.range)) m.value else keepCase(m.value, DE_PREPARED)
            }
        return EN_DONE.replace(de) { m ->
            if (inCode(m.range)) {
                m.value
            } else {
                val (helper, gap, verb) = m.destructured
                helper + gap + keepCase(verb, EN_PREPARED)
            }
        }
    }

    /** "Eingestellt" → "Vorbereitet": a capital first letter stays capital. */
    private fun keepCase(
        original: String,
        replacement: String,
    ): String = if (original.first().isUpperCase()) replacement.replaceFirstChar { it.uppercase() } else replacement
}
