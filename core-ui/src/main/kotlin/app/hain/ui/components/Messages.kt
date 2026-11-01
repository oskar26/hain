package app.hain.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import com.mikepenz.markdown.compose.Markdown
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography

/** User message: grey bubble on the right (DESIGN_SYSTEM.md §1). */
@Composable
fun UserBubble(
    text: String,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
        SelectionContainer {
            BasicText(
                text = text,
                style = LocalHainTypography.current.body.copy(color = colors.text),
                modifier =
                    Modifier
                        .widthIn(max = 320.dp)
                        .background(colors.surface, LocalHainShapes.current.userBubble)
                        .padding(horizontal = spacing.lg, vertical = spacing.md),
            )
        }
    }
}

/** Assistant answer: plain markdown text without bubble, actions row below when finished. */
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
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { actions() }
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

/** Small round action under an answer (copy, regenerate, share). */
@Composable
fun MessageAction(
    kind: MessageActionKind,
    label: String,
    onClick: () -> Unit,
) {
    val icon =
        when (kind) {
            MessageActionKind.COPY -> HainIcons.copy
            MessageActionKind.RETRY -> HainIcons.retry
            MessageActionKind.SHARE -> HainIcons.share
        }
    HainIconButton(icon, label, onClick, tint = LocalHainColors.current.textMuted)
}

enum class MessageActionKind { COPY, RETRY, SHARE }

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
