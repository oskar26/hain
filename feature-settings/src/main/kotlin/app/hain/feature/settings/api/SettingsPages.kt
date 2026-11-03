package app.hain.feature.settings.api

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.hain.common.api.AnswerLanguage
import app.hain.common.api.AnswerMode
import app.hain.common.api.HainSettings
import app.hain.common.api.ThemeChoice
import app.hain.common.api.net.NetworkLogEntry
import app.hain.feature.settings.R
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/** Content of one settings sub page. */
@Suppress("LongMethod") // one `when`, one short column per page
@Composable
internal fun SettingsPageContent(
    page: SettingsPage,
    settings: HainSettings,
    actions: SettingsActions,
    networkLog: List<NetworkLogEntry> = emptyList(),
) {
    when (page) {
        SettingsPage.LOOK -> {
            Choice(
                R.string.settings_theme,
                settings.theme,
                listOf(
                    ThemeChoice.SYSTEM to R.string.settings_theme_system,
                    ThemeChoice.LIGHT to R.string.settings_theme_light,
                    ThemeChoice.DARK to R.string.settings_theme_dark,
                ),
                actions.onTheme,
            )
            Toggle(
                R.string.settings_readable,
                R.string.settings_readable_hint,
                settings.readableFont,
                actions.onReadableFont,
            )
            Toggle(
                R.string.settings_reduce_motion,
                R.string.settings_reduce_motion_hint,
                settings.reduceMotion,
                actions.onReduceMotion,
            )
        }
        SettingsPage.ANSWERS -> {
            Choice(
                R.string.settings_answer_language,
                settings.answerLanguage,
                listOf(
                    AnswerLanguage.AUTO to R.string.settings_lang_auto,
                    AnswerLanguage.DE to R.string.settings_lang_de,
                    AnswerLanguage.EN to R.string.settings_lang_en,
                ),
                actions.onAnswerLanguage,
            )
            Choice(
                R.string.settings_answer_mode,
                settings.answerMode,
                listOf(
                    AnswerMode.AUTO to R.string.settings_mode_auto,
                    AnswerMode.FAST to R.string.settings_mode_fast,
                    AnswerMode.THOROUGH to R.string.settings_mode_thorough,
                ),
                actions.onAnswerMode,
            )
        }
        SettingsPage.YOU -> {
            TextSetting(
                R.string.settings_about_me,
                R.string.settings_about_me_hint,
                settings.aboutMe,
                actions.onAboutMe,
            )
            TextSetting(R.string.settings_style, R.string.settings_style_hint, settings.style, actions.onStyle)
            BasicText(
                stringResource(R.string.settings_reset),
                style = LocalHainTypography.current.bodySmall.copy(color = LocalHainColors.current.accent),
                modifier =
                    Modifier
                        .clickable(onClick = actions.onReset)
                        .padding(vertical = LocalHainSpacing.current.md),
            )
        }
        SettingsPage.PRIVACY -> {
            Info(R.string.settings_privacy_text)
            Toggle(
                R.string.settings_offline,
                R.string.settings_offline_hint,
                settings.offlineMode,
                actions.onOfflineMode,
            )
            NetworkLogSection(networkLog, settings.networkLogDetail, actions)
        }
        SettingsPage.ABOUT -> Info(R.string.settings_about_text)
    }
}

@Composable
private fun Info(text: Int) {
    BasicText(
        stringResource(text),
        style = LocalHainTypography.current.bodySmall.copy(color = LocalHainColors.current.text),
    )
}
