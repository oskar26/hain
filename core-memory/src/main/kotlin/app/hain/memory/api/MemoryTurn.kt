package app.hain.memory.api

import app.hain.memory.internal.ExplicitMemory
import app.hain.memory.internal.MemoryBlock
import app.hain.memory.internal.Ranking
import app.hain.memory.internal.SelfFacts
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
    /**
     * T-262: what the message said about the user ("ich bin nach Köln gezogen" → "wohnt in Köln"),
     * waiting as a suggestion; the chip "Merken" under the answer keeps it with one tap.
     */
    val suggested: List<MemoryFact> = emptyList(),
    /** The suggestions of this turn were kept ("Merken" tapped). */
    val accepted: Boolean = false,
) {
    val isEmpty: Boolean get() = used.isEmpty() && saved.isEmpty() && forgotten.isEmpty() && suggested.isEmpty()
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

    /**
     * The memory line for the answer to the newest of [userMessages] in [lang] (`de`/`en`), or `null`.
     * In a [privateChat] (T-056) the memory neither saves, forgets nor reads anything.
     */
    @Suppress("ReturnCount")
    suspend fun hint(
        userMessages: List<String>,
        lang: String,
        privateChat: Boolean = false,
    ): String? {
        if (privateChat || !store.enabled.value) return null
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
        suggestFrom(last, turn)
        val context = store.recall(last, lang, clock())
        record(turn) { it.copy(used = context?.used.orEmpty()) }
        // T-271: the remember rule only when the message asks to remember something; on every turn it made
        // the small brain answer "Lerntipps für Mathe" with `remember "…"` instead of the tips
        val rule = MemoryBlock.toolRule(lang).takeIf { RememberTool.asks(last) }
        return listOfNotNull(context?.block, rule).joinToString("\n").ifBlank { null }
    }

    /**
     * T-262: facts the plain patterns find in the user's own statement ([SelfFacts.ruleFacts]) become
     * suggestions right away, not only two minutes later through the brain. A newer value of the same
     * kind ("wohnt in Bonn" after "wohnt in Köln") replaces the kept one once accepted.
     */
    private suspend fun suggestFrom(
        message: String,
        turn: String,
    ) {
        val found = SelfFacts.ruleFacts(listOf(message))
        if (found.isEmpty()) return
        val known = store.facts.value
        val new =
            found
                .mapNotNull { f ->
                    if (known.any { same(it.text, f.text) }) return@mapNotNull null
                    val slot = SelfFacts.slot(f.text)
                    val old = known.firstOrNull { !it.suggested && slot != null && SelfFacts.slot(it.text) == slot }
                    store.remember(f.text, f.category, sensitive = f.sensitive, suggested = true, replaces = old?.id)
                }.filter { it.suggested }
        if (new.isNotEmpty()) record(turn) { it.copy(suggested = (it.suggested + new).distinctBy { f -> f.id }) }
    }

    /** "Merken" under the answer of [turnKey]: its suggestions become kept facts. */
    fun accept(turnKey: String) {
        val use = mutableUses.value[turnKey] ?: return
        if (use.accepted || use.suggested.isEmpty()) return
        record(turnKey) { it.copy(accepted = true) }
        scope.launch { use.suggested.forEach { store.accept(it.id) } }
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
        private fun same(
            a: String,
            b: String,
        ): Boolean = a.trim().lowercase() == b.trim().lowercase()

        private const val KEEP = 16
        private const val KEEP_USES = 64
        private const val MARGIN = 0.05
        private const val MAX = 3

        /** User messages a turn is known by (the prompt keeps the last 24 messages, the chat all of them). */
        private const val KEY_MESSAGES = 3

        /**
         * One turn: the last [KEY_MESSAGES] user messages up to the answer, so the same question in
         * another chat is mostly another turn, and the chat (all messages) and the prompt (the newest
         * ones) find the same key.
         */
        fun key(userMessages: List<String>): String =
            userMessages
                .takeLast(KEY_MESSAGES)
                .joinToString("\u0001") { it.trim() }
    }
}
