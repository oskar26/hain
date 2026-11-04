package app.hain.llm.internal

/**
 * Prompt text kept out of the code (4d, T-184): `assets/prompts/tools.<de|en>.txt` with sections
 * like `[tools]`, `[examples]`, `[nudge]`, `[offline]`. Lines starting with `//` are comments.
 * The host eval (`tools/eval/tool_eval.py`) reads the same files with the same rules.
 */
class PromptTemplates(
    private val sections: Map<String, String>,
) {
    operator fun get(name: String): String = sections[name].orEmpty()

    /** Tool rules for the system prompt: the rules and, online, the example dialogs. */
    fun toolRules(offline: Boolean): String =
        if (offline) {
            get(OFFLINE)
        } else {
            listOf(get(TOOLS), get(EXAMPLES)).filter { it.isNotBlank() }.joinToString("\n\n")
        }

    companion object {
        const val TOOLS = "tools"
        const val EXAMPLES = "examples"
        const val NUDGE = "nudge"
        const val OFFLINE = "offline"
        private val HEADER = Regex("""^\[([a-z_]+)]\s*$""")

        fun parse(text: String): PromptTemplates {
            val out = linkedMapOf<String, String>()
            var name: String? = null
            val buf = StringBuilder()

            fun flush() {
                name?.let { out[it] = buf.toString().trim() }
                buf.clear()
            }
            text.lineSequence().forEach { line ->
                val header = HEADER.find(line)
                when {
                    line.startsWith("//") -> Unit
                    header != null -> {
                        flush()
                        name = header.groupValues[1]
                    }
                    name != null -> buf.append(line).append('\n')
                }
            }
            flush()
            return PromptTemplates(out)
        }
    }
}
