package app.hain.memory.internal

import app.hain.memory.api.MemoryFact
import kotlin.math.ln
import kotlin.math.pow

/**
 * Which facts fit a question (T-113, MEMORY.md §3). Until the embedder exists (T-110) the score is
 * keyword only: BM25 over the facts with loose word endings ("Pilz" finds "Pilze"), plus recency
 * (half-life 90 days) and importance. With a cosine it is the spec's hybrid score.
 */
object Ranking {
    const val MIN_SCORE = 0.35
    const val MAX_FACTS = 8

    /** About 500 tokens of facts at most (MEMORY.md: memory ≤ 600 tokens in all). */
    const val MAX_CHARS = 1_800

    data class Scored(
        val fact: MemoryFact,
        val score: Double,
        val keyword: Double,
    )

    /** Words of [text] worth matching: lower case, no stop words, at least 3 letters (or a number). */
    fun tokens(text: String): List<String> =
        WORD
            .findAll(text.lowercase())
            .map { it.value }
            .filter { (it.length >= MIN_WORD || it.all(Char::isDigit)) && it !in STOP_WORDS }
            .toList()

    /** Same word, or the same stem: "pilz" ~ "pilze", "gitarre" ~ "gitarrenunterricht". */
    @Suppress("ReturnCount")
    fun matches(
        query: String,
        word: String,
    ): Boolean {
        if (query == word) return true
        val shorter = minOf(query.length, word.length)
        if (shorter >= STEM && (word.startsWith(query) || query.startsWith(word))) return true
        return shorter > STEM && query.take(STEM + 1) == word.take(STEM + 1)
    }

    /**
     * BM25 of every fact in [facts] for [query], squeezed into 0..1: no shared word = 0, one shared
     * word ≥ 0.5, more and rarer words come closer to 1.
     */
    fun keywordScores(
        facts: List<MemoryFact>,
        query: String,
    ): List<Double> {
        val terms = tokens(query).distinct()
        if (terms.isEmpty() || facts.isEmpty()) return facts.map { 0.0 }
        val docs = facts.map { tokens(it.text) }
        val avg = docs.map { it.size }.average().coerceAtLeast(1.0)
        val tf = docs.map { doc -> terms.map { q -> doc.count { matches(q, it) } } }
        val n = facts.size
        val idf =
            terms.indices.map { i ->
                val df = tf.count { it[i] > 0 }
                ln(1 + (n - df + HALF) / (df + HALF)).coerceAtLeast(MIN_IDF)
            }
        return docs.mapIndexed { d, doc ->
            var sum = 0.0
            terms.indices.forEach { i ->
                val f = tf[d][i].toDouble()
                if (f > 0) sum += idf[i] * f * (K1 + 1) / (f + K1 * (1 - B + B * doc.size / avg))
            }
            if (sum <= 0) 0.0 else HALF + HALF * sum / (sum + 1)
        }
    }

    /** 1 today, 0.5 after 90 days, 0.25 after 180 … */
    fun recency(
        createdAt: Long,
        now: Long,
    ): Double {
        val days = (now - createdAt).coerceAtLeast(0L) / MS_PER_DAY
        return HALF.pow(days / HALF_LIFE_DAYS)
    }

    /** MEMORY.md §3: 0.6·cosine + 0.25·BM25 + 0.1·recency + 0.05·importance; without a cosine keyword-led. */
    fun score(
        keyword: Double,
        recency: Double,
        importance: Double,
        cosine: Double? = null,
    ): Double =
        if (cosine != null) {
            W_COS * cosine + W_BM25 * keyword + W_RECENCY * recency + W_IMPORTANCE * importance
        } else {
            K_BM25 * keyword + K_RECENCY * recency + K_IMPORTANCE * importance
        }

