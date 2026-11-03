package app.hain.llm.internal

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

/** Values for the placeholders of `prompts/system_*.txt` (LLM.md §9). Blank or `null` = left out. */
data class SystemPromptVars(
    val weekday: String? = null,
    val date: String? = null,
    val time: String? = null,
    val timezone: String? = null,
    val city: String? = null,
    val emergencyNumber: String? = null,
    val memorySnippets: String? = null,
    val userCustomInstructions: String? = null,
    val languageRule: String? = null,
) {
    companion object {
        /**
         * Builds the time-related values for [now], formatted with java.time in the app language
         * [appLocale] (`Dienstag` / `28. Oktober 2026`, `Tuesday` / `28 October 2026`), and the emergency
         * number for the device region [regionCountry] (ISO 3166 alpha-2, e.g. `DE`).
         * City, memory and custom instructions are added with [copy].
         */
        fun of(
            now: ZonedDateTime,
            appLocale: Locale,
            regionCountry: String?,
        ): SystemPromptVars =
            SystemPromptVars(
                weekday = now.dayOfWeek.getDisplayName(TextStyle.FULL, appLocale),
                date = now.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(appLocale)),
                time = now.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(appLocale)),
                timezone = now.zone.id,
                emergencyNumber = EmergencyNumbers.forCountry(regionCountry),
            )
    }
}

/** Emergency number by region (LLM.md §9): 112 in the EU and by default, 911 US/Canada, 999 UK. */
object EmergencyNumbers {
    const val DEFAULT = "112"
    private val NUMBERS =
        mapOf(
            "US" to "911",
            "CA" to "911",
            "GB" to "999",
        )

    fun forCountry(country: String?): String = NUMBERS[country?.uppercase(Locale.ROOT)] ?: DEFAULT
}

/** `SystemPrompt.fill` from LLM.md §9: replaces placeholders, drops missing values, tidies blank lines. */
object SystemPromptFiller {
    private val TRIPLE_NEWLINES = Regex("""\n{3,}""")

    fun fill(
        template: String,
        vars: SystemPromptVars,
    ): String {
        val values =
            mapOf(
                "{weekday}" to vars.weekday,
                "{date}" to vars.date,
                "{time}" to vars.time,
                "{timezone}" to vars.timezone,
                "{city}" to vars.city,
                "{emergency_number}" to (vars.emergencyNumber ?: EmergencyNumbers.DEFAULT),
                "{memory_snippets}" to vars.memorySnippets,
                "{user_custom_instructions}" to vars.userCustomInstructions,
                "{language_rule}" to vars.languageRule,
            )
        var result = template
        values.forEach { (placeholder, value) -> result = result.replace(placeholder, value?.trim().orEmpty()) }
        return result.replace(TRIPLE_NEWLINES, "\n\n").trimEnd() + if (template.endsWith("\n")) "\n" else ""
    }
}
