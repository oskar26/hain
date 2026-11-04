package app.hain.voice.api

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ReadAloudControllerTest {
    private class FakeSynth(
        private val speaks: Boolean = true,
    ) : SpeechSynth {
        val queued = mutableListOf<Pair<String, String>>()
        var stops = 0
        var released = false
        var language: String? = null
        override var listener: SynthListener? = null

        override suspend fun prepare(language: String): Boolean {
            this.language = language
            return speaks
        }

        override fun enqueue(
            id: String,
            text: String,
        ) {
            queued += id to text
        }

        override fun stop() {
            stops++
        }

        override fun release() {
            released = true
        }

        fun finish(index: Int) = listener!!.onDone(queued[index].first)

        fun fail(index: Int) = listener!!.onError(queued[index].first)
    }

    private val labels = SpeechLabels("Codeblock übersprungen", "Formel übersprungen")
    private val answer = "**Eins.** Zwei!\n\n```\ncode\n```\n\nDrei? Vier."

    @Test
    fun sentencesGoOutTwoAheadAndInOrder() =
        runTest {
            val synth = FakeSynth()
            val c = ReadAloudController(backgroundScope, synth)
            c.speak("m1", answer, "de", labels)
            runCurrent()
            assertEquals("de", synth.language)
            assertEquals(listOf("Eins.", "Zwei!"), synth.queued.map { it.second })
            synth.finish(0)
            runCurrent()
            assertEquals("Codeblock übersprungen.", synth.queued[2].second)
            assertEquals(ReadAloudState.Speaking("m1", 1, 5), c.state.value)
            synth.finish(1)
            synth.finish(2)
            runCurrent()
            synth.finish(3)
            synth.finish(4)
            runCurrent()
            val spoken = synth.queued.map { it.second }
            assertEquals(listOf("Eins.", "Zwei!", "Codeblock übersprungen.", "Drei?", "Vier."), spoken)
            assertEquals(ReadAloudState.Idle, c.state.value)
        }

    @Test
    fun tappingTheSameAnswerStops() =
        runTest {
            val synth = FakeSynth()
            val c = ReadAloudController(backgroundScope, synth)
            c.toggle("m1", answer, "de", labels)
            runCurrent()
            assertTrue(c.isSpeaking("m1"))
            c.toggle("m1", answer, "de", labels)
            runCurrent()
            assertEquals(ReadAloudState.Idle, c.state.value)
            assertTrue(synth.stops >= 2)
        }

    @Test
    fun anotherAnswerReplacesTheFirst() =
        runTest {
            val synth = FakeSynth()
            val c = ReadAloudController(backgroundScope, synth)
            c.toggle("m1", answer, "de", labels)
            runCurrent()
            c.toggle("m2", "Hello there. Bye.", "en", labels)
            runCurrent()
            assertTrue(c.isSpeaking("m2"))
            assertEquals("en", synth.language)
            // a late "done" of the first answer does not move the second one
            synth.finish(0)
            runCurrent()
            assertEquals(ReadAloudState.Speaking("m2", 0, 2), c.state.value)
        }

    @Test
    fun missingVoiceFails() =
        runTest {
            val c = ReadAloudController(backgroundScope, FakeSynth(speaks = false))
            c.speak("m1", answer, "fr", labels)
            runCurrent()
            assertEquals(ReadAloudState.Failed("m1"), c.state.value)
            c.consume()
            assertEquals(ReadAloudState.Idle, c.state.value)
        }

    @Test
    fun engineErrorStopsAndFails() =
        runTest {
            val synth = FakeSynth()
            val c = ReadAloudController(backgroundScope, synth)
            c.speak("m1", answer, "de", labels)
            runCurrent()
            synth.fail(0)
            runCurrent()
            assertEquals(ReadAloudState.Failed("m1"), c.state.value)
        }

    @Test
    fun nothingToSayStaysIdle() =
        runTest {
            val synth = FakeSynth()
            val c = ReadAloudController(backgroundScope, synth)
            c.speak("m1", "---", "de", labels)
            runCurrent()
            assertEquals(ReadAloudState.Idle, c.state.value)
            assertTrue(synth.queued.isEmpty())
        }

    @Test
    fun releaseFreesTheEngine() =
        runTest {
            val synth = FakeSynth()
            val c = ReadAloudController(backgroundScope, synth)
            c.release()
            assertTrue(synth.released)
        }
}

class LazySynthTest {
    @Test
    fun engineStartsOnlyOnFirstUseAndGetsTheListener() =
        runTest {
            var created = 0
            var inner: SpeechSynth? = null
            val lazy =
                LazySynth {
                    created++
                    object : SpeechSynth {
                        override var listener: SynthListener? = null

                        override suspend fun prepare(language: String) = true

                        override fun enqueue(
                            id: String,
                            text: String,
                        ) = Unit

                        override fun stop() = Unit

                        override fun release() = Unit
                    }.also { inner = it }
                }
            val listener =
                object : SynthListener {
                    override fun onDone(id: String) = Unit

                    override fun onError(id: String) = Unit
                }
            lazy.listener = listener
            lazy.stop()
            assertEquals(0, created)
            assertTrue(lazy.prepare("de"))
            assertEquals(1, created)
            assertTrue(inner?.listener === listener)
            lazy.enqueue("a", "b")
            assertEquals(1, created)
        }
}
