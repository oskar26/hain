@file:Suppress("MatchingDeclarationName") // the glass edge enum and the chrome glass helpers belong together

package app.hain.ui.effects

import android.os.Build
import android.os.PowerManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.isHainDark
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

/**
 * T-230/T-232/T-233 (Oskar 04.11. 22:18): the app's **chrome** is frosted glass over the content that
 * scrolls below it — the chat's top bar, the message box, the side panel. The content is a Haze
 * source ([hainGlassSource]); the chrome blurs it ([CHROME_BLUR_RADIUS]) under a light page-coloured
 * tint, so you see the chat move underneath, softly, while text on the glass stays crisp.
 *
 * Calm and grown-up (Lichtung): no colour, no noise, a hairline where glass meets content.
 * Below Android 12 and in battery saver ([glassAllowed]) the chrome is an almost opaque page colour.
 */
enum class GlassEdge { NONE, TOP, BOTTOM, ALL }

/** Blur under the chrome: a bit stronger than the menus had before (T-233). */
val CHROME_BLUR_RADIUS: Dp = 32.dp

/** Page-coloured tint over the blur: more see-through than before, text stays readable. */
const val CHROME_TINT_LIGHT = 0.62f

/** Dark glass needs a little more tint: busy text shows through dark glass sooner. */
const val CHROME_TINT_DARK = 0.7f

/** Fill where nothing is blurred (Android 11 and older, battery saver). */
const val CHROME_FALLBACK = 0.96f

/**
 * Real blur only where it is cheap and possible: Android 12+ (RenderEffect) and not in battery
 * saver. Screenshot tests force it on ([LocalForceFrost]).
 */
@Composable
fun glassAllowed(): Boolean {
    val context = LocalContext.current
    val power = remember(context) { context.getSystemService(PowerManager::class.java) }
    return LocalForceFrost.current || (Build.VERSION.SDK_INT >= BLUR_MIN_SDK && power?.isPowerSaveMode != true)
}

/** The Haze style of the chrome over the page colour [bg]. */
fun chromeStyle(
    bg: Color,
    dark: Boolean,
): HazeStyle =
    HazeStyle(
        backgroundColor = bg,
        tints = listOf(HazeTint(bg.copy(alpha = if (dark) CHROME_TINT_DARK else CHROME_TINT_LIGHT))),
        blurRadius = CHROME_BLUR_RADIUS,
        noiseFactor = 0f,
        fallbackTint = HazeTint(bg.copy(alpha = CHROME_FALLBACK)),
    )

/**
 * Frosted chrome: blurs what [state] marks as its source (behind this element) in [shape], with a
 * hairline on [edge]. `null` state or no blur → [CHROME_FALLBACK] page colour.
 */
@Composable
fun Modifier.chromeGlass(
    state: HazeState?,
    shape: Shape = RectangleShape,
    edge: GlassEdge = GlassEdge.NONE,
): Modifier {
    val c = LocalHainColors.current
    val dark = isHainDark()
    val force = LocalForceFrost.current
    val clipped = this.clip(shape)
    val glass =
        if (state != null && glassAllowed()) {
            clipped.hazeEffect(state, chromeStyle(c.bg, dark)) { if (force) blurEnabled = true }
        } else {
            clipped.background(c.bg.copy(alpha = CHROME_FALLBACK), shape)
        }
    val line = c.border.copy(alpha = if (dark) HAIRLINE_DARK else HAIRLINE_LIGHT)
    return when (edge) {
        GlassEdge.NONE -> glass
        GlassEdge.ALL -> glass.border(1.dp, line, shape)
        GlassEdge.TOP, GlassEdge.BOTTOM ->
            glass.drawWithContent {
                drawContent()
                val y = if (edge == GlassEdge.TOP) 0f else size.height - 1f
                drawLine(line, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            }
    }
}

private const val BLUR_MIN_SDK = 31
private const val HAIRLINE_LIGHT = 0.9f
private const val HAIRLINE_DARK = 0.8f
