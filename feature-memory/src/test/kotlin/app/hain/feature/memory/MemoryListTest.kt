package app.hain.feature.memory

import app.hain.feature.memory.api.MemoryList
import app.hain.memory.api.MemoryCategory
import app.hain.memory.api.MemoryFact
import org.junit.Assert.assertEquals
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
}
