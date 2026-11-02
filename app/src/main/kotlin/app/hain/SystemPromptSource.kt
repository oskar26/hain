package app.hain

import android.content.Context
import app.hain.llm.internal.SystemPromptFiller
import app.hain.llm.internal.SystemPromptVars
import java.time.ZonedDateTime
import java.util.Locale

/** Fills `assets/prompts/system_full.en.txt` for each turn (LLM.md §9). */
class SystemPromptSource(
    private val context: Context,
    private val customInstructions: () -> String = { "" },
) {
    private val template by lazy {
        context.assets
            .open("prompts/system_full.en.txt")
            .bufferedReader()
            .use { it.readText() }
    }

    fun current(): String {
        val locale = context.resources.configuration.locales[0] ?: Locale.getDefault()
        val vars =
            SystemPromptVars
                .of(ZonedDateTime.now(), locale, locale.country.ifEmpty { null })
                .copy(userCustomInstructions = instructions())
        return SystemPromptFiller.fill(template, vars)
    }

    private fun instructions(): String =
        listOf(NO_TOOLS_YET, customInstructions())
            .filter { it.isNotBlank() }
            .joinToString("\n")

    private companion object {
        // MVP: the tool loop (T-062) is not wired yet, so tell the model it has no tools.
        const val NO_TOOLS_YET =
            "In this version no tools are connected. Answer from your own knowledge and say " +
                "openly when something may be outdated or when you would need the internet."
    }
}
