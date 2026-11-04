package app.hain.cards.api

import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.hain.cards.R
import app.hain.ui.HainIcons
import app.hain.ui.components.pressScale
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.calmTile
import kotlinx.coroutines.delay

/**
 * Renders a [ModelCard] in the answer (CARDS.md §4, T-131), Lichtung style: one calm tile with a
 * hairline (radius 12 dp), the title in Medium, then the blocks — bullets, a table whose columns
 * share the width, a checklist the user can tick ([state] / [onStateChange], saved by the chat),
 * a progress bar, a bar or line chart ([CardChart], 0.1.6.1), and quiet buttons. `copy` and `share` work right here; `ask:"…"` goes to [onAsk]
 * (sends a new message). Nothing opens a URL. While the brain still writes ([complete] = false)
 * a muted line says so and buttons wait.
 */
@Suppress("LongParameterList") // card + its state + callbacks, all with defaults
@Composable
fun CardRenderer(
    card: ModelCard,
    modifier: Modifier = Modifier,
    cardIndex: Int = 0,
    state: CardState = CardState(),
    onStateChange: ((CardState) -> Unit)? = null,
    onAsk: ((String) -> Unit)? = null,
    complete: Boolean = true,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    Column(
        modifier
            .padding(vertical = spacing.xs)
            .fillMaxWidth()
            .calmTile(LocalHainShapes.current.card)
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        card.title?.let { title ->
            BasicText(
                title,
                style = type.button.copy(color = colors.text),
                modifier = Modifier.semantics { heading() },
            )
        }
        val buttons = card.blocks.filterIsInstance<CardBlock.Button>()
        card.blocks.forEachIndexed { blockIndex, block ->
            when (block) {
                is CardBlock.Bullets -> Bullets(block)
                is CardBlock.Checklist ->
                    Checklist(
                        block,
                        isChecked = { i, default -> state.isChecked(cardIndex, blockIndex, i, default) },
                        onToggle = { i, default ->
                            onStateChange?.invoke(state.toggled(cardIndex, blockIndex, i, default))
                        },
                    )
                is CardBlock.Table -> Table(block)
                is CardBlock.Progress -> Progress(block)
                is CardBlock.Chart -> CardChart(block)
                is CardBlock.Button -> Unit // all buttons sit together at the bottom
            }
        }
        if (buttons.isNotEmpty()) {
            Buttons(buttons, enabled = complete, plain = { CardText.plain(card, cardIndex, state) }, onAsk = onAsk)
        }
        if (!complete) {
            BasicText(stringResource(R.string.card_writing), style = type.label.copy(color = colors.textMuted))
        }
    }
}

@Composable
private fun Bullets(block: CardBlock.Bullets) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Column(verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.xs)) {
        block.items.forEach { item ->
            Row(verticalAlignment = Alignment.Top) {
                // a small green dot, centred on the first line (body: 25 sp line height)
                Box(
                    Modifier
                        .padding(top = BULLET_TOP.dp)
                        .size(BULLET.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                )
                Spacer(Modifier.width(LocalHainSpacing.current.md))
                BasicText(item, style = type.body.copy(color = colors.text))
            }
        }
    }
}

@Composable
private fun Checklist(
    block: CardBlock.Checklist,
    isChecked: (Int, Boolean) -> Boolean,
    onToggle: (Int, Boolean) -> Unit,
) {
    val haptics = rememberHainHaptics()
    Column {
        block.items.forEachIndexed { i, item ->
            val checked = isChecked(i, item.checked)
            CheckRow(item.text, checked) {
                haptics.toggle()
                onToggle(i, item.checked)
            }
        }
    }
}

@Composable
private fun CheckRow(
    text: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val reduce = LocalReduceMotion.current
    val fill by animateColorAsState(
        if (checked) colors.accent else Color.Transparent,
        hainTween(HainMotion.FAST_MS, reduce),
        label = "check-fill",
    )
    val edge by animateColorAsState(
        if (checked) colors.accent else colors.textMuted,
        hainTween(HainMotion.FAST_MS, reduce),
        label = "check-edge",
    )
    val state = stringResource(if (checked) R.string.card_item_done else R.string.card_item_open)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(LocalHainShapes.current.row)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics { stateDescription = state }
            .defaultMinSize(minHeight = ROW_MIN.dp)
            .padding(vertical = LocalHainSpacing.current.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(BOX.dp)
                .clip(LocalHainShapes.current.chip)
                .background(fill)
                .border(BOX_EDGE.dp, edge, LocalHainShapes.current.chip),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Image(
                    HainIcons.check,
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(colors.onAccent),
                    modifier = Modifier.size(CHECK_ICON.dp),
                )
            }
        }
        Spacer(Modifier.width(LocalHainSpacing.current.md))
        BasicText(
            text,
            style =
                type.body.copy(
                    color = if (checked) colors.textMuted else colors.text,
                    textDecoration = if (checked) TextDecoration.LineThrough else null,
                ),
        )
    }
}

