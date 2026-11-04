package app.hain.voice.api

import android.content.Context
import app.hain.voice.internal.MicSource
import app.hain.voice.internal.MoonshineStt
import app.hain.voice.internal.SystemSpeech
import java.io.File

/** The Android engines behind the voice interfaces (T-140/T-141). */
object VoiceEngines {
    /** The microphone, or `null` when it cannot open (no permission, busy). */
    fun microphone(): AudioSource? = MicSource.open()

    /** Moonshine from an installed pack folder, or `null` when files are missing or broken. */
    fun moonshine(dir: File): SpeechToText? {
        val names = dir.list()?.toSet().orEmpty()
        if (!VoicePacks.isComplete(names)) return null
        return runCatching { MoonshineStt(dir) }.getOrNull()
    }

    /**
     * Read aloud with the phone's voice in Pim's pitch and speed (Pocket TTS follows in T-143). The
     * system engine only starts on the first "Vorlesen", not with the app.
     */
    fun systemSpeech(context: Context): SpeechSynth = LazySynth { SystemSpeech(context) }
}

/** Creates the real engine on first use and forwards everything to it. */
internal class LazySynth(
    private val create: () -> SpeechSynth,
) : SpeechSynth {
    private var engine: SpeechSynth? = null

    override var listener: SynthListener? = null
        set(value) {
            field = value
            engine?.listener = value
        }

    private fun engine(): SpeechSynth =
        engine ?: create().also {
            it.listener = listener
            engine = it
        }

    override suspend fun prepare(language: String): Boolean = engine().prepare(language)

    override fun enqueue(
        id: String,
        text: String,
    ) = engine().enqueue(id, text)

    override fun stop() {
        engine?.stop()
    }

    override fun release() {
        engine?.release()
        engine = null
    }
}
