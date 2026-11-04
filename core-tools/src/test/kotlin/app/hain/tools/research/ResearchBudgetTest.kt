package app.hain.tools.research

import app.hain.llm.internal.AnswerMode
import app.hain.tools.api.ResearchBudget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResearchBudgetTest {
    @Test
    fun fastSearchesOnceAndReadsOnePage() {
        val b = ResearchBudget.of(AnswerMode.FAST)
        assertEquals(1, b.maxSearches)
        assertEquals(1, b.maxPages)
        assertEquals(1, b.pagesFor(0))
        assertEquals(0, b.pagesFor(1))
        assertFalse(b.liveNotes)
    }

    @Test
    fun autoHasThreeRoundsTwoSearchesThreePagesTwentySeconds() {
        val b = ResearchBudget.of(AnswerMode.AUTO)
        assertEquals(3, b.maxRounds)
        assertEquals(2, b.maxSearches)
        assertEquals(3, b.maxPages)
        assertEquals(20_000L, b.timeMs)
    }

    @Test
    fun thoroughHasOneMinuteEightRoundsAndUpToEightPages() {
        val b = ResearchBudget.of(AnswerMode.THOROUGH)
        assertEquals(60_000L, b.timeMs)
        assertEquals(8, b.maxRounds)
        assertTrue(b.maxSearches >= 3)
        assertTrue(b.callsPerRound > 1)
        assertEquals(5, b.pagesFor(0))
        assertEquals(3, b.pagesFor(5))
        assertEquals(0, b.pagesFor(8))
        assertTrue(b.liveNotes)
    }

    @Test
    fun clockStartsWithTheFirstTool() {
        val b = ResearchBudget.THOROUGH
        assertFalse(b.timeUp(null, 1_000_000L))
        assertEquals(60_000L, b.remaining(null, 5L))
        assertFalse(b.timeUp(1_000L, 60_999L))
        assertTrue(b.timeUp(1_000L, 61_000L))
        assertEquals(10_000L, b.remaining(1_000L, 51_000L))
        assertEquals(0L, b.remaining(1_000L, 99_000L))
    }

    @Test
    fun deeperModesNeverGetLess() {
        val (f, a, t) = listOf(ResearchBudget.FAST, ResearchBudget.AUTO, ResearchBudget.THOROUGH)
        assertTrue(f.maxPages <= a.maxPages && a.maxPages <= t.maxPages)
        assertTrue(f.maxSearches <= a.maxSearches && a.maxSearches <= t.maxSearches)
        assertTrue(f.timeMs <= a.timeMs && a.timeMs <= t.timeMs)
        // page text stays small: a full thorough read is still well under 2048 tokens (≈ 4 chars each)
        assertTrue(t.maxPages * t.charsPerPage / 4 < 2048)
    }
}
