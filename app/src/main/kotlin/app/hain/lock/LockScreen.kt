package app.hain.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hain.R
import app.hain.ui.components.HainButton
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/**
 * Covers Hain while it is locked (T-056): a sleeping Pim, one line, "Entsperren". The system prompt
 * opens by itself when the screen appears; Back leaves Hain instead of reaching the chat.
 */
@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    onLeave: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    BackHandler(onBack = onLeave)
    LaunchedEffect(Unit) { onUnlock() }
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            // swallows every touch, so nothing underneath can be reached
            .clickable(remember { MutableInteractionSource() }, indication = null) {}
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(spacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.widthIn(max = MAX_WIDTH.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            LivePim(PimState.SLEEPING, PIM_SIZE.dp, tappable = false)
            Spacer(Modifier.height(spacing.sm))
            BasicText(
                stringResource(R.string.lock_title),
                style = type.title.copy(color = colors.text, textAlign = TextAlign.Center),
                modifier = Modifier.semantics { heading() },
            )
            BasicText(
                stringResource(R.string.lock_hint),
                style = type.body.copy(color = colors.textMuted, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(spacing.sm))
            HainButton(stringResource(R.string.lock_unlock), onUnlock)
        }
    }
}

private const val PIM_SIZE = 88
private const val MAX_WIDTH = 360
