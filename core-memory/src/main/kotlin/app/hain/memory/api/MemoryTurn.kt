package app.hain.memory.api

import app.hain.memory.internal.ExplicitMemory
import app.hain.memory.internal.MemoryBlock
import app.hain.memory.internal.Ranking
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The memory's part of every turn (T-112, T-113): a "merk dir …" / "vergiss …" at the start of the
 * newest message is done right away (without the brain), and the facts that fit the message go
 * into the short system line right before the answer. That line is never stored, so the cached
 * start of the prompt stays the same.
 */
class MemoryTurn(
    private val store: MemoryStore,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    /** Messages already acted on (a regenerated answer must not save or delete twice), with what it did. */
    private val handled = LinkedHashMap<String, String>()

    /**
     * The memory line for the answer to the newest of [userMessages] in [lang] (`de`/`en`), or `null`.
     * In a [privateChat] (T-056) the memory neither saves, forgets nor reads anything.
     */
    @Synchronized
    @Suppress("ReturnCount")
    fun hint(
        userMessages: List<String>,
        lang: String,
        privateChat: Boolean = false,
    ): String? {
        if (privateChat || !store.enabled.value) return null
        val last = userMessages.lastOrNull()?.trim().orEmpty()
        if (last.isEmpty()) return null
        handled[last]?.let { return it }
        val line =
            when (val command = ExplicitMemory.parse(last)) {
                is ExplicitMemory.Command.Remember -> {
                    scope.launch { store.remember(command.text) }
                    MemoryBlock.remembered(command.text, lang)
                }
                is ExplicitMemory.Command.Forget -> {
                    val gone = forgetNow(command.text)
                    MemoryBlock.forgotten(gone, lang)
                }
                null -> null
            }
        if (line != null) {
            handled[last] = line
            if (handled.size > KEEP) handled.remove(handled.keys.first())
            return line
        }
        val facts = store.context(last, lang, clock())?.block
        return listOfNotNull(facts, MemoryBlock.toolRule(lang)).joinToString("\n")
    }

    /** Which facts "vergiss …" removes (decided now, deleted in the background). */
    private fun forgetNow(query: String): List<String> {
        val all = store.facts.value
        val scores = Ranking.keywordScores(all, query)
        val best = scores.maxOrNull() ?: 0.0
        if (best <= 0.0) return emptyList()
        val gone = all.filterIndexed { i, _ -> scores[i] >= best - MARGIN }.take(MAX)
        scope.launch { gone.forEach { store.delete(it.id) } }
        return gone.map { it.text }
    }

    private companion object {
        const val KEEP = 16
        const val MARGIN = 0.05
        const val MAX = 3
    }
}
