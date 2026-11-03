package app.hain.feature.settings.api

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hain.feature.settings.R
import app.hain.models.api.DeviceCheck
import app.hain.models.api.DeviceProfile
import app.hain.models.api.DownloadState
import app.hain.models.api.Fit
import app.hain.models.api.ModelInfo
import app.hain.models.api.ModelRepository
import app.hain.ui.HainIcons
import app.hain.ui.components.Hain3D
import app.hain.ui.components.Hain3DIcon
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.TopBar
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.glassTile
import app.hain.ui.theme.hainBackdrop
import app.hain.ui.theme.hainSurface

private const val MB = 1_000_000L
private const val GB = 1_000_000_000L

/** Knowledge packs: what is on the phone, what fits, download and delete (SETTINGS.md, MODELS.md). */
@Composable
fun PacksScreen(
    models: ModelRepository,
    ramGb: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalHainSpacing.current
    BackHandler(onBack = onBack)
    val device = DeviceProfile(ramGb, models.freeSpace())
    Column(
        modifier
            .fillMaxSize()
            .hainBackdrop()
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TopBar(
            title = stringResource(R.string.packs_title),
            leading = { HainIconButton(HainIcons.close, stringResource(R.string.settings_back), onBack) },
        )
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            BasicText(
                stringResource(R.string.packs_intro),
                style = LocalHainTypography.current.bodySmall.copy(color = LocalHainColors.current.textMuted),
            )
            models.catalog.forEach { PackRow(it, models, device) }
        }
    }
}

@Composable
private fun PackRow(
    pack: ModelInfo,
    models: ModelRepository,
    device: DeviceProfile,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val flow = remember(pack.id) { models.downloadState(pack) }
    val state by flow.collectAsStateWithLifecycle(DownloadState.Idle)
    val installed = state is DownloadState.Installed || models.isInstalled(pack)
    // T-011: every knowledge pack is a glass tile; the brain glows blue, the other packs warm yellow
    val hue = if (pack.isDefault) GlowHue.SKY else GlowHue.SUN
    Column(
        Modifier
            .fillMaxWidth()
            .glassTile(hue, lightAt = Offset(x = 0.12f, y = 0.2f))
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            Hain3DIcon(if (pack.isDefault) Hain3D.HEAD else Hain3D.PARCEL, size = 40.dp)
            Column(Modifier.weight(1f)) {
                BasicText(pack.name, style = type.button.copy(color = colors.text))
                BasicText(sizeText(pack.totalSize), style = type.label.copy(color = colors.textMuted))
            }
        }
        val fit = DeviceCheck.fit(pack, device)
        fitText(fit)?.let { BasicText(stringResource(it), style = type.label.copy(color = colors.danger)) }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            val running = state as? DownloadState.Running
            when {
                installed -> {
                    BasicText(stringResource(R.string.packs_installed), style = type.label.copy(color = colors.accent))
                    if (!pack.isDefault) ActionButton(R.string.packs_delete) { models.delete(pack) }
                }
                running != null -> {
                    BasicText(
                        stringResource(R.string.packs_loading, (running.fraction * PERCENT).toInt()),
                        style = type.label.copy(color = colors.text),
                    )
                    ActionButton(R.string.packs_cancel) { models.cancelDownload(pack) }
                }
                state is DownloadState.Queued || state is DownloadState.Verifying ->
                    BasicText(stringResource(R.string.packs_wait), style = type.label.copy(color = colors.textMuted))
                fit == Fit.TOO_SMALL || fit == Fit.NO_SPACE -> Unit
                else -> ActionButton(R.string.packs_download) { models.startDownload(pack) }
            }
        }
    }
}

private const val PERCENT = 100

private fun fitText(fit: Fit): Int? =
    when (fit) {
        Fit.GOOD -> null
        Fit.TIGHT -> R.string.packs_fit_tight
        Fit.NO_SPACE -> R.string.packs_fit_space
        Fit.TOO_SMALL -> R.string.packs_fit_small
    }

private fun sizeText(bytes: Long): String {
    if (bytes < GB) return "${bytes / MB} MB"
    return "%.1f GB".format(bytes.toDouble() / GB)
}

@Composable
private fun ActionButton(
    label: Int,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    Box(
        Modifier
            .hainSurface(HainLevel.FLAT, LocalHainShapes.current.button)
            .clickable(onClick = onClick)
            .padding(horizontal = LocalHainSpacing.current.md, vertical = LocalHainSpacing.current.sm),
    ) {
        BasicText(stringResource(label), style = LocalHainTypography.current.label.copy(color = colors.text))
    }
}
