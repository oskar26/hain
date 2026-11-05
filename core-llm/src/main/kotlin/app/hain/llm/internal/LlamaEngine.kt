package app.hain.llm.internal

import android.content.Context
import android.os.SystemClock
import android.util.Log
import app.hain.llm.api.BrainFormat
import app.hain.llm.api.EngineState
import app.hain.llm.api.GenerationEvent
import app.hain.llm.api.GenerationStats
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.SamplingParams
import app.hain.llm.api.StopReason
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.buffer
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

    /** T-241: chat format from the GGUF architecture, read once after loading. */
    @Volatile private var loadedFormat = BrainFormat.LFM

    override val format: BrainFormat get() = loadedFormat

    // Each generation gets a number; cancel() stops every generation up to the current one. A plain
    // flag could be reset by the end of the previous generation and swallow a stop.
    @Volatile private var started = 0

    @Volatile private var cancelledUpTo = 0
    private var initialized = false

    /**
     * T-133: the card grammar from :core-cards (`assets/card.gbnf`, merged into the app's assets). The
     * native loop switches it on only after a line "```card" and off after the closing "```"; `null`
     * when the file is missing, then cards are read leniently as before.
     */
    private val cardGrammar: String? by lazy {
        runCatching { context.assets.open(CARD_GRAMMAR).use { it.readBytes().decodeToString() } }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
    }

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
            if (newHandle != 0L) afterLoad(newHandle, contextSize) else template = null
            mutableState.value = if (newHandle != 0L) EngineState.Ready(path) else EngineState.Failed("load failed")
            newHandle != 0L
        }

    /** Reads template and format of the new model and switches on the disk snapshots (T-211, T-241, T-242). */
    private fun afterLoad(
        h: Long,
        contextSize: Int,
    ) {
        template = runCatching { NativeLlm.nativeChatTemplate(h) }.getOrNull()
        stateDir()?.let { NativeLlm.nativeSetStateDir(h, it) }
        val arch = runCatching { NativeLlm.nativeMeta(h, ARCH_KEY) }.getOrNull()
        // T-260: LFM3-Omni has the LFM3 architecture; its name tells it apart (no thinking, near-greedy)
        val name = runCatching { NativeLlm.nativeMeta(h, NAME_KEY) }.getOrNull()
        loadedFormat = BrainFormat.of(arch?.decodeToString(), name?.decodeToString())
        Log.i(
            TAG,
            "brain loaded in ${NativeLlm.nativeStats(h)[0].toInt()} ms, context $contextSize, " +
                "format ${loadedFormat.name.lowercase()}",
        )
    }

    override suspend fun prefill(
        prompt: String,
        checkpoints: List<Int>,
    ): Boolean =
        withContext(dispatcher) {
            val h = handle
            if (h == 0L) return@withContext false
            NativeLlm.nativeSetCancel(h, false)
            val startedAt = SystemClock.elapsedRealtime()
            val ok = NativeLlm.nativePrefill(h, prompt.encodeToByteArray(), byteOffsets(prompt, checkpoints))
            // T-242: how long the warm-up took and whether a snapshot from disk saved the reading
            val s = NativeLlm.nativeStats(h)
            Log.i(
                TAG,
                "warm-up ${SystemClock.elapsedRealtime() - startedAt} ms, prompt ${s[STAT_PROMPT].toInt()} tok, " +
                    "reused ${s[STAT_REUSED].toInt()}${restoredNote(s)}",
            )
            ok
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
                    // T-133 cards only in free text; a whole-answer grammar (T-114 memory JSON) wins
                    if (params.grammar == null) cardGrammar else null,
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
        }
            // T-264: no token may get lost; the default buffer of 64 dropped tokens when the UI lagged behind
            .buffer(Channel.UNLIMITED)
            .flowOn(dispatcher)

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
            loadedFormat = BrainFormat.LFM
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
                "${stats.genTokens} tok at ${"%.1f".format(stats.tokensPerSecond)} t/s; " +
                "first token after $firstTokenMs ms${restoredNote(s)}",
        )
        return stats
    }

    /** T-242: snapshots of the system prompt live in the app's cache (Android may clear it; then it is rebuilt). */
    private fun stateDir(): String? =
        runCatching { java.io.File(context.cacheDir, STATE_DIR).apply { mkdirs() } }
            .getOrNull()
            ?.takeIf { it.isDirectory }
            ?.absolutePath

    /** T-242: ", snapshot from disk in N ms" when the last call loaded one, else empty. */
    private fun restoredNote(stats: FloatArray): String =
        stats
            .getOrNull(STAT_RESTORED_MS)
            ?.takeIf { it >= 0f }
            ?.let { ", snapshot from disk in ${it.toInt()} ms" }
            .orEmpty()

    /** T-264: only the performance cores (CpuCores); logged once per load. */
    private fun threadCount(): Int {
        val threads = CpuCores.threads(CpuCores.read())
        Log.i(TAG, "brain threads: $threads (performance cores)")
        return threads
    }

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
        const val CARD_GRAMMAR = "card.gbnf"
        const val PROGRESS_STEP = 0.02f
        const val STAT_PROMPT = 1
        const val STAT_REUSED = 2
        const val STAT_PREFILL_MS = 3
        const val STAT_GEN_TOKENS = 4
        const val STAT_GEN_MS = 5
        const val STAT_RESTORED_MS = 6
        const val STATE_DIR = "llm-state"
        const val ARCH_KEY = "general.architecture"
        const val NAME_KEY = "general.name"
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
