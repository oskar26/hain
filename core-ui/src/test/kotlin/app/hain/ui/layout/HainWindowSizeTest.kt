package app.hain.ui.layout

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HainWindowSizeTest {
    @Test
    fun phonesAreCompact() {
        assertEquals(HainWindowSize.COMPACT, HainWindowSize.fromWidth(360))
        assertEquals(HainWindowSize.COMPACT, HainWindowSize.fromWidth(599))
    }

    @Test
    fun tabletPortraitIsMedium() {
        assertEquals(HainWindowSize.MEDIUM, HainWindowSize.fromWidth(600))
        assertEquals(HainWindowSize.MEDIUM, HainWindowSize.fromWidth(839))
        assertFalse(HainWindowSize.fromWidth(800).isExpanded)
    }

    @Test
    fun from840dpTheSidePanelStays() {
        assertEquals(HainWindowSize.EXPANDED, HainWindowSize.fromWidth(840))
        assertTrue(HainWindowSize.fromWidth(1280).isExpanded)
    }
}
