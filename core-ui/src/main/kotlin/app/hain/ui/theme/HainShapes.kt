package app.hain.ui.theme

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * Corner tokens (T-010, docs/design/DESIGN_LANGUAGE.md): only three radii plus the capsule, all
 * G2-continuous squircles via Capsule (UI_LIBRARIES.md §2).
 * - [HainRadius.SMALL] 12 dp: chips, rows in lists and menus
 * - [HainRadius.MEDIUM] 20 dp: cards, bubbles, menus, tiles
 * - [HainRadius.LARGE] 28 dp: sheets and big hero surfaces
 * - [HainRadius.TILE] 24 dp: glass tiles with inner light (T-011)
 * - capsule: buttons and pills
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
    /** Glass tiles with inner light (T-011): settings groups, tool cards, packs, the composer card. */
    val tile: Shape,
)

object HainRadius {
    const val SMALL = 12
    const val MEDIUM = 20
    const val LARGE = 28

    /** Glass tiles (T-011): strongly rounded, between card and sheet. */
    const val TILE = 24
}

val StandardHainShapes =
    HainShapes(
        composer = ContinuousRoundedRectangle(HainRadius.LARGE.dp),
        userBubble = ContinuousRoundedRectangle(HainRadius.MEDIUM.dp),
        card = ContinuousRoundedRectangle(HainRadius.MEDIUM.dp),
        chip = ContinuousRoundedRectangle(HainRadius.SMALL.dp),
        button = ContinuousCapsule,
        row = ContinuousRoundedRectangle(HainRadius.SMALL.dp),
        menu = ContinuousRoundedRectangle(HainRadius.MEDIUM.dp),
        sheet = ContinuousRoundedRectangle(HainRadius.LARGE.dp),
        tile = ContinuousRoundedRectangle(HainRadius.TILE.dp),
    )
