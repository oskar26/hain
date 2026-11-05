package app.hain.feature.memory.api

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hain.feature.memory.R
import app.hain.memory.api.MemoryFact
import app.hain.memory.api.MemoryTurn
import app.hain.memory.api.MemoryUse
import app.hain.ui.components.HainChip
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface

/** Which chips one answer gets (7b, MEMORY.md §2.1 and §3.5); pure, so it is tested without Compose. */
data class AnswerChips(
    /** "Gemerkt ✓" with "Rückgängig" (a "merk dir" or the `remember` tool). */
    val saved: Boolean = false,
    /** "Vergessen ✓" with "Rückgängig" (a "vergiss …"). */
    val forgotten: Boolean = false,
    /** "Rückgängig" was tapped: one quiet chip "Rückgängig gemacht" instead. */
    val undone: Boolean = false,
    /** Facts this answer used: "N Erinnerungen genutzt", tap shows which. */
    val used: List<MemoryFact> = emptyList(),
    /** T-262: what the message said about the user, as "Merken: wohnt in Köln" (one tap keeps it). */
    val suggestion: MemoryFact? = null,
    /** The suggestion was kept: "Gemerkt ✓". */
    val accepted: Boolean = false,
) {
    val isEmpty: Boolean get() = !saved && !forgotten && !undone && used.isEmpty() && suggestion == null

    companion object {
        fun of(use: MemoryUse?): AnswerChips {
            if (use == null || use.isEmpty) return AnswerChips()
            val changed = use.saved.isNotEmpty() || use.forgotten.isNotEmpty()
            return AnswerChips(
                saved = use.saved.isNotEmpty() && !use.undone,
                forgotten = use.forgotten.isNotEmpty() && !use.undone,
                undone = changed && use.undone,
                used = use.used,
                suggestion = use.suggested.firstOrNull(),
                accepted = use.accepted,
            )
        }
    }
}

/**
 * The memory chips under one finished answer (7b): "Gemerkt ✓ · Rückgängig", "Vergessen ✓ ·
 * Rückgängig" and "3 Erinnerungen genutzt", which opens the list of those facts. [userMessages] are
 * the chat's user messages up to this answer (the turn's key, [MemoryTurn.key]).
 */
@Composable
fun MemoryAnswerChips(
    turn: MemoryTurn,
    userMessages: List<String>,
    modifier: Modifier = Modifier,
) {
    val uses by turn.uses.collectAsStateWithLifecycle()
    val key = MemoryTurn.key(userMessages)
    val chips = AnswerChips.of(uses[key])
    if (chips.isEmpty) return
    AnswerChipsView(chips, onUndo = { turn.undo(key) }, modifier = modifier, onKeep = { turn.accept(key) })
}

/** The chips themselves, without the memory (previews, screenshots). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnswerChipsView(
    chips: AnswerChips,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    onKeep: () -> Unit = {},
) {
    val spacing = LocalHainSpacing.current
    val haptics = rememberHainHaptics()
    var open by rememberSaveable { mutableStateOf(false) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
            when {
                chips.undone -> ChipTarget(stringResource(R.string.memory_chip_undone))
                chips.saved || chips.forgotten -> {
                    val doneRes = if (chips.saved) R.string.memory_chip_saved else R.string.memory_chip_forgotten
                    val done = stringResource(doneRes)
                    val undo = stringResource(R.string.memory_undo)
                    ChipTarget(done, selected = true)
                    ChipTarget(undo, description = "$done, $undo") {
                        haptics.toggle()
                        onUndo()
                    }
                }
            }
            chips.suggestion?.let { fact ->
                // T-262: "Merken: wohnt in Köln" – one tap keeps it; it also waits on the memory page
                if (chips.accepted) {
                    ChipTarget(stringResource(R.string.memory_chip_kept, fact.text), selected = true)
                } else {
                    val label = stringResource(R.string.memory_chip_keep, fact.text)
                    ChipTarget(label, description = label) {
                        haptics.toggle()
                        onKeep()
                    }
                }
            }
            if (chips.used.isNotEmpty()) {
                val label = pluralStringResource(R.plurals.memory_chip_used, chips.used.size, chips.used.size)
                val state = stringResource(if (open) R.string.memory_chip_open else R.string.memory_chip_closed)
                ChipTarget(label, selected = open, state = state) {
                    haptics.select()
                    open = !open
                }
            }
        }
        AnimatedVisibility(
            visible = open && chips.used.isNotEmpty(),
            enter = fadeIn(hainTween(HainMotion.FAST_MS)) + expandVertically(hainTween(HainMotion.FAST_MS)),
            exit = fadeOut(hainTween(HainMotion.FAST_MS)) + shrinkVertically(hainTween(HainMotion.FAST_MS)),
        ) { UsedList(chips.used) }
    }
}

/** A chip with a 48 dp touch target; [onClick] `null` = only a label. */
@Suppress("LongParameterList") // all optional but the text
@Composable
private fun ChipTarget(
    text: String,
    selected: Boolean = false,
    description: String? = null,
    state: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val tap =
        if (onClick != null) {
            Modifier.clickable(role = Role.Button, onClick = onClick)
        } else {
            Modifier
        }
    Box(
        Modifier
            .defaultMinSize(minHeight = TOUCH.dp)
            .then(tap)
            .semantics(mergeDescendants = true) {
                description?.let { contentDescription = it }
                state?.let { stateDescription = it }
            },
        contentAlignment = Alignment.CenterStart,
    ) { HainChip(text, selected = selected) }
}

/** Which facts the answer used: text and day, on a calm surface (hairline, no shadow). */
@Composable
private fun UsedList(facts: List<MemoryFact>) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val locale = LocalConfiguration.current.locales[0]
    Column(
        Modifier
            .fillMaxWidth()
            .hainSurface(HainLevel.FLAT, LocalHainShapes.current.card)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        BasicText(stringResource(R.string.memory_chip_used_title), style = type.label.copy(color = colors.textMuted))
        facts.forEach { fact ->
            Column {
                BasicText(fact.text, style = type.body.copy(color = colors.text))
                BasicText(
                    MemoryList.date(fact.createdAt, locale) + " · " +
                        stringResource(MemoryList.categoryLabel(fact.category)),
                    style = type.label.copy(color = colors.textMuted),
                )
            }
        }
        BasicText(stringResource(R.string.memory_chip_used_hint), style = type.label.copy(color = colors.textMuted))
    }
}

private const val TOUCH = 48
