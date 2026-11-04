package app.hain.memory

import app.hain.memory.api.MemoryCategory
import app.hain.memory.internal.ChatLine
import app.hain.memory.internal.Dedupe
import app.hain.memory.internal.DefaultMemoryStore
import app.hain.memory.internal.ExtractionBrain
import app.hain.memory.internal.ExtractionJson
import app.hain.memory.internal.ExtractionPrompts
import app.hain.memory.internal.FactExtractor
import app.hain.memory.internal.InMemoryFactSource
import app.hain.memory.internal.PlainSwitch
import app.hain.memory.internal.Relation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-114: background extraction with a fake brain: suggestions only, dedupe against kept facts. */
@OptIn(ExperimentalCoroutinesApi::class)
class FactExtractorTest {
    private val prompts = ExtractionPrompts(extract = "EXTRACT", compare = "COMPARE")
    private val chat =
        listOf(
            ChatLine(true, "Ich spiele Gitarre und meine Schwester heißt Rosa."),
            ChatLine(false, "Schön!"),
        )

    /** Answers the extraction with [facts] (JSON) and every comparison with [label]. */
    private class FakeBrain(
        private val facts: String,
        private val label: String = """{"label":"new"}""",
    ) : ExtractionBrain {
        val asked = mutableListOf<Pair<String, String>>()

        override suspend fun complete(
            system: String,
            user: String,
            grammar: String,
            maxTokens: Int,
        ): String {
            asked += system to user
            return if (system == "EXTRACT") facts else label
        }
    }

    private fun facts(vararg texts: String) =
        texts.joinToString(",", """{"facts":[""", "]}") { """{"text":"$it","category":"other","sensitive":false}""" }

