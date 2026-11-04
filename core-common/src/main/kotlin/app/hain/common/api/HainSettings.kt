package app.hain.common.api

/** Colour scheme choice (SETTINGS.md `theme`). */
enum class ThemeChoice { SYSTEM, LIGHT, DARK }

/** Strength of the haptics (SETTINGS.md `haptics`, T-096): off, subtle (default), normal, strong. */
enum class HapticStrength { OFF, SUBTLE, NORMAL, STRONG }

/** Launcher icon "Pim im Wald" (SETTINGS.md `app_icon`, T-103): glossy 3D (default) or flat. */
enum class AppIcon { THREE_D, FLAT }

/** Language Hain answers in (SETTINGS.md `answer_language`). */
enum class AnswerLanguage { AUTO, DE, EN }

/** Style of the answers (SETTINGS.md `answer_mode`). */
enum class AnswerMode { AUTO, FAST, THOROUGH }

/** Web search (SETTINGS.md `web_search`, 3b-2): on, ask first in the chat, or off. */
enum class WebSearch { ON, ASK, OFF }

/** Update check (SETTINGS.md `update_check`, T-212): off (default) or once a day when Hain starts. */
enum class UpdateCheck { OFF, DAILY }

/** Which builds the update check looks at (SETTINGS.md `update_channel`): tagged releases or nightly. */
enum class UpdateChannel { STABLE, NIGHTLY }

/** Developer mode: think always, never, or let Hain decide (SETTINGS.md `dev_thinking`, T-211). */
enum class ThinkingOverride { AUTO, ON, OFF }

/**
 * Developer mode (T-211, Settings → Entwickler): hidden until "Version" in "Über Hain" is tapped 7×.
 * Only used while [enabled]; switched off, Hain runs with its defaults again (the values stay saved).
 * Defaults = what Hain uses anyway (LLM.md §6): LFM3-1.7B-QAD-Q4_0, temperature 0.1, top_k 50,
 * top_p 1, min_p 0, repeat penalty 1.1, context 8192 (T-088), 1024 answer tokens, random seed.
 */
data class DevSettings(
    val enabled: Boolean = false,
    val temperature: Float = 0.1f,
    val topK: Int = 50,
    val topP: Float = 1.0f,
    val minP: Float = 0.0f,
    val repeatPenalty: Float = 1.1f,
    val contextSize: Int = DEFAULT_CONTEXT,
    val maxTokens: Int = 1024,
    /** -1 = a new random seed per answer. */
    val seed: Int = -1,
    val thinking: ThinkingOverride = ThinkingOverride.AUTO,
    /** Most tokens an answer with thinking may use (thinking + answer). */
    val thinkingBudget: Int = 4096,
    /** Thinking opens expanded under each answer. */
    val showRawThinking: Boolean = false,
    /** Replaces Hain's system prompt when not blank. */
    val systemPrompt: String = "",
    /** Tokens per second and time to the first token under each answer. */
    val showStats: Boolean = false,
    /** Own GGUF file on the phone (downloaded in developer mode); blank = Hain's brain. */
    val customModelPath: String = "",
    /** Shown name of [customModelPath], e.g. "LiquidAI/LFM3-1.7B-GGUF · LFM3-1.7B-Q8_0.gguf". */
    val customModelName: String = "",
) {
    /** The values of developer mode back to Hain's defaults; mode and own model stay. */
    fun resetTuning(): DevSettings =
        DevSettings(
            enabled = enabled,
            customModelPath = customModelPath,
            customModelName = customModelName,
        )

    companion object {
        const val DEFAULT_CONTEXT = 8192
        const val MIN_CONTEXT = 512
        const val MAX_CONTEXT = 8192
    }
}

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
    /** Web search on, ask first, or off (SETTINGS.md `web_search`, 3b-2). Wikipedia and weather stay. */
    val webSearch: WebSearch = WebSearch.ON,
    /** Own SearXNG address, asked first by the web search (SETTINGS.md `searxng_url`); empty = none. */
    val searxUrl: String = "",
    /**
     * Phone actions wait for a tap on their card (SETTINGS.md `confirm_actions`, 4b-2). Off: the card
     * counts down 3 s with "Rückgängig", then the phone's app opens by itself.
     */
    val confirmActions: Boolean = true,
    /** Haptics, Settings → Look → "Vibration" (SETTINGS.md `haptics`). */
    val haptics: HapticStrength = HapticStrength.SUBTLE,
    /** Launcher icon, Settings → Look → "App icon" and onboarding (T-103). */
    val appIcon: AppIcon = AppIcon.THREE_D,
    /** Best score of the hidden 2048 game (T-092). */
    val game2048Best: Int = 0,
    /** Update check (T-212): off until you switch it on (Hain asks once, after a few chats). */
    val updateCheck: UpdateCheck = UpdateCheck.OFF,
    val updateChannel: UpdateChannel = UpdateChannel.STABLE,
    /** The one-time card "Nach Updates suchen?" was answered (either way). */
    val updateAsked: Boolean = false,
    /** When the update check last ran (ms since 1970); 0 = never. */
    val lastUpdateCheck: Long = 0L,
    /** Developer mode (T-211). */
    val dev: DevSettings = DevSettings(),
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
