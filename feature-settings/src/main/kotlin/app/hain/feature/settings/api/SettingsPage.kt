package app.hain.feature.settings.api

import app.hain.feature.settings.R
import app.hain.ui.components.Hain3D
import app.hain.ui.theme.GlowHue

/**
 * Which settings page is open; HUB is the list of glass tiles (SETTINGS.md level 1). Each group has
 * its own 3D icon and its own soft light (T-011), the same on the hub tile and on its page.
 */
internal enum class SettingsPage(
    val title: Int,
    val hint: Int,
    val icon: Hain3D,
    val hue: GlowHue,
) {
    LOOK(R.string.settings_group_look, R.string.settings_look_hint, Hain3D.PALETTE, GlowHue.PEACH),
    ANSWERS(R.string.settings_group_answers, R.string.settings_answers_hint, Hain3D.BUBBLE, GlowHue.MINT),
    YOU(R.string.settings_group_you, R.string.settings_you_hint, Hain3D.HEART, GlowHue.ROSE),
    PRIVACY(R.string.settings_group_privacy, R.string.settings_privacy_hint, Hain3D.SHIELD, GlowHue.SKY),
    ABOUT(R.string.settings_group_about, R.string.settings_about_hint, Hain3D.TREE, GlowHue.TEAL),
}
