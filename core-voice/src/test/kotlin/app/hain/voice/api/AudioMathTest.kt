package app.hain.voice.api

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class AudioMathTest {
    private fun sine(
        freq: Double,
        rate: Int,
        seconds: Double,
        amplitude: Float = 0.5f,
    ): FloatArray = FloatArray((rate * seconds).toInt()) { (amplitude * sin(2 * PI * freq * it / rate)).toFloat() }

    private fun zeroCrossings(x: FloatArray): Int = (1 until x.size).count { (x[it - 1] < 0f) != (x[it] < 0f) }

    @Test
    fun pcm16BecomesMinusOneToOne() {
        val out = AudioMath.pcm16ToFloat(shortArrayOf(0, Short.MAX_VALUE, Short.MIN_VALUE, 16_384))
        assertEquals(0f, out[0], 0f)
        assertEquals(1f, out[1], 0.001f)
        assertEquals(-1f, out[2], 0f)
        assertEquals(0.5f, out[3], 0.001f)
    }

    @Test
    fun pcm16ReadsOnlyTheFilledPart() {
        assertEquals(2, AudioMath.pcm16ToFloat(ShortArray(10), 2).size)
    }

    @Test
    fun rmsOfSineIsAmplitudeOverRootTwo() {
        assertEquals(0.5f / 1.4142f, AudioMath.rms(sine(440.0, 16_000, 1.0)), 0.002f)
        assertEquals(0f, AudioMath.rms(FloatArray(0)), 0f)
    }

    @Test
    fun levelIsZeroForSilenceAndHighForSpeech() {
        assertEquals(0f, AudioMath.level(0f), 0f)
        assertEquals(AudioMath.FLOOR_DB, AudioMath.dbfs(0f), 0f)
        assertTrue(AudioMath.level(AudioMath.rms(sine(200.0, 16_000, 0.1, 0.3f))) > 0.6f)
        assertTrue(AudioMath.level(0.0005f) < 0.1f)
        assertEquals(1f, AudioMath.level(1f), 0f)
    }

    @Test
    fun downsampling48kKeepsLengthAndPitch() {
        val input = sine(300.0, 48_000, 1.0)
        val out = AudioMath.resample(input, 48_000, AudioMath.STT_RATE)
        assertEquals(16_000, out.size)
        // 300 Hz → 600 zero crossings per second at any rate
        assertEquals(600f, zeroCrossings(out).toFloat(), 2f)
        assertEquals(AudioMath.rms(input), AudioMath.rms(out), 0.01f)
    }

    @Test
    fun downsampling44kHasTheRightLength() {
        val out = AudioMath.resample(sine(300.0, 44_100, 2.0), 44_100, AudioMath.STT_RATE)
        assertEquals(32_000f, out.size.toFloat(), 1f)
    }

    @Test
    fun downsamplingFiltersToneAboveTheNewNyquist() {
        // 7.9 kHz survives, a 20 kHz tone (would alias) is damped a lot by the averaging
        val high = AudioMath.resample(sine(20_000.0, 48_000, 0.5), 48_000, 16_000)
        assertTrue(AudioMath.rms(high) < 0.15f)
    }

    @Test
    fun upsamplingInterpolatesLinearly() {
        val out = AudioMath.resample(floatArrayOf(0f, 1f, 0f, -1f), 8_000, 16_000)
        assertEquals(8, out.size)
        assertArrayEquals(floatArrayOf(0f, 0.5f, 1f, 0.5f, 0f, -0.5f, -1f, -1f), out, 0.0001f)
    }

    @Test
    fun sameRateCopies() {
        val input = floatArrayOf(0.1f, 0.2f)
        val out = AudioMath.resample(input, 16_000, 16_000)
        assertArrayEquals(input, out, 0f)
        assertTrue(out !== input)
    }

    @Test
    fun trimSilenceKeepsSpeechWithMargin() {
        val rate = 16_000
        val audio = FloatArray(rate * 3)
        val speech = sine(220.0, rate, 1.0, 0.3f)
        System.arraycopy(speech, 0, audio, rate, speech.size) // 1 s quiet, 1 s speech, 1 s quiet
        val out = AudioMath.trimSilence(audio, rate)
        val expected = rate + 2 * rate / 4 // speech + 250 ms margin on both sides
        assertEquals(expected.toFloat(), out.size.toFloat(), rate * 0.04f)
    }

    @Test
    fun trimSilenceOfSilenceIsEmpty() {
        assertEquals(0, AudioMath.trimSilence(FloatArray(16_000) { 0.0001f }, 16_000).size)
    }

    @Test
    fun pimPitchIsTwoSemitones() {
        assertEquals(1.1225f, PimVoice.pitch, 0.0005f)
        assertEquals(2f, AudioMath.pitchFactor(12f), 0.0001f)
        assertEquals(1f, AudioMath.pitchFactor(0f), 0f)
        assertEquals(1.15f, PimVoice.SPEED, 0f)
    }

    @Test
    fun durationIsInMilliseconds() {
        assertEquals(1500L, AudioMath.durationMs(24_000, 16_000))
    }

    @Test
    fun sampleBufferGrowsAndStopsAtTheCap() {
        val buffer = SampleBuffer(maxSamples = 50_000)
        repeat(4) { buffer.append(FloatArray(16_000) { 0.25f }) }
        assertEquals(50_000, buffer.size)
        assertTrue(buffer.full)
        assertEquals(0.25f, buffer.toArray().last(), 0f)
        buffer.clear()
        assertEquals(0, buffer.size)
    }

    @Test
    fun sampleBufferAppendsOnlyTheCount() {
        val buffer = SampleBuffer(100)
        buffer.append(floatArrayOf(1f, 2f, 3f), count = 2)
        assertArrayEquals(floatArrayOf(1f, 2f), buffer.toArray(), 0f)
        assertTrue(abs(buffer.toArray().sum() - 3f) < 0.001f)
    }
}
