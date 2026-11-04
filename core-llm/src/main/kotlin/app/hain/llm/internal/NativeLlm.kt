package app.hain.llm.internal

/** Receives generated text from the native loop in complete UTF-8 chunks. Return `false` to stop. */
fun interface TokenCallback {
    fun onToken(bytes: ByteArray): Boolean
}

/** Receives the model loading progress (0..1). Return `false` to abort loading. */
fun interface LoadProgress {
    fun onProgress(progress: Float): Boolean
}

/** The whole JNI surface (LLM.md §1). Blocking calls: run them on one background thread. */
@Suppress("TooManyFunctions") // one function per JNI entry point
internal object NativeLlm {
    const val STOP_EOS = 0
    const val STOP_MAX_TOKENS = 1
    const val STOP_CANCELLED = 2
    const val STOP_ERROR = 3
    const val STOP_CONTEXT_FULL = 4

    /** `null` when the native library is missing (e.g. a build with `-Phain.native=false`). */
    val loadError: Throwable? by lazy {
        runCatching { System.loadLibrary("hain_llm") }.exceptionOrNull()
    }

    external fun nativeInit(nativeLibDir: String)

    external fun nativeLoad(
        path: String,
        nCtx: Int,
        nThreads: Int,
        nGpuLayers: Int,
        progress: LoadProgress?,
    ): Long

    external fun nativeFree(handle: Long)

    external fun nativeTokenize(
        handle: Long,
        text: String,
        addSpecial: Boolean,
    ): IntArray

    @Suppress("LongParameterList") // one flat JNI call
    external fun nativeGenerate(
        handle: Long,
        promptUtf8: ByteArray,
        cutBytes: IntArray,
        params: FloatArray,
        grammar: String?,
        /** T-133: GBNF used only inside ```card blocks (card.gbnf), `null` = off. */
        cardGrammar: String?,
        cb: TokenCallback,
    ): Int

    /** Sets or clears the stop flag; a set flag also aborts a running decode. */
    external fun nativeSetCancel(
        handle: Long,
        cancel: Boolean,
    )

    external fun nativePrefill(
        handle: Long,
        promptUtf8: ByteArray,
        cutBytes: IntArray,
    ): Boolean

    /** [loadMs, promptTokens, reusedTokens, prefillMs, genTokens, genMs] of the last calls. */
    external fun nativeStats(handle: Long): FloatArray

    external fun nativeKvReuse(
        handle: Long,
        enabled: Boolean,
    )

    /** UTF-8 bytes of the GGUF chat template (`tokenizer.chat_template`), `null` if the file has none. */
    external fun nativeChatTemplate(handle: Long): ByteArray?

    /**
     * Applies [template] (UTF-8) to messages with llama.cpp's built-in template engine; `null` when
     * llama.cpp does not know the template. Roles and contents are UTF-8, same length.
     */
    external fun nativeApplyTemplate(
        template: ByteArray,
        roles: Array<ByteArray>,
        contents: Array<ByteArray>,
        addAssistant: Boolean,
    ): ByteArray?

    /** T-110: loads an embedding GGUF with its own context of [nCtx] tokens; 0 on failure. */
    external fun nativeEmbedLoad(
        path: String,
        nCtx: Int,
        nThreads: Int,
    ): Long

    external fun nativeEmbedFree(handle: Long)

    /** L2-normalised vector of [textUtf8], `null` when it failed. */
    external fun nativeEmbed(
        handle: Long,
        textUtf8: ByteArray,
    ): FloatArray?
}
