package app.hain.memory.internal

import app.hain.memory.api.MemoryCategory
import app.hain.memory.api.MemoryFact
import app.hain.memory.api.MemoryStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The local brain for one short job with a fixed answer shape (T-114). */
fun interface ExtractionBrain {
    /** Answer to [user] after [system], forced into [grammar] (GBNF); `null` when it failed or was stopped. */
    suspend fun complete(
        system: String,
        user: String,
        grammar: String,
        maxTokens: Int,
    ): String?
}

/** One message of the chat the facts are taken from. */
data class ChatLine(
    val fromUser: Boolean,
    val text: String,
)

/** A fact the brain found, before it is checked against what is kept already. */
data class Candidate(
    val text: String,
    val category: MemoryCategory,
    val sensitive: Boolean,
)

/** How a new fact relates to the closest kept one (MEMORY.md §2.3). */
enum class Relation { SAME, UPDATE, NEW, CONTRADICTION }

/** The two prompts (from `prompts/`, copied into the assets). */
data class ExtractionPrompts(
    val extract: String,
    val compare: String,
)

/**
 * Background extraction (T-114, MEMORY.md §2.3): the brain reads the last messages of a chat and
 * names lasting facts about the user (JSON forced by a grammar). Each one is checked against what
 * is kept: by meaning with the embedder (cosine ≥ 0.92 duplicate, ≤ 0.75 new, between → the brain
 * decides same/update/new/contradiction), else by shared words. Everything found only becomes a
 * suggestion: nothing is kept until the user says yes on the memory page. An update or
 * contradiction is a suggestion that replaces the old fact once accepted (never overwritten).
 */
