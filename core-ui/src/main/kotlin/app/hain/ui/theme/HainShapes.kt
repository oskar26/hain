package app.hain.ui.theme

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * Corner tokens (03.11.2026, docs/design/STYLE_OPTIONS.md): small, calm radii, all G2-continuous
 * squircles via Capsule (UI_LIBRARIES.md §2). No candy look: nothing above 20 dp.
 * - style A "Nothing Clean": 8 / 12 / 14 / 16 dp, buttons are 12 dp rectangles
 * - style B "Soft Gradient": 10 / 14 / 16 / 20 dp, buttons are slim capsules
 */
data class HainShapes(
    val composer: Shape,
    val userBubble: Shape,
    val card: Shape,
    val chip: Shape,
    val button: Shape,
    val row: Shape,
    val menu: Shape,
    val sheet: Shape,
    /** Panels (settings groups, tool cards, packs): same calm radius as cards. */
    val tile: Shape,
)

object HainRadius {
    const val SMALL = 8
    const val MEDIUM = 12
    const val LARGE = 16

    /** Panels in style A. */
    const val TILE = 12
}

/** Style A: crisp, almost technical. */
val NothingShapes =
    HainShapes(
        composer = ContinuousRoundedRectangle(14.dp),
        userBubble = ContinuousRoundedRectangle(HainRadius.MEDIUM.dp),
        card = ContinuousRoundedRectangle(HainRadius.MEDIUM.dp),
        chip = ContinuousRoundedRectangle(HainRadius.SMALL.dp),
        button = ContinuousRoundedRectangle(HainRadius.MEDIUM.dp),
        row = ContinuousRoundedRectangle(10.dp),
        menu = ContinuousRoundedRectangle(HainRadius.MEDIUM.dp),
        sheet = ContinuousRoundedRectangle(HainRadius.LARGE.dp),
        tile = ContinuousRoundedRectangle(HainRadius.TILE.dp),
    )

/** Style B: a touch softer, pills only for buttons. */
val SoftShapes =
    HainShapes(
        composer = ContinuousRoundedRectangle(16.dp),
        userBubble = ContinuousRoundedRectangle(16.dp),
        card = ContinuousRoundedRectangle(16.dp),
        chip = ContinuousRoundedRectangle(10.dp),
        button = ContinuousCapsule,
        row = ContinuousRoundedRectangle(12.dp),
        menu = ContinuousRoundedRectangle(14.dp),
        sheet = ContinuousRoundedRectangle(20.dp),
        tile = ContinuousRoundedRectangle(16.dp),
    )

/** Default shapes (style A). */
val StandardHainShapes = NothingShapes

fun hainShapes(style: HainStyle): HainShapes =
    when (style) {
        HainStyle.A -> NothingShapes
        HainStyle.B -> SoftShapes
    }
