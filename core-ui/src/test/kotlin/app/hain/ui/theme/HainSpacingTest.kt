package app.hain.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HainSpacingTest {
    @Test
    fun `spacing scale matches spec 4-8-12-16-24-32`() {
        val s = StandardHainSpacing
        assertEquals(4.dp, s.xs)
        assertEquals(8.dp, s.sm)
        assertEquals(12.dp, s.md)
        assertEquals(16.dp, s.lg)
        assertEquals(24.dp, s.xl)
        assertEquals(32.dp, s.xxl)
    }

    @Test
    fun `scale is strictly increasing`() {
        val values =
            listOf(
                StandardHainSpacing.xs,
                StandardHainSpacing.sm,
                StandardHainSpacing.md,
                StandardHainSpacing.lg,
                StandardHainSpacing.xl,
                StandardHainSpacing.xxl,
            )
        values.zipWithNext().forEach { (a, b) ->
            assertTrue("$a must be smaller than $b", a.value < b.value)
        }
    }
}
