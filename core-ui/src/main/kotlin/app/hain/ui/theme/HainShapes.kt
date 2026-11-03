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
 * - capsule: composer and buttons
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
)

object HainRadius {
    const val SMALL = 12
    const val MEDIUM = 20
    const val LARGE = 28
}

val StandardHainShapes =
    HainShapes(
        composer = ContinuousCapsule,
        userBubble = ContinuousRoundedRectangle(HainRadius.MEDIUM.dp),
        card = ContinuousRoundedRectangle(HainRadius.MEDIUM.dp),
        chip = ContinuousRoundedRectangle(HainRadius.SMALL.dp),
        button = ContinuousCapsule,
        row = ContinuousRoundedRectangle(HainRadius.SMALL.dp),
        menu = ContinuousRoundedRectangle(HainRadius.MEDIUM.dp),
        sheet = ContinuousRoundedRectangle(HainRadius.LARGE.dp),
    )
