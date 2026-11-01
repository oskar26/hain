package app.hain.llm.api

/**
 * The finished output of one generation, split into its parts (LLM.md §3–§4).
 *
 * @property reasoning text inside `<think>…</think>`, trimmed; `null` when the output had no think block.
 * @property text the visible answer, trimmed. When tool-call XML was malformed this is the whole raw answer.
 * @property toolCalls the calls to run, in order. Always empty when [errors] is not empty.
 * @property errors parse problems; non-empty means: show [text], log, execute nothing.
 */
data class ParsedOutput(
    val reasoning: String?,
    val text: String,
    val toolCalls: List<ToolCall>,
    val errors: List<String> = emptyList(),
)
