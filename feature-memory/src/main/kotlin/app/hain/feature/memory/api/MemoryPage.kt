package app.hain.feature.memory.api

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.hain.feature.memory.R
import app.hain.memory.api.MemoryFact
import app.hain.memory.api.MemoryTopic
import app.hain.ui.HainIcons
import app.hain.ui.components.EmptyState
import app.hain.ui.components.HainButton
import app.hain.ui.components.HainButtonStyle
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.HainSwitch
import app.hain.ui.components.SegmentedControl
import app.hain.ui.components.TopBar
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.glassTile
import app.hain.ui.theme.hainSurface

/** What the memory page shows (T-115); [notice] is a short line at the bottom ("Gelöscht"). */
data class MemoryPageState(
    val facts: List<MemoryFact> = emptyList(),
    val enabled: Boolean = true,
    val aboutMe: String = "",
    val notice: String? = null,
    /** The last deleted fact, while "Rückgängig" can still bring it back. */
    val undo: MemoryFact? = null,
    /** The user's notes on the topics (7b, "Themen" tab). */
    val topics: List<MemoryTopic> = emptyList(),
    /** The optional pack for recall by meaning (T-110). */
    val smartSearch: SmartSearch = SmartSearch.Hidden,
)

/** Where the pack "Suche nach Bedeutung" stands (T-110), as the memory page shows it. */
sealed interface SmartSearch {
    /** Not offered (the phone has too little memory or the build has no native library). */
    data object Hidden : SmartSearch

    /** Can be downloaded; [sizeMb] for the button. */
    data class Offer(
        val sizeMb: Int,
        val failed: Boolean = false,
    ) : SmartSearch

    /** Downloading; [percent] `null` while it waits or checks the file. */
    data class Loading(
        val percent: Int?,
    ) : SmartSearch

    /** Installed: recall works by meaning too. */
    data object On : SmartSearch

    /** How the pack's download stands, in plain values (so this module needs no model types). */
    enum class Phase { IDLE, WAITING, RUNNING, FAILED, INSTALLED }

    companion object {
        private const val MB = 1_000_000L

        /**
         * The tile for a pack of [sizeBytes]: hidden when the phone [fits] it not at all (too little
         * memory or space) and it is not there yet; [percent] while [Phase.RUNNING].
         */
        fun of(
            phase: Phase,
            fits: Boolean,
            sizeBytes: Long,
            percent: Int = 0,
        ): SmartSearch =
            when (phase) {
                Phase.INSTALLED -> On
                Phase.RUNNING -> Loading(percent.coerceIn(0, PERCENT))
                Phase.WAITING -> Loading(null)
                Phase.FAILED, Phase.IDLE ->
                    if (fits) Offer(((sizeBytes + MB / 2) / MB).toInt(), failed = phase == Phase.FAILED) else Hidden
            }

        private const val PERCENT = 100
    }
}

/** Everything the memory page can do; each callback does one thing. */
@Suppress("LongParameterList") // one callback per action, all with defaults
class MemoryActions(
    val onBack: () -> Unit = {},
    val onEnabled: (Boolean) -> Unit = {},
    val onEdit: (Long, String) -> Unit = { _, _ -> },
    val onDelete: (MemoryFact) -> Unit = {},
    val onUndo: () -> Unit = {},
    val onAccept: (Long) -> Unit = {},
    val onDeleteAll: () -> Unit = {},
    val onAboutMe: (String) -> Unit = {},
    val onExport: () -> Unit = {},
    val onImport: () -> Unit = {},
    /** 7b: sets the note of a topic (title = kind key); empty removes it. */
    val onTopicNote: (String, String) -> Unit = { _, _ -> },
    /** T-110: starts the download of the pack "Suche nach Bedeutung". */
    val onGetSmartSearch: () -> Unit = {},
)

/** Tabs of the page (MEMORY.md §5): kept facts, topics, suggestions waiting for a yes, "Über dich". */
enum class MemoryTab { FACTS, TOPICS, SUGGESTIONS, YOU }

