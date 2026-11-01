package app.hain.llm.api

import kotlinx.serialization.json.JsonObject

/**
 * A tool the model may call, rendered into the system prompt as one JSON line
 * `{"name": .., "description": .., "parameters": ..}` (LLM.md §2 rule 2).
 *
 * @property parameters JSON Schema of the arguments; key order is kept as given.
 */
data class ToolSchema(
    val name: String,
    val description: String,
    val parameters: JsonObject,
)
