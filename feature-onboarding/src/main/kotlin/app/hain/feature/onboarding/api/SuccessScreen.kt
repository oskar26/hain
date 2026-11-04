package app.hain.feature.onboarding.api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hain.feature.onboarding.R
import app.hain.ui.components.HainButton
import app.hain.ui.effects.Celebration
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/**
 * Download done (2e): Pim hops, confetti, a happy buzz. Since T-010 on the same calm wash as the
 * tour, so the whole start feels like one piece instead of a dark island.
 */
@Composable
internal fun SuccessScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val type = LocalHainTypography.current
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val haptics = rememberHainHaptics()
    LaunchedEffect(Unit) { haptics.toolConfirm() }
    Box(modifier.fillMaxSize().background(tourBackground())) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.weight(1f))
            // T-011: Pim cheers inside a big shiny bubble, two small bubbles float beside him
            Box(contentAlignment = Alignment.Center) {
                GlassBubble(SUCCESS_BUBBLE, GlowHue.MINT) { LivePim(PimState.CELEBRATE, 112.dp) }
                GlassBubble(40.dp, GlowHue.SUN, Modifier.align(Alignment.TopEnd))
                GlassBubble(28.dp, GlowHue.LILAC, Modifier.align(Alignment.BottomStart))
            }
            Spacer(Modifier.height(spacing.xl))
            BasicText(
                stringResource(R.string.onboarding_done),
                style = type.display.copy(color = colors.text, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(spacing.md))
            BasicText(
                stringResource(R.string.onboarding_done_body),
                style = type.body.copy(color = colors.textMuted, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.weight(1f))
            HainButton(stringResource(R.string.onboarding_start), onDone, Modifier.fillMaxWidth())
        }
        Celebration(Modifier.fillMaxSize())
    }
}

private val SUCCESS_BUBBLE = 230.dp