/** The memory page itself, without the store (previews, screenshots, tests). */
@Composable
fun MemoryPage(
    state: MemoryPageState,
    actions: MemoryActions,
    modifier: Modifier = Modifier,
    startTab: MemoryTab = MemoryTab.FACTS,
) {
    val spacing = LocalHainSpacing.current
    val colors = LocalHainColors.current
    var tab by rememberSaveable { mutableStateOf(startTab) }
    BackHandler(onBack = actions.onBack)
    Box(modifier.fillMaxSize().background(colors.bg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            TopBar(
                title = stringResource(R.string.memory_title),
                leading = { HainIconButton(HainIcons.back, stringResource(R.string.memory_back), actions.onBack) },
            )
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = spacing.lg),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                BasicText(
                    stringResource(R.string.memory_intro),
                    style = LocalHainTypography.current.bodySmall.copy(color = colors.textMuted),
                )
                val waiting = state.facts.count { it.suggested }
                val suggestionsLabel = stringResource(R.string.memory_tab_suggestions)
                SegmentedControl(
                    options =
                        listOf(
                            stringResource(R.string.memory_tab_facts),
                            stringResource(R.string.memory_tab_topics),
                            if (waiting > 0) "$suggestionsLabel ($waiting)" else suggestionsLabel,
                            stringResource(R.string.memory_tab_you),
                        ),
                    selected = tab.ordinal,
                    onSelect = { tab = MemoryTab.entries[it] },
                )
                when (tab) {
                    MemoryTab.FACTS -> FactsTab(state, actions)
                    MemoryTab.TOPICS -> TopicsTab(state, actions)
                    MemoryTab.SUGGESTIONS -> SuggestionsTab(state, actions)
                    MemoryTab.YOU -> YouTab(state, actions)
                }
                Spacer(Modifier.height(spacing.xl))
            }
        }
        Notice(state, actions, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun FactsTab(
    state: MemoryPageState,
    actions: MemoryActions,
) {
    val spacing = LocalHainSpacing.current
    var query by rememberSaveable { mutableStateOf("") }
    val kept = state.facts.filter { !it.suggested }
    if (kept.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.memory_empty_title),
            body = stringResource(R.string.memory_empty_body),
            modifier = Modifier.fillMaxWidth(),
            art = { LivePim(PimState.IDLE, EMPTY_PIM.dp, tappable = false) },
        )
    } else {
        SearchField(query) { query = it }
        val shown = MemoryList.visible(state.facts, query, suggestions = false)
        Tile(GlowHue.LILAC) {
            if (shown.isEmpty()) {
                BasicText(
                    stringResource(R.string.memory_no_match),
                    style = LocalHainTypography.current.body.copy(color = LocalHainColors.current.textMuted),
                )
            }
            shown.forEach { fact -> FactRow(fact, actions) }
        }
    }
    SmartSearchTile(state.smartSearch, actions.onGetSmartSearch)
    Spacer(Modifier.height(spacing.sm))
    SettingsTile(state, actions)
}

@Composable
private fun SuggestionsTab(
    state: MemoryPageState,
    actions: MemoryActions,
) {
    val type = LocalHainTypography.current
    val colors = LocalHainColors.current
    val waiting = MemoryList.visible(state.facts, "", suggestions = true)
    if (waiting.isEmpty()) {
        BasicText(stringResource(R.string.memory_suggestions_empty), style = type.body.copy(color = colors.textMuted))
        return
    }
    BasicText(stringResource(R.string.memory_suggestions_hint), style = type.bodySmall.copy(color = colors.textMuted))
    Tile(GlowHue.ROSE) {
        waiting.forEach { fact ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    FactText(fact)
                    // 7b: a suggestion from the chats can replace an older fact (kept, but out of use)
                    MemoryList.replaced(fact, state.facts)?.let { old ->
                        BasicText(
                            stringResource(R.string.memory_replaces, old.text),
                            style = type.label.copy(color = colors.textMuted),
                        )
                    }
                }
                HainIconButton(HainIcons.check, stringResource(R.string.memory_accept), { actions.onAccept(fact.id) })
                HainIconButton(HainIcons.close, stringResource(R.string.memory_reject), { actions.onDelete(fact) })
            }
        }
    }
}

