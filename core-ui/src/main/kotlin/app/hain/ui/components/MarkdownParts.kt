@file:Suppress("MatchingDeclarationName")

package app.hain.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hain.ui.HainIcons
import app.hain.ui.theme.HainTypography
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import com.mikepenz.markdown.compose.components.CurrentComponentsBridge
import com.mikepenz.markdown.compose.components.MarkdownComponent
import com.mikepenz.markdown.compose.elements.MarkdownCodeBlock
import com.mikepenz.markdown.compose.elements.MarkdownCodeFence
import com.mikepenz.markdown.model.markdownPadding
import kotlinx.coroutines.delay
import org.intellij.markdown.ast.getTextInNode

/**
 * T-298 (Markdown complete): the words the answer's code blocks need, from the app's strings (core-ui
 * has none of its own). Empty = no label (only in previews).
 */
@Immutable
data class MarkdownLabels(
    /** Label of a code block without a language ("Code"). */
    val code: String = "",
    /** The copy button ("Kopieren"). */
    val copy: String = "",
    /** Shown for a moment after copying ("Kopiert"). */
    val copied: String = "",
    /** Screen reader word for a ticked task ("Erledigt"). */
    val taskDone: String = "",
    /** Screen reader word for an open task ("Offen"). */
    val taskOpen: String = "",
)

val LocalMarkdownLabels = staticCompositionLocalOf { MarkdownLabels() }

/** Headings: H1–H3 a little larger and lighter, H4–H6 the text size, semibold; more air above than below. */
internal fun headingStyles(
    t: HainTypography,
    text: androidx.compose.ui.graphics.Color,
    muted: androidx.compose.ui.graphics.Color,
): List<TextStyle> {
    val title = t.title.copy(color = text)
    val body = t.body.copy(color = text)
    return listOf(
        title.copy(fontSize = title.fontSize * H1_SCALE, lineHeight = title.lineHeight * H1_SCALE),
        title,
        body.copy(
            fontSize = body.fontSize * H3_SCALE,
            lineHeight = body.lineHeight * H3_SCALE,
            fontWeight = FontWeight.SemiBold,
        ),
        body.copy(fontWeight = FontWeight.SemiBold),
        t.bodySmall.copy(color = text, fontWeight = FontWeight.SemiBold),
        t.bodySmall.copy(color = muted, fontWeight = FontWeight.SemiBold),
    )
}

/** Space above a heading of level 1–6 (the first block of an answer gets none, see [headed]). */
internal fun headingSpace(level: Int): Dp = HEADING_SPACE.getOrElse(level - 1) { HEADING_SPACE.last() }.dp

/** Air between blocks, list items and the indent of nested lists. */
@Composable
internal fun hainMarkdownPadding() =
    markdownPadding(
        block = BLOCK_SPACE,
        list = LIST_SPACE.dp,
        listItemTop = ITEM_SPACE.dp,
        listItemBottom = ITEM_SPACE.dp,
        listIndent = LIST_INDENT.dp,
    )

/** T-298: a task list item ("- [x]") as a small rounded box with a tick instead of "[x]" in code font. */
internal val hainCheckbox: MarkdownComponent = { model ->
    val checked = model.node.getTextInNode(model.content).contains("[x]", ignoreCase = true)
    TaskBox(checked)
}

@Composable
internal fun TaskBox(checked: Boolean) {
    val colors = LocalHainColors.current
    val labels = LocalMarkdownLabels.current
    val shape = RoundedCornerShape(TASK_CORNER.dp)
    Box(
        Modifier
            .padding(end = TASK_GAP.dp, top = TASK_TOP.dp)
            .size(TASK_SIZE.dp)
            .clip(shape)
            .background(if (checked) colors.accent else colors.surface)
            .border(HAIRLINE_BORDER.dp, if (checked) colors.accent else colors.textMuted, shape)
            .semantics { contentDescription = if (checked) labels.taskDone else labels.taskOpen },
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Image(HainIcons.check, null, Modifier.size(TASK_TICK.dp), colorFilter = ColorFilter.tint(colors.surface))
        }
    }
}

private fun headed(
    level: Int,
    bridge: MarkdownComponent,
): MarkdownComponent =
    { model ->
        val first = model.node.startOffset == 0
        Column(Modifier.padding(top = if (first) 0.dp else headingSpace(level), bottom = 2.dp)) {
            bridge(model)
        }
    }

