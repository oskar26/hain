package app.hain.ui.effects

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MenuBackdropTest {
    @Test
    fun `menu backdrop is a calm 12 to 16 dp blur with a light page tint`() {
        val bg = Color(0xFFF7F8F5)
        val style = menuBackdropStyle(bg)
        assertTrue(style.blurRadius in 12.dp..16.dp)
        assertEquals(bg, style.backgroundColor)
        assertEquals(1, style.tints.size)
        assertEquals(bg.copy(alpha = MENU_BLUR_TINT), style.tints.first().color)
        assertTrue("no milky glass", MENU_BLUR_TINT <= 0.25f)
    }
}
