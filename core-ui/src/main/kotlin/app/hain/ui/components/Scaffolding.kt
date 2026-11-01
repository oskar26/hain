package app.hain.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/** Top bar (DESIGN_SYSTEM.md §5): left action, title in the middle, right action. */
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    leading: @Composable () -> Unit = {},
    trailing: @Composable () -> Unit = {},
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Row(
        modifier.fillMaxWidth().height(56.dp).padding(horizontal = LocalHainSpacing.current.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            BasicText(title, style = type.label.copy(color = colors.text), maxLines = 1)
            if (subtitle != null) {
                BasicText(
                    subtitle,
                    style = type.label.copy(color = colors.textMuted),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing()
    }
}

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
        BasicText(title, style = type.title.copy(color = colors.text, textAlign = TextAlign.Center))
        if (body != null) {
            BasicText(body, style = type.bodySmall.copy(color = colors.textMuted, textAlign = TextAlign.Center))
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
