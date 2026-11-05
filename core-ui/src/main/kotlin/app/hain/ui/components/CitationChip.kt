package app.hain.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainTypography
import com.mikepenz.markdown.model.MarkdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotator
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.findChildOfType
import org.intellij.markdown.ast.getTextInNode

/** T-274: the chip links of [CitationMarks.toLinks] become inline content instead of a text link. */
internal fun citationAnnotator(): MarkdownAnnotator =
    markdownAnnotator { content, child ->
        val number =
            if (child.type == MarkdownElementTypes.INLINE_LINK) {
                child
                    .findChildOfType(MarkdownElementTypes.LINK_DESTINATION)
                    ?.getTextInNode(content)
                    ?.let { CitationMarks.numberOf(it.toString()) }
            } else {
                null
            }
        if (number != null) appendInlineContent(inlineId(number), "[$number]")
        number != null
    }

/**
 * T-274: the sources of an answer for its text: [count] numbers, [onTap] opens source n, [label] is what
 * a screen reader says for chip n ("Quelle 1: Wikipedia").
 */
class AnswerCitations(
    val count: Int,
    val onTap: (Int) -> Unit,
    val label: (Int) -> String,
)

/** One raised chip per source number 1..count. */
internal fun citationInlineContent(citations: AnswerCitations): Map<String, InlineTextContent> =
    (1..citations.count.coerceAtMost(MAX_CHIPS)).associate { n ->
        val digits = n.toString().length
        inlineId(n) to
            InlineTextContent(
                Placeholder(
                    width = (CHIP_EM + DIGIT_EM * (digits - 1)).em,
                    height = PLACEHOLDER_EM.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextTop,
                ),
            ) { CitationChip(n, citations.label(n)) { citations.onTap(n) } }
    }

/** A small round chip in the Hain green with the number, raised like a footnote; tap → the source. */
@Composable
private fun CitationChip(
    number: Int,
    label: String,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    Box(
        Modifier
            .fillMaxSize()
            .padding(start = 2.dp)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                onClick(label) {
                    onClick()
                    true
                }
            },
        contentAlignment = Alignment.TopStart,
    ) {
        Box(
            Modifier
                .fillMaxHeight(CHIP_HEIGHT)
                .clip(CircleShape)
                .background(colors.accentSoft)
                .clickable(onClick = onClick)
                .padding(horizontal = 1.dp),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                number.toString(),
                style =
                    LocalHainTypography.current.label.copy(
                        color = colors.accent,
                        fontSize = NUMBER_SP.sp,
                        lineHeight = NUMBER_SP.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                    ),
                modifier = Modifier.padding(horizontal = 3.dp),
            )
        }
    }
}

private fun inlineId(number: Int) = "hain-source-$number"

private const val MAX_CHIPS = 99
private const val CHIP_EM = 1.05f
private const val DIGIT_EM = 0.45f
private const val PLACEHOLDER_EM = 0.95f
private const val CHIP_HEIGHT = 0.86f
private const val NUMBER_SP = 9.5f
