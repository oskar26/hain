package app.hain.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HainShapesTest {
    // density 1 → 1 dp renders as exactly `radius` px
    private val density = Density(1f)
    private val anySize = Size(100f, 100f)

    private fun radius(shape: RoundedCornerShape) = shape.topStart.toPx(anySize, density)

    @Test
    fun `corner radii match spec`() {
        val s = StandardHainShapes
        assertEquals(28f, radius(s.composer as RoundedCornerShape))
        assertEquals(20f, radius(s.userBubble as RoundedCornerShape))
        assertEquals(16f, radius(s.card as RoundedCornerShape))
        assertEquals(12f, radius(s.chip as RoundedCornerShape))
        assertEquals(12f, radius(s.button as RoundedCornerShape))
    }

    @Test
    fun `all shape tokens are RoundedCornerShape instances`() {
        // T-018 swaps these for Capsule squircles; until then no token may
        // silently fall back to an arbitrary shape.
        val s = StandardHainShapes
        assertTrue(s.composer is RoundedCornerShape)
        assertTrue(s.userBubble is RoundedCornerShape)
        assertTrue(s.card is RoundedCornerShape)
        assertTrue(s.chip is RoundedCornerShape)
        assertTrue(s.button is RoundedCornerShape)
    }
}
