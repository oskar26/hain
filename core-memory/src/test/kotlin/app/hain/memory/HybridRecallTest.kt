package app.hain.memory

import app.hain.memory.api.EmbedKind
import app.hain.memory.api.MemoryFact
import app.hain.memory.internal.DefaultMemoryStore
import app.hain.memory.internal.InMemoryFactSource
import app.hain.memory.internal.InMemoryVectorSource
import app.hain.memory.internal.PlainSwitch
import app.hain.memory.internal.Ranking
import app.hain.memory.internal.Vectors
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-110: hybrid ranking (cosine + BM25 + recency + importance) and the keyword fallback without the pack. */
@OptIn(ExperimentalCoroutinesApi::class)
class HybridRecallTest {
    private val now = 1_800_000_000_000L

    // the question about pets and the dog fact point the same way; the guitar fact far away
    private val embedder =
        FakeEmbedder(
            mapOf(
                "haustier" to FakeEmbedder.at(0.0),
                "hund" to FakeEmbedder.at(20.0),
                "gitarre" to FakeEmbedder.at(90.0),
                "pilze" to FakeEmbedder.at(80.0),
            ),
        )

    @Test
    fun hybridScoreFollowsTheSpecWeights() {
        // 0.6·cos + 0.25·BM25 + 0.1·recency + 0.05·importance
        assertEquals(0.6 + 0.25 + 0.1 + 0.05, Ranking.score(1.0, 1.0, 1.0, cosine = 1.0), 1e-9)
        assertEquals(0.6 * 0.5 + 0.1 + 0.025, Ranking.score(0.0, 1.0, 0.5, cosine = 0.5), 1e-9)
    }

    @Test
    fun meaningFindsAFactWithoutSharedWords() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope, PlainSwitch(), embedder) { now }
            store.remember("hat einen Hund namens Bello")
            store.remember("spielt Gitarre")
            val ctx = store.recall("Wie kümmere ich mich um mein Haustier?", "de", now)!!
            assertEquals(listOf("hat einen Hund namens Bello"), ctx.used.map { it.text })
            // the question was embedded as a query, the facts as documents
            assertTrue(embedder.calls.any { it.second == EmbedKind.QUERY && it.first.contains("Haustier") })
            assertTrue(embedder.calls.any { it.second == EmbedKind.DOCUMENT && it.first.contains("Hund") })
            // without the pack the same question finds nothing (no shared word)
            assertNull(store.context("Wie kümmere ich mich um mein Haustier?", "de", now))
        }

    @Test
    fun withoutThePackRecallIsKeywordOnly() =
        runTest(UnconfinedTestDispatcher()) {
            embedder.ready = false
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope, PlainSwitch(), embedder) { now }
            store.remember("mag keine Pilze")
            store.remember("hat einen Hund namens Bello")
            val used = store.recall("Rezept mit Pilzen?", "de", now)!!.used
            assertEquals(listOf("mag keine Pilze"), used.map { it.text })
            assertNull(store.recall("Wie kümmere ich mich um mein Haustier?", "de", now))
            assertTrue(embedder.calls.isEmpty())
            assertTrue(!store.smartSearch())
        }

    @Test
    fun keywordAndMeaningTogetherBeatEitherAlone() {
        val day = 86_400_000L
        val facts =
            listOf(
                MemoryFact(1, "hat einen Hund namens Bello", createdAt = now - day),
                MemoryFact(2, "mag Hundefilme", createdAt = now - day),
                MemoryFact(3, "spielt Gitarre", createdAt = now - day),
            )
        val cos = mapOf(1L to 0.9, 2L to 0.3, 3L to 0.0)
        val chosen = Ranking.select(facts, "Mein Hund ist krank", now, cosine = { cos[it.id] })
        assertEquals(1L, chosen.first().fact.id)
        assertTrue(chosen.none { it.fact.id == 3L })
    }

    @Test
    fun vectorsAreStoredOnceAndRenewedAfterAnEdit() =
        runTest(UnconfinedTestDispatcher()) {
            val vectors = InMemoryVectorSource()
            val store =
                DefaultMemoryStore(InMemoryFactSource(), backgroundScope, PlainSwitch(), embedder, vectors) { now }
            val fact = store.remember("hat einen Hund")!!
            assertEquals(listOf(fact.id), vectors.all().map { it.factId })
            assertEquals(Vectors.textHash("hat einen Hund"), vectors.all().single().textHash)
            store.edit(fact.id, "hat zwei Hunde")
            assertEquals(Vectors.textHash("hat zwei Hunde"), vectors.all().single().textHash)
            store.delete(fact.id)
            assertTrue(vectors.all().isEmpty())
        }

    @Test
    fun nearestGivesTheClosestFactByMeaning() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope, PlainSwitch(), embedder) { now }
            store.remember("hat einen Hund")
            store.remember("spielt Gitarre")
            val near = store.nearest("liebt sein Haustier")!!
            assertEquals("hat einen Hund", near.fact.text)
            assertEquals(Math.cos(Math.toRadians(20.0)), near.cosine, 1e-5)
        }
}
