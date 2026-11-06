package app.hain.memory

import app.hain.memory.api.MemoryCategory
import app.hain.memory.internal.DefaultMemoryStore
import app.hain.memory.internal.InMemoryFactSource
import app.hain.memory.internal.PlainSwitch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MemoryStoreTest {
    private var now = 1_000L

    private fun TestScope.store(switch: PlainSwitch = PlainSwitch()) =
        DefaultMemoryStore(InMemoryFactSource(), backgroundScope, switch) { now++ }

    @Test
    fun rememberKeepsOneCopyAndShowsNewestFirst() =
        runTest(UnconfinedTestDispatcher()) {
            val store = store()
            val first = store.remember("mag keine Pilze")
            store.remember("  Mag keine Pilze! ")
            store.remember("spielt Gitarre")
            assertEquals(listOf("spielt Gitarre", "mag keine Pilze"), store.facts.value.map { it.text })
            assertEquals(
                first?.id,
                store.facts.value
                    .last()
                    .id,
            )
        }

    @Test
    fun healthAndSensitiveFactsWaitAsSuggestions() =
        runTest(UnconfinedTestDispatcher()) {
            val store = store()
            val fact = store.remember("hat Asthma", MemoryCategory.HEALTH, suggested = true)!!
            assertTrue(fact.sensitive)
            assertTrue(
                store.facts.value
                    .single()
                    .suggested,
            )
            assertNull(store.context("Asthma beim Sport", "de"))
            store.accept(fact.id)
            assertNotNull(store.context("Asthma beim Sport", "de"))
        }

    @Test
    fun editDeleteRestoreAndDeleteAll() =
        runTest(UnconfinedTestDispatcher()) {
            val store = store()
            val fact = store.remember("wohnt in Köln")!!
            store.edit(fact.id, "wohnt in Bonn")
            assertEquals(
                "wohnt in Bonn",
                store.facts.value
                    .single()
                    .text,
            )
            val kept = store.facts.value.single()
            store.delete(kept.id)
            assertTrue(store.facts.value.isEmpty())
            store.restore(kept)
            assertEquals(
                "wohnt in Bonn",
                store.facts.value
                    .single()
                    .text,
            )
            store.remember("spielt Gitarre")
            store.deleteAll()
            assertTrue(store.facts.value.isEmpty())
        }

    @Test
    fun forgetRemovesTheMatchingFactOnly() =
        runTest(UnconfinedTestDispatcher()) {
            val store = store()
            store.remember("mag keine Pilze")
            store.remember("spielt Gitarre")
            assertEquals(listOf("mag keine Pilze"), store.forget("dass ich Pilze nicht mag").map { it.text })
            assertEquals(listOf("spielt Gitarre"), store.facts.value.map { it.text })
            assertTrue(store.forget("Fußball").isEmpty())
        }

    @Test
    fun offMeansNothingIsReadOrWritten() =
        runTest(UnconfinedTestDispatcher()) {
            val switch = PlainSwitch()
            val store = store(switch)
            store.remember("mag keine Pilze")
            store.setEnabled(false)
            assertEquals(false, switch.get())
            assertNull(store.remember("spielt Gitarre"))
            assertNull(store.context("Pilze", "de"))
            assertEquals(1, store.facts.value.size)
        }

    @Test
    fun contextBlockListsFittingFactsAsSentences() =
        runTest(UnconfinedTestDispatcher()) {
            val store = store()
            store.remember("mag keine Pilze")
            store.remember("spielt Gitarre")
            val context = store.context("Rezept mit Pilzen", "de", now = 2_000L)!!
            assertEquals(listOf("mag keine Pilze"), context.used.map { it.text })
            assertTrue(context.block.startsWith("Das weißt du über den Nutzer:"))
            assertTrue(context.block.contains("- Der Nutzer mag keine Pilze."))
            assertTrue(context.block.endsWith("des Orts und der Beispiele."))
        }

    @Test
    fun exportAndImportRoundTrip() =
        runTest(UnconfinedTestDispatcher()) {
            val store = store()
            store.remember("mag keine Pilze", MemoryCategory.PREFERENCE)
            store.remember("hat Asthma", MemoryCategory.HEALTH, suggested = true)
            val json = store.exportJson()

            val other = store()
            assertEquals(2, other.importJson(json))
            assertEquals(0, other.importJson(json))
            val imported = other.facts.value.associateBy { it.text }
            assertEquals(MemoryCategory.PREFERENCE, imported.getValue("mag keine Pilze").category)
            assertTrue(imported.getValue("hat Asthma").suggested)
        }

    @Test(expected = IllegalArgumentException::class)
    fun importRefusesOtherFiles() =
        runTest(UnconfinedTestDispatcher()) {
            store().importJson("""{"facts":[]}""")
            Unit
        }
}
