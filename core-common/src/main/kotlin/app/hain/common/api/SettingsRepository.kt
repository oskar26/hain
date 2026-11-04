package app.hain.common.api

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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
                haptics = p[HAPTICS].toEnum(HapticStrength.SUBTLE),
                appIcon = p[APP_ICON].toEnum(AppIcon.THREE_D),
                game2048Best = p[GAME_2048_BEST] ?: 0,
            )
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

    suspend fun setHaptics(value: HapticStrength) = store.edit { it[HAPTICS] = value.name }

    suspend fun setAppIcon(value: AppIcon) = store.edit { it[APP_ICON] = value.name }

    /** Keeps the higher of the stored and the new 2048 score. */
    suspend fun recordGame2048(score: Int) {
        store.edit { if (score > (it[GAME_2048_BEST] ?: 0)) it[GAME_2048_BEST] = score }
    }

    /**
     * Back to the defaults; the 2048 best score and the launcher icon stay (changing the icon
     * on the home screen should only ever happen when you pick it yourself).
     */
    suspend fun reset() =
        store.edit {
            val best = it[GAME_2048_BEST]
            val icon = it[APP_ICON]
            it.clear()
            if (best != null) it[GAME_2048_BEST] = best
            if (icon != null) it[APP_ICON] = icon
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
        val HAPTICS = stringPreferencesKey("haptics")
        val GAME_2048_BEST = intPreferencesKey("game_2048_best")
        val APP_ICON = stringPreferencesKey("app_icon")
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T {
    val all = enumValues<T>()
    return all.firstOrNull { it.name == this } ?: default
}
