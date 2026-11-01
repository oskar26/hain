package app.hain.llm.api

/** Builds the raw model prompt; re-implements the model's chat template in pure Kotlin (LLM.md §2). */
interface PromptBuilder {
    fun build(
        system: String?,
        tools: List<ToolSchema>,
        history: List<ChatMessage>,
        addGenerationPrompt: Boolean = true,
        enableThinking: Boolean = true,
    ): String
}

/** Splits finished model output into reasoning, text and tool calls (LLM.md §3). */
interface ToolCallParser {
    fun parse(output: String): ParsedOutput
}
