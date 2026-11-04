package app.hain.voice.api

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Small, pure audio helpers for dictation (VOICE.md, T-140): 16-bit PCM to float, loudness for the
 * level meter, resampling to the 16 kHz Moonshine wants, and cutting off silence at both ends.
 */
object AudioMath {
    /** Moonshine listens at 16 kHz mono. */
    const val STT_RATE = 16_000

    /** Converts [count] 16-bit samples to floats in -1..1. */
    fun pcm16ToFloat(
        pcm: ShortArray,
        count: Int = pcm.size,
    ): FloatArray = FloatArray(count) { pcm[it] / PCM_SCALE }

    /** Root mean square of `samples[from until to]` (0 for an empty range). */
    fun rms(
        samples: FloatArray,
        from: Int = 0,
        to: Int = samples.size,
    ): Float {
        if (to <= from) return 0f
        var sum = 0.0
        for (i in from until to) sum += samples[i] * samples[i]
        return sqrt(sum / (to - from)).toFloat()
    }

    /** Loudness as dBFS (full scale = 0 dB); silence is [FLOOR_DB]. */
    fun dbfs(rms: Float): Float {
        if (rms <= 0f) return FLOOR_DB
        val db = (DB_FACTOR * log10(rms.toDouble())).toFloat()
        return max(FLOOR_DB, db)
    }

    /** Level for the meter, 0..1: quiet room ≈ 0, normal speech ≈ 0.5–0.9. */
    fun level(rms: Float): Float = ((dbfs(rms) - METER_LOW_DB) / (METER_HIGH_DB - METER_LOW_DB)).coerceIn(0f, 1f)

    /**
     * Resamples [input] from [fromRate] to [toRate] with linear interpolation. When going down it first
     * averages over the source span of one output sample (a simple box low-pass) so speech does not
     * get aliasing hiss; that is plenty for speech recognition.
     */
    fun resample(
        input: FloatArray,
        fromRate: Int,
        toRate: Int,
    ): FloatArray {
        require(fromRate > 0 && toRate > 0) { "rates must be positive" }
        if (fromRate == toRate || input.isEmpty()) return input.copyOf()
        val ratio = fromRate.toDouble() / toRate
        val outSize = max(1, floor(input.size / ratio).toInt())
        val out = FloatArray(outSize)
        if (ratio > 1.0) {
            // down: average the source samples that fall into each output slot
            for (i in 0 until outSize) {
                val start = (i * ratio).toInt()
                val end = min(input.size, max(start + 1, ceil((i + 1) * ratio).toInt()))
                var sum = 0f
                for (j in start until end) sum += input[j]
                out[i] = sum / (end - start)
            }
        } else {
            for (i in 0 until outSize) {
                val pos = i * ratio
                val left = pos.toInt().coerceAtMost(input.lastIndex)
                val right = (left + 1).coerceAtMost(input.lastIndex)
                val frac = (pos - left).toFloat()
                out[i] = input[left] * (1f - frac) + input[right] * frac
            }
        }
        return out
    }

    /**
     * Cuts silence at the start and the end: everything before the first and after the last
     * [windowMs] window louder than [thresholdDb], keeping [marginMs] around the speech. Returns an
     * empty array when nothing is loud enough (the mic heard no speech).
     */
    fun trimSilence(
        samples: FloatArray,
        rate: Int,
        thresholdDb: Float = SPEECH_DB,
        windowMs: Int = WINDOW_MS,
        marginMs: Int = MARGIN_MS,
    ): FloatArray {
        val window = max(1, rate * windowMs / MS_PER_S)
        var first = -1
        var last = -1
        var start = 0
        while (start < samples.size) {
            val end = min(samples.size, start + window)
            if (dbfs(rms(samples, start, end)) >= thresholdDb) {
                if (first < 0) first = start
                last = end
            }
            start = end
        }
        if (first < 0) return FloatArray(0)
        val margin = rate * marginMs / MS_PER_S
        return samples.copyOfRange(max(0, first - margin), min(samples.size, last + margin))
    }

    /** Duration of [count] samples at [rate] in milliseconds. */
    fun durationMs(
        count: Int,
        rate: Int,
    ): Long = count * MS_PER_S.toLong() / rate

    /** Playback pitch factor for a shift of [semitones] (VOICE.md: Pim = +2 st ≈ 1.1225). */
    fun pitchFactor(semitones: Float): Float = Math.pow(2.0, semitones / SEMITONES_PER_OCTAVE).toFloat()

    const val FLOOR_DB = -90f
    const val SPEECH_DB = -42f
    private const val METER_LOW_DB = -55f
    private const val METER_HIGH_DB = -12f
    private const val PCM_SCALE = 32_768f
    private const val DB_FACTOR = 20.0
    private const val WINDOW_MS = 30
    private const val MARGIN_MS = 250
    private const val MS_PER_S = 1000
    private const val SEMITONES_PER_OCTAVE = 12.0
}

/**
 * Growing buffer for the samples of one dictation, capped at [maxSamples] (the rest is dropped and
 * [full] turns true, so the recorder can stop by itself).
 */
class SampleBuffer(
    private val maxSamples: Int,
) {
    private var data = FloatArray(INITIAL)
    var size: Int = 0
        private set

    val full: Boolean get() = size >= maxSamples

    fun append(
        samples: FloatArray,
        count: Int = samples.size,
    ) {
        val n = min(count, maxSamples - size)
        if (n <= 0) return
        if (size + n > data.size) data = data.copyOf(max(size + n, min(maxSamples, data.size * 2)))
        System.arraycopy(samples, 0, data, size, n)
        size += n
    }

    fun toArray(): FloatArray = data.copyOf(size)

    fun clear() {
        size = 0
    }

    private companion object {
        const val INITIAL = 16_000
    }
}
