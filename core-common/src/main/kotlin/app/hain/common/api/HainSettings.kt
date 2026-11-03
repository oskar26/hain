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
    val reduceMotion: Boolean = false,
    val answerLanguage: AnswerLanguage = AnswerLanguage.AUTO,
    val answerMode: AnswerMode = AnswerMode.AUTO,
    val aboutMe: String = "",
    val style: String = "",
    /** Offline mode: no tool goes online; only a download you start yourself (SETTINGS.md `offline_mode`). */
    val offlineMode: Boolean = false,
    /** Network log keeps full addresses, not just the host (SETTINGS.md `network_log_detail`). */
    val networkLogDetail: Boolean = false,
    /** Own SearXNG address, asked first by the web search (SETTINGS.md `searxng_url`); empty = none. */
    val searxUrl: String = "",
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

/** The SearXNG address a user typed, made usable (4b). */
object SearxAddress {
    private val HOST = Regex("""^[a-z0-9]([a-z0-9-]*[a-z0-9])?(\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)+(:\d{1,5})?(/.*)?$""")

    /**
     * "searx.example.org/" → "https://searx.example.org"; a trailing "/search" or "?…" is cut off.
     * `null` for blank input, `http://` (Hain only talks https) or anything that is not an address.
     */
    @Suppress("ReturnCount")
    fun normalize(input: String): String? {
        var t = input.trim()
        if (t.isEmpty() || t.any { it.isWhitespace() }) return null
        if (t.startsWith("http://", ignoreCase = true)) return null
        t = t.removePrefix("https://").removePrefix("HTTPS://")
        t =
            t
                .substringBefore('?')
                .substringBefore('#')
                .trimEnd('/')
                .removeSuffix("/search")
                .trimEnd('/')
        if (!HOST.matches(t.lowercase())) return null
        return "https://$t"
    }
}
