package app.hain.feature.onboarding.api

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import app.hain.ui.components.calmIconTint
import app.hain.ui.motion.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.sin
import app.hain.ui.R as UiR

/**
 * The motif of a tour page. Since the style proposals (docs/design/STYLE_OPTIONS.md) it is one calm
 * Lucide line icon instead of a glossy 3D object; it still floats very gently (still with reduce motion).
 */
enum class Hero(
    val line: Int,
) {
    LOCK(UiR.drawable.icl_lock),
    TREE(UiR.drawable.icl_trees),
    SEED(UiR.drawable.icl_sprout),
}

@Composable
internal fun HeroObject(
    hero: Hero,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val reduce = LocalReduceMotion.current
    val float =
        if (reduce) {
            0f
        } else {
            val p by rememberInfiniteTransition(label = "hero").animateFloat(
                0f,
                1f,
                infiniteRepeatable(tween(FLOAT_MS, easing = LinearEasing), RepeatMode.Restart),
                label = "hero-float",
            )
            sin(p * 2f * PI.toFloat())
        }
    Image(
        painterResource(hero.line),
        null,
        modifier
            .size(size * LINE_SHARE)
            .graphicsLayer { translationY = float * this.size.height * FLOAT_AMPLITUDE },
        colorFilter = ColorFilter.tint(calmIconTint()),
    )
}

private const val LINE_SHARE = 0.5f
private const val FLOAT_MS = 4200
private const val FLOAT_AMPLITUDE = 0.02f
