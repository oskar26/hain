package app.hain.feature.settings.api

import app.hain.common.api.AnswerLanguage
import app.hain.common.api.AnswerMode
import app.hain.common.api.HapticStrength
import app.hain.common.api.ThemeChoice

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
)
