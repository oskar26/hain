package app.hain.llm.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.PromptBuilder
import app.hain.llm.api.Role
import app.hain.llm.api.ToolCall
import app.hain.llm.api.ToolSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * T-241: Pure-Kotlin re-implementation of the Qwen3.5 chat template (ChatML with `<tool_call>` XML),
 * used by the "Großes Gehirn" Underdog Poodle 4B (a Qwen3.5-4B fine-tune). Same output as the Jinja
 * template in the GGUF for text, tool calls and tool results; differences on purpose:
 *
 * - Tools come first in the system turn (as in the template), then Hain's system prompt. So the
 *   fixed part of the prompt (tools + rules, before "# Kontext") is one block that is cached and kept
 *   as a snapshot on disk (T-242).
 * - The system text is used as given (not trimmed), so the chat engine finds it in the prompt.
 * - A later system message (language hint) becomes its own system turn; the template would refuse it.
 * - Booleans in tool arguments are written as JSON (`true`), like in the LFM prompt.
 *
 * Thinking off = an empty think block `<think>\n\n</think>\n\n` after the assistant header, exactly
 * like `enable_thinking=false` in the template.
 */
class QwenPromptBuilder : PromptBuilder {
    override fun build(
        system: String?,
        tools: List<ToolSchema>,
        history: List<ChatMessage>,
        addGenerationPrompt: Boolean,
        enableThinking: Boolean,
    ): String {
        val out = StringBuilder()
        appendSystem(system.orEmpty(), tools, out)
        val lastUser = history.indexOfLast { it.role == Role.USER }
        history.forEachIndexed { index, message ->
            when (message.role) {
                Role.SYSTEM ->
                    out
                        .append(IM_START)
                        .append("system\n")
                        .append(message.content.trim())
                        .append(IM_END_NL)
                Role.USER ->
                    out
                        .append(IM_START)
                        .append("user\n")
                        .append(message.content.trim())
                        .append(IM_END_NL)
                Role.ASSISTANT -> appendAssistant(message, index > lastUser, out)
                Role.TOOL -> {
                    val previous = history.getOrNull(index - 1)
                    val next = history.getOrNull(index + 1)
                    if (previous?.role != Role.TOOL) out.append(IM_START).append("user")
                    out
                        .append("\n<tool_response>\n")
                        .append(message.content.trim())
                        .append("\n</tool_response>")
                    if (next?.role != Role.TOOL) out.append(IM_END_NL)
                }
            }
        }
        if (addGenerationPrompt) {
            out.append(IM_START).append("assistant\n").append(if (enableThinking) THINK_START else EMPTY_THINK)
        }
        return out.toString()
    }

    private fun appendSystem(
        system: String,
        tools: List<ToolSchema>,
        out: StringBuilder,
    ) {
        if (tools.isEmpty()) {
            if (system.isNotBlank()) {
                out
                    .append(IM_START)
                    .append("system\n")
                    .append(system)
                    .append(IM_END_NL)
            }
            return
        }
        out.append(IM_START).append("system\n").append(TOOLS_HEAD)
        tools.forEach { out.append('\n').append(toolLine(it)) }
        out.append("\n</tools>").append(TOOLS_RULES)
        if (system.isNotBlank()) out.append("\n\n").append(system)
        out.append(IM_END_NL)
    }

    /** One tool as the template's `tool | tojson` writes an OpenAI-style function entry. */
    private fun toolLine(tool: ToolSchema): String =
        PyJson.encode(
            buildJsonObject {
                put("type", JsonPrimitive("function"))
                put(
                    "function",
                    buildJsonObject {
                        put("name", JsonPrimitive(tool.name))
                        put("description", JsonPrimitive(tool.description))
                        put("parameters", tool.parameters)
                    },
                )
            },
        )

    private fun appendAssistant(
        message: ChatMessage,
        afterLastUser: Boolean,
        out: StringBuilder,
    ) {
        var content = message.content.trim()
        var reasoning = message.reasoning.orEmpty()
        if (message.reasoning == null && THINK_CLOSE in content) {
            reasoning =
                content
                    .substringBefore(THINK_CLOSE)
                    .trimEnd('\n')
                    .substringAfterLast(THINK_OPEN)
                    .trimStart('\n')
            content = content.substringAfterLast(THINK_CLOSE).trimStart('\n')
        }
        out.append(IM_START).append("assistant\n")
        if (afterLastUser) {
            out
                .append(THINK_OPEN)
                .append('\n')
                .append(reasoning.trim())
                .append("\n</think>\n\n")
        }
        out.append(content)
        appendToolCalls(message.toolCalls, content.isNotBlank(), out)
        out.append(IM_END_NL)
    }

    private fun appendToolCalls(
        calls: List<ToolCall>,
        hasText: Boolean,
        out: StringBuilder,
    ) {
        calls.forEachIndexed { index, call ->
            when {
                index > 0 -> out.append('\n')
                hasText -> out.append("\n\n")
            }
            out.append("<tool_call>\n<function=").append(call.name).append(">\n")
            call.arguments.forEach { (name, value) ->
                out
                    .append("<parameter=")
                    .append(name)
                    .append(">\n")
                    .append(renderValue(value))
                    .append("\n</parameter>\n")
            }
            out.append("</function>\n</tool_call>")
        }
    }

    /** Template: maps and lists as JSON, everything else as plain text. */
    private fun renderValue(value: Any?): String =
        when (value) {
            null -> "null"
            is String -> value
            is Boolean -> if (value) "true" else "false"
            is Map<*, *>, is Iterable<*>, is kotlinx.serialization.json.JsonElement -> PyJson.encodeValue(value)
            else -> value.toString()
        }

    companion object {
        const val IM_START = "<|im_start|>"
        const val IM_END = "<|im_end|>"
        private const val IM_END_NL = "$IM_END\n"
        private const val THINK_OPEN = "<think>"
        private const val THINK_CLOSE = "</think>"
        private const val THINK_START = "<think>\n"
        const val EMPTY_THINK = "<think>\n\n</think>\n\n"
        private const val TOOLS_HEAD = "# Tools\n\nYou have access to the following functions:\n\n<tools>"
        private const val TOOLS_RULES =
            "\n\nIf you choose to call a function ONLY reply in the following format with NO suffix:\n\n" +
                "<tool_call>\n<function=example_function_name>\n<parameter=example_parameter_1>\nvalue_1\n" +
                "</parameter>\n<parameter=example_parameter_2>\nThis is the value for the second parameter\n" +
                "that can span\nmultiple lines\n</parameter>\n</function>\n</tool_call>\n\n<IMPORTANT>\n" +
                "Reminder:\n- Function calls MUST follow the specified format: an inner <function=...></function> " +
                "block must be nested within <tool_call></tool_call> XML tags\n" +
                "- Required parameters MUST be specified\n" +
                "- You may provide optional reasoning for your function call in natural language BEFORE the " +
                "function call, but NOT after\n" +
                "- If there is no function call available, answer the question like normal with your current " +
                "knowledge and do not tell the user about function calls\n</IMPORTANT>"
    }
}
