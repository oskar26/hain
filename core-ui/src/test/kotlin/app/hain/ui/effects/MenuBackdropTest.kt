package app.hain.ui.effects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuBackdropTest {
    @Test
    fun `frosted card blurs 24 dp with a 60 to 75 percent tint`() {
        val bg = Color(0xFFFFFFFF)
        val light = frostedStyle(bg, Color.White, dark = false)
        assertEquals(24.dp, light.blurRadius)
        assertEquals(bg, light.backgroundColor)
        assertEquals(1, light.tints.size)
        assertEquals(Color.White.copy(alpha = FROST_TINT_LIGHT), light.tints.first().color)
        val dark = frostedStyle(Color.Black, Color(0xFF171B19), dark = true)
        assertEquals(
            FROST_TINT_DARK,
            dark.tints
                .first()
                .color.alpha,
            0.01f,
        )
        listOf(FROST_TINT_LIGHT, FROST_TINT_DARK).forEach { assertTrue(it in 0.6f..0.75f) }
    }

    @Test
    fun `fallback below Android 12 is almost opaque`() {
        assertTrue(frostedFallback(Color.White, dark = false).alpha > 0.95f)
        assertTrue(frostedFallback(Color.Black, dark = true).alpha > 0.95f)
    }

    @Test
    fun `the rest of the screen is only dimmed lightly`() {
        assertTrue(MENU_DIM_SHARE <= 0.4f)
    }
}
