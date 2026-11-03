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
            add(
                when {
                    current.offlineMode && german -> TOOLS_OFFLINE_DE
                    current.offlineMode -> TOOLS_OFFLINE_EN
                    german -> TOOLS_DE
                    else -> TOOLS_EN
                },
            )
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
        // Werkzeuge I+II (T-062, T-066, T-067): calculator offline, the rest online. Rule against copied
        // "looked it up" answers: every new question calls the tool again (Oskar's test of #19).
        const val TOOLS_DE =
            "Werkzeuge: calculate für genaues Rechnen und Einheiten, weather für Wetter, " +
                "wikipedia für Fakten über Personen, Orte und Dinge, web_search für Aktuelles und Details " +
                "(Hain liest dann die besten Seiten), read_page für einen Link. " +
                "Für jede neue Frage, die Fakten, Wetter oder Aktuelles braucht, rufe das Werkzeug neu auf, " +
                "auch wenn du es vorher schon benutzt hast. " +
                "Sag nie, dass du nachgeschaut hast, wenn du in dieser Antwort kein Werkzeug benutzt hast. " +
                "Antworte danach aus dem Ergebnis und nenne die Quellen als [1], [2]. " +
                "Klappt ein Werkzeug nicht, sag das ehrlich."
        const val TOOLS_EN =
            "Tools: calculate for exact maths and units, weather for forecasts, " +
                "wikipedia for facts about people, places and things, web_search for current news and details " +
                "(Hain then reads the best pages), read_page for a link. " +
                "For every new question that needs facts, weather or current events, call the tool again, " +
                "even if you used it before. " +
                "Never say you looked something up if you used no tool in this answer. " +
                "Then answer from the result and cite the sources as [1], [2]. If a tool fails, say so plainly."
        const val TOOLS_OFFLINE_DE =
            "Der Offline-Modus ist an: nur calculate geht. Für Wetter, Wikipedia oder die Suche sag, " +
                "dass der Nutzer den Offline-Modus in den Einstellungen ausschalten kann."
        const val TOOLS_OFFLINE_EN =
            "Offline mode is on: only calculate works. For weather, Wikipedia or search, say the user can " +
                "turn offline mode off in the settings."
    }
}
