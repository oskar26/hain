package app.hain.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HainSurfaceTest {
    @Test
    fun `levels get deeper step by step and stay soft`() {
        assertEquals(0.dp, HainLevel.FLAT.shadowDp())
        assertEquals(1.dp, HainLevel.RAISED.shadowDp())
        assertEquals(12.dp, HainLevel.FLOATING.shadowDp())
        val all = HainLevel.entries.map { it.shadowDp().value }
        assertTrue(all.zipWithNext().all { (a, b) -> a < b })
    }

    @Test
    fun `dark glass glows stronger inside and keeps a fainter edge`() {
        val light = glowStrength(dark = false)
        val dark = glowStrength(dark = true)
        assertTrue(dark.shadow > light.shadow)
        assertTrue(dark.edge < light.edge)
        assertTrue(light.sheen > dark.sheen)
        assertTrue(GlowHue.entries.all { it.core != it.light })
    }
}
