package app.hain.docs.internal.search

import app.hain.tools.internal.search.Relevance

/** A searchable part of an attached document. */
data class SearchChunk(
    val document: String,
    val page: Int,
    val text: String,
)

/**
 * Finds the parts of the attached documents that fit a question (T-123): BM25 over the chunks
 * (the same [Relevance] the web reading uses since 4c), best [TOP] first.
 */
object DocSearch {
    const val TOP = 4

    fun best(
        chunks: List<SearchChunk>,
        query: String,
        top: Int = TOP,
    ): List<SearchChunk> {
        if (chunks.isEmpty()) return emptyList()
        val scores = Relevance.scores(chunks.map { it.text }, query)
        return chunks.indices
            .filter { scores[it] > 0.0 }
            .sortedByDescending { scores[it] }
            .take(top)
            .map { chunks[it] }
    }
}
