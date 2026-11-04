package app.hain.feature.settings.api

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hain.common.api.HainSettings
import app.hain.common.api.UpdateChannel
import app.hain.common.api.UpdateCheck
import app.hain.feature.settings.R
import app.hain.ui.HainIcons
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import kotlinx.coroutines.delay

/** Taps on "Version" that unlock developer mode (T-211). */
internal const val DEV_TAPS = 7

/** How many taps are still missing after [taps] taps; shown from the 4th tap on, `null` otherwise. */
internal fun tapsLeft(taps: Int): Int? = (DEV_TAPS - taps).takeIf { taps >= DEV_TAPS - HINT_FROM && it > 0 }

/**
 * "Über Hain" (T-214): Pim and the version in a centred header, then rows with an icon for the
 * website, GitHub, the version (tap 7× → developer mode, T-211), the licence and the open-source
 * licences, then the update check (T-212).
 */
@Suppress("LongMethod") // header, link rows and the update block form one page
@Composable
internal fun AboutPage(
    settings: HainSettings,
    actions: SettingsActions,
    info: AppInfo,
    update: UpdateStatus,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val uri = LocalUriHandler.current
    val haptics = rememberHainHaptics()
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        LivePim(PimState.HAPPY, HEADER_PIM.dp, tappable = true)
        BasicText(stringResource(R.string.about_name), style = type.title.copy(color = colors.text))
        BasicText(info.versionName, style = type.label.copy(color = colors.textMuted))
        BasicText(
            stringResource(R.string.settings_about_text),
            style = type.bodySmall.copy(color = colors.textMuted, textAlign = TextAlign.Center),
            modifier = Modifier.padding(top = spacing.xs),
        )
    }
    AboutRow(HainIcons.globe, stringResource(R.string.about_website), "oskar26.github.io/hain") {
        uri.openUri(WEBSITE)
    }
    AboutRow(HainIcons.github, stringResource(R.string.about_github), "github.com/oskar26/hain") {
        uri.openUri(GITHUB)
    }
    // T-211: tap 7× to unlock developer mode
    var taps by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf<String?>(null) }
    val already = stringResource(R.string.about_dev_already)
    val unlocked = stringResource(R.string.about_dev_unlocked)
    val left = tapsLeft(taps)?.let { pluralStringResource(R.plurals.about_dev_taps_left, it, it) }
    LaunchedEffect(left) { if (left != null) hint = left }
    LaunchedEffect(hint) {
        if (hint != null) {
            delay(HINT_MS)
            hint = null
        }
    }
    AboutRow(HainIcons.tag, stringResource(R.string.about_version), info.versionLine()) {
        if (settings.dev.enabled) {
            hint = already
            return@AboutRow
        }
        taps++
        if (taps >= DEV_TAPS) {
            taps = 0
            haptics.confirm()
            hint = unlocked
            actions.onDev(settings.dev.copy(enabled = true))
        } else {
            haptics.tick()
        }
    }
    val reduce = LocalReduceMotion.current
    AnimatedVisibility(
        hint != null,
        enter = fadeIn(hainTween(HainMotion.FAST_MS, reduce)),
        exit = fadeOut(hainTween(HainMotion.PAGE_MS, reduce)),
    ) {
        BasicText(
            hint.orEmpty(),
            style = type.label.copy(color = colors.accent),
            modifier = Modifier.padding(start = ICON_INDENT.dp),
        )
    }
    if (info.buildDate.isNotBlank()) {
        BasicText(
            stringResource(R.string.about_built, info.buildDate),
            style = type.label.copy(color = colors.textMuted),
            modifier = Modifier.padding(start = ICON_INDENT.dp),
        )
    }
    AboutRow(HainIcons.licence, stringResource(R.string.about_licence), "GPL-3.0") { uri.openUri(LICENCE) }
    AboutRow(
        HainIcons.document,
        stringResource(R.string.about_open_source),
        stringResource(R.string.about_open_source_hint),
    ) {
        uri.openUri(OPEN_SOURCE)
    }
    UpdateBlock(settings, actions, update)
    if (settings.dev.enabled) {
        Link(R.string.about_dev_off, R.string.about_dev_off_hint) { actions.onDev(settings.dev.copy(enabled = false)) }
    }
}

/** T-212: the update check — off or once a day at start, channel, "Jetzt prüfen" and its result. */
@Suppress("LongMethod") // two choices, a hint and the check row
@Composable
private fun UpdateBlock(
    settings: HainSettings,
    actions: SettingsActions,
    update: UpdateStatus,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val uri = LocalUriHandler.current
    Group(R.string.about_updates)
    Choice(
        R.string.about_update_check,
        settings.updateCheck,
        listOf(UpdateCheck.OFF to R.string.about_update_off, UpdateCheck.DAILY to R.string.about_update_daily),
        actions.onUpdateCheck,
    )
    Choice(
        R.string.about_update_channel,
        settings.updateChannel,
        listOf(
            UpdateChannel.STABLE to R.string.about_channel_stable,
            UpdateChannel.NIGHTLY to R.string.about_channel_nightly,
        ),
        actions.onUpdateChannel,
    )
    Info(R.string.about_update_privacy)
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            stringResource(R.string.about_check_now),
            style = type.button.copy(color = colors.accent),
            modifier =
                Modifier
                    .clickable(
                        role = Role.Button,
                        enabled = update != UpdateStatus.Checking,
                        onClick = actions.onCheckUpdate,
                    ).padding(vertical = LocalHainSpacing.current.sm)
                    .padding(end = LocalHainSpacing.current.md),
        )
        val (text, color) =
            when (update) {
                UpdateStatus.Idle -> null to colors.textMuted
                UpdateStatus.Checking -> stringResource(R.string.about_update_checking) to colors.textMuted
                UpdateStatus.UpToDate -> stringResource(R.string.about_update_latest) to colors.textMuted
                UpdateStatus.Failed -> stringResource(R.string.about_update_failed) to colors.danger
                is UpdateStatus.Newer -> stringResource(R.string.update_banner, update.versionName) to colors.accent
            }
        if (text != null) {
            BasicText(
                text,
                style = type.label.copy(color = color),
                modifier =
                    if (update is UpdateStatus.Newer) {
                        Modifier.clickable(role = Role.Button) { uri.openUri(update.page) }
                    } else {
                        Modifier
                    },
            )
        }
    }
}

/** One row of "Über Hain": icon, title, a quiet second line, the whole row tappable. */
@Composable
private fun AboutRow(
    icon: Painter,
    title: String,
    detail: String,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = spacing.xs)
            .clip(LocalHainShapes.current.row)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Image(icon, null, Modifier.size(ICON.dp), colorFilter = ColorFilter.tint(colors.icon))
        Column(Modifier.weight(1f)) {
            BasicText(title, style = type.body.copy(color = colors.text))
            BasicText(detail, style = type.bodySmall.copy(color = colors.textMuted))
        }
    }
}

// Links of "Über Hain" (T-214); opened in the browser only after a tap.
private const val WEBSITE = "https://oskar26.github.io/hain/"
private const val GITHUB = "https://github.com/oskar26/hain"
private const val LICENCE = "https://github.com/oskar26/hain/blob/main/LICENSE"
private const val OPEN_SOURCE = "https://github.com/oskar26/hain/blob/main/docs/specs/LICENSES.md"
private const val HEADER_PIM = 72
private const val HINT_FROM = 3
private const val HINT_MS = 1_600L
private const val ICON = 20
private const val ICON_INDENT = 36
