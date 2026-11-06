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

    // T-283: one page for what may go online, with a main switch; "Über dich" joined "Antworten"
    INTERNET(R.string.settings_group_internet, R.string.settings_internet_hint, Hain3D.GLOBE, GlowHue.SKY),
    PRIVACY(R.string.settings_group_privacy, R.string.settings_privacy_hint, Hain3D.SHIELD, GlowHue.ROSE),
    ABOUT(R.string.settings_group_about, R.string.settings_about_hint, Hain3D.TREE, GlowHue.TEAL),

    /** Hidden until developer mode is unlocked in "Über Hain" (T-211). */
    DEVELOPER(R.string.settings_group_dev, R.string.settings_dev_hint, Hain3D.BOLT, GlowHue.LILAC),

    /** T-285: the open-source licences, one level below "Über Hain" (not on the hub). */
    LICENCES(R.string.about_open_source, R.string.licences_hint, Hain3D.DOCUMENT, GlowHue.TEAL),
    ;

    /** The page "back" leads to instead of the hub; the hub marks it on wide windows (T-285). */
    val parent: SettingsPage? get() = if (this == LICENCES) ABOUT else null
}
