package app.hain.common.api

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reads and writes the settings in a DataStore (T-100). Unknown stored values fall back to the default. */
@Suppress("TooManyFunctions") // one setter per setting
class SettingsRepository(
    private val store: DataStore<Preferences>,
) {
    val settings: Flow<HainSettings> =
        store.data.map { p ->
            HainSettings(
                theme = p[THEME].toEnum(ThemeChoice.SYSTEM),
                readableFont = p[READABLE_FONT] ?: false,
                reduceMotion = p[REDUCE_MOTION] ?: false,
                answerLanguage = p[ANSWER_LANGUAGE].toEnum(AnswerLanguage.AUTO),
                answerMode = p[ANSWER_MODE].toEnum(AnswerMode.AUTO),
                aboutMe = p[ABOUT_ME].orEmpty(),
                style = p[STYLE].orEmpty(),
                offlineMode = p[OFFLINE_MODE] ?: false,
                networkLogDetail = p[NETWORK_LOG_DETAIL] ?: false,
                searxUrl = p[SEARXNG_URL].orEmpty(),
                confirmActions = p[CONFIRM_ACTIONS] ?: true,
                webSearch = p[WEB_SEARCH].toEnum(WebSearch.ON),
                haptics = p[HAPTICS].toEnum(HapticStrength.SUBTLE),
                appIcon = p[APP_ICON].toEnum(AppIcon.THREE_D),
                game2048Best = p[GAME_2048_BEST] ?: 0,
                updateCheck = p[UPDATE_CHECK].toEnum(UpdateCheck.OFF),
                updateChannel = p[UPDATE_CHANNEL].toEnum(UpdateChannel.STABLE),
                updateAsked = p[UPDATE_ASKED] ?: false,
                lastUpdateCheck = p[LAST_UPDATE_CHECK] ?: 0L,
                dev = devOf(p),
            )
        }

    @Suppress("CyclomaticComplexMethod") // one fallback per developer setting
    private fun devOf(p: Preferences): DevSettings {
        val d = DevSettings()
        return DevSettings(
            enabled = p[DEV_ENABLED] ?: d.enabled,
            temperature = p[DEV_TEMPERATURE] ?: d.temperature,
            topK = p[DEV_TOP_K] ?: d.topK,
            topP = p[DEV_TOP_P] ?: d.topP,
            minP = p[DEV_MIN_P] ?: d.minP,
            repeatPenalty = p[DEV_REPEAT] ?: d.repeatPenalty,
            contextSize = p[DEV_CONTEXT] ?: d.contextSize,
            maxTokens = p[DEV_MAX_TOKENS] ?: d.maxTokens,
            seed = p[DEV_SEED] ?: d.seed,
            thinking = p[DEV_THINKING].toEnum(d.thinking),
            thinkingBudget = p[DEV_THINKING_BUDGET] ?: d.thinkingBudget,
            showRawThinking = p[DEV_RAW_THINKING] ?: d.showRawThinking,
            systemPrompt = p[DEV_SYSTEM_PROMPT] ?: d.systemPrompt,
            showStats = p[DEV_STATS] ?: d.showStats,
            customModelPath = p[DEV_MODEL_PATH] ?: d.customModelPath,
            customModelName = p[DEV_MODEL_NAME] ?: d.customModelName,
        )
    }

    suspend fun setUpdateCheck(value: UpdateCheck) =
        store.edit {
            it[UPDATE_CHECK] = value.name
            it[UPDATE_ASKED] = true
        }

    suspend fun setUpdateChannel(value: UpdateChannel) = store.edit { it[UPDATE_CHANNEL] = value.name }

    /** The one-time card was answered with "Nein danke". */
    suspend fun dismissUpdateQuestion() = store.edit { it[UPDATE_ASKED] = true }

    suspend fun setLastUpdateCheck(at: Long) = store.edit { it[LAST_UPDATE_CHECK] = at }

    /** Writes all developer values at once (T-211). */
    suspend fun setDev(value: DevSettings) =
        store.edit {
            it[DEV_ENABLED] = value.enabled
            it[DEV_TEMPERATURE] = value.temperature
            it[DEV_TOP_K] = value.topK
            it[DEV_TOP_P] = value.topP
            it[DEV_MIN_P] = value.minP
            it[DEV_REPEAT] = value.repeatPenalty
            it[DEV_CONTEXT] = value.contextSize
            it[DEV_MAX_TOKENS] = value.maxTokens
            it[DEV_SEED] = value.seed
            it[DEV_THINKING] = value.thinking.name
            it[DEV_THINKING_BUDGET] = value.thinkingBudget
            it[DEV_RAW_THINKING] = value.showRawThinking
            it[DEV_SYSTEM_PROMPT] = value.systemPrompt
            it[DEV_STATS] = value.showStats
            it[DEV_MODEL_PATH] = value.customModelPath
            it[DEV_MODEL_NAME] = value.customModelName
        }

    suspend fun setTheme(value: ThemeChoice) = store.edit { it[THEME] = value.name }

    suspend fun setReadableFont(value: Boolean) = store.edit { it[READABLE_FONT] = value }

    suspend fun setReduceMotion(value: Boolean) = store.edit { it[REDUCE_MOTION] = value }

    suspend fun setAnswerLanguage(value: AnswerLanguage) = store.edit { it[ANSWER_LANGUAGE] = value.name }

    suspend fun setAnswerMode(value: AnswerMode) = store.edit { it[ANSWER_MODE] = value.name }

    suspend fun setAboutMe(value: String) = store.edit { it[ABOUT_ME] = value }

    suspend fun setStyle(value: String) = store.edit { it[STYLE] = value }

    suspend fun setOfflineMode(value: Boolean) = store.edit { it[OFFLINE_MODE] = value }

    suspend fun setNetworkLogDetail(value: Boolean) = store.edit { it[NETWORK_LOG_DETAIL] = value }

    suspend fun setSearxUrl(value: String) = store.edit { it[SEARXNG_URL] = value.trim() }

    suspend fun setConfirmActions(value: Boolean) = store.edit { it[CONFIRM_ACTIONS] = value }

    suspend fun setWebSearch(value: WebSearch) = store.edit { it[WEB_SEARCH] = value.name }

    suspend fun setHaptics(value: HapticStrength) = store.edit { it[HAPTICS] = value.name }

    suspend fun setAppIcon(value: AppIcon) = store.edit { it[APP_ICON] = value.name }

    /** Keeps the higher of the stored and the new 2048 score. */
    suspend fun recordGame2048(score: Int) {
        store.edit { if (score > (it[GAME_2048_BEST] ?: 0)) it[GAME_2048_BEST] = score }
    }

    /**
     * Back to the defaults; developer mode, the update choice, the 2048 best score and the launcher
     * icon stay (changing the icon
     * on the home screen should only ever happen when you pick it yourself).
     */
    suspend fun reset() =
        store.edit {
            val best = it[GAME_2048_BEST]
            val icon = it[APP_ICON]
            val kept = it.asMap().filterKeys { key -> key.name.startsWith("dev_") || key.name.startsWith("update_") }
            it.clear()
            if (best != null) it[GAME_2048_BEST] = best
            if (icon != null) it[APP_ICON] = icon
            @Suppress("UNCHECKED_CAST")
            kept.forEach { (key, value) -> it[key as Preferences.Key<Any>] = value }
        }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val READABLE_FONT = booleanPreferencesKey("readable_font")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val ANSWER_LANGUAGE = stringPreferencesKey("answer_language")
        val ANSWER_MODE = stringPreferencesKey("answer_mode")
        val ABOUT_ME = stringPreferencesKey("custom_about_me")
        val STYLE = stringPreferencesKey("custom_style")
        val OFFLINE_MODE = booleanPreferencesKey("offline_mode")
        val NETWORK_LOG_DETAIL = booleanPreferencesKey("network_log_detail")
        val SEARXNG_URL = stringPreferencesKey("searxng_url")
        val CONFIRM_ACTIONS = booleanPreferencesKey("confirm_actions")
        val WEB_SEARCH = stringPreferencesKey("web_search")
        val HAPTICS = stringPreferencesKey("haptics")
        val GAME_2048_BEST = intPreferencesKey("game_2048_best")
        val APP_ICON = stringPreferencesKey("app_icon")
        val UPDATE_CHECK = stringPreferencesKey("update_check")
        val UPDATE_CHANNEL = stringPreferencesKey("update_channel")
        val UPDATE_ASKED = booleanPreferencesKey("update_asked")
        val LAST_UPDATE_CHECK = longPreferencesKey("update_last_check")
        val DEV_ENABLED = booleanPreferencesKey("dev_enabled")
        val DEV_TEMPERATURE = floatPreferencesKey("dev_temperature")
        val DEV_TOP_K = intPreferencesKey("dev_top_k")
        val DEV_TOP_P = floatPreferencesKey("dev_top_p")
        val DEV_MIN_P = floatPreferencesKey("dev_min_p")
        val DEV_REPEAT = floatPreferencesKey("dev_repeat_penalty")
        val DEV_CONTEXT = intPreferencesKey("dev_context")
        val DEV_MAX_TOKENS = intPreferencesKey("dev_max_tokens")
        val DEV_SEED = intPreferencesKey("dev_seed")
        val DEV_THINKING = stringPreferencesKey("dev_thinking")
        val DEV_THINKING_BUDGET = intPreferencesKey("dev_thinking_budget")
        val DEV_RAW_THINKING = booleanPreferencesKey("dev_raw_thinking")
        val DEV_SYSTEM_PROMPT = stringPreferencesKey("dev_system_prompt")
        val DEV_STATS = booleanPreferencesKey("dev_stats")
        val DEV_MODEL_PATH = stringPreferencesKey("dev_model_path")
        val DEV_MODEL_NAME = stringPreferencesKey("dev_model_name")
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T {
    val all = enumValues<T>()
    return all.firstOrNull { it.name == this } ?: default
}
