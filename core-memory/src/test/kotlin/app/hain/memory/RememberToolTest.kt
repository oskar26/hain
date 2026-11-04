package app.hain.memory

import app.hain.memory.api.RememberTool
import app.hain.memory.internal.DefaultMemoryStore
import app.hain.memory.internal.InMemoryFactSource
import app.hain.tools.api.ToolContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
}
