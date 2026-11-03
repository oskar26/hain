package app.hain.ui.pim

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PimDirectorTest {
    @Test
    fun `a tap never repeats the last reaction`() {
        val d = PimDirector(Random(1))
        d.setState(PimState.IDLE, 1)
        var last: PimMove? = null
        val seen = mutableSetOf<PimMove>()
        repeat(60) { i ->
            val r = d.tap(1_000L + i * 2_000L)
            assertNotEquals(last, r)
            assertTrue(r in PimMoves.TAP_REACTIONS)
            seen += r
            last = r
        }
        assertEquals(PimMoves.TAP_REACTIONS.toSet(), seen)
    }

    @Test
    fun `idle moves rotate at random and never twice in a row`() {
        val d = PimDirector(Random(7))
        d.setState(PimState.IDLE, 1)
        val played = mutableListOf<PimMove>()
        var now = 1L
        while (played.size < 30) {
            now += 16
            val before = d.move
            d.tick(now)
            val after = d.move
            if (after != null && after != before) played += after
            // keep him busy so he does not doze
            if (now % 20_000L < 16) d.tap(now)
        }
        val idles = played.filter { it in PimMoves.IDLE_POOL }
        assertTrue(idles.toSet().size >= 5)
        idles.zipWithNext().forEach { (a, b) -> assertNotEquals(a, b) }
    }

    @Test
    fun `states start with their own move`() {
        val d = PimDirector(Random(3))
        d.setState(PimState.THINKING, 10)
        assertEquals(PimMove.THINK, d.move)
        d.setState(PimState.HAPPY, 20)
        assertEquals(PimMove.JOY, d.move)
        d.tick(20 + PimMove.JOY.durationMs + 1L)
        assertNull(d.move)
        d.setState(PimState.ERROR, 5_000)
        assertEquals(PimMove.STARTLE, d.move)
        d.setState(PimState.SLEEPING, 6_000)
        assertEquals(PimMove.SLEEP, d.move)
    }

    @Test
    fun `after a long wait Pim dozes off with Zz`() {
        val d = PimDirector(Random(5))
        d.setState(PimState.IDLE, 1)
        var now = 1L
        var dozed = false
        while (now < 120_000L && !dozed) {
            now += 50
            d.tick(now)
            dozed = d.move == PimMove.DOZE
        }
        assertTrue(dozed)
        assertEquals(PimFx.ZZZ, d.pose(now + 800).fx)
    }

    @Test
    fun `thinking wiggles the leaf and a fright opens the eyes wide`() {
        val base = PimMoves.basePose(PimState.THINKING)
        val a = PimMoves.pose(PimMove.THINK, base, 0.125f).leaf
        val b = PimMoves.pose(PimMove.THINK, base, 0.375f).leaf
        assertTrue(a > 5f && b < -5f)
        val fright = PimMoves.pose(PimMove.STARTLE, PimMoves.basePose(PimState.ERROR), 0.1f)
        assertEquals(PimEyes.WIDE, fright.eyes)
        assertEquals(PimFx.BANG, fright.fx)
    }

    @Test
    fun `a squish squashes first and ends calm`() {
        val start = PimMoves.pose(PimMove.SQUISH, PimPose(), 0f)
        assertTrue(start.sx > 1.1f && start.sy < 0.9f)
        val end = PimMoves.pose(PimMove.SQUISH, PimPose(), 1f)
        assertEquals(1f, end.sx, 0.02f)
        assertEquals(PimEyes.HAPPY, end.eyes)
    }

    @Test
    fun `dragging looks surprised and letting go relieved`() {
        val dragged =
            heroPose(
                PimPose(),
                HeroMood.DRAGGED,
                0f,
                androidx.compose.ui.geometry
                    .Offset(900f, 0f),
                0,
            )
        assertEquals(PimEyes.WIDE, dragged.eyes)
        assertEquals(PimMouth.OPEN, dragged.mouth)
        assertTrue(dragged.leaf < -20f)
        val relieved = heroPose(PimPose(), HeroMood.RELIEVED, 0f, androidx.compose.ui.geometry.Offset.Zero, 100)
        assertEquals(PimEyes.HAPPY, relieved.eyes)
        val charging = heroPose(PimPose(), HeroMood.CHARGING, 0.9f, androidx.compose.ui.geometry.Offset.Zero, 0)
        assertEquals(PimEyes.SQUEEZE, charging.eyes)
    }
}
