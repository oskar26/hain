package app.hain.llm.internal

import android.content.Context
import android.os.SystemClock
import android.util.Log
import app.hain.llm.api.EngineState
import app.hain.llm.api.GenerationEvent
import app.hain.llm.api.GenerationStats
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.SamplingParams
import app.hain.llm.api.StopReason
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/** [LlmEngine] on top of llama.cpp via [NativeLlm] (T-032). All native calls run on one thread. */
@Suppress("TooManyFunctions") // the LlmEngine surface plus small native helpers
@OptIn(ExperimentalCoroutinesApi::class)
class LlamaEngine(
    private val context: Context,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1),
) : LlmEngine {
    private val mutableState = MutableStateFlow<EngineState>(EngineState.Unloaded)
    override val state: StateFlow<EngineState> = mutableState.asStateFlow()

    @Volatile private var handle = 0L

    /** Context size the current model was loaded with; a different one loads it again (T-211). */
    @Volatile private var loadedContext = 0

    /** Chat template of the loaded GGUF (T-211), read once after loading. */
    @Volatile private var template: ByteArray? = null

    // Each generation gets a number; cancel() stops every generation up to the current one. A plain
    // flag could be reset by the end of the previous generation and swallow a stop.
    @Volatile private var started = 0

    @Volatile private var cancelledUpTo = 0
    private var initialized = false

    override val isAvailable: Boolean get() = NativeLlm.loadError == null

    override suspend fun load(
        path: String,
        contextSize: Int,
    ): Boolean =
        withContext(dispatcher) {
            val current = mutableState.value
            val sameModel = current is EngineState.Ready && current.path == path
            if (sameModel && handle != 0L && loadedContext == contextSize) {
                return@withContext true
            }
            NativeLlm.loadError?.let {
                mutableState.value = EngineState.Failed(it.message ?: "native library missing")
                return@withContext false
            }
            if (!initialized) {
                NativeLlm.nativeInit(context.applicationInfo.nativeLibraryDir)
                initialized = true
            }
            freeHandle()
            mutableState.value = EngineState.Loading(path, 0f)
            var shown = 0f
            val progress =
                LoadProgress { p ->
                    // a few steps are enough for the progress bar; no flood of state updates
                    if (p - shown >= PROGRESS_STEP || p >= 1f) {
                        shown = p
                        mutableState.value = EngineState.Loading(path, p)
                    }
                    true
                }
            val newHandle = NativeLlm.nativeLoad(path, contextSize, threadCount(), 0, progress)
            handle = newHandle
            loadedContext = if (newHandle != 0L) contextSize else 0
            template =
                if (newHandle != 0L) runCatching { NativeLlm.nativeChatTemplate(newHandle) }.getOrNull() else null
            if (newHandle != 0L) {
                Log.i(TAG, "brain loaded in ${NativeLlm.nativeStats(newHandle)[0].toInt()} ms, context $contextSize")
            }
            mutableState.value = if (newHandle != 0L) EngineState.Ready(path) else EngineState.Failed("load failed")
            newHandle != 0L
        }

    override suspend fun prefill(
        prompt: String,
        checkpoints: List<Int>,
    ): Boolean =
        withContext(dispatcher) {
            val h = handle
            if (h == 0L) return@withContext false
            NativeLlm.nativeSetCancel(h, false)
            NativeLlm.nativePrefill(h, prompt.encodeToByteArray(), byteOffsets(prompt, checkpoints))
        }

    override fun generate(
        prompt: String,
        params: SamplingParams,
        checkpoints: List<Int>,
    ): Flow<GenerationEvent> =
        callbackFlow {
            val h = handle
            if (h == 0L) {
                trySend(GenerationEvent.Done(StopReason.ERROR))
                close()
                return@callbackFlow
            }
            val me = ++started
            NativeLlm.nativeSetCancel(h, false)
            // T-088: time to the first token in logcat (`adb logcat -s HainLlm`)
            val askedAt = SystemClock.elapsedRealtime()
            var first = true
            var firstAt = 0L
            val code =
                NativeLlm.nativeGenerate(
                    h,
                    prompt.encodeToByteArray(),
                    byteOffsets(prompt, checkpoints),
                    params.toArray(),
                    params.grammar,
                ) { bytes ->
                    if (first) {
                        first = false
                        firstAt = SystemClock.elapsedRealtime() - askedAt
                        Log.i(TAG, "first token after $firstAt ms")
                    }
                    trySend(GenerationEvent.Token(bytes.decodeToString()))
                    me > cancelledUpTo && !isClosedForSend
                }
            val stats = logStats(h, firstAt)
            trySend(GenerationEvent.Done(code.toStopReason(), stats))
            close()
            awaitClose { }
        }.flowOn(dispatcher)

    override fun cancel() {
        cancelledUpTo = started
        val h = handle
        if (h != 0L) NativeLlm.nativeSetCancel(h, true)
    }

    override fun chatTemplate(): String? = template?.decodeToString()

    override fun applyTemplate(
        messages: List<Pair<String, String>>,
        addAssistant: Boolean,
    ): String? {
        val tmpl = template
        if (tmpl == null || NativeLlm.loadError != null) return null
        return runCatching {
            NativeLlm
                .nativeApplyTemplate(
                    tmpl,
                    messages.map { it.first.encodeToByteArray() }.toTypedArray(),
                    messages.map { it.second.encodeToByteArray() }.toTypedArray(),
                    addAssistant,
                )?.decodeToString()
        }.getOrNull()
    }

    override suspend fun unload() =
        withContext(dispatcher) {
            freeHandle()
            mutableState.value = EngineState.Unloaded
        }

    private fun freeHandle() {
        if (handle != 0L) {
            NativeLlm.nativeFree(handle)
            handle = 0L
            loadedContext = 0
            template = null
        }
    }

    private fun logStats(
        h: Long,
        firstTokenMs: Long,
    ): GenerationStats {
        val s = NativeLlm.nativeStats(h)
        val stats =
            GenerationStats(
                promptTokens = s[STAT_PROMPT].toInt(),
                reusedTokens = s[STAT_REUSED].toInt(),
                prefillMs = s[STAT_PREFILL_MS].toLong(),
                genTokens = s[STAT_GEN_TOKENS].toInt(),
                genMs = s[STAT_GEN_MS].toLong(),
                firstTokenMs = firstTokenMs,
            )
        Log.i(
            TAG,
            "prompt ${stats.promptTokens} tok, reused ${stats.reusedTokens}, read in ${stats.prefillMs} ms; " +
                "${stats.genTokens} tok at ${"%.1f".format(stats.tokensPerSecond)} t/s",
        )
        return stats
    }

    private fun threadCount(): Int = (Runtime.getRuntime().availableProcessors() - 2).coerceIn(2, MAX_THREADS)

    private fun Int.toStopReason(): StopReason =
        when (this) {
            NativeLlm.STOP_EOS -> StopReason.END
            NativeLlm.STOP_MAX_TOKENS -> StopReason.MAX_TOKENS
            NativeLlm.STOP_CANCELLED -> StopReason.CANCELLED
            NativeLlm.STOP_CONTEXT_FULL -> StopReason.CONTEXT_FULL
            else -> StopReason.ERROR
        }

    private companion object {
        const val TAG = "HainLlm"
        const val MAX_THREADS = 6
        const val PROGRESS_STEP = 0.02f
        const val STAT_PROMPT = 1
        const val STAT_REUSED = 2
        const val STAT_PREFILL_MS = 3
        const val STAT_GEN_TOKENS = 4
        const val STAT_GEN_MS = 5
    }
}

/** Character offsets in [text] → UTF-8 byte offsets (what the native side cuts at). */
internal fun byteOffsets(
    text: String,
    charOffsets: List<Int>,
): IntArray =
    charOffsets
        .filter { it in 1 until text.length }
        .map { text.substring(0, it).encodeToByteArray().size }
        .toIntArray()
