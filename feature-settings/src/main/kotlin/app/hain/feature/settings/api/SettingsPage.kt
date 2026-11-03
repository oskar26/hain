package app.hain.feature.settings.api

import app.hain.feature.settings.R

/** Which settings page is open; HUB is the list of icon tiles (SETTINGS.md level 1). */
internal enum class SettingsPage(
    val title: Int,
    val hint: Int,
) {
    LOOK(R.string.settings_group_look, R.string.settings_look_hint),
    ANSWERS(R.string.settings_group_answers, R.string.settings_answers_hint),
    YOU(R.string.settings_group_you, R.string.settings_you_hint),
    PRIVACY(R.string.settings_group_privacy, R.string.settings_privacy_hint),
    ABOUT(R.string.settings_group_about, R.string.settings_about_hint),
}
