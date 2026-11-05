package app.hain.llm.internal

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
import org.junit.Assert.assertTrue
import org.junit.Test

class LfmPromptBuilderTest {
    private val builder = LfmPromptBuilder()

    @Test fun g01Plain() = assertGolden("G-01_plain")

    @Test fun g02System() = assertGolden("G-02_system")

    @Test fun g03Tools() = assertGolden("G-03_tools")

    @Test fun g04ToolLoop() = assertGolden("G-04_toolloop")

    @Test fun g05MultiturnThinkingDropped() = assertGolden("G-05_multiturn_thinking_dropped")

    @Test fun g06NoThink() = assertGolden("G-06_nothink")

    // T-260: LFM3-Omni 2B uses this builder; the same scenarios through the Omni Jinja (thinking off)
    @Test fun o01Plain() = assertGolden("omni/O-01_plain")

    @Test fun o02System() = assertGolden("omni/O-02_system")

    @Test fun o03Tools() = assertGolden("omni/O-03_tools")

    @Test fun o04ToolLoop() = assertGolden("omni/O-04_toolloop")

    @Test fun o05Multiturn() = assertGolden("omni/O-05_multiturn_thinking_dropped")

    @Test fun o06NoThink() = assertGolden("omni/O-06_nothink")

    @Test
    fun noSystemTurnWithoutSystemAndTools() {
        val prompt = builder.build(null, emptyList(), listOf(ChatMessage(Role.USER, "Hi")))
        assertEquals("<|startoftext|><|im_start|>user\nHi<|im_end|>\n<|im_start|>assistant\n<think>", prompt)
    }

    @Test
    fun toolsWithoutSystemPromptHaveNoLeadingBlankLine() {
        val tool = ToolSchema("t", "d", buildJsonObject { put("type", JsonPrimitive("object")) })
        val prompt = builder.build("", listOf(tool), emptyList(), addGenerationPrompt = false)
        assertEquals(
            "<|startoftext|><|im_start|>system\n<tools>\n" +
                "{\"name\": \"t\", \"description\": \"d\", \"parameters\": {\"type\": \"object\"}}\n" +
                "</tools><|im_end|>\n",
            prompt,
        )
    }

    @Test
    fun oldAssistantKeepsOnlyTextAfterLastThinkClose() {
        val history =
            listOf(
                ChatMessage(Role.USER, "a"),
                ChatMessage(Role.ASSISTANT, "<think>x</think> old </think>  answer "),
                ChatMessage(Role.USER, "b"),
            )
        val prompt = builder.build(null, emptyList(), history, addGenerationPrompt = false)
        assertTrue(prompt, prompt.contains("<|im_start|>assistant\n<think></think>answer<|im_end|>"))
    }

    @Test
    fun consecutiveResultsForSameCallAreMerged() {
        val history =
            listOf(
                ChatMessage(Role.USER, "q"),
                ChatMessage(
                    Role.ASSISTANT,
                    "",
                    toolCalls = listOf(ToolCall("call_0", "x", emptyMap())),
                    reasoning = "",
                ),
                ChatMessage(Role.TOOL, "part 1", toolCallId = "call_0"),
                ChatMessage(Role.TOOL, "part 2", toolCallId = "call_0"),
            )
        val prompt = builder.build(null, emptyList(), history, addGenerationPrompt = false)
        assertTrue(
            prompt,
            prompt.endsWith(
                "<think></think><tool_call>\n<function=x>\n</function>\n</tool_call><|im_end|>\n" +
                    "<|im_start|>tool\n<tool_response>\npart 1\npart 2\n</tool_response><|im_end|>\n",
            ),
        )
    }

    @Test
    fun jsonEscapesLikePython() {
        val schema = buildJsonObject { put("d", JsonPrimitive("a\"b\\c\nä\u0001")) }
        assertEquals("{\"d\": \"a\\\"b\\\\c\\nä\\u0001\"}", PyJson.encode(schema))
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
        requireNotNull(javaClass.classLoader?.getResource("golden/$name")) { "missing golden/$name" }
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
