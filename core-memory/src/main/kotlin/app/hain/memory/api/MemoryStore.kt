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

    /** "Gedächtnis" switch on the memory page; on by default. */
    val enabled: StateFlow<Boolean>

    fun setEnabled(on: Boolean)

    /**
     * Keeps [text] (trimmed, at most [MemoryFact.MAX_CHARS]). Returns the new fact, the existing one
     * when the same text is already kept, or `null` when the memory is off or the text is empty.
     */
    suspend fun remember(
        text: String,
        category: MemoryCategory = MemoryCategory.OTHER,
        sensitive: Boolean = false,
        suggested: Boolean = false,
        originChatId: Long? = null,
    ): MemoryFact?

    suspend fun edit(
        id: Long,
        text: String,
    )

    /** Deletes the fact for good (MEMORY.md §4). */
    suspend fun delete(id: Long)

    /** Puts a fact deleted a moment ago back ("Rückgängig"). */
    suspend fun restore(fact: MemoryFact)

    /** A suggestion becomes a normal fact. */
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
