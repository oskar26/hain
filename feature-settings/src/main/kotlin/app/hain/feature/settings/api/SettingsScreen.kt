package app.hain.feature.settings.api

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.hain.common.api.HainSettings
import app.hain.common.api.net.NetworkLogEntry
import app.hain.feature.settings.R
import app.hain.ui.HainIcons
import app.hain.ui.components.Hain3D
import app.hain.ui.components.Hain3DBadge
import app.hain.ui.components.Hain3DIcon
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.TopBar
import app.hain.ui.components.pressScale
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.glassTile

/** Settings hub: a few tiles with an icon, each opens its own page. Plain words, no jargon. */
@Composable
fun SettingsScreen(
    settings: HainSettings,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    networkLog: List<NetworkLogEntry> = emptyList(),
    extras: SettingsExtras = SettingsExtras(),
) {
    SettingsBody(settings, actions, modifier, networkLog, startPage = null, extras = extras)
}

/** The settings screen itself; [startPage] opens a group right away (previews and screenshots). */
@Suppress("LongParameterList") // optional extras with defaults
@Composable
internal fun SettingsBody(
    settings: HainSettings,
    actions: SettingsActions,
    modifier: Modifier = Modifier,
    networkLog: List<NetworkLogEntry> = emptyList(),
    startPage: SettingsPage? = null,
    extras: SettingsExtras = SettingsExtras(),
) {
    var page by rememberSaveable { mutableStateOf(startPage) }
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
                    if (current == null) HainIcons.close else HainIcons.back,
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
                Hub(actions, devMode = settings.dev.enabled) { page = it }
            } else {
                // T-214: "Über Hain" has its own centred header with Pim
                if (current != SettingsPage.ABOUT) PageHeader(current)
                // the whole page sits on one glass tile lit from above in the group's colour (T-011)
                Column(
                    Modifier
                        .fillMaxWidth()
                        .glassTile(current.hue, elevation = 8.dp, lightAt = Offset(x = 0.5f, y = 0f))
                        .padding(LocalHainSpacing.current.lg),
                    verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.md),
                ) {
                    SettingsPageContent(current, settings, actions, networkLog, extras)
                }
            }
            Spacer(Modifier.height(LocalHainSpacing.current.xl))
        }
    }
}

@Composable
private fun Hub(
    actions: SettingsActions,
    devMode: Boolean = false,
    open: (SettingsPage) -> Unit,
) {
    // T-089: Pim greets you at the top of the settings
    Row(
        Modifier.fillMaxWidth().padding(top = LocalHainSpacing.current.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.md),
    ) {
        LivePim(PimState.IDLE, HEADER_PIM.dp)
        BasicText(
            stringResource(R.string.settings_hub_hello),
            style = LocalHainTypography.current.body.copy(color = LocalHainColors.current.textMuted),
            modifier = Modifier.weight(1f),
        )
    }
    Tile(SettingsPage.LOOK.icon, SettingsPage.LOOK.hue, R.string.settings_group_look, R.string.settings_look_hint) {
        open(SettingsPage.LOOK)
    }
    Tile(Hain3D.BUBBLE, GlowHue.MINT, R.string.settings_group_answers, R.string.settings_answers_hint) {
        open(SettingsPage.ANSWERS)
    }
    Tile(Hain3D.HEART, GlowHue.ROSE, R.string.settings_group_you, R.string.settings_you_hint) {
        open(SettingsPage.YOU)
    }
    Tile(Hain3D.PARCEL, GlowHue.SUN, R.string.settings_packs, R.string.settings_packs_hint, actions.onOpenPacks)
    Tile(Hain3D.SHIELD, GlowHue.SKY, R.string.settings_group_privacy, R.string.settings_privacy_hint) {
        open(SettingsPage.PRIVACY)
    }
    Tile(Hain3D.TREE, GlowHue.TEAL, R.string.settings_group_about, R.string.settings_about_hint) {
        open(SettingsPage.ABOUT)
    }
    // T-211: only after "Version" was tapped 7× in "Über Hain"
    if (devMode) {
        Tile(
            SettingsPage.DEVELOPER.icon,
            SettingsPage.DEVELOPER.hue,
            R.string.settings_group_dev,
            R.string.settings_dev_hint,
        ) {
            open(SettingsPage.DEVELOPER)
        }
    }
}

/** Big 3D icon of the group in its glowing glass badge, with the one-line hint next to it. */
@Composable
private fun PageHeader(page: SettingsPage) {
    val spacing = LocalHainSpacing.current
    Row(
        Modifier.fillMaxWidth().padding(top = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Hain3DBadge(page.icon, page.hue, size = 64.dp)
        BasicText(
            stringResource(page.hint),
            style = LocalHainTypography.current.body.copy(color = LocalHainColors.current.textMuted),
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * One group on the hub (T-011, reference 3): a glass tile whose colour shines out of its 3D icon on
 * the left, title and one line, a quiet chevron. Presses in a little.
 */
@Composable
private fun Tile(
    icon: Hain3D,
    hue: GlowHue,
    title: Int,
    hint: Int,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }.glassTile(hue, lightAt = Offset(x = 0.1f, y = 0.5f))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Hain3DIcon(icon, size = 40.dp)
        Column(Modifier.weight(1f)) {
            BasicText(stringResource(title), style = type.button.copy(color = colors.text))
            BasicText(stringResource(hint), style = type.bodySmall.copy(color = colors.textMuted))
        }
        Image(
            HainIcons.chevronRight,
            null,
            Modifier.size(20.dp),
            colorFilter = ColorFilter.tint(colors.icon),
        )
    }
}

private const val HEADER_PIM = 56
