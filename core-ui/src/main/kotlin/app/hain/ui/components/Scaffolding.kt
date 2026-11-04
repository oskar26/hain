package app.hain.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.ui.HainIcons
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/**
 * Top bar (DESIGN_SYSTEM.md §5): left action, title in the middle, right actions. With [onTitleClick]
 * the title block is a button (T-055 brain picker) with a small chevron next to the subtitle;
 * [titleAccessory] sits in the same Box, e.g. the popup menu that opens below the title.
 *
 * T-230: the title is centred on the **whole bar**, however many icons sit left or right (iOS-like);
 * only when it is too wide for that it moves into the free room between them. [subtitleLeading]
 * sits in front of the subtitle (the online dot). Compact: 52 dp.
 */
@Suppress("LongParameterList", "LongMethod") // all title options have defaults
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
    onTitleClick: (() -> Unit)? = null,
    titleClickLabel: String? = null,
    titleAccessory: @Composable BoxScope.() -> Unit = {},
    subtitleLeading: @Composable () -> Unit = {},
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Layout(
        content = {
            Row(Modifier.layoutId(LEAD), verticalAlignment = Alignment.CenterVertically) { leading() }
            Box(Modifier.layoutId(TITLE), contentAlignment = Alignment.Center) {
                val click =
                    if (onTitleClick != null) {
                        Modifier
                            .clip(TitleShape)
                            .clickable(onClickLabel = titleClickLabel, role = Role.Button, onClick = onTitleClick)
                            .heightIn(min = 48.dp)
                            .padding(horizontal = 10.dp)
                    } else {
                        Modifier
                    }
                Column(
                    click,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // T-085: long titles (a settings page at 200 % font) end in … instead of being cut
                    BasicText(
                        title,
                        style = type.button.copy(color = colors.text, textAlign = TextAlign.Center),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (subtitle != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            subtitleLeading()
                            BasicText(
                                subtitle,
                                style = type.label.copy(color = colors.textMuted),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false),
                            )
                            if (onTitleClick != null) {
                                Image(
                                    HainIcons.chevronRight,
                                    contentDescription = null,
                                    colorFilter = ColorFilter.tint(colors.textMuted),
                                    modifier = Modifier.padding(start = 3.dp).size(11.dp).rotate(QUARTER_TURN),
                                )
                            }
                        }
                    }
                }
                titleAccessory()
            }
            Row(Modifier.layoutId(TRAIL), verticalAlignment = Alignment.CenterVertically) { trailing() }
        },
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = BAR_HEIGHT.dp)
                .padding(horizontal = LocalHainSpacing.current.xs),
    ) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val width = constraints.maxWidth
        val lead = measurables.first { it.layoutId == LEAD }.measure(loose)
        val trail =
            measurables
                .first { it.layoutId == TRAIL }
                .measure(loose.copy(maxWidth = (width - lead.width).coerceAtLeast(0)))
        val free = (width - lead.width - trail.width).coerceAtLeast(0)
        val titlePlaceable = measurables.first { it.layoutId == TITLE }.measure(loose.copy(maxWidth = free))
        val x = centredTitleX(width, lead.width, trail.width, titlePlaceable.width)
        val height = maxOf(constraints.minHeight, lead.height, trail.height, titlePlaceable.height)
        layout(width, height) {
            lead.place(0, (height - lead.height) / 2)
            trail.place(width - trail.width, (height - trail.height) / 2)
            titlePlaceable.place(x, (height - titlePlaceable.height) / 2)
        }
    }
}

/**
 * Where the title of a top bar starts (T-230): centred on the whole [width] when it fits between
 * equally wide sides, otherwise centred in the room left between [lead] and [trail].
 */
fun centredTitleX(
    width: Int,
    lead: Int,
    trail: Int,
    title: Int,
): Int {
    val side = maxOf(lead, trail)
    val symmetric = (width - 2 * side).coerceAtLeast(0)
    return if (title <= symmetric) {
        (width - title) / 2
    } else {
        val free = (width - lead - trail).coerceAtLeast(0)
        lead + ((free - title) / 2).coerceAtLeast(0)
    }
}

private const val LEAD = "lead"
private const val TITLE = "title"
private const val TRAIL = "trail"
private const val BAR_HEIGHT = 52

private val TitleShape =
    androidx.compose.foundation.shape
        .RoundedCornerShape(12.dp)

private const val QUARTER_TURN = 90f

/** Empty state: content (usually Pim), a title and an optional line below. */
@Composable
fun EmptyState(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    art: @Composable () -> Unit = {},
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Column(
        modifier.padding(LocalHainSpacing.current.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.md),
    ) {
        art()
        BasicText(title, style = type.display.copy(color = colors.text, textAlign = TextAlign.Center))
        if (body != null) {
            BasicText(body, style = type.body.copy(color = colors.textMuted, textAlign = TextAlign.Center))
        }
    }
}

/** Thin determinate progress bar in grove green. */
@Composable
fun ProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(PillShape)
            .background(colors.surfaceHigh),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(PillShape)
                .background(colors.accent),
        )
    }
}

private val PillShape =
    androidx.compose.foundation.shape
        .RoundedCornerShape(percent = PILL_PERCENT)

private const val PILL_PERCENT = 50
