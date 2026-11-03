package app.hain

import android.content.Context
import app.hain.common.api.HainSettings
import app.hain.llm.internal.MessageLanguage
import app.hain.llm.internal.ReplyLanguage
import app.hain.llm.internal.ReplyLanguagePolicy
import app.hain.llm.internal.SystemPromptFiller
import app.hain.llm.internal.SystemPromptVars
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/**
 * Fills `assets/prompts/system_full.<de|en>.txt` for each turn (LLM.md §9). The prompt is written in the
 * language Pim should answer in (German setting, or "Automatisch" on a German phone) and ends with the
 * language rule; [turnHint] names the language of the next answer right before it is generated.
 */
class SystemPromptSource(
    private val context: Context,
    private val settings: () -> HainSettings = { HainSettings() },
) {
    private val templates = mutableMapOf<ReplyLanguage, String>()

    private fun template(language: ReplyLanguage): String =
        templates.getOrPut(language) {
            context.assets
                .open("prompts/system_full.${language.tag}.txt")
                .bufferedReader()
                .use { it.readText() }
        }

    private fun deviceLocale(): Locale = context.resources.configuration.locales[0] ?: Locale.getDefault()

    /** System prompt with the time [atMillis] (the chat engine moves it on only every half hour). */
    fun current(atMillis: Long = System.currentTimeMillis()): String {
        val device = deviceLocale()
        val current = settings()
        val language = ReplyLanguagePolicy.promptLanguage(current.answerLanguage, ReplyLanguage.fromLocale(device))
        val locale =
            Locale
                .Builder()
                .setLanguage(language.tag)
                .setRegion(device.country)
                .build()
        val at = ZonedDateTime.ofInstant(Instant.ofEpochMilli(atMillis), ZoneId.systemDefault())
        val vars =
            SystemPromptVars
                .of(at, locale, device.country.ifEmpty { null })
                .copy(
                    userCustomInstructions = instructions(language, current),
                    languageRule = ReplyLanguagePolicy.rule(language, current.answerLanguage),
                )
        return SystemPromptFiller.fill(template(language), vars)
    }

    /**
     * "Antworte auf Türkisch." / "Answer in Turkish." for the next answer: the language of the newest
     * clear user message in [userMessages] (oldest first), in the prompt's own language.
     */
    fun turnHint(userMessages: List<String>): String {
        val current = settings()
        val device = deviceLocale()
        val prompt = ReplyLanguagePolicy.promptLanguage(current.answerLanguage, ReplyLanguage.fromLocale(device))
        val target =
            ReplyLanguagePolicy.replyLanguage(current.answerLanguage, userMessages, MessageLanguage.fromLocale(device))
        return ReplyLanguagePolicy.turnHint(prompt, target)
    }

    private fun instructions(
        language: ReplyLanguage,
        current: HainSettings,
    ): String {
        val german = language == ReplyLanguage.DE
        return buildList {
            add(if (german) NO_TOOLS_YET_DE else NO_TOOLS_YET_EN)
            if (current.aboutMe.isNotBlank()) {
                val label = if (german) "Der Nutzer über sich: " else "The user about themselves: "
                add(label + current.aboutMe.trim())
            }
            if (current.style.isNotBlank()) {
                val label = if (german) "So will der Nutzer Antworten: " else "How the user wants answers: "
                add(label + current.style.trim())
            }
        }.joinToString("\n")
    }

    private companion object {
        // MVP: the tool loop (T-062) is not wired yet, so tell the model it has no tools.
        const val NO_TOOLS_YET_DE =
            "Du hast in dieser Version kein Internet und keine Werkzeuge. Antworte aus deinem Wissen."
        const val NO_TOOLS_YET_EN =
            "In this version you have no internet and no tools. Answer from your own knowledge."
    }
}
