package app.hain.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks the DESIGN_SYSTEM.md §2 colour table. If this test fails, either the
 * spec changed (then update both spec and test) or someone tweaked a value.
 */
class HainColorsTest {
    @Test
    fun `light palette matches spec`() {
        assertEquals(Color(0xFFFFFFFF), LightHainColors.bg)
        assertEquals(Color(0xFFF4F5F4), LightHainColors.surface)
        assertEquals(Color(0xFFECEEEC), LightHainColors.surfaceHigh)
        assertEquals(Color(0xFFE4E7E5), LightHainColors.border)
        assertEquals(Color(0xFF0D0F0E), LightHainColors.text)
        assertEquals(Color(0xFF5F6662), LightHainColors.textMuted)
        assertEquals(Color(0xFF16A36B), LightHainColors.accent)
        assertEquals(Color(0xFFFFFFFF), LightHainColors.onAccent)
        assertEquals(Color(0xFFE3F5EC), LightHainColors.accentSoft)
        assertEquals(Color(0xFFE8833A), LightHainColors.online)
        assertEquals(Color(0xFFD14343), LightHainColors.danger)
        assertEquals(Color(0xFF7A817D), LightHainColors.icon)
        assertEquals(Color(0xF5FFFFFF), LightHainColors.glass)
    }

    @Test
    fun `dark palette matches spec`() {
        assertEquals(Color(0xFF0F1311), DarkHainColors.bg)
        assertEquals(Color(0xFF1B201D), DarkHainColors.surface)
        assertEquals(Color(0xFF242A26), DarkHainColors.surfaceHigh)
        assertEquals(Color(0xFF2A302C), DarkHainColors.border)
        assertEquals(Color(0xFFECEFED), DarkHainColors.text)
        assertEquals(Color(0xFF9AA39E), DarkHainColors.textMuted)
        assertEquals(Color(0xFF22C17E), DarkHainColors.accent)
        assertEquals(Color(0xFF06130C), DarkHainColors.onAccent)
        assertEquals(Color(0xFF173326), DarkHainColors.accentSoft)
        assertEquals(Color(0xFFF0964F), DarkHainColors.online)
        assertEquals(Color(0xFFF07070), DarkHainColors.danger)
        assertEquals(Color(0xFF8B948F), DarkHainColors.icon)
        assertEquals(Color(0xF21E2420), DarkHainColors.glass)
    }
}
