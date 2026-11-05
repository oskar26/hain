package app.hain.llm.internal

import app.hain.llm.api.BrainFormat
import app.hain.llm.api.ChatMessage
import app.hain.llm.api.Role
import app.hain.llm.api.ToolCall
import app.hain.llm.api.ToolSchema
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-241: the Qwen3.5 prompt equals the GGUF's Jinja template (golden files from tools/render_golden_qwen.py). */
class QwenPromptBuilderTest {
    private val builder = QwenPromptBuilder()

    @Test
    fun q01Plain() {
        assertGolden("Q-01_plain")
    }

    @Test
    fun q02System() {
        assertGolden("Q-02_system")
    }

    @Test
    fun q03Tools() {
        assertGolden("Q-03_tools")
    }

    @Test
    fun q04ToolLoop() {
        assertGolden("Q-04_toolloop")
    }

    @Test
    fun q05Multiturn() {
        assertGolden("Q-05_multiturn")
    }

    @Test
    fun q06Thinking() {
        assertGolden("Q-06_thinking")
    }

    @Test
    fun toolsComeBeforeTheSystemTextSoTheFixedPartCanBeCached() {
        val tool = ToolSchema("t", "d", buildJsonObject { put("type", JsonPrimitive("object")) })
        val system = "Du bist Pim.\n\n# Kontext\nJetzt: 07:30"
        val prompt = builder.build(system, listOf(tool), listOf(ChatMessage(Role.USER, "Hi")))
        assertTrue(prompt.indexOf("</tools>") < prompt.indexOf("# Kontext"))
        // the system text is used as given, so the chat engine finds it (fixed-system checkpoint)
        assertTrue(prompt.contains(system))
    }

    @Test
    fun emptyHistoryPromptIsAPrefixOfTheFullPrompt() {
        val tool = ToolSchema("t", "d", buildJsonObject { put("type", JsonPrimitive("object")) })
        val head = builder.build("S", listOf(tool), emptyList(), addGenerationPrompt = false, enableThinking = false)
        val full = builder.build("S", listOf(tool), listOf(ChatMessage(Role.USER, "Hi")), true, false)
        assertTrue(full.startsWith(head))
        assertTrue(full.endsWith("<|im_start|>assistant\n<think>\n\n</think>\n\n"))
    }

    @Test
    fun booleansInToolArgumentsAreJson() {
        val history =
            listOf(
                ChatMessage(Role.USER, "q"),
                ChatMessage(Role.ASSISTANT, "", listOf(ToolCall("call_0", "x", mapOf("on" to true)))),
            )
        val prompt = builder.build(null, emptyList(), history, addGenerationPrompt = false)
        assertTrue(prompt.contains("<parameter=on>\ntrue\n</parameter>"))
    }

    @Test
    fun laterSystemMessageBecomesItsOwnTurn() {
        val history = listOf(ChatMessage(Role.USER, "Hallo"), ChatMessage(Role.SYSTEM, "Antworte auf Deutsch."))
        val prompt = builder.build(null, emptyList(), history, true, false)
        assertTrue(prompt.contains("<|im_start|>system\nAntworte auf Deutsch.<|im_end|>\n<|im_start|>assistant\n"))
    }

    @Test
    fun formatFollowsTheGgufArchitecture() {
        assertEquals(BrainFormat.QWEN, BrainFormat.ofArchitecture("qwen35"))
        assertEquals(BrainFormat.LFM, BrainFormat.ofArchitecture("lfm2"))
        assertEquals(BrainFormat.LFM, BrainFormat.ofArchitecture(null))
        assertFalse(BrainFormat.QWEN.mayThink)
        assertTrue(BrainFormat.LFM.mayThink)
        assertEquals(0.6f, BrainFormat.QWEN.chatSampling.temperature)
        assertEquals(20, BrainFormat.QWEN.chatSampling.topK)
        assertTrue(BrainFormat.QWEN.promptBuilder() is QwenPromptBuilder)
        assertTrue(BrainFormat.LFM.promptBuilder() is LfmPromptBuilder)
    }

    @Test
    fun outputOfTheBrainIsReadByTheSameToolCallParser() {
        val out = "<tool_call>\n<function=set_alarm>\n<parameter=time>\n06:30\n</parameter>\n</function>\n</tool_call>"
        val parsed = XmlToolCallParser().parse(out)
        assertEquals("set_alarm", parsed.toolCalls.single().name)
        assertEquals("06:30", parsed.toolCalls.single().arguments["time"])
    }

    private fun assertGolden(name: String) {
        val scenario = Json.parseToJsonElement(resource("$name.json")).jsonObject
        val expected = resource("$name.txt")
        val actual =
            builder.build(
                system = scenario["system"]?.jsonPrimitive?.contentOrNull,
                tools = scenario.getValue("tools").jsonArray.map { it.toToolSchema() },
                history = scenario.getValue("messages").jsonArray.map { it.toMessage() },
                addGenerationPrompt = scenario.getValue("add_generation_prompt").jsonPrimitive.boolean,
                enableThinking = scenario.getValue("enable_thinking").jsonPrimitive.boolean,
            )
        assertEquals("golden $name", expected, actual)
    }

    private fun resource(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("golden/qwen/$name")) { "missing golden/qwen/$name" }
            .readBytes()
            .toString(Charsets.UTF_8)

    private fun JsonElement.toToolSchema(): ToolSchema {
        val o = jsonObject
        return ToolSchema(
            name = o.getValue("name").jsonPrimitive.content,
            description = o.getValue("description").jsonPrimitive.content,
            parameters = o.getValue("parameters").jsonObject,
        )
    }

    private fun JsonElement.toMessage(): ChatMessage {
        val o = jsonObject
        val calls =
            o["tool_calls"]?.jsonArray.orEmpty().mapIndexed { i, c ->
                val call = c.jsonObject
                ToolCall(
                    id = "call_$i",
                    name = call.getValue("name").jsonPrimitive.content,
                    arguments = call.getValue("arguments").jsonObject.mapValues { it.value.toKotlin() },
                )
            }
        return ChatMessage(
            role = Role.entries.first { it.wire == o.getValue("role").jsonPrimitive.content },
            content = o.getValue("content").jsonPrimitive.content,
            toolCalls = calls,
            reasoning = o["reasoning"]?.jsonPrimitive?.contentOrNull,
            toolCallId = o["tool_call_id"]?.jsonPrimitive?.contentOrNull,
        )
    }

    private fun JsonElement.toKotlin(): Any? =
        when (this) {
            is JsonNull -> null
            is JsonObject -> mapValues { it.value.toKotlin() }
            is JsonArray -> map { it.toKotlin() }
            is JsonPrimitive ->
                when {
                    isString -> content
                    booleanOrNull != null -> boolean
                    longOrNull != null -> longOrNull!!.toInt()
                    else -> content.toDouble()
                }
        }
}
