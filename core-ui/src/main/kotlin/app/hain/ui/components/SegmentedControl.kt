package app.hain.ui.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.hain.ui.motion.HainMotionMoment
import app.hain.ui.motion.motionSpec
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/** Row of 2–5 equal options, one picked (T-014): filters and small either-or choices. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val shape = LocalHainShapes.current.button
    val spacing = LocalHainSpacing.current
    val haptics = rememberHainHaptics()
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surface, shape)
            .padding(3.dp),
    ) {
        options.forEachIndexed { index, label ->
            val on = index == selected
            val background by animateColorAsState(
                if (on) colors.bg else colors.surface,
                motionSpec(HainMotionMoment.PILL_FADE),
                label = "segment-bg",
            )
            Box(
                Modifier
                    .weight(1f)
                    .clip(shape)
                    .background(background, shape)
                    .semantics { this.selected = on }
                    .clickable(role = Role.Tab) {
                        if (index != selected) haptics.toggle()
                        onSelect(index)
                    }.heightIn(min = MIN_HEIGHT.dp)
                    .padding(vertical = spacing.xs, horizontal = spacing.xs),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    label,
                    // T-094: centred, and a long label wraps to a second line instead of being cut off
                    style =
                        LocalHainTypography.current.label.copy(
                            color = if (on) colors.text else colors.textMuted,
                            textAlign = TextAlign.Center,
                        ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Preview(name = "Light — segmented")
@Preview(name = "Dark — segmented", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun SegmentedControlPreview() {
    HainTheme {
        var pick by remember { mutableIntStateOf(0) }
        SegmentedControl(listOf("Alle", "Wetter", "Wikipedia"), pick, { pick = it }, Modifier.padding(16.dp))
    }
}

/** 44 dp + the 3 dp frame on each side: every option is a 50 dp touch target. */
private const val MIN_HEIGHT = 44