class FactExtractor(
    private val store: MemoryStore,
    private val brain: ExtractionBrain,
    private val prompts: ExtractionPrompts,
    /** T-262: facts from plain patterns ([SelfFacts.ruleFacts]) first; off only in tests of the brain part. */
    private val useRules: Boolean = true,
) {
    /** What one run did. */
    data class Outcome(
        val suggested: List<MemoryFact>,
        val skipped: Int,
    )

    /**
     * T-262: only the user's own first-person statements count ([SelfFacts.selfSentences]). Without
     * any (only questions, requests, Pim's answers) the brain is not asked at all. Pattern facts
     * ("wohnt in Köln") come first; the brain's facts are kept only when the user's own sentences
     * back them ([SelfFacts.grounded]) and they do not repeat a pattern fact.
     */
    @Suppress("ReturnCount")
    suspend fun run(
        lines: List<ChatLine>,
        chatId: Long?,
    ): Outcome {
        if (!store.enabled.value || lines.none { it.fromUser }) return Outcome(emptyList(), 0)
        val userTexts = lines.takeLast(MAX_LINES).filter { it.fromUser }.map { it.text }
        val selfs = SelfFacts.selfSentences(userTexts)
        if (selfs.isEmpty()) return Outcome(emptyList(), 0)
        val rules =
            if (useRules) {
                SelfFacts.ruleFacts(userTexts).map { Candidate(it.text, it.category, it.sensitive) }
            } else {
                emptyList()
            }
        val answer = brain.complete(prompts.extract, transcript(selfs), ExtractionJson.FACTS_GRAMMAR, MAX_TOKENS)
        val fromBrain =
            answer
                ?.let { ExtractionJson.parseFacts(it) }
                .orEmpty()
                .filter { SelfFacts.grounded(it.text, selfs) }
                .filterNot { c -> rules.any { SelfFacts.grounded(c.text, listOf(it.text)) } }
        val candidates = (rules + fromBrain).distinctBy { it.text.lowercase() }.take(MAX_FACTS)
        val out = mutableListOf<MemoryFact>()
        var skipped = 0
        for (candidate in candidates) {
            val replaces =
                when (val decision = decide(candidate)) {
                    Decision.Skip -> {
                        skipped++
                        continue
                    }
                    Decision.New -> null
                    is Decision.Replace -> decision.old.id
                }
            val fact =
                store.remember(
                    candidate.text,
                    candidate.category,
                    sensitive = candidate.sensitive,
                    suggested = true,
                    originChatId = chatId,
                    replaces = replaces,
                )
            if (fact != null && fact.suggested && out.none { it.id == fact.id }) out += fact else skipped++
        }
        return Outcome(out, skipped)
    }

    private sealed interface Decision {
        data object Skip : Decision

        data object New : Decision

        data class Replace(
            val old: MemoryFact,
        ) : Decision
    }

    @Suppress("ReturnCount")
    private suspend fun decide(candidate: Candidate): Decision {
        val all = store.facts.value
        if (all.isEmpty()) return Decision.New
        val nearest = store.nearest(candidate.text)
        val (closest, similarity) =
            if (nearest != null) {
                nearest.fact to nearest.cosine
            } else {
                val best = all.maxByOrNull { Dedupe.wordSimilarity(it.text, candidate.text) } ?: return Decision.New
                best to Dedupe.asCosine(Dedupe.wordSimilarity(best.text, candidate.text))
            }
        return when {
            similarity >= Dedupe.DUPLICATE -> Decision.Skip
            similarity <= Dedupe.DIFFERENT -> Decision.New
            else ->
                when (compare(closest, candidate)) {
                    Relation.SAME, null -> Decision.Skip
                    Relation.NEW -> Decision.New
                    // an open suggestion is not replaced by another suggestion: the newer one waits on its own
                    Relation.UPDATE, Relation.CONTRADICTION ->
                        if (closest.suggested) Decision.New else Decision.Replace(closest)
                }
        }
    }

    private suspend fun compare(
        old: MemoryFact,
        candidate: Candidate,
    ): Relation? {
        val ask = "Old: ${old.text}\nNew: ${candidate.text}"
        val answer = brain.complete(prompts.compare, ask, ExtractionJson.LABEL_GRAMMAR, LABEL_TOKENS) ?: return null
        return ExtractionJson.parseLabel(answer)
    }

    companion object {
        /** MEMORY.md §2.3: the last ≤ 12 messages. */
        const val MAX_LINES = 12
        const val MAX_FACTS = 5
        private const val MAX_TOKENS = 400
        private const val LABEL_TOKENS = 24
        private const val USER_CHARS = 800

        /**
         * T-262: what the brain reads: only the user's own statements about themselves, one per line.
         * Pim's answers and the user's questions are left out, so their topics cannot become "facts".
         */
        fun transcript(selfSentences: List<String>): String =
            selfSentences.takeLast(MAX_LINES).joinToString("\n") { s ->
                "User: " + s.replace(Regex("\\s+"), " ").trim().take(USER_CHARS)
            }
    }
}

/** Duplicate rules of the extraction (MEMORY.md §2.3). */
object Dedupe {
    /** Cosine from which a candidate is the same fact. */
    const val DUPLICATE = 0.92

    /** Cosine up to which it is clearly a different fact. */
    const val DIFFERENT = 0.75

    /**
     * Without the embedder: share of words both texts have (loose endings like the keyword search),
     * 0..1. "mag keine Pilze" ~ "mag Pilze nicht" is high, different topics are 0.
     */
    fun wordSimilarity(
        a: String,
        b: String,
    ): Double {
        val x = Ranking.tokens(a).toSet()
        val y = Ranking.tokens(b).toSet()
        if (x.isEmpty() || y.isEmpty()) return if (normal(a) == normal(b)) 1.0 else 0.0
        val shared = x.count { w -> y.any { Ranking.matches(w, it) } }
        return shared.toDouble() / (x.size + y.size - shared)
    }

    /**
     * Word overlap on the cosine scale of [DUPLICATE]/[DIFFERENT]: the same words = 1.0, one shared
     * word of four ≈ 0.78 (the brain decides: "wohnt in Köln" / "wohnt jetzt in Bonn"), none = new.
     */
    fun asCosine(words: Double): Double = DIFFERENT_WORDS_AS_COSINE + (1 - DIFFERENT_WORDS_AS_COSINE) * words

