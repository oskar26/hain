package app.hain.feature.memory

import app.hain.feature.memory.api.AnswerChips
import app.hain.feature.memory.api.MemoryList
import app.hain.feature.memory.api.MemoryTab
import app.hain.feature.memory.api.SmartSearch
import app.hain.memory.api.MemoryCategory
import app.hain.memory.api.MemoryFact
import app.hain.memory.api.MemoryTopic
import app.hain.memory.api.MemoryUse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.util.Locale

class MemoryListTest {
    private val facts =
        listOf(
            MemoryFact(1, "mag keine Pilze", MemoryCategory.PREFERENCE, createdAt = 0),
            MemoryFact(2, "spielt Gitarre im Orchester", createdAt = 0),
            MemoryFact(3, "hat Asthma", MemoryCategory.HEALTH, createdAt = 0, suggested = true),
        )

    @Test
    fun wideWindowsShowAboutYouBesideTheListNotAsATab() {
        assertEquals(MemoryTab.entries, MemoryList.tabs(wide = false))
        assertEquals(
            listOf(MemoryTab.FACTS, MemoryTab.TOPICS, MemoryTab.SUGGESTIONS),
            MemoryList.tabs(wide = true),
        )
    }

    @Test
    fun tabsSplitKeptFactsAndSuggestions() {
        assertEquals(listOf(1L, 2L), MemoryList.visible(facts, "", suggestions = false).map { it.id })
        assertEquals(listOf(3L), MemoryList.visible(facts, "", suggestions = true).map { it.id })
    }

    @Test
    fun searchNeedsEveryWord() {
        assertEquals(listOf(2L), MemoryList.visible(facts, "gitarre ORCH", suggestions = false).map { it.id })
        assertEquals(emptyList<Long>(), MemoryList.visible(facts, "gitarre pilze", suggestions = false).map { it.id })
    }

    @Test
    fun dateFollowsTheLanguage() {
        val millis = 1_793_750_400_000L // 2026-11-04
        assertEquals("04.11.2026", MemoryList.date(millis, Locale.GERMANY, ZoneOffset.UTC))
        assertEquals("Nov 4, 2026", MemoryList.date(millis, Locale.US, ZoneOffset.UTC))
    }

    @Test
    fun topicsGroupKeptFactsByKindWithTheirNote() {
        val notes = listOf(MemoryTopic("preference", "isst vegetarisch", 1), MemoryTopic("place", "Köln", 1))
        val groups = MemoryList.topics(facts, notes)
        // the suggestion (health) is not a topic yet; a note without facts still is
        val kinds = listOf(MemoryCategory.PREFERENCE, MemoryCategory.PLACE, MemoryCategory.OTHER)
        assertEquals(kinds, groups.map { it.category })
        assertEquals("isst vegetarisch", groups.first().note)
        assertEquals(listOf(1L), groups.first().facts.map { it.id })
        assertTrue(groups[1].facts.isEmpty())
    }

    @Test
    fun aSuggestionNamesTheFactItReplaces() {
        val newer = MemoryFact(4, "mag jetzt Pilze", createdAt = 1, suggested = true, replaces = 1)
        assertEquals("mag keine Pilze", MemoryList.replaced(newer, facts + newer)?.text)
        assertNull(MemoryList.replaced(newer.copy(replaces = 3), facts + newer)) // only a kept fact
        assertNull(MemoryList.replaced(facts[2], facts))
    }

    @Test
    fun chipsShowWhatTheAnswerDid() {
        assertTrue(AnswerChips.of(null).isEmpty)
        val saved = AnswerChips.of(MemoryUse(saved = listOf(facts[0])))
        assertTrue(saved.saved)
        assertFalse(saved.undone)
        val undone = AnswerChips.of(MemoryUse(saved = listOf(facts[0]), undone = true))
        assertFalse(undone.saved)
        assertTrue(undone.undone)
        val used = AnswerChips.of(MemoryUse(used = facts.take(2)))
        assertEquals(2, used.used.size)
        assertFalse(used.saved || used.forgotten || used.undone)
        assertTrue(AnswerChips.of(MemoryUse(forgotten = listOf(facts[1]))).forgotten)
    }

    @Test
    fun smartSearchTileFollowsTheDownload() {
        val size = 219_308_800L
        assertEquals(SmartSearch.Offer(219), SmartSearch.of(SmartSearch.Phase.IDLE, fits = true, sizeBytes = size))
        assertEquals(SmartSearch.Hidden, SmartSearch.of(SmartSearch.Phase.IDLE, fits = false, sizeBytes = size))
        assertEquals(SmartSearch.Loading(42), SmartSearch.of(SmartSearch.Phase.RUNNING, true, size, 42))
        assertEquals(SmartSearch.Loading(null), SmartSearch.of(SmartSearch.Phase.WAITING, true, size))
        assertEquals(SmartSearch.Offer(219, failed = true), SmartSearch.of(SmartSearch.Phase.FAILED, true, size))
        assertEquals(SmartSearch.On, SmartSearch.of(SmartSearch.Phase.INSTALLED, fits = false, sizeBytes = size))
    }
}
