package app.hain.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Corner tokens, ported verbatim from DESIGN_SYSTEM.md §2.
 * Plain `RoundedCornerShape` for now — T-018 swaps these for Capsule
 * `ContinuousRoundedRectangle` / `ContinuousCapsule` squircles.
 */
data class HainShapes(
    val composer: Shape,
    val userBubble: Shape,
    val card: Shape,
    val chip: Shape,
    val button: Shape,
)

val StandardHainShapes =
    HainShapes(
        composer = RoundedCornerShape(28.dp),
        userBubble = RoundedCornerShape(20.dp),
        card = RoundedCornerShape(16.dp),
        chip = RoundedCornerShape(12.dp),
        button = RoundedCornerShape(12.dp),
    )
