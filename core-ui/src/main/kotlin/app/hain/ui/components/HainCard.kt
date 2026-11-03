package app.hain.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface

/** Raised card (T-014, T-010 level RAISED) for grouped content: network log rows, tool cards, packs. */
@Composable
fun HainCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalHainColors.current
    val shape = LocalHainShapes.current.card
    val spacing = LocalHainSpacing.current
    Column(
        modifier
            .fillMaxWidth()
            .hainSurface(HainLevel.RAISED, shape)
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        if (title != null) {
            BasicText(title, style = LocalHainTypography.current.label.copy(color = colors.textMuted))
        }
        content()
    }
}

@Preview(name = "Light — card")
@Preview(name = "Dark — card", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun HainCardPreview() {
    HainTheme {
        HainCard(Modifier.padding(16.dp), title = "Heute") {
            BasicText("Köln: 9 °C, leichter Regen", style = LocalHainTypography.current.body)
        }
    }
}
