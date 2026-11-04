package app.hain.tools.internal.search

import kotlin.math.ln

/**
 * Which parts of a page fit the question (T-069), without a library: BM25 over the paragraphs of
 * one page. Words are cut to their first [STEM] letters so "Wärmepumpen" still finds "Wärmepumpe",
 * and common German and English filler words do not count.
 */
object Relevance {
    private const val STEM = 6
    private const val MIN_WORD = 3
    private const val K1 = 1.2
    private const val B = 0.75
    private const val HALF = 0.5
    private val SPLIT = Regex("[^\\p{L}\\p{N}]+")

    private val STOP =
        setOf(
            "der",
            "die",
            "das",
            "den",
            "dem",
            "des",
            "ein",
            "eine",
            "einen",
            "einem",
            "einer",
            "und",
            "oder",
            "ist",
            "sind",
            "war",
            "wie",
            "was",
            "wer",
            "wann",
            "wo",
            "warum",
            "mit",
            "von",
            "für",
            "auf",
            "aus",
            "bei",
            "nach",
            "über",
            "unter",
            "zum",
            "zur",
            "sich",
            "auch",
            "nicht",
            "noch",
            "mal",
            "kurz",
            "bitte",
            "mir",
            "mich",
            "ich",
            "du",
            "dir",
            "viel",
            "viele",
            "gibt",
            "es",
            "hat",
            "haben",
            "the",
            "and",
            "for",
            "are",
            "was",
            "what",
            "who",
            "when",
            "where",
            "why",
            "how",
            "with",
            "from",
            "about",
            "this",
            "that",
            "into",
            "does",
            "did",
            "can",
            "you",
            "your",
            "much",
            "many",
            "please",
            "tell",
            "find",
            "search",
            "suche",
            "recherchier",
            "erklär",
            "erkläre",
        )

    /** The words of [text] that carry meaning, cut to their stem. */
    fun terms(text: String): List<String> =
        text
            .lowercase()
            .split(SPLIT)
            .filter { (it.length >= MIN_WORD || it.any(Char::isDigit)) && it !in STOP }
            .map { it.take(STEM) }

    /** One BM25 score per paragraph for the words of [query]; all zero when nothing matches. */
    fun scores(
        paragraphs: List<String>,
        query: String,
    ): DoubleArray {
        val q = terms(query).distinct()
        val docs = paragraphs.map { terms(it) }
        val n = docs.size
        if (q.isEmpty() || n == 0) return DoubleArray(n)
        val avg = docs.sumOf { it.size }.toDouble() / n
        val df = q.associateWith { t -> docs.count { t in it } }
        return DoubleArray(n) { i ->
            val doc = docs[i]
            val len = doc.size.toDouble()
            q.sumOf { t ->
                val tf = doc.count { it == t }.toDouble()
                if (tf == 0.0) {
                    0.0
                } else {
                    val d = df.getValue(t).toDouble()
                    val idf = ln((n - d + HALF) / (d + HALF) + 1.0)
                    idf * tf * (K1 + 1) / (tf + K1 * (1 - B + B * len / avg.coerceAtLeast(1.0)))
                }
            }
        }
    }

    /** Share of [a]'s words that are also in [b] (0…1); used to drop notes that say the same. */
    fun overlap(
        a: String,
        b: String,
    ): Double {
        val x = terms(a).toSet()
        if (x.isEmpty()) return 0.0
        val y = terms(b).toSet()
        return x.count { it in y }.toDouble() / x.size
    }
}

/**
 * The parts of a page the brain gets (T-069): never the raw page, only the paragraphs that fit the
 * question best (BM25), kept in page order, together at most `maxChars` (≈ 250 tokens per page).
 */
object Excerpt {
    private const val MIN_PARAGRAPH = 40
    private val SENTENCE_END = Regex("(?<=[.!?])\\s+")

    @Suppress("ReturnCount", "LoopWithTooManyJumpStatements")
    fun select(
        text: String,
        focus: String,
        maxChars: Int,
    ): String {
        val paragraphs = split(text, maxChars)
        if (paragraphs.isEmpty()) return HtmlText.cut(text, maxChars)
        val scores = Relevance.scores(paragraphs, focus)
        if (scores.all { it <= 0.0 }) return HtmlText.cut(paragraphs.joinToString("\n"), maxChars)
        val chosen = mutableListOf<Int>()
        var used = 0
        for (i in paragraphs.indices.sortedByDescending { scores[it] }) {
            if (scores[i] <= 0.0) break
            val len = paragraphs[i].length + 1
            if (used + len > maxChars) continue
            chosen += i
            used += len
        }
        if (chosen.isEmpty()) return HtmlText.cut(paragraphs[scores.indices.maxBy { scores[it] }], maxChars)
        return chosen.sorted().joinToString("\n") { paragraphs[it] }
    }

    /** Lines of the page; a very long one is cut into pieces of whole sentences. */
    private fun split(
        text: String,
        maxChars: Int,
    ): List<String> {
        val piece = (maxChars / 2).coerceAtLeast(MIN_PARAGRAPH * 2)
        return text
            .lines()
            .map { it.trim() }
            .filter { it.length >= MIN_PARAGRAPH }
            .flatMap { line ->
                if (line.length <= piece) {
                    listOf(line)
                } else {
                    val out = mutableListOf<String>()
                    val current = StringBuilder()
                    line.split(SENTENCE_END).forEach { s ->
                        if (current.isNotEmpty() && current.length + s.length + 1 > piece) {
                            out += current.toString()
                            current.clear()
                        }
                        if (current.isNotEmpty()) current.append(' ')
                        current.append(s)
                    }
                    if (current.isNotEmpty()) out += current.toString()
                    out.map { HtmlText.cut(it, piece) }
                }
            }
    }
}
