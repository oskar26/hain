package app.hain.llm.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.PromptBuilder
import app.hain.llm.api.Role
import app.hain.llm.api.ToolSchema

/**
 * Prompt for an own model in developer mode (T-211): the chat template from the GGUF file, applied by
 * llama.cpp ([apply]). Tools are left out (untested models rarely call them correctly), tool results
 * and thinking are not sent back. When llama.cpp does not know the template, plain ChatML is used.
 */
class GgufPromptBuilder(
    private val apply: (messages: List<Pair<String, String>>, addAssistant: Boolean) -> String?,
) : PromptBuilder {
    override fun build(
        system: String?,
        tools: List<ToolSchema>,
        history: List<ChatMessage>,
        addGenerationPrompt: Boolean,
        enableThinking: Boolean,
    ): String {
        val messages = messagesOf(system, history)
        return apply(messages, addGenerationPrompt) ?: chatMl(messages, addGenerationPrompt)
    }

    companion object {
        /** System + user/assistant turns as (role, text); tool turns are dropped. */
        fun messagesOf(
            system: String?,
            history: List<ChatMessage>,
        ): List<Pair<String, String>> =
            buildList {
                if (!system.isNullOrBlank()) add("system" to system)
                history.forEach { m ->
                    when (m.role) {
                        Role.SYSTEM -> add("system" to m.content)
                        Role.USER -> add("user" to m.content)
                        Role.ASSISTANT -> if (m.content.isNotBlank()) add("assistant" to m.content)
                        else -> Unit
                    }
                }
            }

        /** ChatML, the format most GGUF chat models understand. */
        fun chatMl(
            messages: List<Pair<String, String>>,
            addAssistant: Boolean,
        ): String =
            buildString {
                messages.forEach { (role, text) ->
                    append("<|im_start|>")
                        .append(role)
                        .append('\n')
                        .append(text)
                        .append("<|im_end|>\n")
                }
                if (addAssistant) append("<|im_start|>assistant\n")
            }
    }
}
