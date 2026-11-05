@file:Suppress("MatchingDeclarationName") // the picker state and its composables live together

package app.hain.feature.chat.api

import android.text.format.Formatter
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.models.api.BrainOption
import app.hain.models.api.BrainSize
import app.hain.ui.HainIcons
import app.hain.ui.components.HainPopupItem
import app.hain.ui.components.HainPopupMenu
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/**
 * What the brain picker in the top bar shows (T-055): the brains from the catalogue (on the phone
 * first, then the ones still to download), whether the own model of developer mode is in use, and
 * what a tap does. `null` on [ChatScreen] = no picker.
 */
class BrainPickerState(
    /** Read when the menu opens (checks files on the phone), not on every frame. */
    val options: () -> List<BrainOption>,
    val ownModelActive: () -> Boolean = { false },
    /** A brain on the phone was tapped: remember it and load it in the background. */
    val onPick: (BrainOption) -> Unit = {},
    /** A brain that is not on the phone yet, or "more brains": open the knowledge packs. */
    val onGetMore: () -> Unit = {},
)

/** Plain-language name of a brain size (EN + DE strings). */
internal fun BrainSize.label(): Int =
    when (this) {
        BrainSize.SMALL -> R.string.chat_brain_size_small
        BrainSize.MEDIUM -> R.string.chat_brain_size_medium
        BrainSize.LARGE -> R.string.chat_brain_size_large
    }

/**
 * Compact frosted-glass menu that opens below the title (T-055, like the model picker in ChatGPT or
 * Gemini). Put it into the title Box of the [app.hain.ui.components.TopBar] (`titleAccessory`).
 */
@Composable
internal fun BoxScope.ModelPicker(
    expanded: Boolean,
    onDismiss: () -> Unit,
    state: BrainPickerState,
) {
    var anchorHeight by remember { mutableIntStateOf(0) }
    Box(Modifier.matchParentSize().onSizeChanged { anchorHeight = it.height })
    // the tile keeps room for its shadow; pull it up by that much so it sits right under the title
    val lift = with(LocalDensity.current) { MENU_LIFT.dp.roundToPx() }
    val haptics = rememberHainHaptics()
    // read the catalogue and the files on the phone once per opening; keep the rows while it fades out
    val opened = remember(expanded) { if (expanded) state.options() to state.ownModelActive() else null }
    val shown = remember { arrayOfNulls<Pair<List<BrainOption>, Boolean>>(1) }
    if (opened != null) shown[0] = opened
    val (options, ownModel) = shown[0] ?: (emptyList<BrainOption>() to false)
    HainPopupMenu(
        expanded = expanded,
        onDismiss = onDismiss,
        alignment = Alignment.TopCenter,
        offset = IntOffset(0, (anchorHeight - lift).coerceAtLeast(0)),
        growFrom = TransformOrigin(PIVOT_CENTER, 0f),
    ) {
        ModelPickerContent(
            options = options,
            ownModelActive = ownModel,
            onPick = { option ->
                if (option.installed) {
                    haptics.select()
                    if (!option.active || ownModel) state.onPick(option)
                } else {
                    state.onGetMore()
                }
                onDismiss()
            },
            onGetMore = {
                state.onGetMore()
                onDismiss()
            },
        )
    }
}

/** The menu's rows without the popup window, also used by the screenshot tests. */
@Composable
internal fun ModelPickerContent(
    options: List<BrainOption>,
    ownModelActive: Boolean,
    onPick: (BrainOption) -> Unit,
    onGetMore: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    Column {
        BasicText(
            stringResource(R.string.chat_brain_picker_title),
            style = type.label.copy(color = colors.textMuted),
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
        )
        if (ownModelActive) {
            BasicText(
                stringResource(R.string.chat_brain_own_active),
                style = type.bodySmall.copy(color = colors.accent),
                modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.xs),
            )
        }
        options.forEach { option ->
            BrainRow(option, showActive = !ownModelActive) { onPick(option) }
        }
        Box(
            Modifier
                .padding(vertical = spacing.xs)
                .fillMaxWidth()
                .height(HAIRLINE.dp)
                .background(colors.border),
        )
        HainPopupItem(HainIcons.download, stringResource(R.string.chat_brain_get_more), onGetMore)
    }
}

/**
 * T-240: the optional strong brain gets its own plain name instead of the size word; T-270: so does
 * the optional Omni brain, so it is not mixed up with the standard one.
 */
internal fun brainName(option: BrainOption): Int =
    when {
        option.strong -> R.string.chat_brain_strong
        option.model.isOptional -> R.string.chat_brain_optional
        else -> option.size.label()
    }

/** One brain: plain name, model name + size, "recommended" tag, green check on the one in use. */
@Suppress("LongMethod") // one row: semantics, texts, tag and check
@Composable
private fun BrainRow(
    option: BrainOption,
    showActive: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val name = stringResource(brainName(option))
    val size = Formatter.formatShortFileSize(LocalContext.current, option.model.totalSize)
    val detail =
        if (option.installed) {
            stringResource(R.string.chat_brain_detail, option.model.name, size)
        } else {
            stringResource(R.string.chat_brain_not_installed, size)
        }
    val recommended = stringResource(R.string.chat_brain_recommended)
    val active = showActive && option.active
    val description =
        buildString {
            append(name).append(", ").append(detail)
            if (option.recommended) append(", ").append(recommended)
        }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(LocalHainShapes.current.row)
            .background(if (active) colors.accentSoft.copy(alpha = SELECTED_ALPHA) else Color.Transparent)
            .semantics(mergeDescendants = true) {
                role = if (option.installed) Role.RadioButton else Role.Button
                if (option.installed) selected = active
                contentDescription = description
            }.clickable(onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            HainIcons.brain,
            contentDescription = null,
            colorFilter = ColorFilter.tint(if (active) colors.accent else colors.icon),
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(spacing.md))
        Column(Modifier.weight(1f)) {
            BasicText(
                name,
                style = type.button.copy(color = if (option.installed) colors.text else colors.textMuted),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            BasicText(
                detail,
                style = type.bodySmall.copy(color = colors.textMuted),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (option.recommended) {
                BasicText(
                    recommended,
                    style = type.label.copy(color = colors.accent),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Spacer(Modifier.width(spacing.sm))
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            when {
                active ->
                    Image(
                        HainIcons.check,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colors.accent),
                        modifier = Modifier.size(18.dp),
                    )
                !option.installed ->
                    Image(
                        HainIcons.download,
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(colors.icon),
                        modifier = Modifier.size(18.dp),
                    )
            }
        }
    }
}

private const val SELECTED_ALPHA = 0.7f
private const val HAIRLINE = 1
private const val PIVOT_CENTER = 0.5f
private const val MENU_LIFT = 8
