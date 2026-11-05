package app.hain.ui.effects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HainGlassTest {
    @Test
    fun `glass style is the chrome glass, 32dp blur with a 62 percent background tint`() {
        val bg = Color(0xFFFFFFFF)
        val style = hainGlassStyle(bg)
        assertEquals(32.dp, style.blurRadius)
        assertEquals(bg, style.backgroundColor)
        assertEquals(1, style.tints.size)
        assertEquals(bg.copy(alpha = 0.62f), style.tints.first().color)
    }

    @Test
    fun `chrome glass is see-through but readable, dark a bit denser`() {
        val bg = Color(0xFF0E1110)
        val light = chromeStyle(Color.White, dark = false)
        val dark = chromeStyle(bg, dark = true)
        assertEquals(32.dp, light.blurRadius)
        assertEquals(
            CHROME_TINT_LIGHT,
            light.tints
                .first()
                .color.alpha,
            0.01f,
        )
        assertEquals(
            CHROME_TINT_DARK,
            dark.tints
                .first()
                .color.alpha,
            0.01f,
        )
        listOf(CHROME_TINT_LIGHT, CHROME_TINT_DARK).forEach { assertTrue(it in 0.55f..0.75f) }
        assertTrue(CHROME_TINT_DARK > CHROME_TINT_LIGHT)
        assertTrue("no sharp text through the fallback", light.fallbackTint.color.alpha > 0.95f)
    }
}
