package app.hain.common.api

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Reads and writes the settings in a DataStore (T-100). Unknown stored values fall back to the default. */
class SettingsRepository(
    private val store: DataStore<Preferences>,
) {
    val settings: Flow<HainSettings> =
        store.data.map { p ->
            HainSettings(
                theme = p[THEME].toEnum(ThemeChoice.SYSTEM),
                readableFont = p[READABLE_FONT] ?: false,
                answerLanguage = p[ANSWER_LANGUAGE].toEnum(AnswerLanguage.AUTO),
                answerMode = p[ANSWER_MODE].toEnum(AnswerMode.AUTO),
                aboutMe = p[ABOUT_ME].orEmpty(),
                style = p[STYLE].orEmpty(),
            )
        }

    suspend fun setTheme(value: ThemeChoice) = store.edit { it[THEME] = value.name }

    suspend fun setReadableFont(value: Boolean) = store.edit { it[READABLE_FONT] = value }

    suspend fun setAnswerLanguage(value: AnswerLanguage) = store.edit { it[ANSWER_LANGUAGE] = value.name }

    suspend fun setAnswerMode(value: AnswerMode) = store.edit { it[ANSWER_MODE] = value.name }

    suspend fun setAboutMe(value: String) = store.edit { it[ABOUT_ME] = value }

    suspend fun setStyle(value: String) = store.edit { it[STYLE] = value }

    /** Back to the defaults. */
    suspend fun reset() = store.edit { it.clear() }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val READABLE_FONT = booleanPreferencesKey("readable_font")
        val ANSWER_LANGUAGE = stringPreferencesKey("answer_language")
        val ANSWER_MODE = stringPreferencesKey("answer_mode")
        val ABOUT_ME = stringPreferencesKey("custom_about_me")
        val STYLE = stringPreferencesKey("custom_style")
    }
}

private inline fun <reified T : Enum<T>> String?.toEnum(default: T): T {
    val all = enumValues<T>()
    return all.firstOrNull { it.name == this } ?: default
}
