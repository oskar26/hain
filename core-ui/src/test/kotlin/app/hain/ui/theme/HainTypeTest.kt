package app.hain.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HainTypeTest {
    @Test
    fun `inter type scale matches spec`() {
        val t = HainType.inter()
        assertEquals(28.sp, t.display.fontSize)
        assertEquals(34.sp, t.display.lineHeight)
        assertEquals(FontWeight.SemiBold, t.display.fontWeight)

        assertEquals(20.sp, t.title.fontSize)
        assertEquals(26.sp, t.title.lineHeight)
        assertEquals(FontWeight.SemiBold, t.title.fontWeight)

        assertEquals(16.sp, t.body.fontSize)
        assertEquals(24.sp, t.body.lineHeight)
        assertEquals(FontWeight.Normal, t.body.fontWeight)

        assertEquals(14.sp, t.bodySmall.fontSize)
        assertEquals(20.sp, t.bodySmall.lineHeight)
        assertEquals(FontWeight.Normal, t.bodySmall.fontWeight)

        assertEquals(13.sp, t.label.fontSize)
        assertEquals(16.sp, t.label.lineHeight)
        assertEquals(FontWeight.Medium, t.label.fontWeight)

        assertEquals(14.sp, t.code.fontSize)
        assertEquals(20.sp, t.code.lineHeight)
        assertEquals(FontWeight.Normal, t.code.fontWeight)
    }

    @Test
    fun `body and code use different families`() {
        val t = HainType.inter()
        assertNotEquals(t.body.fontFamily, t.code.fontFamily)
    }

    @Test
    fun `atkinson variant differs from inter but keeps the same metrics`() {
        val inter = HainType.inter()
        val atkinson = HainType.atkinson()
        assertNotEquals(inter.body.fontFamily, atkinson.body.fontFamily)
        assertEquals(inter.body.fontSize, atkinson.body.fontSize)
        assertEquals(inter.title.lineHeight, atkinson.title.lineHeight)
    }
}
