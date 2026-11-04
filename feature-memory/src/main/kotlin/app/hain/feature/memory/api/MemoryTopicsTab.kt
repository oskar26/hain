package app.hain.feature.memory.api

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.hain.feature.memory.R
import app.hain.memory.api.MemoryTopic
import app.hain.ui.HainIcons
import app.hain.ui.components.EmptyState
import app.hain.ui.components.Hain3D
import app.hain.ui.components.Hain3DIcon
import app.hain.ui.components.HainButton
import app.hain.ui.components.HainButtonStyle
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.ProgressBar
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface

/**
 * "Themen" (7b, MEMORY.md L1): the kept facts grouped by kind, each with a short note of the user's
 * own that Pim reads when a fact of that kind fits the question.
 */
@Composable
internal fun TopicsTab(
    state: MemoryPageState,
    actions: MemoryActions,
) {
    val type = LocalHainTypography.current
    val colors = LocalHainColors.current
    val groups = MemoryList.topics(state.facts, state.topics)
    if (groups.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.memory_topics_empty_title),
            body = stringResource(R.string.memory_topics_empty_body),
            modifier = Modifier.fillMaxWidth(),
            art = { LivePim(PimState.IDLE, EMPTY_PIM.dp, tappable = false) },
        )
        return
    }
    BasicText(stringResource(R.string.memory_topics_hint), style = type.bodySmall.copy(color = colors.textMuted))
    groups.forEach { group -> TopicTile(group, actions) }
}

@Composable
private fun TopicTile(
    group: TopicGroup,
    actions: MemoryActions,
) {
    val type = LocalHainTypography.current
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val title = stringResource(MemoryList.categoryLabel(group.category))
    var editing by rememberSaveable(group.category) { mutableStateOf(false) }
    var draft by rememberSaveable(group.category) { mutableStateOf(group.note) }
    Tile(GlowHue.LILAC) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                BasicText(title, style = type.button.copy(color = colors.text))
                BasicText(
                    pluralStringResource(R.plurals.memory_topic_count, group.facts.size, group.facts.size),
                    style = type.label.copy(color = colors.textMuted),
                )
            }
            if (!editing) {
                HainIconButton(HainIcons.edit, stringResource(R.string.memory_topic_note_edit, title), {
                    draft = group.note
                    editing = true
                }, tint = colors.icon)
            }
        }
        group.facts.take(MAX_SHOWN).forEach { fact ->
            BasicText("· " + fact.text, style = type.body.copy(color = colors.text))
        }
        if (group.facts.size > MAX_SHOWN) {
            BasicText(
                stringResource(R.string.memory_topic_more, group.facts.size - MAX_SHOWN),
                style = type.label.copy(color = colors.textMuted),
            )
        }
        when {
            editing ->
                NoteEditor(draft, onChange = { draft = it.take(MemoryTopic.MAX_NOTE) }) {
                    actions.onTopicNote(group.category.key, draft)
                    editing = false
                }
            group.note.isNotBlank() ->
                Column(Modifier.padding(top = spacing.xs)) {
                    val label = stringResource(R.string.memory_topic_note)
                    BasicText(label, style = type.label.copy(color = colors.textMuted))
                    BasicText(group.note, style = type.bodySmall.copy(color = colors.text))
                }
        }
    }
}

@Composable
private fun NoteEditor(
    draft: String,
    onChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    val type = LocalHainTypography.current
    val colors = LocalHainColors.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .weight(1f)
                .hainSurface(HainLevel.FLAT, LocalHainShapes.current.card)
                .padding(LocalHainSpacing.current.sm),
        ) {
            if (draft.isEmpty()) {
                val hint = stringResource(R.string.memory_topic_note_hint)
                BasicText(hint, style = type.body.copy(color = colors.textMuted))
            }
            BasicTextField(
                value = draft,
                onValueChange = onChange,
                textStyle = type.body.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        HainIconButton(HainIcons.check, stringResource(R.string.memory_save), onSave)
    }
}

/**
 * Hint tile for the pack "Suche nach Bedeutung" (T-110): what it does, its size and a download
 * button; while it loads a bar, once it is there one quiet line.
 */
@Composable
internal fun SmartSearchTile(
    state: SmartSearch,
    onGet: () -> Unit,
) {
    if (state == SmartSearch.Hidden) return
    val type = LocalHainTypography.current
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    Tile(GlowHue.SKY) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(spacing.md)) {
            Hain3DIcon(Hain3D.SPARKLES, size = ICON.dp)
            Column(Modifier.weight(1f)) {
                BasicText(stringResource(R.string.memory_smart_title), style = type.button.copy(color = colors.text))
                val line =
                    when (state) {
                        SmartSearch.On -> stringResource(R.string.memory_smart_on)
                        else -> stringResource(R.string.memory_smart_body)
                    }
                BasicText(line, style = type.bodySmall.copy(color = colors.textMuted))
            }
        }
        when (state) {
            is SmartSearch.Offer -> {
                if (state.failed) {
                    val failed = stringResource(R.string.memory_smart_failed)
                    BasicText(failed, style = type.label.copy(color = colors.danger))
                }
                HainButton(
                    stringResource(R.string.memory_smart_get, state.sizeMb),
                    onGet,
                    Modifier.fillMaxWidth(),
                    HainButtonStyle.SECONDARY,
                )
            }
            is SmartSearch.Loading -> {
                val percent = state.percent
                ProgressBar((percent ?: 0) / PERCENT, Modifier.fillMaxWidth())
                BasicText(
                    if (percent == null) {
                        stringResource(R.string.memory_smart_wait)
                    } else {
                        stringResource(R.string.memory_smart_loading, percent)
                    },
                    style = type.label.copy(color = colors.textMuted),
                )
            }
            SmartSearch.On, SmartSearch.Hidden -> Unit
        }
    }
}

private const val EMPTY_PIM = 72
private const val ICON = 36
private const val MAX_SHOWN = 6
private const val PERCENT = 100f
