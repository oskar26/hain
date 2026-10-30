package app.hain.ui.effects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class HainGlassTest {
    @Test
    fun `glass style is 24dp blur with a 70 percent background tint`() {
        val bg = Color(0xFFFFFFFF)
        val style = hainGlassStyle(bg)
        assertEquals(24.dp, style.blurRadius)
        assertEquals(bg, style.backgroundColor)
        assertEquals(1, style.tints.size)
        assertEquals(bg.copy(alpha = 0.7f), style.tints.first().color)
    }
}
