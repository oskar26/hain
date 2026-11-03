package app.hain.tools.api

import app.hain.tools.internal.search.Relevance
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Running research notes of one answer (T-069): after each round the pages read so far shrink to
 * a few short facts, each with the number of its source ("[2] Eine Wärmepumpe …"), so older raw
 * text can leave the prompt while what it said stays. Picked from the page excerpts by how well a
 * sentence fits the question; a sentence that says the same as an earlier note is left out.
 */
class ResearchNotes(
    private val perSource: Int = PER_SOURCE,
    private val maxNotes: Int = MAX_NOTES,
) {
    /** One fact; [sources] holds every source that said it (a repeat from another site confirms it). */
    data class Note(
        val sources: List<Int>,
        val text: String,
    )

    private val notes = mutableListOf<Note>()

    val all: List<Note> get() = notes.toList()

    /** Number of facts noted so far. */
    val facts: Int get() = notes.size

    /** Number of different sources the facts come from. */
    val sources: Int get() = notes.flatMap { it.sources }.distinct().size

    /** Notes the best sentences of [excerpt] (source number [source]) for [focus]. */
    fun add(
        source: Int,
        excerpt: String,
        focus: String,
    ) {
        if (notes.any { source in it.sources }) return
        val sentences =
            excerpt
                .split(SENTENCE_END)
                .map { it.trim() }
                .filter { it.length in MIN_SENTENCE..MAX_SENTENCE_IN }
        if (sentences.isEmpty()) return
        val scores = Relevance.scores(sentences, focus)
        val ranked =
            sentences.indices
                .sortedByDescending { scores[it] }
                .filter { scores[it] > 0.0 || sentences.size == 1 }
                .map { sentences[it] }
                .ifEmpty { listOf(sentences.first()) }
        var added = 0
        for (sentence in ranked) {
            if (added == perSource) break
            val same = notes.indexOfFirst { Relevance.overlap(sentence, it.text) >= SAME }
            if (same >= 0) {
                // another site says the same: the fact now has two sources
                val n = notes[same]
                if (source !in n.sources) notes[same] = n.copy(sources = n.sources + source)
            } else if (notes.size < maxNotes) {
                notes += Note(listOf(source), cut(sentence))
            }
            added++
        }
    }

    /** `["[1] …", "[2] …"]` for the notes of [source], or of all sources. */
    fun lines(source: Int? = null): List<String> =
        notes
            .filter { source == null || source in it.sources }
            .map { n -> n.sources.joinToString("") { "[$it]" } + " " + n.text }

    /** `{"notes":[…]}` for the brain. */
    fun forModel(source: Int? = null): String {
        val list = JsonArray(lines(source).map { JsonPrimitive(it) })
        return JsonObject(mapOf("notes" to list)).toString()
    }

    private fun cut(s: String): String = if (s.length <= MAX_NOTE) s else s.take(MAX_NOTE).trimEnd() + "…"

    companion object {
        const val PER_SOURCE = 2
        const val MAX_NOTES = 16
        private const val MIN_SENTENCE = 30
        private const val MAX_SENTENCE_IN = 600
        private const val MAX_NOTE = 220
        private const val SAME = 0.8
        private val SENTENCE_END = Regex("(?<=[.!?])\\s+|\\n+")
    }
}
