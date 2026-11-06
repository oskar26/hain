@file:Suppress("MatchingDeclarationName") // the sections are the point of this file

package app.hain.feature.settings.api

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import app.hain.common.api.HainSettings
import app.hain.common.api.LockDelay
import app.hain.feature.settings.R
import app.hain.ui.LocalExternalLaunch
import java.time.LocalDate

/**
 * Privacy → "App-Sperre" (T-056): lock Hain with your fingerprint or the phone's PIN, choose how
 * soon it locks again, and block screenshots. Without a screen lock on the phone the switch is
 * replaced by a short hint.
 */
@Composable
internal fun AppLockSection(
    settings: HainSettings,
    actions: SettingsActions,
    lockAvailable: Boolean,
) {
    if (lockAvailable || settings.appLock) {
        Toggle(R.string.settings_lock, R.string.settings_lock_hint, settings.appLock, actions.onAppLock)
    } else {
        Info(R.string.settings_lock_unavailable)
    }
    if (settings.appLock) {
        Choice(
            R.string.settings_lock_delay,
            settings.lockDelay,
            listOf(
                LockDelay.IMMEDIATE to R.string.settings_lock_now,
                LockDelay.ONE_MINUTE to R.string.settings_lock_1min,
                LockDelay.FIVE_MINUTES to R.string.settings_lock_5min,
            ),
            actions.onLockDelay,
        )
    }
    Toggle(
        R.string.settings_screenshots,
        R.string.settings_screenshots_hint,
        settings.blockScreenshots,
        actions.onBlockScreenshots,
    )
}

/**
 * Privacy → "Alle Chats exportieren" (T-054): one ZIP with a Markdown file per chat, saved where you
 * choose in the system file picker. Private chats are never in it.
 */
@Composable
internal fun ExportAllSection(actions: SettingsActions) {
    val external = LocalExternalLaunch.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
            if (uri != null) actions.onExportAll(uri)
        }
    Link(R.string.settings_export_all, R.string.settings_export_all_hint) {
        external()
        runCatching { launcher.launch("hain-chats-${LocalDate.now()}.zip") }
    }
}