/** Columns share the width evenly and wrap; the header sits on the soft grey surface. */
@Composable
private fun Table(block: CardBlock.Table) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val shape = LocalHainShapes.current.chip
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(HAIRLINE.dp, colors.border, shape),
    ) {
        TableRow(
            block.header,
            type.label.copy(color = colors.textMuted, fontWeight = FontWeight.Medium),
            Modifier.background(colors.surface),
        )
        block.rows.forEach { row ->
            Box(Modifier.fillMaxWidth().height(HAIRLINE.dp).background(colors.border))
            TableRow(row, type.bodySmall.copy(color = colors.text))
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalHainSpacing.current
    Row(modifier.fillMaxWidth()) {
        cells.forEach { cell ->
            BasicText(
                cell,
                style = textStyle,
                modifier = Modifier.weight(1f).padding(horizontal = spacing.md, vertical = spacing.sm),
            )
        }
    }
}

@Composable
private fun Progress(block: CardBlock.Progress) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val reduce = LocalReduceMotion.current
    // grows from 0 when the card appears (snaps with "reduce motion")
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val fraction by animateFloatAsState(
        if (shown) block.value / PERCENT else 0f,
        hainTween(HainMotion.PAGE_MS * 2, reduce),
        label = "card-progress",
    )
    val description = stringResource(R.string.card_progress, block.value)
    Column(
        Modifier.semantics(mergeDescendants = true) {
            contentDescription = listOfNotNull(block.label, description).joinToString(", ")
            progressBarRangeInfo = ProgressBarRangeInfo(block.value / PERCENT, 0f..1f)
        },
        verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.xs),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                block.label.orEmpty(),
                style = type.bodySmall.copy(color = colors.text),
                modifier = Modifier.weight(1f),
            )
            BasicText("${block.value} %", style = type.label.copy(color = colors.textMuted))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(TRACK.dp)
                .clip(CircleShape)
                .background(colors.surfaceHigh),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(TRACK.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
            )
        }
    }
}

@Composable
private fun Buttons(
    buttons: List<CardBlock.Button>,
    enabled: Boolean,
    plain: () -> String,
    onAsk: ((String) -> Unit)?,
) {
    val spacing = LocalHainSpacing.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.card_share_title)
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_MS)
            copied = false
        }
    }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        buttons.forEach { button ->
            val action = button.action
            val label = if (action == CardAction.Copy && copied) stringResource(R.string.card_copied) else button.label
            CardButton(label, enabled = enabled && (action !is CardAction.Ask || onAsk != null)) {
                when (action) {
                    CardAction.Copy -> {
                        clipboard.setText(AnnotatedString(plain()))
                        copied = true
                    }
                    CardAction.Share -> {
                        val send =
                            Intent(Intent.ACTION_SEND)
                                .setType("text/plain")
                                .putExtra(Intent.EXTRA_TEXT, plain())
                        context.startActivity(Intent.createChooser(send, shareTitle))
                    }
                    is CardAction.Ask -> onAsk?.invoke(action.text)
                }
            }
        }
    }
}

/** A quiet Lichtung button: soft green fill, green Medium label, presses in with a light tick. */
@Composable
private fun CardButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    val haptics = rememberHainHaptics()
    val shape = LocalHainShapes.current.button
    Box(
        Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                alpha = if (enabled) 1f else DISABLED_ALPHA
            }.defaultMinSize(minHeight = BUTTON_MIN.dp)
            .clip(shape)
            .background(colors.accentSoft, shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = {
                    haptics.tick()
                    onClick()
                },
            ).padding(horizontal = LocalHainSpacing.current.lg, vertical = LocalHainSpacing.current.sm),
        contentAlignment = Alignment.Center,
    ) {
        BasicText(label, style = LocalHainTypography.current.button.copy(color = colors.accent))
    }
}

private const val PERCENT = 100f
private const val BULLET = 6
private const val BULLET_TOP = 10
private const val BOX = 20
private const val BOX_EDGE = 1.5f
private const val CHECK_ICON = 14
private const val ROW_MIN = 40
private const val HAIRLINE = 1
private const val TRACK = 8
private const val BUTTON_MIN = 40
private const val DISABLED_ALPHA = 0.45f
private const val COPIED_MS = 1500L
