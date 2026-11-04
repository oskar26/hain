package app.hain.docs.internal.ephemeral

import app.hain.docs.api.DocInfo
import app.hain.docs.api.DocPage
import app.hain.docs.internal.chunk.Chunker
import app.hain.docs.internal.search.SearchChunk

/**
 * Files attached in a private chat (T-056): they live only in memory for that chat and never go
 * into "Meine Dateien" (`hain_docs.db`). Their ids are negative, so they never clash with saved
 * documents, and [clear] forgets all of them when the private chat ends.
 */
internal class EphemeralDocs {
    private data class Entry(
        val info: DocInfo,
        val pages: List<DocPage>,
    )

    private val entries = LinkedHashMap<Long, Entry>()
    private var nextId = -1L

    /** Keeps [info] (its id is ignored) with its [pages]; gives it back with its new negative id. */
    @Synchronized
    fun add(
        info: DocInfo,
        pages: List<DocPage>,
    ): DocInfo {
        val kept = info.copy(id = nextId--)
        entries[kept.id] = Entry(kept, pages)
        return kept
    }

    @Synchronized
    fun info(ids: List<Long>): List<DocInfo> = ids.mapNotNull { entries[it]?.info }

    @Synchronized
    fun pages(id: Long): List<DocPage> = entries[id]?.pages.orEmpty()

    /** The searchable parts of [ids], cut like saved documents. */
    @Synchronized
    fun chunks(ids: List<Long>): List<SearchChunk> =
        ids.flatMap { id ->
            val entry = entries[id] ?: return@flatMap emptyList()
            Chunker.chunk(entry.pages).map { SearchChunk(entry.info.name, it.page, it.text) }
        }

    @Synchronized
    fun remove(id: Long) {
        entries.remove(id)
    }

    @Synchronized
    fun clear() = entries.clear()

    @get:Synchronized
    val size: Int get() = entries.size

    companion object {
        /** `true` for an id of a file that is only kept for a private chat. */
        fun isEphemeral(id: Long): Boolean = id < 0
    }
}
