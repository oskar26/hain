package app.hain.ui.motion

import android.view.HapticFeedbackConstants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HapticsTest {
    @Test
    fun `every moment is short and crisp, never a buzz`() {
        HainHaptic.entries.forEach { h ->
            val steps = h.pattern()
            assertTrue("$h has 1–2 primitives", steps.size in 1..2)
            assertTrue("$h stays short", steps.sumOf { it.delayMs } <= 100)
            assertTrue(steps.all { it.primitive in listOf(P_CLICK, P_THUD, P_QUICK_FALL, P_TICK, P_LOW_TICK) })
        }
    }

    @Test
    fun `moments use the agreed primitives`() {
        assertEquals(
            P_CLICK,
            HainHaptic.SEND
                .pattern()
                .single()
                .primitive,
        )
        assertEquals(
            P_TICK,
            HainHaptic.TOGGLE
                .pattern()
                .single()
                .primitive,
        )
        assertEquals(
            P_LOW_TICK,
            HainHaptic.SELECT
                .pattern()
                .single()
                .primitive,
        )
        assertEquals(listOf(P_TICK, P_TICK), HainHaptic.DONE.pattern().map { it.primitive })
        assertEquals(
            P_THUD,
            HainHaptic.LONG_PRESS
                .pattern()
                .single()
                .primitive,
        )
        assertEquals(2, HainHaptic.ERROR.pattern().size)
    }

    @Test
    fun `levels scale the strength and off is silent`() {
        val send = HainHaptic.SEND.pattern()
        assertTrue(scaled(send, HapticLevel.OFF).isEmpty())
        val subtle = scaled(send, HapticLevel.SUBTLE).single().scale
        val normal = scaled(send, HapticLevel.NORMAL).single().scale
        val strong = scaled(send, HapticLevel.STRONG).single().scale
        assertTrue(subtle < normal && normal < strong && strong <= 1f)
        assertEquals(HapticLevel.SUBTLE, HapticLevel.entries.first { it.scale in 0.5f..0.6f })
    }

    @Test
    fun `fallbacks are framework constants`() {
        assertEquals(HapticFeedbackConstants.KEYBOARD_TAP, HainHaptic.SEND.fallback())
        assertEquals(HapticFeedbackConstants.CLOCK_TICK, HainHaptic.TOGGLE.fallback())
        assertEquals(HapticFeedbackConstants.LONG_PRESS, HainHaptic.LONG_PRESS.fallback())
    }

    @Test
    fun `bigger 2048 tiles merge firmer`() {
        assertTrue(mergeStrength(4) < mergeStrength(64))
        assertEquals(1f, mergeStrength(2048), 0.001f)
    }

    @Test
    fun `the easter egg charge gets denser and firmer`() {
        assertTrue(chargeGapMs(0f) > chargeGapMs(0.5f))
        assertTrue(chargeGapMs(0.5f) > chargeGapMs(1f))
        assertTrue(chargeGapMs(1f) >= 40L)
        assertTrue(chargeStrength(0f) < chargeStrength(0.5f))
        assertEquals(1f, chargeStrength(1f), 0.0001f)
        assertEquals(HainHaptic.SELECT, chargeMoment(0.1f))
        assertEquals(HainHaptic.SEND, chargeMoment(0.95f))
    }

    @Test
    fun `menus, confirm, pull and burst are short primitives`() {
        assertEquals(
            P_TICK,
            HainHaptic.MENU
                .pattern()
                .single()
                .primitive,
        )
        assertEquals(
            P_CLICK,
            HainHaptic.CONFIRM
                .pattern()
                .single()
                .primitive,
        )
        assertEquals(
            P_LOW_TICK,
            HainHaptic.PULL
                .pattern()
                .single()
                .primitive,
        )
        assertEquals(
            P_THUD,
            HainHaptic.BURST
                .pattern()
                .first()
                .primitive,
        )
    }
}
