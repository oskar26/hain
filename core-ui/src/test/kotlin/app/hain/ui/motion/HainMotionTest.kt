package app.hain.ui.motion

import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HainMotionTest {
    @Test
    fun `motion constants match UI_LIBRARIES section 3`() {
        assertEquals(0.82f, HainMotion.SEND_DAMPING)
        assertEquals(380f, HainMotion.SEND_STIFFNESS)
        assertEquals(0.85f, HainMotion.CARD_DAMPING)
        assertEquals(0.9f, HainMotion.DRAWER_DAMPING)
        assertEquals(160, HainMotion.TOKEN_FADE_MS)
        assertEquals(30, HainMotion.MAX_ANIMATED_WORDS)
        assertEquals(220, HainMotion.TOOL_CHIP_MS)
        assertEquals(180, HainMotion.SEND_STOP_MORPH_MS)
        assertEquals(150, HainMotion.PILL_FADE_MS)
        assertEquals(1400, HainMotion.SHIMMER_MS)
        assertEquals(40, HainMotion.STAGGER_MS)
    }

    @Test
    fun `reduce motion returns snap for every moment`() {
        HainMotionMoment.entries.forEach { moment ->
            val spec = motionSpec<Float>(moment, reduceMotion = true)
            assertTrue("$moment must snap", spec is SnapSpec)
        }
    }

    @Test
    fun `send uses the spec spring values`() {
        val send = motionSpec<Float>(HainMotionMoment.SEND, reduceMotion = false)
        assertTrue(send is SpringSpec)
        assertEquals(HainMotion.SEND_DAMPING, (send as SpringSpec<Float>).dampingRatio)
        assertEquals(HainMotion.SEND_STIFFNESS, send.stiffness)
    }

    @Test
    fun `card and drawer use their dampings and the calm stiffness`() {
        val card = motionSpec<Float>(HainMotionMoment.CARD, reduceMotion = false) as SpringSpec
        val drawer = motionSpec<Float>(HainMotionMoment.DRAWER, reduceMotion = false) as SpringSpec
        assertEquals(HainMotion.CARD_DAMPING, card.dampingRatio)
        assertEquals(HainMotion.DRAWER_DAMPING, drawer.dampingRatio)
        assertEquals(Spring.StiffnessMediumLow, card.stiffness)
        assertEquals(Spring.StiffnessMediumLow, drawer.stiffness)
    }

    @Test
    fun `tween moments use their table durations`() {
        val pairs =
            listOf(
                HainMotionMoment.TOKEN_FADE to HainMotion.TOKEN_FADE_MS,
                HainMotionMoment.TOOL_CHIP to HainMotion.TOOL_CHIP_MS,
                HainMotionMoment.SEND_STOP_MORPH to HainMotion.SEND_STOP_MORPH_MS,
                HainMotionMoment.PILL_FADE to HainMotion.PILL_FADE_MS,
            )
        pairs.forEach { (moment, ms) ->
            val spec = motionSpec<Float>(moment, reduceMotion = false)
            assertTrue("$moment must be a tween", spec is TweenSpec)
            assertEquals(ms, (spec as TweenSpec<Float>).durationMillis)
        }
    }
}

class HainMotionLanguageTest {
    @Test
    fun `shared durations stay short and calm`() {
        assertEquals(150, HainMotion.FAST_MS)
        assertEquals(180, HainMotion.MENU_MS)
        assertTrue(HainMotion.MENU_MS in 150..200)
    }

    @Test
    fun `tween and spring snap when motion is reduced`() {
        assertTrue(hainTween<Float>(HainMotion.MENU_MS, reduceMotion = true) is SnapSpec)
        assertTrue(hainSpring<Float>(reduceMotion = true) is SnapSpec)
        assertTrue(hainTween<Float>(HainMotion.MENU_MS, reduceMotion = false) is TweenSpec)
        val spring = hainSpring<Float>(reduceMotion = false) as SpringSpec
        assertEquals(HainMotion.CALM_DAMPING, spring.dampingRatio)
    }
}
