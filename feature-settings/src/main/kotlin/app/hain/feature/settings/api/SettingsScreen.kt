package app.hain.feature.settings.api

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import app.hain.ui.layout.HainPaneDivider
import app.hain.ui.layout.currentHainWindowSize
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
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
    // T-235: from 840 dp on list and page sit side by side; the first group is open right away
    val wide = currentHainWindowSize().isExpanded
    BackHandler { if (page != null && !wide) page = null else actions.onBack() }
    val current = page
    Column(
        modifier
            .fillMaxSize()
            .background(LocalHainColors.current.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        if (wide) {
            SettingsListDetail(current ?: SettingsPage.LOOK, { page = it }, settings, actions, networkLog, extras)
        } else {
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
                    PageBody(current, settings, actions, networkLog, extras)
                }
                Spacer(Modifier.height(LocalHainSpacing.current.xl))
            }
        }
    }
}

/**
 * T-235: list-detail on wide windows. Left the hub (the open group is marked), a hairline, right
 * the page itself with its own title. Memory, packs and news still open as their own screens.
 */
@Suppress("LongParameterList") // the same optional extras as [SettingsBody]
@Composable
private fun SettingsListDetail(
    page: SettingsPage,
    onPage: (SettingsPage) -> Unit,
    settings: HainSettings,
    actions: SettingsActions,
    networkLog: List<NetworkLogEntry>,
    extras: SettingsExtras,
) {
    val spacing = LocalHainSpacing.current
    Row(Modifier.fillMaxSize()) {
        Column(Modifier.width(LIST_PANE_WIDTH.dp).fillMaxHeight()) {
            TopBar(
                title = stringResource(R.string.settings_title),
                leading = { HainIconButton(HainIcons.close, stringResource(R.string.settings_back), actions.onBack) },
            )
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                Hub(actions, devMode = settings.dev.enabled, selected = page, open = onPage)
                Spacer(Modifier.height(spacing.xl))
            }
        }
        HainPaneDivider()
        Column(Modifier.weight(1f).fillMaxHeight()) {
            TopBar(title = stringResource(page.title))
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    // T-238: the page stays readable and sits in the middle of the free space
                    .wrapContentWidth(Alignment.CenterHorizontally)
                    .padding(horizontal = spacing.xl)
                    .widthIn(max = DETAIL_MAX_WIDTH.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                PageBody(page, settings, actions, networkLog, extras)
                Spacer(Modifier.height(spacing.xl))
            }
        }
    }
}

/** Header and the one glass tile of a settings page (T-011, T-214). */
@Composable
private fun PageBody(
    page: SettingsPage,
    settings: HainSettings,
    actions: SettingsActions,
    networkLog: List<NetworkLogEntry>,
    extras: SettingsExtras,
) {
    // T-214: "Über Hain" has its own centred header with Pim
    if (page != SettingsPage.ABOUT) PageHeader(page)
    // the whole page sits on one glass tile lit from above in the group's colour (T-011)
    Column(
        Modifier
            .fillMaxWidth()
            .glassTile(page.hue, elevation = 8.dp, lightAt = Offset(x = 0.5f, y = 0f))
            .padding(LocalHainSpacing.current.lg),
        verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.md),
    ) {
        SettingsPageContent(page, settings, actions, networkLog, extras)
    }
}

@Composable
private fun Hub(
    actions: SettingsActions,
    devMode: Boolean = false,
    /** T-235: the group open next to the list (wide windows); `null` on phones. */
    selected: SettingsPage? = null,
    open: (SettingsPage) -> Unit,
) {
    val tile: @Composable (SettingsPage) -> Unit = { p ->
        Tile(p.icon, p.hue, p.title, p.hint, selected = p == selected) { open(p) }
    }
    // T-234: the first page only lists the groups (icon, name, one line); everything else is one level down
    tile(SettingsPage.LOOK)
    tile(SettingsPage.ANSWERS)
    tile(SettingsPage.YOU)
    // T-115: what Pim keeps about you, on its own page
    Tile(Hain3D.BULB, GlowHue.LILAC, R.string.settings_memory, R.string.settings_memory_hint) { actions.onOpenMemory() }
    // T-234: "Modelle & Pakete" (knowledge, brains, multimedia, voice, extras), each with its own page
    Tile(Hain3D.PARCEL, GlowHue.SUN, R.string.settings_packs, R.string.settings_packs_hint) { actions.onOpenPacks() }
    actions.onOpenNews?.let { news ->
        Tile(Hain3D.NEWS, GlowHue.PEACH, R.string.settings_news, R.string.settings_news_hint, onClick = news)
    }
    tile(SettingsPage.PRIVACY)
    tile(SettingsPage.ABOUT)
    // T-211: only after "Version" was tapped 7× in "Über Hain"
    if (devMode) tile(SettingsPage.DEVELOPER)
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
@Suppress("LongParameterList") // icon, light, two texts, the T-235 mark and the click
@Composable
private fun Tile(
    icon: Hain3D,
    hue: GlowHue,
    title: Int,
    hint: Int,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    val shape = LocalHainShapes.current.tile
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }.glassTile(hue, lightAt = Offset(x = 0.1f, y = 0.5f))
            // T-235: the group open on the right gets a quiet green edge
            .then(if (selected) Modifier.border(SELECTED_EDGE.dp, colors.accent, shape) else Modifier)
            .semantics { this.selected = selected }
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

/** T-235: width of the list on wide windows, and how wide a page may grow next to it. */
private const val LIST_PANE_WIDTH = 360
private const val DETAIL_MAX_WIDTH = 720
private const val SELECTED_EDGE = 1.5f
