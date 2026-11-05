@file:Suppress("MatchingDeclarationName") // the backdrop state and its composables live together

package app.hain.ui.effects

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.isHainDark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

/**
 * T-104 + T-210, style A "Lichtung": popup menus, long-press menus and popovers are **frosted glass**
 * (Oskar 04.11.: "mit Blur meinte ich, dass der Hintergrund der Aktionskarte geblurt ist"). The
 * screen content is a Haze source; the card itself blurs what lies behind it ([frostedGlass]: 24 dp
 * blur, white or dark tint of ~68 %, thin light edge, soft shadow). Behind the card the rest of the
 * screen only gets a very light dimming, so the frosted card stands out; it fades with the menu in
 * [HainMotion.MENU_MS] and is simply there with "reduce motion". Below Android 12 the card is an
 * opaque tinted surface.
 *
 * Wrap the app's content once in [HainMenuBackdrop]; menus call [rememberMenuBackdrop] and use
 * [frostedGlass] (done inside `HainPopupMenu` / `HainPopover`).
 */
class MenuBackdropState internal constructor() {
    internal val haze = HazeState()
    internal var open by mutableIntStateOf(0)

    /** Number of menus that want the backdrop right now. */
    val openMenus: Int get() = open
}

private val LocalMenuBackdrop = staticCompositionLocalOf<MenuBackdropState?> { null }

/**
 * Blur radius inside a frosted card. T-233 (Oskar 04.11. 22:18, "Frosted Glass etwas ausgeprägter"):
 * 32 dp instead of 24 dp, the same as the chat's chrome ([CHROME_BLUR_RADIUS]).
 */
val FROST_BLUR_RADIUS: Dp = CHROME_BLUR_RADIUS

/** White tint over the blur in light mode: frosted, text stays crisp (T-233: 0.66 → 0.58). */
const val FROST_TINT_LIGHT = 0.58f

/** Dark tint over the blur in dark mode (a bit more, dark glass shows busy text sooner; T-233: 0.72 → 0.66). */
const val FROST_TINT_DARK = 0.66f

/** The quiet dimming of the rest of the screen while a card is open, as a share of the theme's `scrim`. */
const val MENU_DIM_SHARE = 0.35f

/**
 * [overlay] draws on top of the dimming, outside the Haze source, in the same window. Only the
 * screenshot tests use it, to show a frosted card where Robolectric can really blur it.
 */
@Composable
fun HainMenuBackdrop(
    overlay: @Composable BoxScope.() -> Unit = {},
    content: @Composable () -> Unit,
) {
    val state = remember { MenuBackdropState() }
    val reduce = LocalReduceMotion.current
    val colors = LocalHainColors.current
    val target = if (state.open > 0) 1f else 0f
    val progress by animateFloatAsState(target, hainTween(HainMotion.MENU_MS, reduce), label = "menu-backdrop")
    CompositionLocalProvider(LocalMenuBackdrop provides state) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().hazeSource(state.haze)) { content() }
            if (progress > 0f) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = progress }
                        .background(colors.scrim.copy(alpha = colors.scrim.alpha * MENU_DIM_SHARE)),
                )
            }
            overlay()
        }
    }
}

/** Haze style inside a frosted card: 24 dp blur, [tint] at 66 % (light) or 72 % (dark), no noise. */
internal fun frostedStyle(
    bg: Color,
    tint: Color,
    dark: Boolean,
): HazeStyle =
    HazeStyle(
        backgroundColor = bg,
        tints = listOf(HazeTint(tint.copy(alpha = if (dark) FROST_TINT_DARK else FROST_TINT_LIGHT))),
        blurRadius = FROST_BLUR_RADIUS,
        noiseFactor = 0f,
        // where Haze cannot blur (software drawing), never show sharp text through the card
        fallbackTint = HazeTint(frostedFallback(tint, dark)),
    )

/** Fill of a frosted card where nothing can be blurred (below Android 12, outside a backdrop). */
internal fun frostedFallback(
    tint: Color,
    dark: Boolean,
): Color = tint.copy(alpha = if (dark) FALLBACK_DARK else FALLBACK_LIGHT)

/**
 * T-210: makes a floating card frosted glass. The screen behind shows through blurred (Haze, API
 * 31+, inside a [HainMenuBackdrop]); a soft shadow lifts it and a thin edge, light at the top, frames
 * it. Below API 31 (and in battery saver, T-233) an opaque tinted surface.
 */
@Composable
fun Modifier.frostedGlass(shape: Shape): Modifier {
    val c = LocalHainColors.current
    val dark = isHainDark()
    val state = LocalMenuBackdrop.current
    val force = LocalForceFrost.current
    val tint = if (dark) c.surface else Color.White
    val edge =
        Brush.verticalGradient(
            listOf(
                if (dark) c.glassEdge.copy(alpha = EDGE_TOP_DARK) else Color.White,
                c.border.copy(alpha = if (dark) EDGE_BOTTOM_DARK else EDGE_BOTTOM_LIGHT),
            ),
        )
    val lifted =
        this
            .shadow(FROST_SHADOW.dp, shape, clip = false, ambientColor = c.shadow, spotColor = c.shadow)
            .clip(shape)
    val glass =
        // T-233: no blur in battery saver either (glassAllowed)
        if (state != null && glassAllowed()) {
            lifted.hazeEffect(state.haze, frostedStyle(c.bg, tint, dark)) { if (force) blurEnabled = true }
        } else {
            lifted.background(frostedFallback(tint, dark), shape)
        }
    return glass.border(EDGE_WIDTH.dp, edge, shape)
}

/**
 * Screenshot hook: `true` asks Haze to blur even where it would not by default. Robolectric draws
 * popup windows in software (Haze then shows the solid fallback), so the screenshot tests put the
 * card into [HainMenuBackdrop]'s overlay, which is in the main window and really blurs.
 */
val LocalForceFrost = staticCompositionLocalOf { false }

/** Asks for the backdrop while [open] is true; does nothing outside a [HainMenuBackdrop]. */
@Composable
fun rememberMenuBackdrop(open: Boolean) {
    val state = LocalMenuBackdrop.current ?: return
    DisposableEffect(state, open) {
        if (open) state.open++
        onDispose { if (open) state.open-- }
    }
}

private const val FROST_SHADOW = 12
private const val EDGE_WIDTH = 1
private const val EDGE_TOP_DARK = 0.22f
private const val EDGE_BOTTOM_DARK = 0.6f
private const val EDGE_BOTTOM_LIGHT = 0.9f
private const val FALLBACK_LIGHT = 1f
private const val FALLBACK_DARK = 1f
