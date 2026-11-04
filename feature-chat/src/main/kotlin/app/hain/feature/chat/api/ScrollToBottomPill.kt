package app.hain.feature.chat.api

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.ui.HainIcons
import app.hain.ui.effects.hainGlass
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainTypography
import dev.chrisbanes.haze.HazeState

/**
 * The floating "↓" over the chat (T-058): frosted glass (the messages blur behind it, [glass] is the
 * list's haze state; solid below Android 12), a hairline edge and a soft shadow. With [newAnswer]
 * it grows into "↓ Neue Antwort" with a green dot. Fades and slides in from below, softly; with
 * "Bewegung reduzieren" it only fades. 48 dp touch target, a light tick on tap.
 */
@Composable
internal fun ScrollToBottomPill(
    visible: Boolean,
    newAnswer: Boolean,
    glass: HazeState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduce = LocalReduceMotion.current
    val haptics = rememberHainHaptics()
    val spec = hainTween<Float>(HainMotion.PAGE_MS, reduce)
    AnimatedVisibility(
        visible,
        modifier = modifier,
        enter =
            if (reduce) {
                fadeIn(spec)
            } else {
                fadeIn(spec) + scaleIn(spec, initialScale = ENTER_SCALE) +
                    slideInVertically(hainTween(HainMotion.PAGE_MS)) { it / 2 }
            },
        exit =
            if (reduce) {
                fadeOut(hainTween(HainMotion.FAST_MS, reduce))
            } else {
                fadeOut(hainTween(HainMotion.FAST_MS)) + scaleOut(hainTween(HainMotion.FAST_MS), ENTER_SCALE) +
                    slideOutVertically(hainTween(HainMotion.FAST_MS)) { it / 2 }
            },
    ) {
        val label =
            stringResource(if (newAnswer) R.string.chat_scroll_down_new else R.string.chat_scroll_down)
        Box(
            Modifier
                .sizeIn(minWidth = TOUCH.dp, minHeight = TOUCH.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClickLabel = label) {
                    haptics.tick()
                    onClick()
                }.semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            PillFace(newAnswer, glass)
        }
    }
}

@Composable
private fun PillFace(
    newAnswer: Boolean,
    glass: HazeState,
) {
    val colors = LocalHainColors.current
    val reduce = LocalReduceMotion.current
    val shape = RoundedCornerShape(percent = 50)
    Row(
        Modifier
            .shadow(SHADOW.dp, shape, clip = false, ambientColor = colors.shadow, spotColor = colors.shadow)
            .clip(shape)
            .hainGlass(glass)
            .border(1.dp, colors.border, shape)
            .height(FACE.dp)
            .animateContentSize(hainTween(HainMotion.PAGE_MS, reduce))
            .padding(horizontal = if (newAnswer) 14.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            HainIcons.chevronRight,
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.text),
            modifier = Modifier.size(ICON.dp).rotate(QUARTER_TURN),
        )
        if (newAnswer) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(DOT.dp).background(colors.accent, CircleShape))
            Spacer(Modifier.width(6.dp))
            BasicText(
                stringResource(R.string.chat_new_answer),
                style = LocalHainTypography.current.label.copy(color = colors.text),
            )
        }
    }
}

private const val TOUCH = 48
private const val FACE = 40
private const val ICON = 20
private const val DOT = 6
private const val SHADOW = 10
private const val QUARTER_TURN = 90f
private const val ENTER_SCALE = 0.9f
