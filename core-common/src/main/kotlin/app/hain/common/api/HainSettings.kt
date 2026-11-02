package app.hain.common.api

/** Colour scheme choice (SETTINGS.md `theme`). */
enum class ThemeChoice { SYSTEM, LIGHT, DARK }

/** Language Hain answers in (SETTINGS.md `answer_language`). */
enum class AnswerLanguage { AUTO, DE, EN }

/** Style of the answers (SETTINGS.md `answer_mode`). */
enum class AnswerMode { AUTO, FAST, THOROUGH }

/** Everything the user can change in the settings (T-100). Defaults follow SETTINGS.md. */
data class HainSettings(
    val theme: ThemeChoice = ThemeChoice.SYSTEM,
    val readableFont: Boolean = false,
    val answerLanguage: AnswerLanguage = AnswerLanguage.AUTO,
    val answerMode: AnswerMode = AnswerMode.AUTO,
    val aboutMe: String = "",
    val style: String = "",
) {
    /** Text for the "user instructions" slot of the system prompt; empty if nothing is set. */
    fun customInstructions(deviceLanguage: String = "English"): String =
        buildList {
            when (answerLanguage) {
                AnswerLanguage.DE -> add("Always answer in German.")
                AnswerLanguage.EN -> add("Always answer in English.")
                AnswerLanguage.AUTO ->
                    add("Answer in the language the user writes in. If unsure, answer in $deviceLanguage.")
            }
            if (aboutMe.isNotBlank()) add("About the user: ${aboutMe.trim()}")
            if (style.isNotBlank()) add("How the user wants answers: ${style.trim()}")
        }.joinToString("\n")
}
