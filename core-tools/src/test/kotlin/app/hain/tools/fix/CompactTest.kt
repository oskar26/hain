package app.hain.tools.fix

import app.hain.llm.api.ToolCall
import app.hain.tools.api.Compact
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolRouter
import app.hain.tools.internal.phone.SetTimerTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class CompactTest {
    @Test
    fun keepsWholeSentences() {
        val text = "Eins ist hier. Zwei ist da. Drei folgt. Vier auch. Fünf ist zu viel."
        assertEquals("Eins ist hier. Zwei ist da. Drei folgt. Vier auch.", Compact.facts(text))
        assertEquals("Kurz.", Compact.facts("  Kurz. "))
        val long = "A".repeat(700)
        assertTrue(Compact.facts(long).length <= Compact.MAX_CHARS + 1)
    }

    @Test
    fun phoneActionSaysWhatToAnswer() =
        runTest {
            val ctx = ToolContext("de", ZonedDateTime.now(), offline = false)
            val r = ToolRouter(listOf(SetTimerTool())).run(ToolCall("1", "set_timer", mapOf("minutes" to 10)), ctx)
            assertTrue(r.forModel, r.forModel.contains("\"say\":\"Der Timer über 10 Min. ist vorbereitet"))
            assertTrue(r.forModel.contains("waiting_for_tap"))
        }
}
