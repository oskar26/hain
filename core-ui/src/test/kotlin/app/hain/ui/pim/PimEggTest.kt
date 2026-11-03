package app.hain.ui.pim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-097: the easter egg only starts after 5 s of pressing and Pim grows to at most 5×. */
class PimEggTest {
    @Test
    fun `the game opens only after at least five seconds of pressing`() {
        assertTrue(PimEgg.HOLD_MS >= 5_000L)
        assertFalse(PimEgg.shouldBurst(0L))
        assertFalse(PimEgg.shouldBurst(2_500L))
        assertFalse(PimEgg.shouldBurst(4_999L))
        assertTrue(PimEgg.shouldBurst(5_000L))
        assertTrue(PimEgg.shouldBurst(7_000L))
    }

    @Test
    fun `charging reaches full exactly at the threshold`() {
        assertEquals(0f, PimEgg.progress(0L), 0f)
        assertEquals(0f, PimEgg.progress(PimEgg.ARM_MS), 0f)
        assertTrue(PimEgg.progress(4_000L) < 1f)
        assertEquals(1f, PimEgg.progress(PimEgg.HOLD_MS), 0.0001f)
        assertEquals(PimEgg.HOLD_MS - PimEgg.ARM_MS, PimEgg.CHARGE_MS.toLong())
    }

    @Test
    fun `Pim grows steadily and never beyond five times`() {
        assertTrue(PimEgg.MAX_SCALE <= 5f)
        assertEquals(1f, PimEgg.scale(0f), 0f)
        assertEquals(PimEgg.MAX_SCALE, PimEgg.scale(1f), 0.0001f)
        assertEquals(PimEgg.MAX_SCALE, PimEgg.scale(3f), 0.0001f)
        assertEquals(1f, PimEgg.scale(-1f), 0f)
        var last = 1f
        for (i in 1..100) {
            val s = PimEgg.scale(i / 100f)
            assertTrue("grows at $i", s > last)
            assertTrue(s <= 5f)
            last = s
        }
    }

    @Test
    fun `the trembling builds up`() {
        assertEquals(0f, PimEgg.shakeDp(0f), 0f)
        assertTrue(PimEgg.shakeDp(0.3f) < PimEgg.shakeDp(0.9f))
        assertEquals(PimEgg.MAX_SHAKE_DP, PimEgg.shakeDp(1f), 0.0001f)
    }
}
