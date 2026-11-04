package app.hain.ui.effects

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.LocalHainColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * T-104, style A "Lichtung": while a ⋯ menu, a long-press menu or a popover is open, the screen
 * behind it gets a soft, quiet blur (Haze, Android 12+) with a light page tint; below API 31 a
 * half-transparent dimming instead. Fades in and out with the menu in [HainMotion.MENU_MS]; with
 * "reduce motion" the blur is simply there or not (no animation).
 *
 * Wrap the app's content once in [HainMenuBackdrop]; menus call [rememberMenuBackdrop] (done inside
 * `HainPopupMenu` / `HainPopover`).
 */
class MenuBackdropState internal constructor() {
    internal val haze = HazeState()
    internal var open by mutableIntStateOf(0)

    /** Number of menus that want the backdrop right now. */
    val openMenus: Int get() = open
}

private val LocalMenuBackdrop = staticCompositionLocalOf<MenuBackdropState?> { null }

/** Blur radius of the backdrop (DESIGN_LANGUAGE.md: menus, 12–16 dp, calm). */
val MENU_BLUR_RADIUS: Dp = 14.dp

/** Page tint over the blur: just enough to calm busy text, never milky glass. */
const val MENU_BLUR_TINT = 0.16f

/** Fallback dimming below API 31, as a share of the theme's `scrim`. */
const val MENU_DIM_SHARE = 0.75f

@Composable
fun HainMenuBackdrop(content: @Composable () -> Unit) {
    val state = remember { MenuBackdropState() }
    val reduce = LocalReduceMotion.current
    val colors = LocalHainColors.current
    val target = if (state.open > 0) 1f else 0f
    val progress by animateFloatAsState(target, hainTween(HainMotion.MENU_MS, reduce), label = "menu-backdrop")
    CompositionLocalProvider(LocalMenuBackdrop provides state) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().hazeSource(state.haze)) { content() }
            if (progress > 0f) {
                if (Build.VERSION.SDK_INT >= BLUR_MIN_SDK) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = progress }
                            .hazeEffect(state.haze, menuBackdropStyle(colors.bg)),
                    )
                } else {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = progress }
                            .background(colors.scrim.copy(alpha = colors.scrim.alpha * MENU_DIM_SHARE)),
                    )
                }
            }
        }
    }
}

/** The haze style of the menu backdrop: 14 dp blur, 16 % page tint. */
internal fun menuBackdropStyle(bg: Color): HazeStyle =
    HazeStyle(
        backgroundColor = bg,
        tints = listOf(HazeTint(bg.copy(alpha = MENU_BLUR_TINT))),
        blurRadius = MENU_BLUR_RADIUS,
        noiseFactor = 0f,
    )

/** Asks for the backdrop while [open] is true; does nothing outside a [HainMenuBackdrop]. */
@Composable
fun rememberMenuBackdrop(open: Boolean) {
    val state = LocalMenuBackdrop.current ?: return
    DisposableEffect(state, open) {
        if (open) state.open++
        onDispose { if (open) state.open-- }
    }
}

// Android 12 is the first release with RenderEffect-based blur.
private const val BLUR_MIN_SDK = 31
