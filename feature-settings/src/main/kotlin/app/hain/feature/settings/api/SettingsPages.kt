package app.hain.feature.settings.api

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.hain.common.api.AnswerLanguage
import app.hain.common.api.AnswerMode
import app.hain.common.api.AppIcon
import app.hain.common.api.HainSettings
import app.hain.common.api.HapticStrength
import app.hain.common.api.SearxAddress
import app.hain.common.api.ThemeChoice
import app.hain.common.api.net.NetworkLogEntry
import app.hain.feature.settings.R
import app.hain.ui.components.AppIconPicker
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/** Content of one settings sub page. */
@Suppress("LongMethod", "LongParameterList") // one `when`, one short column per page; T-285 sub page
@Composable
internal fun SettingsPageContent(
    page: SettingsPage,
    settings: HainSettings,
    actions: SettingsActions,
    networkLog: List<NetworkLogEntry> = emptyList(),
    extras: SettingsExtras = SettingsExtras(),
    nav: SubNav = SubNav(),
) {
    when (page) {
        SettingsPage.LOOK -> {
            // T-292: blocks with a heading each instead of one long column
            SettingsSection(R.string.settings_section_appearance, page.hue, 0) {
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
                // T-103: launcher icon "Pim im Wald", 3D or flat
                Column(verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.sm)) {
                    BasicText(
                        stringResource(R.string.settings_app_icon),
                        style = LocalHainTypography.current.body.copy(color = LocalHainColors.current.text),
                    )
                    AppIconPicker(
                        flat = settings.appIcon == AppIcon.FLAT,
                        label3d = stringResource(R.string.settings_app_icon_3d),
                        labelFlat = stringResource(R.string.settings_app_icon_flat),
                        onSelect = { flat -> actions.onAppIcon(if (flat) AppIcon.FLAT else AppIcon.THREE_D) },
                    )
                    Info(R.string.settings_app_icon_hint)
                }
            }
            SettingsSection(R.string.settings_section_feel, page.hue, 1) {
                Column(verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.sm)) {
                    Choice(
                        R.string.settings_haptics,
                        settings.haptics,
                        listOf(
                            HapticStrength.OFF to R.string.settings_haptics_off,
                            HapticStrength.SUBTLE to R.string.settings_haptics_subtle,
                            HapticStrength.NORMAL to R.string.settings_haptics_normal,
                            HapticStrength.STRONG to R.string.settings_haptics_strong,
                        ),
                        actions.onHaptics,
                    )
                    Info(R.string.settings_haptics_hint)
                }
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
        }
        SettingsPage.ANSWERS -> {
            SettingsSection(R.string.settings_section_answer_style, page.hue, 0) {
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
            // T-283: "Über dich" is part of this page (one level less)
            SettingsSection(R.string.settings_answers_group_you, page.hue, 1) {
                TextSetting(
                    R.string.settings_about_me,
                    R.string.settings_about_me_hint,
                    settings.aboutMe,
                    actions.onAboutMe,
                )
                TextSetting(R.string.settings_style, R.string.settings_style_hint, settings.style, actions.onStyle)
            }
            BasicText(
                stringResource(R.string.settings_reset),
                style = LocalHainTypography.current.bodySmall.copy(color = LocalHainColors.current.accent),
                modifier =
                    Modifier
                        .clickable(onClick = actions.onReset)
                        .padding(vertical = LocalHainSpacing.current.md, horizontal = LocalHainSpacing.current.sm),
            )
        }
        SettingsPage.INTERNET -> InternetPage(settings, actions, networkLog, page.hue)
        SettingsPage.PRIVACY -> {
            Info(R.string.settings_privacy_text)
            SettingsSection(R.string.settings_privacy_group_safety, page.hue, 0) {
                Toggle(
                    R.string.settings_confirm_actions,
                    R.string.settings_confirm_actions_hint,
                    settings.confirmActions,
                    actions.onConfirmActions,
                )
            }
            SettingsSection(R.string.settings_lock_title, page.hue, 1) {
                AppLockSection(settings, actions, extras.lockAvailable)
            }
            SettingsSection(R.string.settings_privacy_group_data, page.hue, 2) { ExportAllSection(actions) }
        }
        SettingsPage.ABOUT ->
            AboutPage(settings, actions, extras.appInfo, extras.updateStatus, page.hue) {
                nav.goTo(SettingsPage.LICENCES)
            }
        SettingsPage.LICENCES -> LicencesPage(extras.appInfo, nav)
        SettingsPage.DEVELOPER ->
            DeveloperPage(settings.dev, actions, extras.devModels, hue = page.hue, network = {
                // T-266: moved here from "Privatsphäre"
                SearxSetting(settings.searxUrl, actions.onSearxUrl)
                Toggle(
                    R.string.settings_log_detail,
                    R.string.settings_log_detail_hint,
                    settings.networkLogDetail,
                    actions.onNetworkLogDetail,
                )
            })
    }
}

/**
 * Own SearXNG address (4b): asked first when Pim searches the web. A quiet line below says whether
 * Hain can use what you typed (only https).
 */
@Composable
internal fun SearxSetting(
    value: String,
    onChange: (String) -> Unit,
) {
    var typed by remember { mutableStateOf(value) }
    TextSetting(R.string.settings_searx, R.string.settings_searx_placeholder, value) {
        typed = it
        onChange(it)
    }
    val colors = LocalHainColors.current
    val (text, color) =
        when {
            typed.isBlank() -> R.string.settings_searx_hint to colors.textMuted
            SearxAddress.normalize(typed) != null -> R.string.settings_searx_ok to colors.accent
            else -> R.string.settings_searx_bad to colors.danger
        }
    BasicText(stringResource(text), style = LocalHainTypography.current.bodySmall.copy(color = color))
}

@Composable
internal fun Info(text: Int) {
    BasicText(
        stringResource(text),
        style = LocalHainTypography.current.bodySmall.copy(color = LocalHainColors.current.text),
    )
}
