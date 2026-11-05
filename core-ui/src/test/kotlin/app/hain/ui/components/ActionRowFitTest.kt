package app.hain.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ActionRowFitTest {
    @Test
    fun rowThatFitsKeepsItsWidths() {
        assertEquals(listOf(100, 40, 40), ActionRowFit.widths(listOf(100, 40, 40), 300, 34))
    }

    @Test
    fun onlyIconButtonsShrinkAndNeverBelowTheMinimum() {
        // "‹ 3/3 ›" (110) + six 40-wide icons = 350 on a 320 room → icons 35 each, picker untouched
        val widths = ActionRowFit.widths(listOf(110) + List(6) { 40 }, 320, 34)
        assertEquals(110, widths[0])
        assertEquals(List(6) { 35 }, widths.drop(1))
        // far too narrow: icons stop at the minimum
        assertEquals(List(6) { 34 }, ActionRowFit.widths(listOf(110) + List(6) { 40 }, 200, 34).drop(1))
    }
}