    @Test
    fun foundFactsOnlyBecomeSuggestions() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            val brain = FakeBrain(facts("spielt Gitarre", "Schwester heißt Rosa"))
            val out = FactExtractor(store, brain, prompts).run(chat, chatId = 4)
            assertEquals(listOf("spielt Gitarre", "Schwester heißt Rosa"), out.suggested.map { it.text })
            assertTrue(store.facts.value.all { it.suggested && it.originChatId == 4L })
            // nothing is used in answers until the user says yes
            assertNull(store.context("Gitarre", "de"))
            // the transcript went to the brain, Pim's lines marked as such
            assertEquals("User: Ich spiele Gitarre und meine Schwester heißt Rosa.\nPim: Schön!", brain.asked[0].second)
        }

    @Test
    fun exactAndNearDuplicatesOfKeptFactsAreSkipped() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            store.remember("spielt Gitarre")
            // "spielt Gitarre" is kept already; "Spielt Gitarre!" is the same text; the brain says "same" to the rest
            val brain = FakeBrain(facts("Spielt Gitarre!", "spielt gern Gitarre"), """{"label":"same"}""")
            val out = FactExtractor(store, brain, prompts).run(chat, null)
            assertTrue(out.suggested.isEmpty())
            assertEquals(2, out.skipped)
            assertEquals(listOf("spielt Gitarre"), store.facts.value.map { it.text })
        }

    @Test
    fun anUpdateWaitsAsSuggestionAndReplacesTheOldFactOnYes() =
        runTest(UnconfinedTestDispatcher()) {
            val source = InMemoryFactSource()
            val store = DefaultMemoryStore(source, backgroundScope)
            val old = store.remember("wohnt in Köln")!!
            val brain = FakeBrain(facts("wohnt jetzt in Bonn"), """{"label":"update"}""")
            val out = FactExtractor(store, brain, prompts).run(chat, null)
            val suggestion = out.suggested.single()
            assertEquals(old.id, suggestion.replaces)
            // the old fact is still in use while the suggestion waits
            assertEquals(
                setOf("wohnt in Köln", "wohnt jetzt in Bonn"),
                store.facts.value
                    .map { it.text }
                    .toSet(),
            )
            val asked = "Old: wohnt in Köln\nNew: wohnt jetzt in Bonn"
            assertTrue(brain.asked.any { it.first == "COMPARE" && it.second == asked })

            store.accept(suggestion.id)
            assertEquals(listOf("wohnt jetzt in Bonn"), store.facts.value.map { it.text })
            assertFalse(
                store.facts.value
                    .single()
                    .suggested,
            )
            // never overwritten: the old row is kept, only out of use
            assertEquals(suggestion.id, source.superseded[old.id])
        }

    @Test
    fun withTheEmbedderTheCosineThresholdsDecide() =
        runTest(UnconfinedTestDispatcher()) {
            // angles: 0° = same (cos 1.0), 30° = in between (0.87), 120° = different (−0.5, 0 to Bonn)
            val embedder =
                FakeEmbedder(
                    mapOf(
                        "köln" to FakeEmbedder.at(0.0),
                        "kölner" to FakeEmbedder.at(0.0),
                        "bonn" to FakeEmbedder.at(30.0),
                        "hund" to FakeEmbedder.at(120.0),
                    ),
                )
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope, PlainSwitch(), embedder)
            store.remember("wohnt in Köln")
            val found = facts("ist Kölnerin", "zieht nach Bonn", "hat einen Hund")
            val brain = FakeBrain(found, """{"label":"contradiction"}""")
            val out = FactExtractor(store, brain, prompts).run(chat, null)
            // "ist Kölnerin" ≥ 0.92 → duplicate, no question; "Bonn" 0.87 → asked; "Hund" ≤ 0 → new, no question
            assertEquals(listOf("zieht nach Bonn", "hat einen Hund"), out.suggested.map { it.text })
            assertEquals(1, brain.asked.count { it.first == "COMPARE" })
            assertEquals(
                store.facts.value
                    .first { it.text == "wohnt in Köln" }
                    .id,
                out.suggested[0].replaces,
            )
            assertNull(out.suggested[1].replaces)
        }

    @Test
    fun nothingHappensWhenTheMemoryIsOffOrTheBrainFails() =
        runTest(UnconfinedTestDispatcher()) {
            val off = DefaultMemoryStore(InMemoryFactSource(), backgroundScope, PlainSwitch(false))
            val brain = FakeBrain(facts("spielt Gitarre"))
            assertTrue(FactExtractor(off, brain, prompts).run(chat, null).suggested.isEmpty())
            assertTrue(brain.asked.isEmpty())

            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            val broken = ExtractionBrain { _, _, _, _ -> null }
            assertTrue(FactExtractor(store, broken, prompts).run(chat, null).suggested.isEmpty())
            assertTrue(FactExtractor(store, FakeBrain("not json"), prompts).run(chat, null).suggested.isEmpty())
        }

    @Test
    fun jsonIsReadStrictlyAndHealthIsSensitive() {
        val facts =
            ExtractionJson.parseFacts(
                """noise {"facts":[{"text":"Der Nutzer hat Asthma","category":"health","sensitive":false},""" +
                    """{"text":"x","category":"other","sensitive":false},{"category":"work"},""" +
                    """{"text":"mag Mathe","category":"bogus","sensitive":true}]} trailing""",
            )
        assertEquals(listOf("hat Asthma", "mag Mathe"), facts.map { it.text })
        assertEquals(MemoryCategory.HEALTH, facts[0].category)
        assertTrue(facts[0].sensitive)
        assertEquals(MemoryCategory.OTHER, facts[1].category)
        assertTrue(facts[1].sensitive)
        assertTrue(ExtractionJson.parseFacts("""{"facts":"no list"}""").isEmpty())
        assertEquals(Relation.UPDATE, ExtractionJson.parseLabel("""{"label":"update"}"""))
        assertNull(ExtractionJson.parseLabel("""{"label":"maybe"}"""))
    }

    @Test
    fun wordSimilarityWithoutTheEmbedder() {
        assertEquals(1.0, Dedupe.wordSimilarity("spielt Gitarre", "Spielt Gitarre!"), 1e-9)
        assertTrue(Dedupe.wordSimilarity("wohnt in Köln", "spielt Gitarre") == 0.0)
        val half = Dedupe.asCosine(Dedupe.wordSimilarity("wohnt in Köln", "wohnt jetzt in Bonn"))
        assertTrue(half > Dedupe.DIFFERENT && half < Dedupe.DUPLICATE)
    }

    @Test
    fun theTranscriptKeepsTheLastTwelveMessages() {
        val lines = (1..20).map { ChatLine(it % 2 == 1, "m$it") }
        val text = FactExtractor.transcript(lines)
        assertEquals(12, text.lines().size)
        assertTrue(text.startsWith("Pim: m9") || text.startsWith("User: m9"))
    }
}
