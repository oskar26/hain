package app.hain.memory.api

import app.hain.llm.api.ToolSchema
import app.hain.tools.api.Tool
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolResult
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * `remember` (T-112, MEMORY.md §2.2): the brain keeps a fact the user asked it to keep. Offline, on
 * the phone. Sensitive facts and health only become suggestions the user accepts on the memory page.
 */
class RememberTool(
    private val store: MemoryStore,
) : Tool {
    override val online = false
    override val schema =
        ToolSchema(
            name = NAME,
            description =
                "Save one lasting fact about the user when they ask you to remember it, " +
                    "as a short sentence in their language, e.g. 'mag keine Pilze'.",
            parameters =
                buildJsonObject {
                    put("type", "object")
                    putJsonObject("properties") {
                        putJsonObject("text") {
                            put("type", "string")
                            put("description", "The fact, at most 200 characters")
                        }
                        putJsonObject("category") {
                            put("type", "string")
                            putJsonArray("enum") { MemoryCategory.entries.forEach { add(JsonPrimitive(it.key)) } }
                        }
                        putJsonObject("sensitive") {
                            put("type", "boolean")
                            put("description", "true for health, beliefs or other private things")
                        }
                    }
                    putJsonArray("required") { add(JsonPrimitive("text")) }
                },
        )

    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val text =
            args["text"]?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
                ?: throw IllegalArgumentException("Missing 'text'.")
        if (!store.enabled.value) {
            return ToolResult(
                forModel = json("error" to "The memory is switched off in the settings."),
                error = "memory_off",
            )
        }
        val category = MemoryCategory.fromKey(args["category"]?.toString())
        val flagged = args["sensitive"].let { it == true || it?.toString() == "true" }
        val sensitive = flagged || category == MemoryCategory.HEALTH
        val fact =
            store.remember(text, category, sensitive, suggested = sensitive)
                ?: throw IllegalArgumentException("Nothing to remember.")
        val forModel =
            if (fact.suggested) {
                json("suggested" to fact.text, "note" to PRIVATE_NOTE)
            } else {
                json("saved" to fact.text)
            }
        return ToolResult(forModel = forModel, label = fact.text)
    }

    private fun json(vararg pairs: Pair<String, String>): String {
        val fields = pairs.associate { (key, value) -> key to JsonPrimitive(value) }
        return JsonObject(fields).toString()
    }

    companion object {
        const val NAME = "remember"
        private const val PRIVATE_NOTE = "Private: the user confirms it under Settings → Memory first."
    }
}
