package app.hain.feature.onboarding.api

import android.Manifest
import android.os.Build
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hain.feature.onboarding.R
import app.hain.models.api.DownloadState
import app.hain.models.api.FailReason
import app.hain.models.api.ModelInfo
import app.hain.models.api.ModelRepository
import app.hain.ui.components.HainButton
import app.hain.ui.components.HainButtonStyle
import app.hain.ui.components.ProgressBar
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/**
 * First start (T-083): welcome tour, then the brain gets downloaded, then Pim cheers.
 * [onSkip] (2d-2): the download can wait (or keeps running in the background); the chat then
 * shows a friendly reminder.
 */
@Suppress("LongMethod", "ReturnCount") // step switch: language, tour, success, download
@Composable
fun OnboardingScreen(
    models: ModelRepository,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: () -> Unit = onDone,
    onLanguage: (String) -> Unit = {},
) {
    val model = remember { models.defaultBrain }
    val download by remember(model) { models.downloadState(model) }.collectAsState(DownloadState.Idle)
    val context = LocalContext.current
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val size = Formatter.formatShortFileSize(context, model.totalSize)
    val notifications =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { models.startDownload(model) }
    val start = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            models.startDownload(model)
        }
    }

    var languageDone by rememberSaveable { mutableStateOf(false) }
    var tourDone by rememberSaveable { mutableStateOf(false) }
    if (!languageDone && download == DownloadState.Idle) {
        LanguageStep(onPick = { tag ->
            onLanguage(tag)
            languageDone = true
        }, modifier = modifier)
        return
    }
    if (!tourDone && download == DownloadState.Idle) {
        WelcomeTour(onFinish = { tourDone = true }, modifier = modifier)
        return
    }
    if (download is DownloadState.Installed) {
        SuccessScreen(onDone = onDone, modifier = modifier)
        return
    }

    Column(
        modifier
            .fillMaxSize()
            .background(tourBackground())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        LivePim(state = pimState(download), size = 112.dp)
        Spacer(Modifier.height(spacing.xl))
        BasicText(
            stringResource(R.string.onboarding_title),
            style = type.display.copy(color = colors.text, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(spacing.md))
        BasicText(
            stringResource(R.string.onboarding_body, size),
            style = type.body.copy(color = colors.textMuted, textAlign = TextAlign.Center),
        )
        Spacer(Modifier.height(spacing.xl))
        DownloadBlock(download, model, size, models, start, onDone, onSkip)
        Spacer(Modifier.height(spacing.xl))
        BasicText(
            stringResource(R.string.onboarding_licence, model.name, model.licence),
            style = type.label.copy(color = colors.textMuted, textAlign = TextAlign.Center),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun DownloadBlock(
    download: DownloadState,
    model: ModelInfo,
    size: String,
    models: ModelRepository,
    start: () -> Unit,
    onDone: () -> Unit,
    onSkip: () -> Unit,
) {
    val context = LocalContext.current
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val muted = type.bodySmall.copy(color = colors.textMuted, textAlign = TextAlign.Center)
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        when (download) {
            DownloadState.Idle -> {
                HainButton(stringResource(R.string.onboarding_download), start, Modifier.fillMaxWidth())
                BasicText(stringResource(R.string.onboarding_wifi), style = muted)
                HainButton(stringResource(R.string.onboarding_skip), onSkip, style = HainButtonStyle.GHOST)
            }
            DownloadState.Queued -> {
                ProgressBar(0f)
                BasicText(stringResource(R.string.onboarding_queued), style = muted)
                CancelButton { models.cancelDownload(model) }
                HainButton(stringResource(R.string.onboarding_background), onSkip, style = HainButtonStyle.GHOST)
            }
            is DownloadState.Running -> {
                ProgressBar(download.fraction)
                val done = Formatter.formatShortFileSize(context, download.downloaded)
                BasicText(stringResource(R.string.onboarding_progress, done, size), style = muted)
                CancelButton { models.cancelDownload(model) }
                HainButton(stringResource(R.string.onboarding_background), onSkip, style = HainButtonStyle.GHOST)
            }
            DownloadState.Verifying -> {
                ProgressBar(1f)
                BasicText(stringResource(R.string.onboarding_verifying), style = muted)
            }
            DownloadState.Installed ->
                HainButton(stringResource(R.string.onboarding_start), onDone, Modifier.fillMaxWidth())
            is DownloadState.Failed -> {
                BasicText(failText(download.reason, size), style = type.bodySmall.copy(color = colors.danger))
                HainButton(stringResource(R.string.onboarding_retry), start, Modifier.fillMaxWidth())
                HainButton(stringResource(R.string.onboarding_skip), onSkip, style = HainButtonStyle.GHOST)
            }
        }
    }
}

@Composable
private fun CancelButton(onClick: () -> Unit) {
    HainButton(stringResource(R.string.onboarding_cancel), onClick, style = HainButtonStyle.GHOST)
}

@Composable
private fun failText(
    reason: FailReason,
    size: String,
): String =
    when (reason) {
        FailReason.NETWORK -> stringResource(R.string.onboarding_fail_network)
        FailReason.STORAGE -> stringResource(R.string.onboarding_fail_storage, size)
        FailReason.CHECKSUM -> stringResource(R.string.onboarding_fail_checksum)
        FailReason.UNKNOWN -> stringResource(R.string.onboarding_fail_unknown)
    }

private fun pimState(download: DownloadState): PimState =
    when (download) {
        is DownloadState.Running, DownloadState.Queued, DownloadState.Verifying -> PimState.DOWNLOADING
        DownloadState.Installed -> PimState.CELEBRATE
        is DownloadState.Failed -> PimState.ERROR
        DownloadState.Idle -> PimState.HAPPY
    }
