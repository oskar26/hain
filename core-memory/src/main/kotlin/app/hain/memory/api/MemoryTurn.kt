package app.hain.memory.api

import app.hain.memory.internal.ExplicitMemory
import app.hain.memory.internal.MemoryBlock
import app.hain.memory.internal.Ranking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * What the memory did for one answer (7b), for the chips under it: the facts it used
 * ("3 Erinnerungen genutzt"), a fact it just kept ("Gemerkt ✓ · Rückgängig") or forgot.
 */
data class MemoryUse(
    val used: List<MemoryFact> = emptyList(),
    val saved: List<MemoryFact> = emptyList(),
    val forgotten: List<MemoryFact> = emptyList(),
    /** "Rückgängig" was tapped: the save or the forget is undone. */
    val undone: Boolean = false,
) {
    val isEmpty: Boolean get() = used.isEmpty() && saved.isEmpty() && forgotten.isEmpty()
}

/**
 * The memory's part of every turn (T-112, T-113): a "merk dir …" / "vergiss …" at the start of the
 * newest message is done right away (without the brain), and the facts that fit the message go
 * into the short system line right before the answer. That line is never stored, so the cached
 * start of the prompt stays the same. With the embedder pack the facts are found by meaning too
 * (T-110). What it did is kept per turn in [uses] for the chips under the answer (7b).
 */
class MemoryTurn(
    private val store: MemoryStore,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** Messages already acted on (a regenerated answer must not save or delete twice), with what it did. */
    private val handled = LinkedHashMap<String, String>()
    private val lock = Mutex()

    private val mutableUses = MutableStateFlow<Map<String, MemoryUse>>(emptyMap())

    /** What the memory did per turn; the key is [key] of the user messages up to that answer. */
    val uses: StateFlow<Map<String, MemoryUse>> = mutableUses.asStateFlow()

    /** The turn being answered right now (a `remember` tool call belongs to it). */
    @Volatile private var current: String? = null

    /** The memory line for the answer to the newest of [userMessages] in [lang] (`de`/`en`), or `null`. */
    @Suppress("ReturnCount")
    suspend fun hint(
        userMessages: List<String>,
        lang: String,
    ): String? {
        if (!store.enabled.value) return null
        val last = userMessages.lastOrNull()?.trim().orEmpty()
        if (last.isEmpty()) return null
        val turn = key(userMessages)
        current = turn
        val explicit =
            lock.withLock {
                handled[last]?.let { return@withLock it }
                val line =
                    when (val command = ExplicitMemory.parse(last)) {
                        is ExplicitMemory.Command.Remember -> {
                            val fact = store.remember(command.text)
                            if (fact != null) record(turn) { it.copy(saved = listOf(fact), undone = false) }
                            MemoryBlock.remembered(command.text, lang)
                        }
                        is ExplicitMemory.Command.Forget -> {
                            val gone = forgetNow(command.text)
                            if (gone.isNotEmpty()) record(turn) { it.copy(forgotten = gone, undone = false) }
                            MemoryBlock.forgotten(gone.map { it.text }, lang)
                        }
                        null -> null
                    }
                if (line != null) {
                    handled[last] = line
                    if (handled.size > KEEP) handled.remove(handled.keys.first())
                }
                line
            }
        if (explicit != null) return explicit
        val context = store.recall(last, lang, clock())
        record(turn) { it.copy(used = context?.used.orEmpty()) }
        return listOfNotNull(context?.block, MemoryBlock.toolRule(lang)).joinToString("\n")
    }

    /** The `remember` tool kept [fact] while answering the current turn. */
    fun onToolSaved(fact: MemoryFact) {
        val turn = current ?: return
        record(turn) { use -> use.copy(saved = (use.saved + fact).distinctBy { it.id }, undone = false) }
    }

    /** "Rückgängig" on the chip of turn [turnKey]: deletes what it kept, brings back what it forgot. */
    fun undo(turnKey: String) {
        val use = mutableUses.value[turnKey] ?: return
        if (use.undone) return
        record(turnKey) { it.copy(undone = true) }
        scope.launch {
            use.saved.forEach { store.delete(it.id) }
            use.forgotten.forEach { store.restore(it) }
        }
    }

    /** Which facts "vergiss …" removes. */
    private suspend fun forgetNow(query: String): List<MemoryFact> {
        val all = store.facts.value
        val scores = Ranking.keywordScores(all, query)
        val best = scores.maxOrNull() ?: 0.0
        if (best <= 0.0) return emptyList()
        val gone = all.filterIndexed { i, _ -> scores[i] >= best - MARGIN }.take(MAX)
        gone.forEach { store.delete(it.id) }
        return gone
    }

    private fun record(
        turn: String,
        change: (MemoryUse) -> MemoryUse,
    ) {
        mutableUses.update { map ->
            val next = LinkedHashMap(map)
            next[turn] = change(map[turn] ?: MemoryUse())
            while (next.size > KEEP_USES) next.remove(next.keys.first())
            next
        }
    }

    companion object {
        private const val KEEP = 16
        private const val KEEP_USES = 64
        private const val MARGIN = 0.05
        private const val MAX = 3

        /** One turn: all user messages of the chat up to the answer (so equal questions in other chats differ). */
        fun key(userMessages: List<String>): String = userMessages.joinToString("\u0001") { it.trim() }
    }
}