    private const val DIFFERENT_WORDS_AS_COSINE = 0.7
    private val NOT_WORD = Regex("[^\\p{L}\\p{N}]+")

    private fun normal(text: String) = text.lowercase().replace(NOT_WORD, " ").trim()
}

/** JSON shapes and grammars of the extraction (llama.cpp GBNF). */
object ExtractionJson {
    /** `{"facts":[{"text":"…","category":"…","sensitive":false}, …]}`, at most 5 facts of ≤ 200 chars. */
    val FACTS_GRAMMAR =
        """
        root ::= "{" ws "\"facts\"" ws ":" ws "[" ws (fact (ws "," ws fact){0,4})? ws "]" ws "}"
        fact ::= "{" ws "\"text\"" ws ":" ws str ws "," ws catkey ws "," ws senskey ws "}"
        catkey ::= "\"category\"" ws ":" ws cat
        senskey ::= "\"sensitive\"" ws ":" ws bool
        cat ::= "\"preference\"" | "\"person\"" | "\"place\"" | "\"work\"" | "\"health\"" | "\"other\""
        bool ::= "true" | "false"
        str ::= "\"" char{1,200} "\""
        char ::= [^"\\\x7F\x00-\x1F] | "\\" ["\\/bfnrt]
        ws ::= [ \t\n]{0,4}
        """.trimIndent()

    /** `{"label":"same|update|new|contradiction"}`. */
    val LABEL_GRAMMAR =
        """
        root ::= "{\"label\":\"" ("same" | "update" | "new" | "contradiction") "\"}"
        """.trimIndent()

    private val json = Json { ignoreUnknownKeys = true }

    /** The facts of an answer; anything broken is left out (model output is untrusted). */
    @Suppress("ReturnCount")
    fun parseFacts(answer: String): List<Candidate> {
        val root = parseObject(answer) ?: return emptyList()
        val rows = runCatching { root["facts"]?.jsonArray }.getOrNull() ?: return emptyList()
        return rows
            .mapNotNull { row ->
                val o = runCatching { row.jsonObject }.getOrNull() ?: return@mapNotNull null
                val text = runCatching { o["text"]?.jsonPrimitive?.content }.getOrNull()?.let(::cleanText)
                if (text.isNullOrBlank()) return@mapNotNull null
                val category = MemoryCategory.fromKey(runCatching { o["category"]?.jsonPrimitive?.content }.getOrNull())
                val flagged = runCatching { o["sensitive"]?.jsonPrimitive?.booleanOrNull }.getOrNull() == true
                Candidate(text, category, flagged || category == MemoryCategory.HEALTH)
            }.distinctBy { it.text.lowercase() }
    }

    fun parseLabel(answer: String): Relation? {
        val label = runCatching { parseObject(answer)?.get("label")?.jsonPrimitive?.content }.getOrNull()
        return Relation.entries.firstOrNull { it.name.equals(label?.trim(), ignoreCase = true) }
    }

    /** "Der Nutzer mag …" / "The user likes …" → "mag …": facts are about the user anyway. */
    private fun cleanText(raw: String): String? {
        val text = raw.trim().replace(USER_START, "").trim()
        if (text.length < MIN_CHARS) return null
        return ExplicitMemory.cut(text)
    }

    private fun parseObject(answer: String): JsonObject? {
        val start = answer.indexOf('{')
        val end = answer.lastIndexOf('}')
        if (start < 0 || end <= start) return null
        return runCatching { json.parseToJsonElement(answer.substring(start, end + 1)).jsonObject }.getOrNull()
    }

    private const val MIN_CHARS = 3
    private val USER_START = Regex("(?iu)^(?:der nutzer|die nutzerin|the user)\\s+")
}
