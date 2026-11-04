package app.hain.cards.api

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.cards.R
import app.hain.ui.components.Hain3D
import app.hain.ui.components.Hain3DIcon
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.glassTile

/**
 * WikiCard (T-065, 3b-2): the Wikipedia article Pim read, as a glass tile under the answer — title,
 * three lines of the summary and "Auf Wikipedia lesen". The page opens only when you tap it.
 */
@Composable
fun WikiCard(
    data: WikiCardData,
    onOpen: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    val url = data.url
    Column(
        modifier
            .padding(top = spacing.sm)
            .fillMaxWidth()
            .glassTile(GlowHue.LILAC, elevation = 6.dp, lightAt = Offset(x = 0.08f, y = 0.3f))
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Hain3DIcon(Hain3D.BOOK, size = 36.dp)
            Spacer(Modifier.width(spacing.sm))
            BasicText(
                data.title,
                style = type.body.copy(color = colors.text),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
        }
        if (data.summary.isNotBlank()) {
            BasicText(
                data.summary,
                style = type.bodySmall.copy(color = colors.textMuted),
                maxLines = SUMMARY_LINES,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (url != null) {
            BasicText(
                stringResource(R.string.cards_wiki_open),
                style = type.label.copy(color = colors.accent),
                modifier =
                    Modifier
                        .clickable(role = Role.Button) { onOpen(url) }
                        .padding(vertical = spacing.xs),
            )
        }
    }
}

private const val SUMMARY_LINES = 3
