package app.hain.ui.theme

import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousCapsule
import com.kyant.capsule.ContinuousRoundedRectangle

/**
 * Corner tokens, ported verbatim from DESIGN_SYSTEM.md §2.
 * All shapes are G2-continuous squircles via Capsule (UI_LIBRARIES.md §2);
 * the composer is a full capsule.
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
        composer = ContinuousCapsule,
        userBubble = ContinuousRoundedRectangle(20.dp),
        card = ContinuousRoundedRectangle(16.dp),
        chip = ContinuousRoundedRectangle(12.dp),
        button = ContinuousRoundedRectangle(12.dp),
    )
