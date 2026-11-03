package app.hain.ui.theme

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HainSurfaceTest {
    @Test
    fun `levels get deeper step by step and stay soft`() {
        assertEquals(0.dp, HainLevel.FLAT.shadowDp())
        assertEquals(2.dp, HainLevel.RAISED.shadowDp())
        assertEquals(14.dp, HainLevel.FLOATING.shadowDp())
        val all = HainLevel.entries.map { it.shadowDp().value }
        assertTrue(all.zipWithNext().all { (a, b) -> a < b })
    }
}
