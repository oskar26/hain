package app.hain.voice.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

@OptIn(ExperimentalCoroutinesApi::class)
class DictationControllerTest {
    /** A mic that plays [chunks] reads of speech (or silence) at [rate], then hangs up like a finish. */
    private class FakeMic(
        override val sampleRate: Int,
        private val chunks: Int,
        private val loud: Boolean = true,
        private val onRead: (Int) -> Unit = {},
    ) : AudioSource {
        var reads = 0
        var closed = false
        private var t = 0L

        override fun read(buffer: ShortArray): Int {
            onRead(reads)
            if (reads >= chunks) return -1
            reads++
            for (i in buffer.indices) {
                val v = if (loud) 0.3 * sin(2 * PI * 220 * (t + i) / sampleRate) else 0.0
                buffer[i] = (v * Short.MAX_VALUE).toInt().toShort()
            }
            t += buffer.size
            return buffer.size
        }

        override fun close() {
            closed = true
        }
    }

    private class FakeStt(
        private val text: String,
    ) : SpeechToText {
        var heard = 0
        var closed = false

        override fun transcribe(audio: FloatArray): String {
            heard = audio.size
            return text
        }

        override fun close() {
            closed = true
        }
    }

    private fun TestScope.controller(
        mic: () -> AudioSource?,
        stt: (String) -> SpeechToText?,
        clockStep: Long = 50L,
    ): DictationController {
        val dispatcher = StandardTestDispatcher(testScheduler)
        var now = 0L
        return DictationController(
            scope = this,
            openMic = mic,
            loadEngine = stt,
            io = dispatcher,
            compute = dispatcher,
            clock = { now.also { now += clockStep } },
            maxMs = 60_000L,
            keepEngineMs = 1_000L,
        )
    }

    @Test
    fun speechBecomesText() =
        runTest {
            val stt = FakeStt(" Hallo Pim ")
            val mic = FakeMic(48_000, chunks = 40) // 2 s at 48 kHz, then the mic ends
            val c = controller({ mic }, { lang -> stt.takeIf { lang == "de" } })
            c.start("de")
            assertTrue(c.state.value is DictationState.Recording)
            advanceUntilIdle()
            assertEquals(DictationState.Done("Hallo Pim"), c.state.value)
            assertTrue(mic.closed)
            // resampled to 16 kHz: about 2 s of audio reached the engine
            assertTrue(stt.heard in 30_000..33_000)
            c.consume()
            assertEquals(DictationState.Idle, c.state.value)
        }

    @Test
    fun finishStopsTheRecording() =
        runTest {
            lateinit var c: DictationController
            val mic = FakeMic(16_000, chunks = 10_000, onRead = { if (it == 30) c.finish() })
            c = controller({ mic }, { FakeStt("ok") })
            c.start("en")
            advanceUntilIdle()
            assertEquals(31, mic.reads) // the chunk being read when you let go still counts
            assertEquals(DictationState.Done("ok"), c.state.value)
        }

    @Test
    fun recordingShowsLevelsAndTime() =
        runTest {
            val seen = mutableListOf<DictationState>()
            lateinit var c: DictationController
            val mic = FakeMic(16_000, chunks = 100, onRead = { if (it == 10) seen += c.state.value })
            c = controller({ mic }, { FakeStt("x") })
            c.start("de")
            advanceUntilIdle()
            val rec = seen.single() as DictationState.Recording
            assertTrue(rec.level > 0.5f)
            assertTrue(rec.elapsedMs > 0)
            assertTrue(rec.levels.isNotEmpty() && rec.levels.size <= DictationController.LEVEL_HISTORY)
        }

    @Test
    fun cancelDropsEverything() =
        runTest {
            lateinit var c: DictationController
            val stt = FakeStt("nope")
            val mic = FakeMic(16_000, chunks = 10_000, onRead = { if (it == 20) c.cancel() })
            c = controller({ mic }, { stt })
            c.start("de")
            advanceUntilIdle()
            assertEquals(DictationState.Idle, c.state.value)
            assertEquals(0, stt.heard)
        }

    @Test
    fun silenceIsNoSpeech() =
        runTest {
            val c = controller({ FakeMic(16_000, chunks = 40, loud = false) }, { FakeStt("ghost") })
            c.start("de")
            advanceUntilIdle()
            assertEquals(DictationState.Failed(DictationError.NO_SPEECH), c.state.value)
        }

    @Test
    fun noMicFails() =
        runTest {
            val c = controller({ null }, { FakeStt("x") })
            c.start("de")
            advanceUntilIdle()
            assertEquals(DictationState.Failed(DictationError.NO_MIC), c.state.value)
        }

    @Test
    fun missingEngineFails() =
        runTest {
            val c = controller({ FakeMic(16_000, chunks = 40) }, { null })
            c.start("de")
            advanceUntilIdle()
            assertEquals(DictationState.Failed(DictationError.ENGINE), c.state.value)
        }

    @Test
    fun emptyTextIsNoSpeech() =
        runTest {
            val c = controller({ FakeMic(16_000, chunks = 40) }, { FakeStt("  ") })
            c.start("en")
            advanceUntilIdle()
            assertEquals(DictationState.Failed(DictationError.NO_SPEECH), c.state.value)
        }

    @Test
    fun maxLengthStopsByItself() =
        runTest {
            // the clock moves 50 ms per read; 60 s are reached after 1200 reads
            val mic = FakeMic(16_000, chunks = 100_000)
            val c = controller({ mic }, { FakeStt("lang") })
            c.start("de")
            advanceUntilIdle()
            assertTrue(mic.reads in 1_150..1_250)
            assertEquals(DictationState.Done("lang"), c.state.value)
        }

    @Test
    fun engineIsKeptForTheSameLanguageAndClosedLater() =
        runTest {
            var loads = 0
            val stt = FakeStt("a")
            val c =
                controller({ FakeMic(16_000, chunks = 40) }, {
                    loads++
                    stt
                })
            c.start("de")
            advanceUntilIdle()
            // idle time passed (advanceUntilIdle runs the delayed release too)
            assertTrue(stt.closed)
            c.consume()
            c.start("de")
            advanceUntilIdle()
            assertEquals(2, loads)
        }

    @Test
    fun secondStartWhileRecordingIsIgnored() =
        runTest {
            var opened = 0
            val c =
                controller({
                    opened++
                    FakeMic(16_000, chunks = 40)
                }, { FakeStt("a") })
            c.start("de")
            c.start("de")
            advanceUntilIdle()
            assertEquals(1, opened)
        }
}
