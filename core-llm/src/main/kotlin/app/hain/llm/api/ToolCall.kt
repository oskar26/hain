package app.hain.llm.api

/**
 * One tool call the model asked for (LLM.md §3).
 *
 * @property id stable id inside one assistant turn (`call_0`, `call_1`, …); tool results point back to it.
 * @property arguments argument values: [String], [Boolean], [Int], [Long], [Double], `null`,
 *   [Map] (String keys) or [List] — never a JSON-encoded string.
 */
data class ToolCall(
    val id: String,
    val name: String,
    val arguments: Map<String, Any?>,
)
