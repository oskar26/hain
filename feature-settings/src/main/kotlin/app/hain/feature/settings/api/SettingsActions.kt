package app.hain.feature.settings.api

import android.net.Uri
import app.hain.common.api.AnswerLanguage
import app.hain.common.api.AnswerMode
import app.hain.common.api.AppIcon
import app.hain.common.api.DevSettings
import app.hain.common.api.HapticStrength
import app.hain.common.api.LockDelay
import app.hain.common.api.ThemeChoice
import app.hain.common.api.UpdateChannel
import app.hain.common.api.UpdateCheck

/** What the settings screen can change; every callback writes one value (T-101). */
@Suppress("LongParameterList") // one callback per setting
class SettingsActions(
    val onBack: () -> Unit,
    val onTheme: (ThemeChoice) -> Unit,
    val onReadableFont: (Boolean) -> Unit,
    val onReduceMotion: (Boolean) -> Unit,
    val onAnswerLanguage: (AnswerLanguage) -> Unit,
    val onAnswerMode: (AnswerMode) -> Unit,
    val onAboutMe: (String) -> Unit,
    val onStyle: (String) -> Unit,
    val onReset: () -> Unit,
    val onOpenPacks: () -> Unit,
    val onOfflineMode: (Boolean) -> Unit = {},
    val onNetworkLogDetail: (Boolean) -> Unit = {},
    val onClearNetworkLog: () -> Unit = {},
    val onSearxUrl: (String) -> Unit = {},
    val onHaptics: (HapticStrength) -> Unit = {},
    val onAppIcon: (AppIcon) -> Unit = {},
    /** Developer mode on/off and all its values at once (T-211). */
    val onDev: (DevSettings) -> Unit = {},
    /** Update check off / daily (T-212). */
    val onUpdateCheck: (UpdateCheck) -> Unit = {},
    val onUpdateChannel: (UpdateChannel) -> Unit = {},
    /** "Jetzt prüfen" in "Über Hain". */
    val onCheckUpdate: () -> Unit = {},
    /** App lock on/off (T-056); switching it on asks for the fingerprint or PIN first. */
    val onAppLock: (Boolean) -> Unit = {},
    val onLockDelay: (LockDelay) -> Unit = {},
    val onBlockScreenshots: (Boolean) -> Unit = {},
    /** All chats as a Markdown ZIP into the file the user picked (T-054). */
    val onExportAll: (Uri) -> Unit = {},
)
