package app.hain.ui.layout

import org.junit.Assert.assertEquals
import org.junit.Test

class HainColumnsTest {
    @Test
    fun phonesAndPortraitTabletsKeepOneColumn() {
        assertEquals(1, HainColumns.count(HainWindowSize.fromWidth(360)))
        assertEquals(1, HainColumns.count(HainWindowSize.fromWidth(600)))
        assertEquals(1, HainColumns.count(HainWindowSize.fromWidth(839)))
    }

    @Test
    fun wideWindowsGetTwoColumns() {
        assertEquals(2, HainColumns.count(HainWindowSize.fromWidth(840)))
        assertEquals(2, HainColumns.count(HainWindowSize.fromWidth(1280)))
    }

    @Test
    fun pageGetsWiderOnlyWithColumns() {
        assertEquals(HainColumns.PAGE_MAX_DP, HainColumns.pageMaxDp(1))
        assertEquals(HainColumns.WIDE_PAGE_MAX_DP, HainColumns.pageMaxDp(2))
    }

    @Test
    fun rowsKeepOrderAndAShortLastRow() {
        assertEquals(listOf(listOf(1, 2), listOf(3, 4), listOf(5)), HainColumns.rows(listOf(1, 2, 3, 4, 5), 2))
        assertEquals(listOf(listOf(1), listOf(2)), HainColumns.rows(listOf(1, 2), 1))
        assertEquals(listOf(listOf(1), listOf(2)), HainColumns.rows(listOf(1, 2), 0))
        assertEquals(emptyList<List<Int>>(), HainColumns.rows(emptyList<Int>(), 2))
    }
}
