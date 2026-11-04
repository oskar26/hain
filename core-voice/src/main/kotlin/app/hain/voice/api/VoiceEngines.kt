package app.hain.voice.api

import app.hain.voice.internal.MicSource
import app.hain.voice.internal.MoonshineStt
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
}
