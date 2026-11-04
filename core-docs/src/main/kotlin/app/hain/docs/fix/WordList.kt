package app.hain.docs.fix

import java.io.InputStream

/**
 * The German word list for [GermanOcrFixer] (`assets/dict/de_words.txt`, one lower-case word per
 * line, sorted; licence in LICENSES.md). Kept as a sorted array: about 208,000 words, looked up by
 * binary search, so it needs far less memory than a hash set.
 */
class WordList private constructor(
    private val words: Array<String>,
) {
    val size: Int get() = words.size

    /** `true` when [word] (any case) is a German word. */
    operator fun contains(word: String): Boolean = words.binarySearch(word.lowercase()) >= 0

    companion object {
        const val ASSET = "dict/de_words.txt"

        fun read(input: InputStream): WordList =
            input.bufferedReader(Charsets.UTF_8).useLines { lines ->
                of(lines.map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }.toList())
            }

        fun of(words: Collection<String>): WordList =
            WordList(
                words
                    .map { it.lowercase() }
                    .distinct()
                    .sorted()
                    .toTypedArray(),
            )
    }
}
