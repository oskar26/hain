package app.hain.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-230: the title of a top bar is centred on the whole bar, not between unequal icon groups. */
class TopBarTitleTest {
    @Test
    fun centredOnTheWholeBarWhenItFits() {
        // one icon left (48), two right (96): still exactly in the middle of 400
        assertEquals(150, centredTitleX(width = 400, lead = 48, trail = 96, title = 100))
    }

    @Test
    fun movesIntoTheFreeRoomWhenTooWide() {
        // 400 − 2 × 96 = 208 < 240 → centred between the icons: 48 + (256 − 240) / 2
        assertEquals(56, centredTitleX(width = 400, lead = 48, trail = 96, title = 240))
    }

    @Test
    fun neverStartsUnderTheLeftIcons() {
        assertEquals(48, centredTitleX(width = 300, lead = 48, trail = 96, title = 400))
    }
}
