package app.hain.ui.motion

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MotionGovernorTest {
    private fun governor() = MotionGovernor()

    private fun ms(n: Long) = n * 1_000_000L

    @Test
    fun `smooth frames keep blur enabled`() {
        val g = governor()
        g.onFrame(0)
        repeat(50) { g.onFrame(ms(10L)) }
        assertTrue(g.blurEnabled)
    }

    @Test
    fun `one slow frame does not disable blur`() {
        val g = governor()
        g.onFrame(0)
        g.onFrame(ms(30L)) // slow
        g.onFrame(ms(10L)) // fast again → streak resets
        g.onFrame(ms(30L)) // slow, streak only 1
        assertTrue(g.blurEnabled)
    }

    @Test
    fun `two slow frames in a row disable blur`() {
        val g = governor()
        g.onFrame(0)
        g.onFrame(ms(20L)) // slow #1 (>16 ms)
        g.onFrame(ms(40L)) // slow #2 → off
        assertFalse(g.blurEnabled)
    }

    @Test
    fun `blur stays off for the session once disabled`() {
        val g = governor()
        g.onFrame(0)
        g.onFrame(ms(20L))
        g.onFrame(ms(40L))
        var t = 40L
        repeat(100) {
            t += 5
            g.onFrame(ms(t))
        }
        assertFalse(g.blurEnabled)
    }

    @Test
    fun `exactly 16ms frames are within budget`() {
        val g = governor()
        g.onFrame(0)
        repeat(10) { g.onFrame(ms(16L)) }
        assertTrue(g.blurEnabled) // 16 ms is not > 16 ms
    }
}
