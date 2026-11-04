package app.hain.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hain.ui.R
import app.hain.ui.motion.HainMotionMoment
import app.hain.ui.motion.motionSpec
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface

/**
 * Two preview tiles for the launcher icon (T-103): "Pim im Wald" in 3D and flat. [flat] says which
 * one is picked; a tap on the other one gives a soft select haptic and calls [onSelect].
 * Used in the onboarding and in Settings → Look → "App icon".
 */
@Composable
fun AppIconPicker(
    flat: Boolean,
    label3d: String,
    labelFlat: String,
    onSelect: (flat: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.md),
    ) {
        IconTile(R.drawable.hain_app_icon_3d, label3d, !flat, Modifier.weight(1f)) { onSelect(false) }
        IconTile(R.drawable.hain_app_icon_flat, labelFlat, flat, Modifier.weight(1f)) { onSelect(true) }
    }
}

@Composable
private fun IconTile(
    image: Int,
    label: String,
    on: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val shapes = LocalHainShapes.current
    val spacing = LocalHainSpacing.current
    val haptics = rememberHainHaptics()
    val edge by animateColorAsState(
        if (on) colors.accent else Color.Transparent,
        motionSpec(HainMotionMoment.PILL_FADE),
        label = "icon-tile-edge",
    )
    Column(
        modifier
            .hainSurface(HainLevel.FLAT, shapes.card, fill = if (on) colors.accentSoft else null)
            .border(2.dp, edge, shapes.card)
            .semantics { selected = on }
            .clickable(role = Role.RadioButton) {
                if (!on) haptics.select()
                onClick()
            }.padding(spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Image(
            painterResource(image),
            contentDescription = null,
            modifier = Modifier.size(PREVIEW_DP.dp),
        )
        BasicText(
            label,
            style =
                LocalHainTypography.current.label.copy(
                    color = if (on) colors.text else colors.textMuted,
                    textAlign = TextAlign.Center,
                ),
        )
    }
}

private const val PREVIEW_DP = 72