@Composable
private fun YouTab(
    state: MemoryPageState,
    actions: MemoryActions,
) {
    val type = LocalHainTypography.current
    val colors = LocalHainColors.current
    var text by rememberSaveable { mutableStateOf(state.aboutMe) }
    Tile(GlowHue.ROSE) {
        BasicText(stringResource(R.string.memory_you_label), style = type.body.copy(color = colors.text))
        Box(
            Modifier
                .fillMaxWidth()
                .hainSurface(HainLevel.FLAT, LocalHainShapes.current.card)
                .padding(LocalHainSpacing.current.md),
        ) {
            if (text.isEmpty()) {
                BasicText(stringResource(R.string.memory_you_hint), style = type.body.copy(color = colors.textMuted))
            }
            BasicTextField(
                value = text,
                onValueChange = {
                    text = it
                    actions.onAboutMe(it)
                },
                textStyle = type.body.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** One kept fact: text, date and kind; pencil edits in place, bin deletes (with "Rückgängig"). */
@Composable
private fun FactRow(
    fact: MemoryFact,
    actions: MemoryActions,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    var editing by rememberSaveable(fact.id) { mutableStateOf(false) }
    var draft by rememberSaveable(fact.id) { mutableStateOf(fact.text) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (editing) {
            Box(
                Modifier
                    .weight(1f)
                    .hainSurface(HainLevel.FLAT, LocalHainShapes.current.card)
                    .padding(LocalHainSpacing.current.sm),
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it.take(MemoryFact.MAX_CHARS) },
                    textStyle = type.body.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.accent),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            HainIconButton(HainIcons.check, stringResource(R.string.memory_save), {
                actions.onEdit(fact.id, draft)
                editing = false
            })
        } else {
            FactText(fact, Modifier.weight(1f))
            HainIconButton(HainIcons.edit, stringResource(R.string.memory_edit), {
                draft = fact.text
                editing = true
            }, tint = colors.icon)
            HainIconButton(HainIcons.delete, stringResource(R.string.memory_delete), {
                actions.onDelete(fact)
            }, tint = colors.icon)
        }
    }
}

@Composable
private fun FactText(
    fact: MemoryFact,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier.padding(vertical = LocalHainSpacing.current.xs)) {
        BasicText(fact.text, style = type.body.copy(color = colors.text))
        BasicText(
            MemoryList.date(fact.createdAt, locale) + " · " + stringResource(MemoryList.categoryLabel(fact.category)),
            style = type.label.copy(color = colors.textMuted),
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onChange: (String) -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Box(
        Modifier
            .fillMaxWidth()
            .hainSurface(HainLevel.FLAT, LocalHainShapes.current.card)
            .padding(LocalHainSpacing.current.md),
    ) {
        if (query.isEmpty()) {
            BasicText(stringResource(R.string.memory_search), style = type.body.copy(color = colors.textMuted))
        }
        BasicTextField(
            value = query,
            onValueChange = onChange,
            singleLine = true,
            textStyle = type.body.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Switch, export/import and "Alles löschen" (with a second tap to confirm). */
@Composable
private fun SettingsTile(
    state: MemoryPageState,
    actions: MemoryActions,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val haptics = rememberHainHaptics()
    var confirm by remember { mutableStateOf(false) }
    BasicText(stringResource(R.string.memory_settings), style = type.label.copy(color = colors.textMuted))
    Tile(GlowHue.CLEAR) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(LocalHainShapes.current.row)
                .toggleable(value = state.enabled, role = Role.Switch) {
                    haptics.toggle()
                    actions.onEnabled(it)
                }.padding(vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = spacing.md)) {
                BasicText(stringResource(R.string.memory_switch), style = type.body.copy(color = colors.text))
                BasicText(
                    stringResource(R.string.memory_switch_hint),
                    style = type.bodySmall.copy(color = colors.textMuted),
                )
            }
            HainSwitch(state.enabled)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            val secondary = HainButtonStyle.SECONDARY
            HainButton(stringResource(R.string.memory_export), actions.onExport, Modifier.weight(1f), secondary)
            HainButton(stringResource(R.string.memory_import), actions.onImport, Modifier.weight(1f), secondary)
        }
        BasicText(stringResource(R.string.memory_backup_hint), style = type.bodySmall.copy(color = colors.textMuted))
        if (!confirm) {
            HainButton(stringResource(R.string.memory_delete_all), { confirm = true }, style = HainButtonStyle.GHOST)
        } else {
            BasicText(stringResource(R.string.memory_delete_all_confirm), style = type.body.copy(color = colors.danger))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                HainButton(
                    stringResource(R.string.memory_cancel),
                    { confirm = false },
                    Modifier.weight(1f),
                    HainButtonStyle.SECONDARY,
                )
                HainButton(
                    stringResource(R.string.memory_delete_all),
                    {
                        confirm = false
                        actions.onDeleteAll()
                    },
                    Modifier.weight(1f),
                    HainButtonStyle.DARK,
                )
            }
        }
    }
}

/** "Gelöscht · Rückgängig" or a short result line, fading in at the bottom. */
@Composable
private fun Notice(
    state: MemoryPageState,
    actions: MemoryActions,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    val colors = LocalHainColors.current
    AnimatedVisibility(
        visible = state.notice != null,
        modifier = modifier.navigationBarsPadding().padding(spacing.lg),
        enter = fadeIn(hainTween(HainMotion.FAST_MS)),
        exit = fadeOut(hainTween(HainMotion.FAST_MS)),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .hainSurface(HainLevel.FLOATING, LocalHainShapes.current.card)
                .padding(horizontal = spacing.md, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                state.notice.orEmpty(),
                style = type.body.copy(color = colors.text),
                modifier = Modifier.weight(1f),
            )
            if (state.undo != null) {
                HainButton(stringResource(R.string.memory_undo), actions.onUndo, style = HainButtonStyle.GHOST)
            }
        }
    }
}

/** A calm glass tile like the settings pages (Lichtung). */
@Composable
internal fun Tile(
    hue: GlowHue,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .glassTile(hue, elevation = 8.dp, lightAt = Offset(x = 0.5f, y = 0f))
            .padding(LocalHainSpacing.current.lg),
        verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.sm),
    ) { content() }
}

private const val EMPTY_PIM = 72
