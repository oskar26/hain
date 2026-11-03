package app.hain.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HainTypeTest {
    @Test
    fun `hanken type scale matches DESIGN_LANGUAGE`() {
        val t = HainType.hanken()
        assertEquals(30.sp, t.display.fontSize)
        assertEquals(38.sp, t.display.lineHeight)
        assertEquals(FontWeight.Light, t.display.fontWeight)

        assertEquals(20.sp, t.title.fontSize)
        assertEquals(28.sp, t.title.lineHeight)
        assertEquals(FontWeight.Medium, t.title.fontWeight)

        assertEquals(16.sp, t.body.fontSize)
        assertEquals(25.sp, t.body.lineHeight)
        assertEquals(FontWeight.Normal, t.body.fontWeight)

        assertEquals(14.sp, t.bodySmall.fontSize)
        assertEquals(20.sp, t.bodySmall.lineHeight)
        assertEquals(FontWeight.Normal, t.bodySmall.fontWeight)

        assertEquals(13.sp, t.label.fontSize)
        assertEquals(FontWeight.Medium, t.label.fontWeight)

        assertEquals(15.sp, t.button.fontSize)
        assertEquals(FontWeight.Medium, t.button.fontWeight)

        assertEquals(14.sp, t.code.fontSize)
        assertEquals(20.sp, t.code.lineHeight)
        assertEquals(FontWeight.Normal, t.code.fontWeight)
    }

    @Test
    fun `running text is at least 16 sp in both families`() {
        listOf(HainType.hanken(), HainType.atkinson()).forEach { t ->
            assertTrue(t.body.fontSize.value >= 16f)
        }
    }

    @Test
    fun `body and code use different families`() {
        val t = HainType.hanken()
        assertNotEquals(t.body.fontFamily, t.code.fontFamily)
    }

    @Test
    fun `atkinson variant differs from hanken but keeps the same metrics`() {
        val hanken = HainType.hanken()
        val atkinson = HainType.atkinson()
        assertNotEquals(hanken.body.fontFamily, atkinson.body.fontFamily)
        assertEquals(hanken.body.fontSize, atkinson.body.fontSize)
        assertEquals(hanken.title.lineHeight, atkinson.title.lineHeight)
    }
}
