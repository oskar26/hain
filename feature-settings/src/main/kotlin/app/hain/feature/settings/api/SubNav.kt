package app.hain.feature.settings.api

/**
 * Where a settings page leads below itself (T-285): the licence whose text is open on the
 * open-source page ([licence], `null` = the list) and a way to open another page ([goTo]).
 */
internal class SubNav(
    val licence: String? = null,
    val openLicence: (String?) -> Unit = {},
    val goTo: (SettingsPage) -> Unit = {},
)
