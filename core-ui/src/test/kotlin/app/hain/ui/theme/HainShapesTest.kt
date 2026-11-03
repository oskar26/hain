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
    fun `corner radii are small and calm in both styles`() {
        val a = NothingShapes
        assertEquals(12f, radius(a.userBubble as ContinuousRoundedRectangle))
        assertEquals(12f, radius(a.card as ContinuousRoundedRectangle))
        assertEquals(12f, radius(a.menu as ContinuousRoundedRectangle))
        assertEquals(8f, radius(a.chip as ContinuousRoundedRectangle))
        assertEquals(10f, radius(a.row as ContinuousRoundedRectangle))
        assertEquals(16f, radius(a.sheet as ContinuousRoundedRectangle))
        assertEquals(14f, radius(a.composer as ContinuousRoundedRectangle))
        assertEquals(12f, radius(a.tile as ContinuousRoundedRectangle))
        assertEquals(12f, radius(a.button as ContinuousRoundedRectangle))
        val b = SoftShapes
        assertEquals(16f, radius(b.card as ContinuousRoundedRectangle))
        assertEquals(16f, radius(b.composer as ContinuousRoundedRectangle))
        assertEquals(20f, radius(b.sheet as ContinuousRoundedRectangle))
        // nothing is a candy: no squircle above 20 dp
        listOf(a, b).forEach { s ->
            listOf(
                s.composer,
                s.userBubble,
                s.card,
                s.chip,
                s.row,
                s.menu,
                s.sheet,
                s.tile,
            ).forEach {
                assertTrue(radius(it as ContinuousRoundedRectangle) <= 20f)
            }
        }
    }

    @Test
    fun `style A is the default`() {
        assertEquals(NothingShapes, StandardHainShapes)
        assertEquals(HainStyle.A, HainStyle.fromKey(null))
        assertEquals(HainStyle.A, HainStyle.fromKey("x"))
        assertEquals(HainStyle.B, HainStyle.fromKey("b"))
        assertEquals(NothingLight, hainColors(HainStyle.A, dark = false))
        assertEquals(SoftDark, hainColors(HainStyle.B, dark = true))
    }

    @Test
    fun `all shape tokens are continuous (squircle) shapes`() {
        val s = StandardHainShapes
        assertTrue(s.composer is ContinuousRoundedRectangle)
        assertTrue(s.userBubble is ContinuousRoundedRectangle)
        assertTrue(s.card is ContinuousRoundedRectangle)
        assertTrue(s.chip is ContinuousRoundedRectangle)
        assertTrue(s.button is ContinuousRoundedRectangle)
        assertTrue(hainShapes(HainStyle.B).button is com.kyant.capsule.ContinuousCapsule)
    }
}
