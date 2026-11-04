package app.hain.llm.internal

import app.hain.llm.api.ParsedOutput
import app.hain.llm.api.UiDelta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StreamSplitterTest {
    private val withTool =
        "Der Nutzer fragt nach dem Wetter.\n</think>\n\nIch schaue nach.\n" +
            "<tool_call>\n<function=weather>\n<parameter=city>\nKöln\n</parameter>\n</function>\n</tool_call>"
    private val plain = "Kurz nachgedacht: a < b, also <t ok.</think>Antwort mit <b>Tag</b> und ü ö ä."
    private val noThink = "Hallo! <tool nicht> und </think-artiges> bleibt Text."

    private data class Run(
        val deltas: List<UiDelta>,
        val parsed: ParsedOutput,
    )

    private fun runChunks(
        output: String,
        thinking: Boolean,
        chunks: List<String>,
    ): Run {
        val splitter = StreamSplitter(thinkingEnabled = thinking)
        val deltas = chunks.flatMap { splitter.feed(it) }.toMutableList()
        val (rest, parsed) = splitter.finish()
        deltas += rest
        assertEquals(output, chunks.joinToString(""))
        return Run(deltas, parsed)
    }

    private fun charByChar(s: String) = s.map { it.toString() }

    private fun randomChunks(
        s: String,
        seed: Int,
    ): List<String> {
        val random = Random(seed)
        val out = mutableListOf<String>()
        var i = 0
        while (i < s.length) {
            val n = random.nextInt(1, 16).coerceAtMost(s.length - i)
            out += s.substring(i, i + n)
            i += n
        }
        return out
    }

    private fun reasoning(deltas: List<UiDelta>): String {
        val parts = deltas.filterIsInstance<UiDelta.Reasoning>()
        return parts.joinToString("") { it.text }
    }

    private fun text(deltas: List<UiDelta>) = deltas.filterIsInstance<UiDelta.Text>().joinToString("") { it.text }

    private fun assertSplit(
        output: String,
        thinking: Boolean,
        reasoning: String,
        text: String,
        tools: List<String?>,
    ) {
        val chunkings = listOf(listOf(output), charByChar(output)) + (1..20).map { randomChunks(output, it) }
        for (chunks in chunkings) {
            val run = runChunks(output, thinking, chunks)
            assertEquals("reasoning for $chunks", reasoning, reasoning(run.deltas))
            assertEquals("text for $chunks", text, text(run.deltas))
            assertEquals(tools, run.deltas.filterIsInstance<UiDelta.ToolCallStarted>().map { it.name })
        }
    }

    @Test
    fun reasoningTextAndToolCall() {
        assertSplit(withTool, true, "Der Nutzer fragt nach dem Wetter.", "Ich schaue nach.", listOf("weather"))
    }

    @Test
    fun partialTagLookalikesAreReleasedAsText() {
        assertSplit(
            plain,
            true,
            "Kurz nachgedacht: a < b, also <t ok.",
            "Antwort mit <b>Tag</b> und ü ö ä.",
            emptyList(),
        )
    }

    @Test
    fun thinkingOffStartsInText() {
        assertSplit(noThink, false, "", noThink, emptyList())
    }

    @Test
    fun streamedPartsMatchFinalParse() {
        val run = runChunks(withTool, true, randomChunks(withTool, 7))
        assertEquals(run.parsed.reasoning, reasoning(run.deltas))
        assertEquals(run.parsed.text, text(run.deltas))
        assertEquals(
            "weather",
            run.parsed.toolCalls
                .single()
                .name,
        )
    }

    @Test
    fun neverHoldsMoreThanTwelveChars() {
        val splitter = StreamSplitter(thinkingEnabled = false)
        val deltas = splitter.feed("abc</tool_cal")
        assertEquals("abc</tool_cal", text(deltas))
        val held = StreamSplitter(thinkingEnabled = false).feed("abc<tool_cal")
        assertEquals("abc", text(held))
    }

    @Test
    fun unterminatedToolCallIsRepaired() {
        // 4d: the brain stopped before </parameter></function></tool_call>; the call still runs
        val output = "</think>Moment.<tool_call>\n<function=weather>\n<parameter=city>\nKöln"
        val run = runChunks(output, true, charByChar(output))
        assertEquals(listOf<String?>("weather"), run.deltas.filterIsInstance<UiDelta.ToolCallStarted>().map { it.name })
        assertEquals(
            mapOf("city" to "Köln"),
            run.parsed.toolCalls
                .single()
                .arguments,
        )
        assertTrue(run.parsed.errors.isEmpty())
        assertEquals("Moment.", run.parsed.text)
    }

    @Test
    fun unreadableToolCallStaysText() {
        val output = "</think>Moment.<tool_call>\nkaputt"
        val run = runChunks(output, true, charByChar(output))
        assertTrue(run.parsed.toolCalls.isEmpty())
        assertTrue(run.parsed.errors.isNotEmpty())
        assertTrue(run.parsed.text.startsWith("Moment."))
    }

    @Test
    fun toolBlockWithoutFunctionAnnouncesNullName() {
        val output = "</think><tool_call>\nkaputt\n</tool_call>"
        val run = runChunks(output, true, listOf(output))
        assertEquals(listOf<String?>(null), run.deltas.filterIsInstance<UiDelta.ToolCallStarted>().map { it.name })
    }
}
