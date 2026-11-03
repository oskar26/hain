package app.hain.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import com.kyant.capsule.ContinuousRoundedRectangle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HainShapesTest {
    // density 1 → 1 dp renders as exactly `radius` px
    private val density = Density(1f)
    private val anySize = Size(100f, 100f)

    private fun radius(shape: ContinuousRoundedRectangle) = shape.topStart.toPx(anySize, density)

    @Test
    fun `corner radii match spec`() {
        val s = StandardHainShapes
        // composer is a full capsule (no fixed radius), all others are squircles
        assertEquals(20f, radius(s.userBubble as ContinuousRoundedRectangle))
        assertEquals(20f, radius(s.card as ContinuousRoundedRectangle))
        assertEquals(20f, radius(s.menu as ContinuousRoundedRectangle))
        assertEquals(12f, radius(s.chip as ContinuousRoundedRectangle))
        assertEquals(12f, radius(s.row as ContinuousRoundedRectangle))
        assertEquals(28f, radius(s.sheet as ContinuousRoundedRectangle))
        // composer and buttons are capsules
        assertEquals(s.composer, s.button)
    }

    @Test
    fun `all shape tokens are continuous (squircle) shapes`() {
        val s = StandardHainShapes
        assertTrue(s.composer is ContinuousRoundedRectangle)
        assertTrue(s.userBubble is ContinuousRoundedRectangle)
        assertTrue(s.card is ContinuousRoundedRectangle)
        assertTrue(s.chip is ContinuousRoundedRectangle)
        assertTrue(s.button is ContinuousRoundedRectangle)
    }
}
