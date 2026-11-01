package app.hain.llm.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.PromptBuilder
import app.hain.llm.api.Role
import app.hain.llm.api.ToolCall
import app.hain.llm.api.ToolSchema
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Pure-Kotlin re-implementation of `prompts/lfm3-1.7b_chat_template.jinja` (LLM.md §2, rules 1–8).
 * Output must equal the Jinja output byte-for-byte; proven by the golden files G-01…G-06.
 */
class LfmPromptBuilder : PromptBuilder {
    override fun build(
        system: String?,
        tools: List<ToolSchema>,
        history: List<ChatMessage>,
        addGenerationPrompt: Boolean,
        enableThinking: Boolean,
    ): String {
        val out = StringBuilder(BOS)
        val systemBlock = systemBlock(system.orEmpty(), tools)
        if (systemBlock.isNotEmpty()) {
            out
                .append(IM_START)
                .append("system\n")
                .append(systemBlock)
                .append(IM_END)
                .append('\n')
        }
        val lastUserIndex = history.indexOfLast { it.role == Role.USER }
        var pendingToolTexts = mutableListOf<String>()
        history.forEachIndexed { index, message ->
            val next = history.getOrNull(index + 1)
            if (message.role == Role.TOOL && mergesWithNext(message, next)) {
                if (message.content.isNotEmpty()) pendingToolTexts.add(message.content)
                return@forEachIndexed
            }
            out.append(IM_START).append(message.role.wire).append('\n')
            when (message.role) {
                Role.ASSISTANT -> appendAssistant(message, keepThinking = index > lastUserIndex, out = out)
                Role.TOOL -> {
                    if (pendingToolTexts.isNotEmpty()) {
                        if (message.content.isNotEmpty()) pendingToolTexts.add(message.content)
                        out
                            .append(TOOL_RESPONSE_OPEN)
                            .append(pendingToolTexts.joinToString("\n"))
                            .append(TOOL_RESPONSE_CLOSE)
                        pendingToolTexts = mutableListOf()
                    } else {
                        out.append(toolResponse(message.content))
                    }
                }
                Role.SYSTEM, Role.USER -> out.append(message.content)
            }
            out.append(IM_END).append('\n')
        }
        if (addGenerationPrompt) {
            out.append(IM_START).append("assistant\n").append(if (enableThinking) THINK_OPEN else EMPTY_THINK)
        }
        return out.toString()
    }

    private fun systemBlock(
        system: String,
        tools: List<ToolSchema>,
    ): String {
        if (tools.isEmpty()) return system
        val lines = tools.joinToString("\n") { toolLine(it) }
        return system + (if (system.isNotEmpty()) "\n\n" else "") + "<tools>\n" + lines + "\n</tools>"
    }

    private fun toolLine(tool: ToolSchema): String =
        PyJson.encode(
            buildJsonObject {
                put("name", JsonPrimitive(tool.name))
                put("description", JsonPrimitive(tool.description))
                put("parameters", tool.parameters)
            },
        )

    /** Template: two adjacent tool results for the same call id are merged into one response. */
    private fun mergesWithNext(
        message: ChatMessage,
        next: ChatMessage?,
    ): Boolean =
        next != null &&
            next.role == Role.TOOL &&
            !message.toolCallId.isNullOrEmpty() &&
            next.toolCallId == message.toolCallId

    private fun toolResponse(content: String): String =
        if (TOOL_RESPONSE_TAG in content) content else TOOL_RESPONSE_OPEN + content + TOOL_RESPONSE_CLOSE

    private fun appendAssistant(
        message: ChatMessage,
        keepThinking: Boolean,
        out: StringBuilder,
    ) {
        if (keepThinking) {
            if (message.reasoning != null) {
                out.append(THINK_OPEN).append(message.reasoning).append(THINK_CLOSE)
            } else if (THINK_CLOSE !in message.content) {
                out.append(EMPTY_THINK)
            }
        }
        var content = message.content
        if (!keepThinking) {
            if (THINK_CLOSE in content) content = content.substringAfterLast(THINK_CLOSE).trim()
            content = EMPTY_THINK + content
        }
        val continueFinal = content.endsWith(CONTINUE_FINAL_TAG)
        if (continueFinal) content = content.dropLast(CONTINUE_FINAL_TAG.length)
        out.append(content)
        appendToolCalls(message.toolCalls, out)
        if (continueFinal) out.append(CONTINUE_FINAL_TAG)
    }

    private fun appendToolCalls(
        calls: List<ToolCall>,
        out: StringBuilder,
    ) {
        calls.forEachIndexed { index, call ->
            if (index > 0) out.append("\n\n")
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

    /** Template macro `render_value`: strings raw, bool/null as JSON words, maps/lists as JSON. */
    private fun renderValue(value: Any?): String =
        when (value) {
            null -> "null"
            is String -> value
            is Boolean -> if (value) "true" else "false"
            is Map<*, *>, is Iterable<*>, is kotlinx.serialization.json.JsonElement -> PyJson.encodeValue(value)
            else -> value.toString()
        }

    companion object {
        const val BOS = "<|startoftext|>"
        const val IM_START = "<|im_start|>"
        const val IM_END = "<|im_end|>"
        const val THINK_OPEN = "<think>"
        const val THINK_CLOSE = "</think>"
        const val EMPTY_THINK = "<think></think>"
        private const val TOOL_RESPONSE_TAG = "<tool_response>"
        private const val TOOL_RESPONSE_OPEN = "<tool_response>\n"
        private const val TOOL_RESPONSE_CLOSE = "\n</tool_response>"
        private const val CONTINUE_FINAL_TAG = "CONTINUE_FINAL_MESSAGE_TAG "
    }
}
