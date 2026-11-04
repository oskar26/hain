package app.hain.memory

import app.hain.memory.api.MemoryFact
import app.hain.memory.internal.Ranking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RankingTest {
    private val now = 1_800_000_000_000L
    private val day = 86_400_000L

    private fun fact(
        id: Long,
        text: String,
        daysAgo: Long = 1,
        suggested: Boolean = false,
    ) = MemoryFact(id = id, text = text, createdAt = now - daysAgo * day, suggested = suggested)

    private val facts =
        listOf(
            fact(1, "mag keine Pilze"),
            fact(2, "spielt Gitarre im Schulorchester"),
            fact(3, "Schwester heißt Rosa, Bruder heißt Artur"),
            fact(4, "wohnt in Köln"),
            fact(5, "plays football on Saturdays"),
        )

    @Test
    fun stemsMatchLooseEndings() {
        assertTrue(Ranking.matches("pilz", "pilze"))
        assertTrue(Ranking.matches("gitarre", "gitarrenunterricht"))
        assertTrue(Ranking.matches("schwestern", "schwester"))
        assertFalse(Ranking.matches("rot", "rosa"))
        assertFalse(Ranking.matches("koch", "köln"))
    }

    @Test
    fun stopWordsAreDropped() {
        assertEquals(listOf("rezept", "pilzen"), Ranking.tokens("Hast du ein Rezept mit Pilzen für mich?"))
    }

    @Test
    fun theFittingFactWinsAndUnrelatedOnesStayOut() {
        val chosen = Ranking.select(facts, "Hast du ein Rezept mit Pilzen?", now)
        assertEquals(listOf(1L), chosen.map { it.fact.id })

        val music = Ranking.select(facts, "Welche Gitarre soll ich kaufen?", now)
        assertEquals(listOf(2L), music.map { it.fact.id })

        assertTrue(Ranking.select(facts, "Wie hoch ist der Eiffelturm?", now).isEmpty())
        assertEquals(listOf(5L), Ranking.select(facts, "Any football tips?", now).map { it.fact.id })
    }

    @Test
    fun suggestionsAreNeverUsed() {
        val list = listOf(fact(1, "hat Asthma", suggested = true))
        assertTrue(Ranking.select(list, "Asthma Sport", now).isEmpty())
    }

    @Test
    fun oldFactsStillCountWhenTheyMatch() {
        val list = listOf(fact(1, "mag keine Pilze", daysAgo = 720))
        assertEquals(1, Ranking.select(list, "Pilze", now).size)
        assertEquals(0.5, Ranking.recency(now - 90 * day, now), 1e-9)
    }

    @Test
    fun betterMatchRanksFirstAndBudgetIsKept() {
        val list =
            listOf(
                fact(1, "Bruder heißt Artur"),
                fact(2, "Bruder Artur spielt Handball mit Artur"),
            )
        val chosen = Ranking.select(list, "Was macht Artur beim Handball?", now)
        assertEquals(2L, chosen.first().fact.id)

        val many = (1L..20L).map { fact(it, "Pilze Sorte $it") }
        assertEquals(Ranking.MAX_FACTS, Ranking.select(many, "Pilze", now).size)
        assertEquals(2, Ranking.select(many, "Pilze", now, maxChars = 30).size)
    }

    @Test
    fun hybridScoreFollowsTheSpecWithACosine() {
        assertEquals(0.6 + 0.25 + 0.1 + 0.05, Ranking.score(1.0, 1.0, 1.0, cosine = 1.0), 1e-9)
        assertTrue(Ranking.score(0.0, 1.0, 1.0) < Ranking.MIN_SCORE)
        assertTrue(Ranking.score(0.5, 0.0, 0.0) >= Ranking.MIN_SCORE)
    }
}
