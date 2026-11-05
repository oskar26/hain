package app.hain.memory

import app.hain.memory.api.RememberTool
import app.hain.memory.internal.DefaultMemoryStore
import app.hain.memory.internal.InMemoryFactSource
import app.hain.tools.api.ToolContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class RememberToolTest {
    private val ctx = ToolContext("de", ZonedDateTime.now(), offline = true)

    @Test
    fun savesAFactAndShowsIt() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            val tool = RememberTool(store)
            assertEquals("remember", tool.schema.name)
            val result = tool.run(mapOf("text" to "spielt Gitarre", "category" to "preference"), ctx)
            assertNull(result.error)
            assertEquals("spielt Gitarre", result.label)
            assertEquals("""{"saved":"spielt Gitarre"}""", result.forModel)
            assertEquals(
                "spielt Gitarre",
                store.facts.value
                    .single()
                    .text,
            )
        }

    @Test
    fun sensitiveFactsBecomeSuggestions() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            val result = RememberTool(store).run(mapOf("text" to "hat Asthma", "sensitive" to true), ctx)
            assertTrue(result.forModel.contains("\"suggested\""))
            assertTrue(
                store.facts.value
                    .single()
                    .suggested,
            )
        }

    @Test
    fun privateChatSavesNothing() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            val result = RememberTool(store) { true }.run(mapOf("text" to "spielt Gitarre"), ctx)
            assertEquals("private_chat", result.error)
            assertTrue(store.facts.value.isEmpty())
        }

    @Test
    fun memoryOffIsAnError() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            store.setEnabled(false)
            assertEquals("memory_off", RememberTool(store).run(mapOf("text" to "x y z"), ctx).error)
        }

    @Test(expected = IllegalArgumentException::class)
    fun missingTextIsRefused() =
        runTest(UnconfinedTestDispatcher()) {
            val store = DefaultMemoryStore(InMemoryFactSource(), backgroundScope)
            RememberTool(store).run(emptyMap(), ctx)
            Unit
        }

    @Test
    fun rememberOnlyRunsForOwnStatementsOrWhenAsked() {
        // T-271: never for a topic question; a statement the chip offers is left to the chip
        assertFalse(RememberTool.allowed("Lerntipps für Mathe", "Lerntipps für Mathe: regelmäßig üben"))
        assertFalse(RememberTool.allowed("Bastelidee mit Altpapier", "Bastelidee mit Altpapier"))
        assertFalse(RememberTool.allowed("hi ich lerne gerade spanisch", "lernt Spanisch"))
        assertTrue(RememberTool.allowed("Kannst du dir bitte merken, dass mein Bus um 7 fährt?", "Bus fährt um 7"))
        assertTrue(RememberTool.allowed("Ich trinke morgens immer Tee", "trinkt morgens Tee"))
        assertFalse(RememberTool.allowed("Ich trinke morgens immer Tee", "mag Kaffee"))
    }
}
