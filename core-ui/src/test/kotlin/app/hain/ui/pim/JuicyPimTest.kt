package app.hain.ui.pim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class JuicyPimTest {
    private val idle = poseFor(PimState.IDLE)

    @Test
    fun `breathing keeps Pim on the ground and squashes like jelly`() {
        val pose = livePose(idle, PimState.IDLE, phase = 0.25f, squish = 0f, blinking = false, delighted = false)
        assertEquals(0f, pose.y, 0.001f)
        assertTrue(pose.sx > 1f)
        assertTrue(pose.sy < 1f)
    }

    @Test
    fun `celebrating Pim hops up and stretches`() {
        val base = poseFor(PimState.CELEBRATE)
        val pose = livePose(base, PimState.CELEBRATE, phase = 0.25f, squish = 0f, blinking = false, delighted = false)
        assertTrue(pose.y < -10f)
        assertTrue(pose.sy > 1f)
    }

    @Test
    fun `blink closes pill eyes and a tap makes him happy`() {
        val blink = livePose(idle, PimState.IDLE, phase = 0f, squish = 0f, blinking = true, delighted = false)
        assertEquals(PimEyes.CLOSED, blink.eyes)
        val tapped = livePose(idle, PimState.IDLE, phase = 0f, squish = 1f, blinking = false, delighted = true)
        assertEquals(PimEyes.HAPPY, tapped.eyes)
        assertTrue(tapped.sx > 1.1f)
    }
}
