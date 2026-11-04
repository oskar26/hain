package app.hain.ui.effects

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import app.hain.ui.theme.LocalHainColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeEffect

/**
 * Frosted glass over scrolled content (UI_LIBRARIES.md §3, "Composer over content"):
 * since T-233 the chrome's 32 dp blur with a 62 % background tint on Android 12+; solid surface
 * fallback below API 31 and in battery saver (DESIGN_SYSTEM.md §1: never blur on low-end devices).
 *
 * Mark the content that should shine through with `Modifier.hainGlassSource(state)`.
 */

@Composable
fun Modifier.hainGlass(state: HazeState): Modifier {
    val colors = LocalHainColors.current
    val force = LocalForceFrost.current
    // T-233: the same, a bit stronger glass as the chat's chrome; none in battery saver
    return if (glassAllowed()) {
        hazeEffect(state, hainGlassStyle(bgColor = colors.bg)) { if (force) blurEnabled = true }
    } else {
        background(colors.surface)
    }
}

/** The haze style behind [hainGlass] (T-233): the chrome's 32 dp blur and light page tint. */
internal fun hainGlassStyle(bgColor: Color): HazeStyle =
    HazeStyle(
        backgroundColor = bgColor,
        tints = listOf(HazeTint(bgColor.copy(alpha = CHROME_TINT_LIGHT))),
        blurRadius = CHROME_BLUR_RADIUS,
    )

/** Remember a glass state and mark the scrolling content as its source. */
@Composable
fun rememberHainGlassState(): HazeState = remember { HazeState() }

fun Modifier.hainGlassSource(state: HazeState): Modifier = haze(state)
