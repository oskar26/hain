package app.hain.ui.pim

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PimGeometryTest {
    @Test
    fun `body path is the closed gumdrop starting at M60 34`() {
        val commands = parsePathCommands(PimGeometry.BODY_PATH)
        assertEquals(PathCommand.MoveTo(60f, 34f), commands.first())
        assertEquals(PathCommand.Close, commands.last())
        assertEquals(4, commands.count { it is PathCommand.CubicTo })
    }

    @Test
    fun `closed shapes end with close, open shapes do not`() {
        assertTrue(parsePathCommands(PimGeometry.LEAF_PATH).last() is PathCommand.Close)
        assertTrue(parsePathCommands(PimGeometry.STEM_PATH).last() !is PathCommand.Close)
        assertTrue(parsePathCommands(PimGeometry.LEAF_VEIN_PATH).last() !is PathCommand.Close)
    }

    @Test
    fun `parser handles decimals from the stem and leaf`() {
        val stem = parsePathCommands(PimGeometry.STEM_PATH)
        assertEquals(PathCommand.MoveTo(60f, 42f), stem.first())
        assertEquals(
            PathCommand.CubicTo(60f, 33f, 59.5f, 26f, 57.5f, 20.5f),
            stem[1],
        )
    }

    @Test
    fun `pathOf reproduces the commands in order`() {
        val commands = parsePathCommands(PimGeometry.LEAF_PATH)
        // leaf: M, C, C, Z
        assertEquals(4, commands.size)
        assertEquals(PathCommand.MoveTo(57.5f, 20.5f), commands[0])
        assertTrue(commands[1] is PathCommand.CubicTo)
        assertTrue(commands[2] is PathCommand.CubicTo)
        assertEquals(PathCommand.Close, commands[3])
    }

    @Test
    fun `static poses map states to the rig eyes and fx of pim_motion`() {
        assertEquals(PimEyes.PILL, poseFor(PimState.IDLE).eyes)
        assertEquals(PimEyes.WIDE, poseFor(PimState.LISTENING).eyes)
        assertEquals(PimEyes.CLOSED, poseFor(PimState.SLEEPING).eyes)
        assertEquals(PimFx.ZZZ, poseFor(PimState.SLEEPING).fx)
        assertEquals(PimEyes.HAPPY, poseFor(PimState.HAPPY).eyes)
        assertEquals(PimFx.SPARKLE, poseFor(PimState.CELEBRATE).fx)
        assertEquals(PimFx.DROP, poseFor(PimState.ERROR).fx)
        assertEquals(PimEyes.PILL, poseFor(PimState.DOWNLOADING).eyes)
    }

    @Test
    fun `neutral pose equals the neutral rig from pim_motion`() {
        val neutral = PimPose()
        assertEquals(0f, neutral.x)
        assertEquals(1f, neutral.sx)
        assertEquals(1f, neutral.sy)
        assertEquals(0f, neutral.tilt)
        assertEquals(0f, neutral.bend)
        assertEquals(0f, neutral.leaf)
        assertEquals(1f, neutral.leafScale)
        assertEquals(PimEyes.PILL, neutral.eyes)
        assertEquals(1f, neutral.eyeH)
        assertEquals(PimFx.NONE, neutral.fx)
    }

    @Test
    fun `anchors match the geometry block of pim_motion`() {
        assertEquals(Offset(60f, 36f), PimGeometry.STEM_BASE)
        assertEquals(Offset(57.5f, 20.5f), PimGeometry.LEAF_TIP)
        assertEquals(Offset(48.5f, 71f), PimGeometry.EYE_L)
        assertEquals(Offset(71.5f, 71f), PimGeometry.EYE_R)
        assertEquals(-12f, PimGeometry.VIEW_BOX_X)
        assertEquals(-18f, PimGeometry.VIEW_BOX_Y)
        assertEquals(144f, PimGeometry.VIEW_BOX_SIZE)
    }
}