    /**
     * The facts for the prompt: suggested ones never, then best first, score ≥ [MIN_SCORE], at most
     * [MAX_FACTS] and [maxChars] characters of fact text.
     */
    fun select(
        facts: List<MemoryFact>,
        query: String,
        now: Long,
        maxChars: Int = MAX_CHARS,
        cosine: ((MemoryFact) -> Double?)? = null,
    ): List<Scored> {
        val usable = facts.filter { !it.suggested }
        val keywords = keywordScores(usable, query)
        val ranked =
            usable
                .mapIndexed { i, fact ->
                    val k = keywords[i]
                    Scored(fact, score(k, recency(fact.createdAt, now), fact.importance, cosine?.invoke(fact)), k)
                }.filter { it.score >= MIN_SCORE }
                .sortedWith(compareByDescending<Scored> { it.score }.thenByDescending { it.fact.createdAt })
        val out = mutableListOf<Scored>()
        var chars = 0
        for (s in ranked) {
            if (out.size >= MAX_FACTS || chars + s.fact.text.length > maxChars) break
            out += s
            chars += s.fact.text.length
        }
        return out
    }

    private val WORD = Regex("[\\p{L}\\p{N}]+")
    private const val MIN_WORD = 3
    private const val STEM = 4
    private const val K1 = 1.2
    private const val B = 0.75
    private const val HALF = 0.5
    private const val MIN_IDF = 0.5
    private const val HALF_LIFE_DAYS = 90.0
    private const val MS_PER_DAY = 86_400_000.0
    private const val W_COS = 0.6
    private const val W_BM25 = 0.25
    private const val W_RECENCY = 0.1
    private const val W_IMPORTANCE = 0.05
    private const val K_BM25 = 0.75
    private const val K_RECENCY = 0.15
    private const val K_IMPORTANCE = 0.1

    /** German and English filler words that say nothing about a topic. */
    private val STOP_WORDS =
        setOf(
            // de
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
            "eines",
            "und",
            "oder",
            "aber",
            "ich",
            "du",
            "er",
            "sie",
            "es",
            "wir",
            "ihr",
            "mir",
            "mich",
            "dir",
            "dich",
            "mein",
            "meine",
            "meinen",
            "meinem",
            "meiner",
            "dein",
            "deine",
            "sein",
            "seine",
            "ist",
            "bin",
            "bist",
            "sind",
            "war",
            "habe",
            "hast",
            "hat",
            "haben",
            "wird",
            "werde",
            "kann",
            "kannst",
            "soll",
            "sollte",
            "muss",
            "mag",
            "nicht",
            "kein",
            "keine",
            "auch",
            "noch",
            "schon",
            "nur",
            "sehr",
            "mal",
            "bitte",
            "was",
            "wer",
            "wie",
            "wann",
            "warum",
            "welche",
            "welcher",
            "für",
            "mit",
            "von",
            "vom",
            "zum",
            "zur",
            "auf",
            "aus",
            "bei",
            "nach",
            "über",
            "unter",
            "vor",
            "dass",
            "wenn",
            "weil",
            "als",
            "dann",
            "denn",
            "doch",
            "hier",
            "heute",
            "morgen",
            "gibt",
            "gib",
            "mach",
            "machen",
            "sag",
            "sage",
            "erzähl",
            "uns",
            "euch",
            "ihm",
            "ihn",
            "ihnen",
            "man",
            "etwas",
            "alles",
            "viel",
            "mehr",
            "gut",
            "gern",
            "gerne",
            // en
            "the",
            "and",
            "for",
            "you",
            "your",
            "are",
            "was",
            "were",
            "has",
            "have",
            "had",
            "can",
            "could",
            "would",
            "should",
            "will",
            "not",
            "but",
            "with",
            "what",
            "who",
            "how",
            "when",
            "where",
            "why",
            "which",
            "this",
            "that",
            "these",
            "those",
            "from",
            "about",
            "into",
            "some",
            "any",
            "please",
            "tell",
            "give",
            "make",
            "does",
            "did",
            "don",
            "its",
            "our",
            "they",
            "them",
            "she",
            "him",
            "her",
            "his",
            "today",
            "tomorrow",
            "like",
            "just",
            "also",
            "very",
            "much",
            "more",
            "get",
            "got",
        )
}