/** T-288 + T-298: formulas as formula blocks; other code blocks with language, copy button, sideways scroll. */
internal val hainCodeFence: MarkdownComponent = { model ->
    MarkdownCodeFence(model.content, model.node, model.typography.code) { code, language, style ->
        if (language == MathText.FENCE_LANG) MathBlock(code) else CodeBlock(code, language, style)
    }
}

internal val hainCodeBlock: MarkdownComponent = { model ->
    MarkdownCodeBlock(model.content, model.node, model.typography.code) { code, language, style ->
        CodeBlock(code, language, style)
    }
}

/** The renderer's parts with Hain's headings and code blocks (kept once, see [AssistantBlock]). */
internal fun hainMarkdownComponents() =
    com.mikepenz.markdown.compose.components.markdownComponents(
        codeFence = hainCodeFence,
        codeBlock = hainCodeBlock,
        heading1 = headed(H1, CurrentComponentsBridge.heading1),
        heading2 = headed(H2, CurrentComponentsBridge.heading2),
        heading3 = headed(H3, CurrentComponentsBridge.heading3),
        heading4 = headed(H4, CurrentComponentsBridge.heading4),
        heading5 = headed(H5, CurrentComponentsBridge.heading5),
        heading6 = headed(H6, CurrentComponentsBridge.heading6),
        setextHeading1 = headed(H1, CurrentComponentsBridge.setextHeading1),
        setextHeading2 = headed(H2, CurrentComponentsBridge.setextHeading2),
        checkbox = hainCheckbox,
    )

/**
 * A code block in an answer: a quiet bar with the language and a copy button (48 dp target), then the
 * code in the code font, scrolling sideways instead of wrapping.
 */
@Composable
internal fun CodeBlock(
    code: String,
    language: String?,
    style: TextStyle,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val clipboard = LocalClipboardManager.current
    var copied by remember(code) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_MS)
            copied = false
        }
    }
    val shape = LocalHainShapes.current.card
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = spacing.sm)
            .clip(shape)
            .background(colors.surface),
    ) {
        CodeBar(language, copied, shape) {
            clipboard.setText(AnnotatedString(code))
            copied = true
        }
        Box(Modifier.fillMaxWidth().height(HAIRLINE.dp).background(colors.border))
        BasicText(
            code,
            style = style,
            softWrap = false,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = spacing.md, vertical = spacing.sm),
        )
    }
}

/** The bar above a code block: its language (or "Code") and the copy button. */
@Composable
private fun CodeBar(
    language: String?,
    copied: Boolean,
    shape: Shape,
    onCopy: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val labels = LocalMarkdownLabels.current
    Row(
        Modifier.fillMaxWidth().padding(start = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            language?.takeIf { it.isNotBlank() } ?: labels.code,
            style = type.label.copy(color = colors.textMuted),
            modifier = Modifier.weight(1f),
        )
        Row(
            Modifier
                .sizeIn(minWidth = TOUCH.dp, minHeight = TOUCH.dp)
                .clip(shape)
                .clickable(role = Role.Button, onClick = onCopy)
                .semantics { contentDescription = if (copied) labels.copied else labels.copy }
                .padding(horizontal = spacing.md),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Image(
                if (copied) HainIcons.check else HainIcons.copy,
                null,
                Modifier.size(ICON.dp),
                colorFilter = ColorFilter.tint(if (copied) colors.accent else colors.icon),
            )
            BasicText(
                if (copied) labels.copied else labels.copy,
                style = type.label.copy(color = if (copied) colors.accent else colors.textMuted),
            )
        }
    }
}

/** Space between the blocks of an answer (paragraphs, lists, quotes …). */
internal val BLOCK_SPACE = 6.dp

private const val H1 = 1
private const val H2 = 2
private const val H3 = 3
private const val H4 = 4
private const val H5 = 5
private const val H6 = 6
private const val H1_SPACE = 14
private const val H2_SPACE = 12
private const val H3_SPACE = 10
private const val MINOR_SPACE = 6
private val HEADING_SPACE = intArrayOf(H1_SPACE, H2_SPACE, H3_SPACE, MINOR_SPACE)
private const val LIST_SPACE = 4
private const val ITEM_SPACE = 2
private const val LIST_INDENT = 12
private const val TASK_SIZE = 16
private const val TASK_TICK = 12
private const val TASK_CORNER = 4
private const val TASK_GAP = 8
private const val TASK_TOP = 2
private const val HAIRLINE_BORDER = 1
private const val H1_SCALE = 1.25f
private const val H3_SCALE = 1.1f
private const val COPIED_MS = 1500L
private const val TOUCH = 48
private const val ICON = 16
private const val HAIRLINE = 0.5f
