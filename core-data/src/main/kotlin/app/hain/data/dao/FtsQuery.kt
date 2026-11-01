package app.hain.data.dao

/** Turns free user input into a safe FTS MATCH expression: every word as a quoted prefix term. */
object FtsQuery {
    private val wordChars = Regex("[\\p{L}\\p{N}]+")
    private const val MAX_TERMS = 8

    /** `null` when the input has no searchable word. */
    fun of(input: String): String? {
        val terms =
            wordChars
                .findAll(input.lowercase())
                .map { it.value }
                .take(MAX_TERMS)
                .toList()
        return if (terms.isEmpty()) null else terms.joinToString(" ") { "\"$it*\"" }
    }
}
