package app.hain.memory.api

import kotlinx.coroutines.flow.StateFlow

/**
 * The memory (MEMORY.md): facts the user told Pim to keep. Everything stays on the phone, in the
 * encrypted database. When [enabled] is off nothing is read or written.
 */
@Suppress("TooManyFunctions") // one function per action of the memory page
interface MemoryStore {
    /** Active and suggested facts, newest first. */
    val facts: StateFlow<List<MemoryFact>>

    /** Topics with the user's own notes (MEMORY.md L1, "Themen" tab). */
    val topics: StateFlow<List<MemoryTopic>>

    /** "Gedächtnis" switch on the memory page; on by default. */
    val enabled: StateFlow<Boolean>

    fun setEnabled(on: Boolean)

    /**
     * Keeps [text] (trimmed, at most [MemoryFact.MAX_CHARS]). Returns the new fact, the existing one
     * when the same text is already kept, or `null` when the memory is off or the text is empty.
     */
    @Suppress("LongParameterList") // all optional, named at the call
    suspend fun remember(
        text: String,
        category: MemoryCategory = MemoryCategory.OTHER,
        sensitive: Boolean = false,
        suggested: Boolean = false,
        originChatId: Long? = null,
        replaces: Long? = null,
    ): MemoryFact?

    suspend fun edit(
        id: Long,
        text: String,
    )

    /** Deletes the fact for good (MEMORY.md §4). */
    suspend fun delete(id: Long)

    /** Puts a fact deleted a moment ago back ("Rückgängig"). */
    suspend fun restore(fact: MemoryFact)

    /** A suggestion becomes a normal fact; one that [MemoryFact.replaces] an older fact puts that one out of use. */
    suspend fun accept(id: Long)

    suspend fun deleteAll()

    /** Facts that fit [query] best ("vergiss, dass ich Pilze mag"); they are deleted. */
    suspend fun forget(query: String): List<MemoryFact>

    /**
     * The block for the prompt (MEMORY.md §3) with the facts that fit [query] in [lang] (`de`/`en`),
     * or `null` when none fits or the memory is off. Reads only what is in memory already, so it is
     * cheap enough for every turn.
     */
    fun context(
        query: String,
        lang: String,
        now: Long = System.currentTimeMillis(),
    ): MemoryContext?

    /**
     * Like [context], but with the embedder (T-110) when its pack is installed: the question is
     * embedded and the ranking is hybrid (MEMORY.md §3); without the pack, keywords only.
     */
    suspend fun recall(
        query: String,
        lang: String,
        now: Long = System.currentTimeMillis(),
    ): MemoryContext? = context(query, lang, now)

    /**
     * The kept or suggested fact closest in meaning to [text] and its cosine (T-114 dedupe), or
     * `null` without the embedder or without facts.
     */
    suspend fun nearest(text: String): Nearest? = null

    /**
     * T-262: kept facts that describe the user in one value (where they live or come from, name, age,
     * class, job; never sensitive ones), newest first. They go into the system prompt as "Über den
     * Nutzer", so Pim knows them in every answer without the keyword search. Empty while the memory is off.
     */
    fun profile(): List<MemoryFact> = emptyList()

    /** T-262: the place of the kept home fact ("wohnt in Köln" → "Köln"), e.g. for the weather; `null` if none. */
    fun homePlace(): String? = null

    /** True when the embedder pack is installed, so recall is by meaning too. */
    fun smartSearch(): Boolean = false

    /** Sets the note of the topic [title]; an empty note removes it. */
    suspend fun setTopicNote(
        title: String,
        note: String,
    )

    /** All facts as JSON for the user's own backup (MEMORY.md §4). */
    fun exportJson(): String

    /** Adds the facts of an [exportJson] file; returns how many were new. Throws on a wrong file. */
    suspend fun importJson(json: String): Int
}

/** The prompt block and the facts in it (for "3 Erinnerungen genutzt"). */
data class MemoryContext(
    val block: String,
    val used: List<MemoryFact>,
)

/** The closest fact to a text by meaning and their cosine (−1..1). */
data class Nearest(
    val fact: MemoryFact,
    val cosine: Double,
)
