package app.hain.llm.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The 8 cases of LLM.md §5, plus strictness checks. */
class XmlToolCallParserTest {
    private val parser = XmlToolCallParser()

    private fun call(
        name: String,
        vararg params: Pair<String, String>,
    ): String =
        "<tool_call>\n<function=$name>\n" +
            params.joinToString("") { (k, v) -> "<parameter=$k>\n$v\n</parameter>\n" } +
            "</function>\n</tool_call>"

    @Test
    fun case1OneCallOneStringParam() {
        val out = parser.parse("Wetter gefragt.</think>" + call("weather", "city" to "Köln"))
        assertEquals("Wetter gefragt.", out.reasoning)
        assertEquals(1, out.toolCalls.size)
        assertEquals("weather", out.toolCalls[0].name)
        assertEquals(mapOf("city" to "Köln"), out.toolCalls[0].arguments)
        assertEquals("", out.text)
        assertTrue(out.errors.isEmpty())
    }

    @Test
    fun case2IntParam() {
        val out = parser.parse("</think>" + call("weather", "city" to "Köln", "days" to "2"))
        val days = out.toolCalls.single().arguments["days"]
        assertEquals(2, days)
        assertTrue(days is Int)
    }

    @Test
    fun case3TwoCallsInOrder() {
        val raw = "</think>" + call("weather", "city" to "Köln") + "\n\n" + call("news", "topic" to "Klima")
        val out = parser.parse(raw)
        assertEquals(listOf("weather", "news"), out.toolCalls.map { it.name })
        assertEquals(listOf("call_0", "call_1"), out.toolCalls.map { it.id })
    }

    @Test
    fun case4TextBeforeCall() {
        val out = parser.parse("ok</think>\nIch schaue nach.\n" + call("weather", "city" to "Köln"))
        assertEquals("Ich schaue nach.", out.text)
        assertEquals(1, out.toolCalls.size)
    }

    @Test
    fun case5MissingCloseTagExecutesNothing() {
        val raw =
            "Ich schaue nach.\n<tool_call>\n<function=weather>\n<parameter=city>\n" +
                "Köln\n</parameter>\n</function>\n"
        val out = parser.parse("</think>$raw")
        assertTrue(out.toolCalls.isEmpty())
        assertTrue(out.errors.isNotEmpty())
        assertEquals(raw.trim(), out.text)
    }

    @Test
    fun case6ValueWithLessThanKeptLiterally() {
        val out = parser.parse("</think>" + call("calculator", "expression" to "a < b && <c>"))
        assertEquals("a < b && <c>", out.toolCalls.single().arguments["expression"])
    }

    @Test
    fun case7JsonObjectParam() {
        val out = parser.parse("</think>" + call("x", "filter" to "{\"a\":1,\"b\":[true,null,\"s\"],\"c\":1.5}"))
        val expected = mapOf("a" to 1, "b" to listOf(true, null, "s"), "c" to 1.5)
        assertEquals(expected, out.toolCalls.single().arguments["filter"])
    }

    @Test
    fun case8NoThinkBlock() {
        val out = parser.parse("Hallo! Wie kann ich helfen?")
        assertNull(out.reasoning)
        assertEquals("Hallo! Wie kann ich helfen?", out.text)
        assertTrue(out.toolCalls.isEmpty())
    }

    @Test
    fun leadingZeroAndPlainWordsStayStrings() {
        assertEquals("01234", XmlToolCallParser.toValue("01234"))
        assertEquals("Köln", XmlToolCallParser.toValue("Köln"))
        assertEquals("2 Tage", XmlToolCallParser.toValue("2 Tage"))
        assertEquals(false, XmlToolCallParser.toValue("false"))
        assertNull(XmlToolCallParser.toValue("null"))
        assertEquals(5_000_000_000L, XmlToolCallParser.toValue("5000000000"))
        assertEquals("{broken", XmlToolCallParser.toValue("{broken"))
    }

    @Test
    fun missingFunctionHeaderIsAnError() {
        val out = parser.parse("</think><tool_call>\nweather Köln\n</tool_call>")
        assertTrue(out.toolCalls.isEmpty())
        assertEquals(1, out.errors.size)
    }

    @Test
    fun oneBadCallBlocksAllCalls() {
        val bad = "<tool_call>\n<function=bad name>\n</function>\n</tool_call>"
        val out = parser.parse("</think>" + call("weather", "city" to "Köln") + "\n\n" + bad)
        assertTrue(out.toolCalls.isEmpty())
        assertTrue(out.errors.isNotEmpty())
    }

    @Test
    fun duplicateParameterIsAnError() {
        val out = parser.parse("</think>" + call("w", "city" to "A", "city" to "B"))
        assertTrue(out.toolCalls.isEmpty())
    }

    @Test
    fun roundTripsWhatThePromptBuilderRenders() {
        val args =
            linkedMapOf<String, Any?>("q" to "Köln \"morgen\"", "n" to 3, "on" to true, "f" to mapOf("lang" to "de"))
        val rendered =
            LfmPromptBuilder().build(
                null,
                emptyList(),
                listOf(
                    app.hain.llm.api
                        .ChatMessage(app.hain.llm.api.Role.USER, "x"),
                    app.hain.llm.api.ChatMessage(
                        app.hain.llm.api.Role.ASSISTANT,
                        "Text",
                        toolCalls =
                            listOf(
                                app.hain.llm.api
                                    .ToolCall("call_0", "web_search", args),
                            ),
                        reasoning = "r",
                    ),
                ),
                addGenerationPrompt = false,
            )
        val output = rendered.substringAfter("<|im_start|>assistant\n<think>").removeSuffix("<|im_end|>\n")
        val parsed = parser.parse(output)
        assertEquals("r", parsed.reasoning)
        assertEquals("Text", parsed.text)
        assertEquals(args, parsed.toolCalls.single().arguments)
    }
}
