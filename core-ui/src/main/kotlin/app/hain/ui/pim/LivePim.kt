package app.hain.ui.pim

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.rememberHainHaptics

/**
 * Pim, alive (T-089, docs/design/PIM_MOTION.md): breathes, blinks, glances around, waves, hops,
 * nods, yawns, sends a little heart, dozes off with "Zz" after a long wait. [state] picks his mood:
 * the leaf wiggles while he thinks, he jumps for joy when an answer is done (HAPPY) and gets a
 * fright on an error. A tap gets a different reaction each time (squash & stretch, giggle, boing,
 * leaf twirl, wink, heart) when [tappable]. Flat and calm in the Lichtung style.
 *
 * Everything moves in the draw phase from one frame clock: no recomposition per frame, nothing
 * heavy on the main thread. With "reduce motion" he stays still in his state's pose.
 * [overlay] lets a parent bend the pose (dragging, the easter egg) without its own clock;
 * [colors] does the same for the palette.
 */
@Suppress("LongParameterList") // optional hooks with defaults
@Composable
fun LivePim(
    state: PimState,
    size: Dp,
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null,
    tappable: Boolean = true,
    overlay: ((PimPose) -> PimPose)? = null,
    colors: (() -> PimColors)? = null,
    driver: PimDriver = rememberPimDriver(),
) {
    val reduce = LocalReduceMotion.current
    val haptics = rememberHainHaptics()
    LaunchedEffect(state, reduce) {
        if (!reduce) runClock(driver, state)
    }
    val tap =
        if (tappable || onTap != null) {
            Modifier.pointerInput(onTap, reduce) {
                detectTapGestures {
                    haptics.tick()
                    if (!reduce) driver.tap()
                    onTap?.invoke()
                }
            }
        } else {
            Modifier
        }
    PimCanvas(
        pose = {
            val base = if (reduce) PimMoves.basePose(state) else driver.director.pose(driver.now)
            overlay?.invoke(base) ?: base
        },
        size = size,
        modifier = modifier.then(tap),
        shadowLift = 0f,
        colors = colors,
    )
}
