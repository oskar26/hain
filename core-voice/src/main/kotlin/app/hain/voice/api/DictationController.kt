package app.hain.voice.api

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One dictation at a time (T-140): records from [openMic] while the mic button is held or until it
 * is tapped again, shows the level, then lets Moonshine write the text. The engine loads while you
 * speak and stays loaded for [keepEngineMs] after the last use, then it is closed to free memory.
 * Audio never leaves the phone and is dropped as soon as the text is there.
 */
@Suppress("LongParameterList") // injectable dispatchers and clock for the unit tests
class DictationController(
    private val scope: CoroutineScope,
    private val openMic: () -> AudioSource?,
    private val loadEngine: (language: String) -> SpeechToText?,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val compute: CoroutineDispatcher = Dispatchers.Default,
    private val clock: () -> Long = System::currentTimeMillis,
    private val maxMs: Long = MAX_MS,
    private val keepEngineMs: Long = KEEP_ENGINE_MS,
) {
    private val mutableState = MutableStateFlow<DictationState>(DictationState.Idle)
    val state: StateFlow<DictationState> = mutableState.asStateFlow()

    private val stopRequested = AtomicBoolean(false)
    private val cancelled = AtomicBoolean(false)
    private var job: Job? = null
    private var releaseJob: Job? = null
    private val engineLock = Mutex()
    private var engine: Pair<String, SpeechToText>? = null

    val busy: Boolean
        get() = state.value is DictationState.Recording || state.value is DictationState.Transcribing

    /** Starts recording in [language] (`de`, `en`); does nothing while a dictation runs. */
    fun start(language: String) {
        if (busy || job?.isActive == true) return
        stopRequested.set(false)
        cancelled.set(false)
        releaseJob?.cancel()
        mutableState.value = DictationState.Recording(0f, 0L)
        // the engine loads while you speak, so the text is there sooner afterwards
        val warm = scope.launch(compute) { engineFor(language) }
        job =
            scope.launch(io) {
                val audio = record() ?: return@launch
                if (cancelled.get()) {
                    mutableState.value = DictationState.Idle
                    return@launch
                }
                mutableState.value = DictationState.Transcribing
                warm.join()
                mutableState.value = transcribe(language, audio)
                scheduleRelease()
            }
    }

    /** Stops recording and writes the text (tap again, or let go of the button). */
    fun finish() {
        stopRequested.set(true)
    }

    /** Drops the recording; nothing lands in the message box. */
    fun cancel() {
        cancelled.set(true)
        stopRequested.set(true)
        if (state.value is DictationState.Transcribing) mutableState.value = DictationState.Idle
    }

    /** The composer took the text (or showed the error): back to the mic button. */
    fun consume() {
        val now = state.value
        if (now is DictationState.Done || now is DictationState.Failed) mutableState.value = DictationState.Idle
    }

    /** Frees the engine right away (app goes to the background, low memory). */
    fun release() {
        cancel()
        scope.launch(compute) { closeEngine() }
    }

    /** Returns 16 kHz samples with the silence cut off, or `null` when it already set a final state. */
    @Suppress("CyclomaticComplexMethod", "ReturnCount", "LoopWithTooManyJumpStatements") // the mic loop
    private fun record(): FloatArray? {
        val mic = runCatching { openMic() }.getOrNull()
        if (mic == null) {
            mutableState.value = DictationState.Failed(DictationError.NO_MIC)
            return null
        }
        val rate = mic.sampleRate
        val samples = SampleBuffer((rate * maxMs / MS_PER_S).toInt())
        val chunk = ShortArray(rate / CHUNKS_PER_S)
        val levels = ArrayDeque<Float>()
        val started = clock()
        var failed = false
        mic.use {
            while (!stopRequested.get()) {
                val n = it.read(chunk)
                if (n < 0) {
                    failed = true
                    break
                }
                if (n == 0) continue
                val floats = AudioMath.pcm16ToFloat(chunk, n)
                samples.append(floats)
                val level = AudioMath.level(AudioMath.rms(floats))
                levels.addLast(level)
                while (levels.size > LEVEL_HISTORY) levels.removeFirst()
                val elapsed = clock() - started
                if (!cancelled.get()) mutableState.value = DictationState.Recording(level, elapsed, levels.toList())
                if (elapsed >= maxMs || samples.full) break
            }
        }
        if (cancelled.get()) return FloatArray(0)
        if (failed && samples.size == 0) {
            mutableState.value = DictationState.Failed(DictationError.NO_MIC)
            return null
        }
        val audio = AudioMath.resample(samples.toArray(), rate, AudioMath.STT_RATE)
        return AudioMath.trimSilence(audio, AudioMath.STT_RATE)
    }

    @Suppress("ReturnCount") // one early answer per way it can fail
    private suspend fun transcribe(
        language: String,
        audio: FloatArray,
    ): DictationState {
        if (audio.size < MIN_SPEECH_SAMPLES) return DictationState.Failed(DictationError.NO_SPEECH)
        val stt = engineFor(language) ?: return DictationState.Failed(DictationError.ENGINE)
        val text =
            withContext(compute) {
                runCatching { stt.transcribe(audio) }.getOrNull()
            } ?: return DictationState.Failed(DictationError.ENGINE)
        return when {
            cancelled.get() -> DictationState.Idle
            text.isBlank() -> DictationState.Failed(DictationError.NO_SPEECH)
            else -> DictationState.Done(text.trim())
        }
    }

    private suspend fun engineFor(language: String): SpeechToText? =
        engineLock.withLock {
            val cached = engine
            if (cached != null && cached.first == language) return@withLock cached.second
            cached?.second?.let { runCatching { it.close() } }
            engine = null
            val loaded = runCatching { loadEngine(language) }.getOrNull() ?: return@withLock null
            engine = language to loaded
            loaded
        }

    private suspend fun closeEngine() {
        engineLock.withLock {
            engine?.second?.let { runCatching { it.close() } }
            engine = null
        }
    }

    private fun scheduleRelease() {
        releaseJob?.cancel()
        releaseJob =
            scope.launch(compute) {
                delay(keepEngineMs)
                if (!busy) closeEngine()
            }
    }

    companion object {
        /** One dictation is at most a minute (Moonshine handles up to 82 s per segment). */
        const val MAX_MS = 60_000L
        const val KEEP_ENGINE_MS = 90_000L
        const val LEVEL_HISTORY = 24
        private const val CHUNKS_PER_S = 20
        private const val MS_PER_S = 1000L

        /** Under 0.6 s (with the 0.25 s margins of the silence cut) is a click or a tap, not speech. */
        private const val MIN_SPEECH_SAMPLES = AudioMath.STT_RATE * 6 / 10
    }
}
