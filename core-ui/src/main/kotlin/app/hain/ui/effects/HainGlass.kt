package app.hain.ui.effects

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.hain.ui.theme.LocalHainColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeEffect

// Android 12 is the first release with RenderEffect-based blur.
private const val GLASS_MIN_SDK = 31

/**
 * Frosted glass over scrolled content (UI_LIBRARIES.md §3, "Composer over content"):
 * 24 dp blur with a 70 % background tint on Android 12+; solid surface fallback
 * below API 31 (DESIGN_SYSTEM.md §1: never blur on low-end devices).
 *
 * Mark the content that should shine through with `Modifier.hainGlassSource(state)`.
 */

@Composable
fun Modifier.hainGlass(state: HazeState): Modifier {
    val colors = LocalHainColors.current
    return if (Build.VERSION.SDK_INT >= GLASS_MIN_SDK) {
        hazeEffect(state, hainGlassStyle(bgColor = colors.bg))
    } else {
        background(colors.surface)
    }
}

/** The haze style behind [hainGlass]: 24 dp blur, 70 % background tint. */
internal fun hainGlassStyle(bgColor: Color): HazeStyle =
    HazeStyle(
        backgroundColor = bgColor,
        tints = listOf(HazeTint(bgColor.copy(alpha = 0.7f))),
        blurRadius = 24.dp,
    )

/** Remember a glass state and mark the scrolling content as its source. */
@Composable
fun rememberHainGlassState(): HazeState = remember { HazeState() }

fun Modifier.hainGlassSource(state: HazeState): Modifier = haze(state)
