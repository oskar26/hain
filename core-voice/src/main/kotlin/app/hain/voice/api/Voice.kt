package app.hain.voice.api

/**
 * Voice packs and Pim's voice (VOICE.md, T-140/T-141). The packs are entries of `catalog.json`
 * (kind `stt`); they are downloaded on demand and never ship inside the APK.
 */
object VoicePacks {
    const val KIND_STT = "stt"
    const val STT_DE = "moonshine-small-de"
    const val STT_EN = "moonshine-small-en"

    /** Files of one Moonshine Streaming pack (the five ONNX graphs plus tokenizer and config). */
    val MOONSHINE_FILES =
        listOf(
            "frontend.onnx",
            "encoder_int8.onnx",
            "adapter_int8.onnx",
            "cross_kv_int8.onnx",
            "decoder_kv_int8.onnx",
            "tokenizer.json",
            "streaming_config.json",
        )

    /** Dictation pack for a language tag: German for `de*`, English for everything else. */
    fun sttPackId(language: String): String = if (language.lowercase().startsWith("de")) STT_DE else STT_EN

    /** `true` when every file of a Moonshine pack is present in [fileNames]. */
    fun isComplete(fileNames: Collection<String>): Boolean = MOONSHINE_FILES.all { it in fileNames }
}

/** Pim's voice, the same in every language (VOICE.md "Pim voice"): +2 semitones, speed 1.15. */
object PimVoice {
    const val SEMITONES = 2f
    const val SPEED = 1.15f

    /** ≈ 1.1225: playback pitch for +2 semitones. */
    val pitch: Float get() = AudioMath.pitchFactor(SEMITONES)
}

/** Where a dictation stands; the composer shows this (mic button, level meter, "Pim schreibt mit"). */
sealed interface DictationState {
    data object Idle : DictationState

    /** Recording; [level] 0..1 drives the meter, [elapsedMs] the clock. */
    data class Recording(
        val level: Float,
        val elapsedMs: Long,
        val levels: List<Float> = emptyList(),
    ) : DictationState

    /** Recording stopped, Moonshine turns the speech into text. */
    data object Transcribing : DictationState

    /** The text that goes into the message box (editable there). */
    data class Done(
        val text: String,
    ) : DictationState

    data class Failed(
        val reason: DictationError,
    ) : DictationState
}

enum class DictationError { NO_MIC, NO_SPEECH, ENGINE }

/** Speech to text. [transcribe] gets 16 kHz mono floats and blocks while it works. */
interface SpeechToText : AutoCloseable {
    fun transcribe(audio: FloatArray): String
}

/** A microphone: 16-bit PCM at [sampleRate]. */
interface AudioSource : AutoCloseable {
    val sampleRate: Int

    /** Reads up to `buffer.size` samples; returns the count, or a negative number on an error. */
    fun read(buffer: ShortArray): Int
}

/** Text to speech engine: the system voice now, Pocket TTS (Pim's own voice) later (T-143). */
interface SpeechSynth {
    /** Prepares the voice for [language] (a tag like `de` or `en`); `false` when it cannot speak it. */
    suspend fun prepare(language: String): Boolean

    /** Queues one sentence; [listener] hears when it is done. Engines may synthesize ahead. */
    fun enqueue(
        id: String,
        text: String,
    )

    /** Stops speaking at once and drops the queue. */
    fun stop()

    var listener: SynthListener?

    fun release()
}

interface SynthListener {
    fun onDone(id: String)

    fun onError(id: String)
}
