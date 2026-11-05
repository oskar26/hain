package app.hain.llm.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.PromptBuilder
import app.hain.llm.api.Role
import app.hain.llm.api.ToolCall
import app.hain.llm.api.ToolSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * T-241: Pure-Kotlin re-implementation of the Qwen3 chat template (ChatML, tool calls as JSON inside
 * `<tool_call>`), used by the "Großes Gehirn" Jan-v3.5-4B (Qwen3-4B architecture, ADR-24). Output
 * equals the Jinja template stored in the Jan GGUF (`prompts/jan-v3.5-4b_chat_template.jinja`),
 * proven by the golden files Q-01…Q-06 (tools/render_golden_qwen.py). Layout as in the template and
 * as in Hain's LFM prompt: system text first, then the tool block, so the fixed part of the system
 * text (before "# Kontext") is cached and kept as a snapshot on disk (T-242).
 *
 * Differences on purpose:
 * - The system text is used as given (the chat engine finds it in the prompt for its checkpoints).
 * - A think block an older answer may carry (`…</think>`) is dropped from the history; Jan does not
 *   think, the template has no think handling at all.
 *
 * Jan is a non-thinking model: the template has no `enable_thinking`, so [build] ignores it and the
 * generation prompt is just the assistant header.
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
        appendSystem(system, tools, out)
        history.forEachIndexed { index, message ->
            when (message.role) {
                Role.SYSTEM, Role.USER ->
                    out
                        .append(IM_START)
                        .append(message.role.wire)
                        .append('\n')
                        .append(message.content)
                        .append(IM_END_NL)
                Role.ASSISTANT -> appendAssistant(message, out)
                Role.TOOL -> {
                    val previous = history.getOrNull(index - 1)
                    val next = history.getOrNull(index + 1)
                    if (previous?.role != Role.TOOL) out.append(IM_START).append("user")
                    out
                        .append("\n<tool_response>\n")
                        .append(message.content)
                        .append("\n</tool_response>")
                    if (next?.role != Role.TOOL) out.append(IM_END_NL)
                }
            }
        }
        if (addGenerationPrompt) out.append(IM_START).append("assistant\n")
        return out.toString()
    }

    private fun appendSystem(
        system: String?,
        tools: List<ToolSchema>,
        out: StringBuilder,
    ) {
        if (tools.isEmpty()) {
            if (system != null) {
                out
                    .append(IM_START)
                    .append("system\n")
                    .append(system)
                    .append(IM_END_NL)
            }
            return
        }
        out.append(IM_START).append("system\n")
        if (system != null) out.append(system).append("\n\n")
        out.append(TOOLS_HEAD)
        tools.forEach { out.append('\n').append(toolLine(it)) }
        out.append(TOOLS_TAIL).append(IM_END_NL)
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
        out: StringBuilder,
    ) {
        var content = message.content
        if (THINK_CLOSE in content) content = content.substringAfterLast(THINK_CLOSE).trimStart('\n')
        out.append(IM_START).append("assistant\n").append(content)
        appendToolCalls(message.toolCalls, content.isNotEmpty(), out)
        out.append(IM_END_NL)
    }

    private fun appendToolCalls(
        calls: List<ToolCall>,
        hasText: Boolean,
        out: StringBuilder,
    ) {
        calls.forEachIndexed { index, call ->
            if (index > 0 || hasText) out.append('\n')
            out
                .append("<tool_call>\n{\"name\": \"")
                .append(call.name)
                .append("\", \"arguments\": ")
                .append(PyJson.encodeValue(call.arguments))
                .append("}\n</tool_call>")
        }
    }

    companion object {
        const val IM_START = "<|im_start|>"
        const val IM_END = "<|im_end|>"
        private const val IM_END_NL = "$IM_END\n"
        private const val THINK_CLOSE = "</think>"
        private const val TOOLS_HEAD =
            "# Tools\n\nYou may call one or more functions to assist with the user query.\n\n" +
                "You are provided with function signatures within <tools></tools> XML tags:\n<tools>"
        private const val TOOLS_TAIL =
            "\n</tools>\n\nFor each function call, return a json object with function name and arguments " +
                "within <tool_call></tool_call> XML tags:\n<tool_call>\n" +
                "{\"name\": <function-name>, \"arguments\": <args-json-object>}\n</tool_call>"
    }
}
