package app.hain.llm.api

/**
 * Turns a short text into one vector (T-110, MEMORY.md §3): a small embedding model next to the
 * brain, loaded when it is first needed and freed again after a while without use.
 */
interface TextEmbedder {
    /** True when the native library is part of this build. */
    val isAvailable: Boolean

    /**
     * The L2-normalised vector of [text] with the embedding model at [modelPath] (loaded on demand),
     * or `null` when the model cannot be loaded or the text is empty.
     */
    suspend fun embed(
        modelPath: String,
        text: String,
    ): FloatArray?

    /** Frees the model now (it also goes by itself after a while without use). */
    suspend fun unload()
}
