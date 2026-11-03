package app.hain.ui.pim

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Frame tests of Pim's rig (T-089, Oskar 03.11. 23:16: "the leaf must never come off"). Every move
 * is sampled at 21 points in time, in every state, with breathing on top; at the biggest Pim (160 dp)
 * the leaf's root must stay within 1 dp of the head anchor, and the leaf within 1 dp of the stem tip.
 */
class PimRigTest {
    /** dp per viewBox unit at the biggest Pim of the app (start page, 160 dp). */
    private val dpPerUnit = BIGGEST_PIM_DP / PimGeometry.FRAME_SIZE

    private fun frames(): Sequence<Triple<String, Float, PimPose>> =
        sequence {
            PimState.entries.forEach { state ->
                PimMoves.ALL.forEach { move ->
                    (0..STEPS).forEach { i ->
                        val t = i / STEPS.toFloat()
                        val moved = PimMoves.pose(move, PimMoves.basePose(state), t)
                        val pose = PimMoves.breathe(moved, (t * BREATH_MS).toLong())
                        yield(Triple("$state/$move@$t", t, pose))
                    }
                }
            }
        }

    private fun Offset.dpTo(other: Offset) = (this - other).getDistance() * dpPerUnit

    @Test
    fun `leaf root stays on the head in every frame of every move`() {
        frames().forEach { (name, _, pose) ->
            val head = PimRig.body(pose).map(PimGeometry.STEM_BASE)
            val root = PimRig.stem(pose).map(PimGeometry.STEM_BASE)
            assertTrue("$name: leaf root ${head.dpTo(root)} dp off the head", head.dpTo(root) <= 1f)
        }
    }

    @Test
    fun `leaf stays on the stem tip in every frame`() {
        frames().forEach { (name, _, pose) ->
            val tip = PimRig.stem(pose).map(PimGeometry.LEAF_TIP)
            val leaf = PimRig.leaf(pose).map(PimGeometry.LEAF_TIP)
            assertTrue("$name: leaf ${tip.dpTo(leaf)} dp off the stem", tip.dpTo(leaf) <= 1f)
        }
    }

    @Test
    fun `stem grows out of the head, even when it bends`() {
        frames().forEach { (name, _, pose) ->
            // the stem's lower end in the body's own coordinates must stay inside the head
            val local = Affine.rotate(pose.bend, PimGeometry.STEM_BASE).map(Offset(60f, 42f))
            assertTrue("$name: stem end at $local", local.y in 34f..50f && local.x in 50f..70f)
        }
    }

    @Test
    fun `the leaf follows a squash and a hop of the body`() {
        val squashed = PimPose(sx = 1.2f, sy = 0.8f, y = -14f, tilt = 10f)
        val head = PimRig.body(squashed).map(PimGeometry.STEM_BASE)
        val root = PimRig.stem(squashed).map(PimGeometry.STEM_BASE)
        assertEquals(0f, head.dpTo(root), 0.001f)
        // the old rig only moved the leaf by x/y and bend: it would be several dp off here
        val oldRoot = PimGeometry.STEM_BASE + Offset(squashed.x, squashed.y)
        assertTrue(head.dpTo(oldRoot) > 1f)
    }

    @Test
    fun `affine composition applies the inner transform first`() {
        val moveThenTurn = Affine.rotate(90f, Offset.Zero).then(Affine.translate(10f, 0f))
        val p = moveThenTurn.map(Offset.Zero)
        assertEquals(0f, p.x, 0.001f)
        assertEquals(10f, p.y, 0.001f)
        val scaled = Affine.scale(2f, 3f, Offset(10f, 10f)).map(Offset(11f, 11f))
        assertEquals(Offset(12f, 13f), scaled)
    }

    private companion object {
        const val BIGGEST_PIM_DP = 160f
        const val STEPS = 20
        const val BREATH_MS = 3200
    }
}
