package app.hain.voice.internal

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import app.hain.voice.api.AudioMath
import app.hain.voice.api.AudioSource

/**
 * The microphone through [AudioRecord] (T-140): 16 kHz mono when the phone offers it (no resampling
 * needed), otherwise 48 or 44.1 kHz, which the dictation resamples. VOICE_RECOGNITION turns off the
 * phone's own effects that would colour the speech.
 */
class MicSource private constructor(
    private val record: AudioRecord,
    override val sampleRate: Int,
) : AudioSource {
    override fun read(buffer: ShortArray): Int = record.read(buffer, 0, buffer.size)

    override fun close() {
        runCatching { record.stop() }
        record.release()
    }

    companion object {
        private val RATES = intArrayOf(AudioMath.STT_RATE, 48_000, 44_100)
        private const val BUFFER_FACTOR = 4
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT

        /** Opens and starts the mic; `null` without the permission or when no rate works. */
        @SuppressLint("MissingPermission") // the caller checks RECORD_AUDIO first
        @Suppress("LoopWithTooManyJumpStatements") // try the next rate on every failure
        fun open(): MicSource? {
            for (rate in RATES) {
                val min = AudioRecord.getMinBufferSize(rate, CHANNEL, ENCODING)
                if (min <= 0) continue
                val record =
                    runCatching {
                        AudioRecord(
                            MediaRecorder.AudioSource.VOICE_RECOGNITION,
                            rate,
                            CHANNEL,
                            ENCODING,
                            min * BUFFER_FACTOR,
                        )
                    }.getOrNull() ?: continue
                if (record.state != AudioRecord.STATE_INITIALIZED) {
                    record.release()
                    continue
                }
                val started = runCatching { record.startRecording() }.isSuccess
                val running = started && record.recordingState == AudioRecord.RECORDSTATE_RECORDING
                if (running) return MicSource(record, rate)
                record.release()
            }
            return null
        }
    }
}
