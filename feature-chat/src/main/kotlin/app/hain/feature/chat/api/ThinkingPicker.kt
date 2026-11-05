@file:Suppress("MatchingDeclarationName") // the enum and its composables live together

package app.hain.feature.chat.api

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.common.api.AnswerMode
import app.hain.feature.chat.R
import app.hain.ui.HainIcons
import app.hain.ui.components.Hain3D
import app.hain.ui.components.Hain3DIcon
import app.hain.ui.components.HainPopover
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/** The three thinking levels with their 3D icon, name and one plain sentence (T-084). */
internal enum class ThinkingLevel(
    val mode: AnswerMode,
    val icon: Hain3D,
    val label: Int,
    val hint: Int,
    /** T-273: the sentence for a brain that does not think out loud (LFM3-Omni): only the research differs. */
    val hintNoThinking: Int = hint,
) {
    AUTO(AnswerMode.AUTO, Hain3D.SPARKLES, R.string.chat_mode_auto, R.string.chat_mode_auto_hint),
    FAST(
        AnswerMode.FAST,
        Hain3D.BOLT,
        R.string.chat_mode_fast,
        R.string.chat_mode_fast_hint,
        R.string.chat_mode_fast_hint_nothink,
    ),
    THOROUGH(
        AnswerMode.THOROUGH,
        Hain3D.BULB,
        R.string.chat_mode_thorough,
        R.string.chat_mode_thorough_hint,
        R.string.chat_mode_thorough_hint_nothink,
    ),
    ;

    companion object {
        fun of(mode: AnswerMode): ThinkingLevel = entries.firstOrNull { it.mode == mode } ?: AUTO
    }
}

/**
 * Brain button inside the message box (T-084, like the model picker in Gemini or ChatGPT): the
 * Lucide brain, plus the level's name when it is not Auto. A tap opens a small glass popover above
 * the box with Auto / Fast / Thorough; the choice is the same setting as Settings → Answers.
 */
@Composable
fun ThinkingPicker(
    mode: AnswerMode,
    onMode: (AnswerMode) -> Unit,
    modifier: Modifier = Modifier,
    /** T-273: `false` for a brain without thinking: "Gründlich" then means more research steps. */
    canThink: Boolean = true,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val level = ThinkingLevel.of(mode)
    var open by rememberSaveable { mutableStateOf(false) }
    val name = stringResource(level.label)
    val description = stringResource(R.string.chat_mode_button, name)
    val active = level != ThinkingLevel.AUTO
    val reduce = LocalReduceMotion.current
    val haptics = rememberHainHaptics()
    Box(modifier) {
        Row(
            Modifier
                // T-254: touch target at least 48 × 48 dp (TalkBack, big fingers)
                .heightIn(min = 48.dp)
                .widthIn(min = 48.dp)
                .clip(LocalHainShapes.current.button)
                .background(if (active || open) colors.accentSoft else Color.Transparent)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                    contentDescription = description
                }.clickable {
                    haptics.tick()
                    open = !open
                }.padding(horizontal = spacing.sm + 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Image(
                HainIcons.brain,
                contentDescription = null,
                colorFilter = ColorFilter.tint(if (active) colors.accent else colors.icon),
                modifier = Modifier.size(20.dp),
            )
            AnimatedVisibility(
                visible = active,
                enter = fadeIn(hainTween(HainMotion.FAST_MS, reduce)) + expandHorizontally(hainTween(FAST, reduce)),
                exit = fadeOut(hainTween(HainMotion.FAST_MS, reduce)) + shrinkHorizontally(hainTween(FAST, reduce)),
            ) {
                BasicText(
                    name,
                    style = LocalHainTypography.current.label.copy(color = colors.accent),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
        HainPopover(expanded = open, onDismiss = { open = false }) {
            ThinkingLevel.entries.forEach { option ->
                LevelRow(option, selected = option == level, canThink = canThink) {
                    haptics.select()
                    onMode(option.mode)
                    open = false
                }
            }
        }
    }
}

/** One row of the popover: 3D icon, name, one sentence, a green check on the active level. */
@Composable
private fun LevelRow(
    level: ThinkingLevel,
    selected: Boolean,
    canThink: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val name = stringResource(level.label)
    val selectedLabel = stringResource(R.string.chat_mode_selected, name)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(LocalHainShapes.current.row)
            .background(if (selected) colors.accentSoft.copy(alpha = SELECTED_ALPHA) else Color.Transparent)
            .semantics(mergeDescendants = true) {
                role = Role.RadioButton
                this.selected = selected
                if (selected) contentDescription = selectedLabel
            }.clickable(onClick = onClick)
            .padding(horizontal = spacing.sm, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Hain3DIcon(level.icon, size = 32.dp)
        Spacer(Modifier.width(spacing.md))
        Column(Modifier.weight(1f)) {
            BasicText(name, style = type.button.copy(color = colors.text))
            val hint = if (canThink) level.hint else level.hintNoThinking
            BasicText(stringResource(hint), style = type.bodySmall.copy(color = colors.textMuted))
        }
        Spacer(Modifier.width(spacing.sm))
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (selected) {
                Image(
                    HainIcons.check,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.accent),
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

private const val SELECTED_ALPHA = 0.7f
private const val FAST = HainMotion.FAST_MS
