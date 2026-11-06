package app.hain.feature.chat.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-296: the bars of the dictation pill follow the microphone level, tallest in the middle. */
class DictationPillTest {
    @Test
    fun silenceGivesOnlyTheDottedLine() {
        assertTrue(pillBarHeights(emptyList(), 15).all { it == 0f })
        assertTrue(pillBarHeights(listOf(0f, 0f, 0f), 15).all { it == 0f })
    }

    @Test
    fun middleBarIsTheTallest() {
        val h = pillBarHeights(List(10) { 0.8f }, 15)
        assertEquals(15, h.size)
        val mid = h[7]
        assertTrue(h.all { it <= mid })
        assertTrue(h.first() < mid && h.last() < mid)
        assertEquals(h.first(), h.last(), 1e-4f)
    }

    @Test
    fun louderVoiceGivesTallerBars() {
        val quiet = pillBarHeights(List(10) { 0.2f }, 15)
        val loud = pillBarHeights(List(10) { 0.9f }, 15)
        assertTrue(loud[7] > quiet[7])
        assertTrue(loud.sum() > quiet.sum())
    }

    @Test
    fun heightsStayInRange() {
        val h = pillBarHeights(listOf(2f, -1f, 5f, 1.5f), 9)
        assertTrue(h.all { it in 0f..1f })
    }

    @Test
    fun reducedMotionKeepsOneSteadyShape() {
        val a = pillBarHeights(listOf(0.1f), 15, steady = true)
        val b = pillBarHeights(listOf(0.9f), 15, steady = true)
        assertEquals(a, b)
        assertTrue(a[7] > a[0])
    }

    @Test
    fun noBarsForZeroCount() {
        assertTrue(pillBarHeights(listOf(1f), 0).isEmpty())
    }
}
