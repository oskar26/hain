package app.hain.ui.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordFadeInTest {
    @Test
    fun `no animation when text did not grow`() {
        assertTrue(animatedWordIndices(5, 5).isEmpty())
        assertTrue(animatedWordIndices(5, 3).isEmpty())
    }

    @Test
    fun `appended words animate`() {
        // 3 existing words, one appended → only index 3 animates
        assertEquals(3..3, animatedWordIndices(3, 4))
    }

    @Test
    fun `burst larger than 30 animates only the last 30`() {
        val range = animatedWordIndices(previousWordCount = 0, currentWordCount = 100)
        assertEquals(70..99, range)
        assertEquals(30, range.count())
    }

    @Test
    fun `burst of exactly 30 animates all of them`() {
        assertEquals(0..29, animatedWordIndices(0, 30))
    }

    @Test
    fun `custom max in flight is honoured`() {
        assertEquals(8..9, animatedWordIndices(7, 10, maxInFlight = 2))
    }
}
