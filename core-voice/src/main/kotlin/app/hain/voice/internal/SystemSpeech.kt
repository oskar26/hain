package app.hain.voice.internal

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import app.hain.voice.api.PimVoice
import app.hain.voice.api.SpeechSynth
import app.hain.voice.api.SynthListener
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Read aloud with the phone's own voice (T-141, base engine until Pocket TTS lands, T-143). Pim's
 * settings apply here too: pitch +2 semitones and speed 1.15 (VOICE.md). The engine queues the
 * sentences (QUEUE_ADD) and prepares the next one while the current one plays.
 */
class SystemSpeech(
    context: Context,
) : SpeechSynth {
    private val ready = CompletableDeferred<Boolean>()
    private val tts: TextToSpeech =
        TextToSpeech(context.applicationContext) { status -> ready.complete(status == TextToSpeech.SUCCESS) }

    override var listener: SynthListener? = null

    init {
        tts.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit

                override fun onDone(utteranceId: String?) {
                    utteranceId?.let { listener?.onDone(it) }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    utteranceId?.let { listener?.onError(it) }
                }

                override fun onError(
                    utteranceId: String?,
                    errorCode: Int,
                ) {
                    utteranceId?.let { listener?.onError(it) }
                }

                override fun onStop(
                    utteranceId: String?,
                    interrupted: Boolean,
                ) = Unit
            },
        )
    }

    @Suppress("ReturnCount") // not ready, language missing, ready
    override suspend fun prepare(language: String): Boolean {
        val ok = withTimeoutOrNull(INIT_TIMEOUT_MS) { ready.await() } ?: false
        if (!ok) return false
        val result = tts.setLanguage(Locale.forLanguageTag(language))
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) return false
        tts.setPitch(PimVoice.pitch)
        tts.setSpeechRate(PimVoice.SPEED)
        return true
    }

    override fun enqueue(
        id: String,
        text: String,
    ) {
        val result = tts.speak(text.take(TextToSpeech.getMaxSpeechInputLength()), TextToSpeech.QUEUE_ADD, null, id)
        if (result != TextToSpeech.SUCCESS) listener?.onError(id)
    }

    override fun stop() {
        runCatching { tts.stop() }
    }

    override fun release() {
        runCatching {
            tts.stop()
            tts.shutdown()
        }
    }

    private companion object {
        const val INIT_TIMEOUT_MS = 5_000L
    }
}
