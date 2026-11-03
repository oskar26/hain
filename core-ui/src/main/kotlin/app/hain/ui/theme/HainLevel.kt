package app.hain.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The three depth levels of the whole app (docs/design/STYLE_OPTIONS.md). Every surface picks one of
 * these instead of its own shadow or border, so Hain feels like one calm piece.
 * - [FLAT]: soft grey fill, no edge (bubbles, search field, chips at rest)
 * - [RAISED]: a panel ([hainPanel]): hairline in style A, translucent layer in style B
 * - [FLOATING]: menus and popovers: opaque, hairline, one wide and very soft shadow
 */
enum class HainLevel { FLAT, RAISED, FLOATING }

/** Shadow size of each level; pure so it is unit-testable. */
fun HainLevel.shadowDp(): Dp =
    when (this) {
        HainLevel.FLAT -> 0.dp
        HainLevel.RAISED -> RAISED_SHADOW.dp
        HainLevel.FLOATING -> FLOATING_SHADOW.dp
    }

/**
 * Paints a surface of the given [level] in [shape]. [fill] overrides the level's colour
 * (for example the accent-soft selection).
 */
@Composable
fun Modifier.hainSurface(
    level: HainLevel,
    shape: Shape = LocalHainShapes.current.card,
    fill: Color? = null,
): Modifier {
    val c = LocalHainColors.current
    return when (level) {
        HainLevel.FLAT -> clip(shape).background(fill ?: c.surface, shape)
        HainLevel.RAISED -> hainPanel(shape, fill = fill)
        HainLevel.FLOATING -> {
            val dark = isHainDark()
            val opaque =
                when {
                    !dark -> Color.White
                    LocalHainStyle.current == HainStyle.A -> FLOAT_DARK_A
                    else -> FLOAT_DARK_B
                }
            shadow(
                level.shadowDp(),
                shape,
                clip = false,
                ambientColor = c.shadow,
                spotColor = c.shadow,
            ).clip(shape)
                .background(fill ?: opaque, shape)
                .border(HAIRLINE.dp, c.border, shape)
        }
    }
}

private val FLOAT_DARK_A = Color(color = 0xFF141414)
private val FLOAT_DARK_B = Color(color = 0xFF161C19)
private const val HAIRLINE = 1
private const val RAISED_SHADOW = 1
private const val FLOATING_SHADOW = 12
