package app.hain.memory.api

import app.hain.llm.api.ToolSchema
import app.hain.memory.internal.SelfFacts
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
 * In a private chat ([privateChat], T-056) nothing is saved.
 */
class RememberTool(
    private val store: MemoryStore,
    /** A fact was kept (not a suggestion): the chat shows "Gemerkt ✓ · Rückgängig" (7b). */
    private val onSaved: (MemoryFact) -> Unit = {},
    private val privateChat: () -> Boolean = { false },
) : Tool {
    override val online = false
    override val schema =
        ToolSchema(
            name = NAME,
            description =
                "Only when the user asks you to remember something about themselves: save that one fact, " +
                    "short, in their language, e.g. 'mag keine Pilze'. Never for questions, tips or topics. " +
                    "Afterwards still answer the user.",
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

    @Suppress("ReturnCount") // private chat, memory off, saved
    override suspend fun run(
        args: Map<String, Any?>,
        ctx: ToolContext,
    ): ToolResult {
        val text =
            args["text"]?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
                ?: throw IllegalArgumentException("Missing 'text'.")
        if (privateChat()) {
            return ToolResult(
                forModel = json("error" to "This is a private chat: nothing is saved. Tell the user so."),
                error = "private_chat",
            )
        }
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
        if (!fact.suggested) onSaved(fact)
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

        /**
         * T-271: whether a `remember` call for [fact] may run for the user's [message]. Only when the
         * user asks to remember ("merk dir", "vergiss nicht", "remember") or says [fact] about
         * themselves; a self statement the rules already turn into the chip "Merken: …" is left to
         * that chip (the user decides). Topic questions ("Lerntipps für Mathe") never save anything.
         */
        fun allowed(
            message: String,
            fact: String,
        ): Boolean {
            if (asks(message)) return true
            val own = SelfFacts.selfSentences(listOf(message))
            return own.isNotEmpty() && SelfFacts.ruleFacts(listOf(message)).isEmpty() && SelfFacts.grounded(fact, own)
        }

        /** T-271: the user asks to remember something ("merk dir", "vergiss nicht", "remember"). */
        fun asks(message: String): Boolean = ASK.containsMatchIn(message)

        private val ASK =
            Regex(
                "(?iu)\\b(?:merk(?:e|st)?\\s+dir|merken|vergiss\\s+(?:es\\s+)?(?:bitte\\s+)?nicht|notier|speicher|" +
                    "remember|don['’]?t\\s+forget|keep\\s+in\\s+mind|save\\s+that)",
            )
        private const val PRIVATE_NOTE = "Private: the user confirms it under Settings → Memory first."
    }
}
