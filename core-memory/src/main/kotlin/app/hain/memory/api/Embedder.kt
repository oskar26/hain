package app.hain.memory.api

import app.hain.llm.api.TextEmbedder
import java.io.File

/** What a text is for: LFM2.5-Embedding is asymmetric and wants a prefix for each side. */
enum class EmbedKind(
    val prefix: String,
) {
    QUERY("query: "),
    DOCUMENT("document: "),
}

/**
 * The memory's embedder (T-110, MEMORY.md §3): an optional pack. Without it the memory works on
 * keywords only (BM25), with it the ranking is hybrid.
 */
interface Embedder {
    /** Names the model; vectors of another model are made again. */
    val model: String

    /** True when the pack is on the phone and the native library is there. */
    fun ready(): Boolean

    /** The normalised vector of [text], or `null` (not ready, load failed). */
    suspend fun embed(
        text: String,
        kind: EmbedKind,
    ): FloatArray?
}

/** [Embedder] on the app's [TextEmbedder]; [file] is the pack's GGUF, `null` while it is not installed. */
class PackEmbedder(
    private val engine: TextEmbedder,
    override val model: String,
    private val file: () -> File?,
) : Embedder {
    override fun ready(): Boolean = engine.isAvailable && file()?.isFile == true

    override suspend fun embed(
        text: String,
        kind: EmbedKind,
    ): FloatArray? {
        val path = file()?.takeIf { it.isFile }?.absolutePath ?: return null
        return engine.embed(path, kind.prefix + text.trim())
    }
}
