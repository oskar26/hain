package app.hain.llm.internal

/** Receives generated text from the native loop in complete UTF-8 chunks. Return `false` to stop. */
fun interface TokenCallback {
    fun onToken(bytes: ByteArray): Boolean
}

/** The whole JNI surface (LLM.md §1). Blocking calls: run them on one background thread. */
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
    ): Long

    external fun nativeFree(handle: Long)

    external fun nativeTokenize(
        handle: Long,
        text: String,
        addSpecial: Boolean,
    ): IntArray

    external fun nativeGenerate(
        handle: Long,
        promptUtf8: ByteArray,
        params: FloatArray,
        grammar: String?,
        cb: TokenCallback,
    ): Int

    external fun nativeCancel(handle: Long)

    external fun nativeKvReuse(
        handle: Long,
        enabled: Boolean,
    )
}
