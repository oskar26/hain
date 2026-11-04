package app.hain.feature.chat.api

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.ui.HainIcons
import app.hain.ui.components.HainIconButton
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/** Top-bar button for a private chat (T-056); green on a soft green ground while one is open. */
@Composable
internal fun PrivateChatButton(
    active: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val haptics = rememberHainHaptics()
    val state = stringResource(if (active) R.string.chat_private_on else R.string.chat_private_off)
    HainIconButton(
        HainIcons.privateChat,
        stringResource(if (active) R.string.chat_private_end else R.string.chat_private_start),
        {
            haptics.toggle()
            onClick()
        },
        modifier = Modifier.semantics { stateDescription = state },
        tint = if (active) colors.accent else colors.icon,
        background = if (active) colors.accentSoft else androidx.compose.ui.graphics.Color.Transparent,
    )
}

/**
 * The clear mark of a private chat (T-056), right under the top bar: what it means in one line and
 * "Beenden", which deletes the chat at once.
 */
@Composable
internal fun PrivateChatBanner(onEnd: () -> Unit) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    Row(
        Modifier
            .padding(horizontal = spacing.md)
            .fillMaxWidth()
            .background(colors.accentSoft, LocalHainShapes.current.card)
            .padding(start = spacing.md, end = spacing.xs)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            HainIcons.privateChat,
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.accent),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(spacing.sm))
        BasicText(
            stringResource(R.string.chat_private_banner),
            style = type.bodySmall.copy(color = colors.text),
            modifier = Modifier.weight(1f).padding(vertical = spacing.sm),
        )
        BasicText(
            stringResource(R.string.chat_private_end_short),
            style = type.label.copy(color = colors.accent),
            modifier =
                Modifier
                    .heightIn(min = MIN_TOUCH.dp)
                    .clickable(role = Role.Button, onClick = onEnd)
                    .padding(horizontal = spacing.sm, vertical = spacing.md),
        )
    }
}

private const val MIN_TOUCH = 48
