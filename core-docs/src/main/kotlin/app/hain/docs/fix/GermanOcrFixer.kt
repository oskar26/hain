package app.hain.docs.fix

/**
 * Fixes the typical mistakes of PP-OCRv5 on German text (T-122, DOCS_OCR.md "GermanOcrFixer"):
 *
 * 1. `Uber…`/`UBER…` at the start of a word → `Über…`, when that is a word.
 * 2. `B` between lower-case letters (`GröBe`, `StraBe`) → `ß`, when that is a word.
 * 3. `A`/`O`/`U` + lower-case rest → `Ä`/`Ö`/`Ü`, when that is a word and the original is not.
 * 4. A word split at a line end (`Ver-\nsicherung`) is joined when the joined word is a word.
 * 5. Words that already are words are never changed; lines that read as English skip rules 1–3.
 */
class GermanOcrFixer(
    private val isWord: (String) -> Boolean,
) {
    constructor(words: WordList) : this({ words.contains(it) })

    fun fix(text: String): String = joinHyphens(text).lines().joinToString("\n") { fixLine(it) }

    /** Rules 1–3 on one line, unless the line is English. */
    internal fun fixLine(line: String): String {
        if (isEnglish(line)) return line
        return WORD.replace(line) { m -> fixWord(m.value) }
    }

    @Suppress("ReturnCount")
    internal fun fixWord(word: String): String {
        if (word.length < 2) return word
        if (known(word)) return word
        candidates(word).firstOrNull { known(it) }?.let { return it }
        return word
    }

    /** Spellings to try for an unknown word, most likely first. */
    private fun candidates(word: String): List<String> {
        val out = mutableListOf<String>()
        // rule 1: Uber… / UBER…
        if (word.startsWith("Uber")) out += "Über" + word.substring(UBER)
        if (word.startsWith("UBER")) out += "ÜBER" + word.substring(UBER)
        // rule 2: a B between lower-case letters is a ß (every such B, then each one alone)
        val bs = word.indices.filter { i -> word[i] == 'B' && i > 0 && i < word.lastIndex && lower(word, i) }
        if (bs.isNotEmpty()) {
            out += word.mapIndexed { i, c -> if (i in bs) 'ß' else c }.joinToString("")
            if (bs.size > 1) bs.forEach { b -> out += word.substring(0, b) + 'ß' + word.substring(b + 1) }
        }
        out += umlauts(word, out.toList())
        return out.distinct()
    }

    /** Rule 3: A/O/U + lower-case rest at the start of a word → Ä/Ö/Ü (also on the other candidates). */
    private fun umlauts(
        word: String,
        others: List<String>,
    ): List<String> {
        val first = word[0]
        val umlaut = UMLAUTS[first]
        val rest = word.drop(1).all { !it.isLetter() || it.isLowerCase() }
        if (umlaut == null || !rest) return emptyList()
        return listOf(umlaut + word.substring(1)) +
            others.filter { it.isNotEmpty() && it[0] == first }.map { umlaut + it.substring(1) }
    }

    private fun lower(
        word: String,
        i: Int,
    ): Boolean = word[i - 1].isLowerCase() && word[i + 1].isLowerCase()

    private fun known(word: String): Boolean = isWord(word.lowercase())

    /** Rule 4: `Ver-\nsicherung` → `Versicherung` when that is a word; `Baden-\nWürttemberg` keeps its dash. */
    internal fun joinHyphens(text: String): String =
        HYPHEN_BREAK.replace(text) { m ->
            val left = m.groupValues[1]
            val right = m.groupValues[2]
            val joined = left + right
            when {
                right[0].isLowerCase() && known(joined) -> joined
                right[0].isLowerCase() && known(fixWord(joined)) -> fixWord(joined)
                right[0].isUpperCase() -> "$left-$right"
                else -> m.value
            }
        }

    /** A line counts as English when clearly more of its words are English than German filler words. */
    internal fun isEnglish(line: String): Boolean {
        val words = WORD.findAll(line).map { it.value.lowercase() }.toList()
        if (words.isEmpty()) return false
        val en = words.count { it in EN_STOP }
        val de = words.count { it in DE_STOP }
        return en >= 1 && en > de && en.toDouble() / words.size >= EN_SHARE
    }

    private companion object {
        const val UBER = 4
        const val EN_SHARE = 0.15
        val WORD = Regex("[\\p{L}]+")
        val HYPHEN_BREAK = Regex("(\\p{L}+)-[ \\t]*\\n[ \\t]*(\\p{L}+)")
        val UMLAUTS = mapOf('A' to 'Ä', 'O' to 'Ö', 'U' to 'Ü')
        val EN_STOP =
            setOf(
                "the",
                "and",
                "is",
                "are",
                "was",
                "were",
                "of",
                "to",
                "in",
                "on",
                "for",
                "with",
                "that",
                "this",
                "it",
                "you",
                "my",
                "your",
                "we",
                "they",
                "he",
                "she",
                "a",
                "an",
                "at",
                "by",
                "from",
                "or",
                "not",
                "be",
                "have",
                "has",
                "had",
                "will",
                "can",
                "i",
                "me",
                "our",
                "their",
                "there",
                "what",
                "which",
            )
        val DE_STOP =
            setOf(
                "der",
                "die",
                "das",
                "und",
                "ist",
                "sind",
                "war",
                "ein",
                "eine",
                "einen",
                "den",
                "dem",
                "des",
                "zu",
                "im",
                "auf",
                "für",
                "mit",
                "von",
                "nicht",
                "sich",
                "auch",
                "es",
                "wir",
                "ich",
                "du",
                "sie",
                "er",
                "bei",
                "aus",
                "nach",
                "oder",
                "wie",
                "dass",
                "wird",
                "werden",
                "hat",
                "haben",
                "zum",
                "zur",
                "bitte",
                "ihre",
                "ihr",
                "über",
            )
    }
}
