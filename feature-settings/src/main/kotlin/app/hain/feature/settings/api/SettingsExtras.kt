package app.hain.feature.settings.api

import app.hain.models.api.DevModels

/** Where the update check stands (T-212), shown under "Jetzt prüfen". */
sealed interface UpdateStatus {
    data object Idle : UpdateStatus

    data object Checking : UpdateStatus

    data object UpToDate : UpdateStatus

    /** A newer build: [versionName] and the release page a tap opens. */
    data class Newer(
        val versionName: String,
        val page: String,
    ) : UpdateStatus

    data object Failed : UpdateStatus
}

/** What the settings show beyond the stored values: build facts, update state, own models. */
data class SettingsExtras(
    val appInfo: AppInfo = AppInfo(),
    val updateStatus: UpdateStatus = UpdateStatus.Idle,
    /** Own models in developer mode; `null` in previews and screenshots. */
    val devModels: DevModels? = null,
)
