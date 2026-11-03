package app.hain.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.hain.ui.HainIcons
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography

/**
 * User message: soft grey bubble on the right (DESIGN_SYSTEM.md §1). Like Gemini (T-057), a short
 * tap ([onTap]) reveals [actions] below the bubble on the right — no long-press popup. The row
 * fades and unfolds in [HainMotion.FAST_MS]; with reduced motion it simply appears.
 */
@Composable
fun UserBubble(
    text: String,
    modifier: Modifier = Modifier,
    actionsVisible: Boolean = false,
    onTap: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val shape = LocalHainShapes.current.userBubble
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        val bubble =
            Modifier
                .widthIn(max = 320.dp)
                .hainSurface(HainLevel.FLAT, shape)
        if (onTap == null) {
            SelectionContainer {
                BasicText(
                    text = text,
                    style = LocalHainTypography.current.body.copy(color = colors.text),
                    modifier = bubble.padding(horizontal = spacing.lg, vertical = spacing.md),
                )
            }
        } else {
            BasicText(
                text = text,
                style = LocalHainTypography.current.body.copy(color = colors.text),
                modifier =
                    bubble
                        .clickable(onClick = onTap)
                        .padding(horizontal = spacing.lg, vertical = spacing.md),
            )
        }
        val reduce = LocalReduceMotion.current
        AnimatedVisibility(
            visible = actionsVisible,
            enter =
                fadeIn(hainTween(HainMotion.FAST_MS, reduce)) +
                    expandVertically(hainTween(HainMotion.FAST_MS, reduce), expandFrom = Alignment.Top),
            exit =
                fadeOut(hainTween(HainMotion.FAST_MS, reduce)) +
                    shrinkVertically(hainTween(HainMotion.FAST_MS, reduce), shrinkTowards = Alignment.Top),
        ) {
            Row(
                Modifier.padding(top = spacing.xs).offset(x = ICON_INSET.dp),
                horizontalArrangement = Arrangement.spacedBy(ACTION_GAP.dp),
                content = actions,
            )
        }
    }
}

/** Assistant answer: plain markdown text without bubble, the action row (T-057) below when finished. */
@Composable
fun AssistantBlock(
    markdown: String,
    modifier: Modifier = Modifier,
    actions: (@Composable () -> Unit)? = null,
) {
    Column(modifier.fillMaxWidth()) {
        SelectionContainer {
            Markdown(
                content = markdown,
                colors = hainMarkdownColors(),
                typography = hainMarkdownTypography(),
                modifier = Modifier.fillMaxWidth(),
                retainState = true,
            )
        }
        if (actions != null) {
            // quiet grey row, evenly spaced; shifted so the first icon lines up with the text
            Row(
                Modifier.padding(top = LocalHainSpacing.current.xs).offset(x = -ICON_INSET.dp),
                horizontalArrangement = Arrangement.spacedBy(ACTION_GAP.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) { actions() }
        }
    }
}

/** Collapsed thinking ("Gedacht für 4 s"); tap to expand. */
@Composable
fun ReasoningBlock(
    title: String,
    reasoning: String,
    modifier: Modifier = Modifier,
    initiallyExpanded: Boolean = false,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(title, style = type.label.copy(color = colors.textMuted))
            BasicText(if (expanded) "  ▾" else "  ▸", style = type.label.copy(color = colors.textMuted))
        }
        AnimatedVisibility(expanded) {
            BasicText(
                reasoning,
                style = type.bodySmall.copy(color = colors.textMuted),
                modifier = Modifier.padding(start = spacing.md, bottom = spacing.sm),
            )
        }
    }
}

/**
 * One quiet grey icon under a message (T-057): copy, share, thumbs, speak, retry, edit.
 * [selected] tints it green (a given thumb); [done] swaps in a check for a moment (copied).
 */
@Suppress("LongParameterList") // all optional states with defaults
@Composable
fun MessageAction(
    kind: MessageActionKind,
    label: String,
    onClick: () -> Unit,
    selected: Boolean = false,
    done: Boolean = false,
    enabled: Boolean = true,
) {
    val colors = LocalHainColors.current
    val icon =
        when {
            done -> HainIcons.check
            else ->
                when (kind) {
                    MessageActionKind.COPY -> HainIcons.copy
                    MessageActionKind.SHARE -> HainIcons.share
                    MessageActionKind.THUMB_UP -> HainIcons.thumbUp
                    MessageActionKind.THUMB_DOWN -> HainIcons.thumbDown
                    MessageActionKind.SPEAK -> HainIcons.volume
                    MessageActionKind.RETRY -> HainIcons.retry
                    MessageActionKind.EDIT -> HainIcons.edit
                }
        }
    HainIconButton(
        icon = icon,
        contentDescription = label,
        onClick = onClick,
        tint = if (selected || done) colors.accent else colors.icon,
        enabled = enabled,
        iconSize = ACTION_ICON.dp,
    )
}

enum class MessageActionKind { COPY, SHARE, THUMB_UP, THUMB_DOWN, SPEAK, RETRY, EDIT }

/** Icon buttons are 40 dp with a centred 18 dp icon: this much empty room on each side. */
private const val ICON_INSET = 11
private const val ACTION_GAP = 2
private const val ACTION_ICON = 18

@Composable
fun hainMarkdownColors(): MarkdownColors {
    val c = LocalHainColors.current
    return remember(c) {
        DefaultMarkdownColors(
            text = c.text,
            codeBackground = c.surface,
            inlineCodeBackground = c.surface,
            dividerColor = c.border,
            tableBackground = c.surface,
        )
    }
}

@Composable
fun hainMarkdownTypography(): MarkdownTypography {
    val t = LocalHainTypography.current
    val c = LocalHainColors.current
    return remember(t, c) {
        val body = t.body.copy(color = c.text)
        DefaultMarkdownTypography(
            h1 = t.display.copy(color = c.text),
            h2 = t.title.copy(color = c.text),
            h3 = t.title.copy(color = c.text),
            h4 = body.copy(fontWeight = FontWeight.SemiBold),
            h5 = body.copy(fontWeight = FontWeight.SemiBold),
            h6 = body.copy(fontWeight = FontWeight.SemiBold),
            text = body,
            code = t.code.copy(color = c.text),
            inlineCode = t.code.copy(color = c.text),
            quote = body.copy(color = c.textMuted),
            paragraph = body,
            ordered = body,
            bullet = body,
            list = body,
            textLink = TextLinkStyles(SpanStyle(color = c.accent, textDecoration = TextDecoration.Underline)),
            table = t.bodySmall.copy(color = c.text),
        )
    }
}
