package app.hain.ui.components

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.Assert.assertEquals
import org.junit.Test

class PopoverPlacementTest {
    private val window = IntSize(1080, 2340)

    @Test
    fun `popover opens above the brain button, left edges aligned`() {
        val anchor = IntRect(left = 40, top = 2000, right = 160, bottom = 2100)
        val offset = popoverOffset(anchor, window, IntSize(800, 600), gap = 20)
        assertEquals(IntOffset(40, 1420), offset)
    }

    @Test
    fun `popover stays inside a small window`() {
        val anchor = IntRect(left = 600, top = 300, right = 700, bottom = 380)
        val offset = popoverOffset(anchor, IntSize(720, 1280), IntSize(500, 600), gap = 20)
        // too wide → pushed left; no room above → opens below the anchor
        assertEquals(220, offset.x)
        assertEquals(360, offset.y)
    }
}
