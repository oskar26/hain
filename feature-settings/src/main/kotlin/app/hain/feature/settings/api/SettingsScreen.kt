package app.hain.feature.settings.api

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.hain.common.api.HainSettings
import app.hain.common.api.net.NetworkLogEntry
import app.hain.feature.settings.R
import app.hain.ui.HainIcons
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.TopBar
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/** Settings hub: a few tiles with an icon, each opens its own page. Plain words, no jargon. */
@Composable
fun SettingsScreen(
    settings: HainSettings,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    networkLog: List<NetworkLogEntry> = emptyList(),
) {
    var page by rememberSaveable { mutableStateOf<SettingsPage?>(null) }
    BackHandler { if (page != null) page = null else actions.onBack() }
    val current = page
    Column(
        modifier
            .fillMaxSize()
            .background(LocalHainColors.current.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        TopBar(
            title = stringResource(current?.title ?: R.string.settings_title),
            leading = {
                HainIconButton(
                    if (current == null) HainIcons.close else HainIcons.chevronRight,
                    stringResource(R.string.settings_back),
                    { if (current != null) page = null else actions.onBack() },
                )
            },
        )
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LocalHainSpacing.current.lg),
            verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.md),
        ) {
            if (current == null) {
                Hub(actions) { page = it }
            } else {
                SettingsPageContent(current, settings, actions, networkLog)
            }
            Spacer(Modifier.height(LocalHainSpacing.current.xl))
        }
    }
}

@Composable
private fun Hub(
    actions: SettingsActions,
    open: (SettingsPage) -> Unit,
) {
    Tile(HainIcons.image, R.string.settings_group_look, R.string.settings_look_hint) { open(SettingsPage.LOOK) }
    Tile(HainIcons.send, R.string.settings_group_answers, R.string.settings_answers_hint) {
        open(SettingsPage.ANSWERS)
    }
    Tile(HainIcons.edit, R.string.settings_group_you, R.string.settings_you_hint) { open(SettingsPage.YOU) }
    Tile(HainIcons.brain, R.string.settings_packs, R.string.settings_packs_hint, actions.onOpenPacks)
    Tile(HainIcons.offline, R.string.settings_group_privacy, R.string.settings_privacy_hint) {
        open(SettingsPage.PRIVACY)
    }
    Tile(HainIcons.globe, R.string.settings_group_about, R.string.settings_about_hint) { open(SettingsPage.ABOUT) }
}

@Composable
private fun Tile(
    icon: Painter,
    title: Int,
    hint: Int,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(colors.surfaceHigh, LocalHainShapes.current.card)
            .clickable(onClick = onClick)
            .padding(spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Box(Modifier.size(44.dp).background(colors.accent, CircleShape), contentAlignment = Alignment.Center) {
            Image(icon, null, Modifier.size(22.dp), colorFilter = ColorFilter.tint(colors.onAccent))
        }
        Column(Modifier.weight(1f)) {
            BasicText(stringResource(title), style = type.bodySmall.copy(color = colors.text))
            BasicText(stringResource(hint), style = type.label.copy(color = colors.textMuted))
        }
        Image(
            HainIcons.chevronRight,
            null,
            Modifier.size(22.dp),
            colorFilter = ColorFilter.tint(colors.textMuted),
        )
    }
}
