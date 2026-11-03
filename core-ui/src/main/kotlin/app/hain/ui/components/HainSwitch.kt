package app.hain.ui.components

import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.hainSpring
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.LocalHainColors

/**
 * Our own drawn switch (DESIGN_SYSTEM.md §3, T-010): a capsule track that turns Hain green, a
 * white thumb with a soft shadow that glides with the calm spring. Display only — the row around
 * it handles the tap so the whole row is the touch target.
 */
@Composable
fun HainSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val track by animateColorAsState(
        if (checked) colors.accent else colors.surfaceHigh,
        hainTween(HainMotion.FAST_MS),
        label = "switch-track",
    )
    val x by animateDpAsState(if (checked) TRAVEL.dp else 0.dp, hainSpring(), label = "switch-thumb")
    Box(
        modifier
            .semantics {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
            }.size(width = WIDTH.dp, height = HEIGHT.dp)
            .background(track, CircleShape)
            .padding(INSET.dp),
    ) {
        Box(
            Modifier
                .offset(x = x)
                .size(THUMB.dp)
                .shadow(THUMB_SHADOW.dp, CircleShape, ambientColor = colors.shadow, spotColor = colors.shadow)
                .background(Color.White, CircleShape),
        )
    }
}

private const val WIDTH = 44
private const val HEIGHT = 26
private const val INSET = 3
private const val THUMB = 20
private const val TRAVEL = WIDTH - THUMB - 2 * INSET
private const val THUMB_SHADOW = 2

@Preview(name = "Light — switch")
@Preview(name = "Dark — switch", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun HainSwitchPreview() {
    HainTheme {
        Row(Modifier.padding(16.dp)) {
            HainSwitch(checked = true)
            HainSwitch(checked = false, modifier = Modifier.padding(start = 16.dp))
        }
    }
}
