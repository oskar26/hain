// Drawing geometry in fractions of the canvas width; naming every fraction would hide the shapes.
@file:Suppress("MagicNumber")

package app.hain.feature.onboarding.api

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import app.hain.ui.motion.LocalReduceMotion
import kotlin.math.PI
import kotlin.math.sin

/** The glossy hero object of a tour page (Opal style, docs/design/REFERENCES_3D.md). */
enum class Hero { LOCK, TREE, SEED }

private val GREEN = Color(color = 0xFF16A36B)
private val MINT = Color(color = 0xFF7FD6A4)
private val LEAF = Color(color = 0xFF3FAF77)
private val BARK = Color(color = 0xFF9A6B4F)
private val SUN = Color(color = 0xFFF4D35E)

/** A soft 3D object that floats gently (still with reduce motion). */
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
    Canvas(
        modifier
            .size(size)
            .graphicsLayer {
                translationY = float * this.size.height * FLOAT_AMPLITUDE
                rotationZ = float * FLOAT_TILT
            },
    ) {
        drawShadow(1f - float * SHADOW_BREATH)
        when (hero) {
            Hero.LOCK -> drawLock()
            Hero.TREE -> drawTree()
            Hero.SEED -> drawSeed()
        }
    }
}

private fun DrawScope.drawShadow(scale: Float) {
    val w = size.width * 0.5f * scale
    val h = size.height * 0.06f
    val c = Offset(size.width / 2, size.height * 0.94f)
    drawOval(
        Brush.radialGradient(listOf(Color.Black.copy(alpha = 0.16f), Color.Transparent), c, w / 2),
        topLeft = c - Offset(w / 2, h / 2),
        size = Size(w, h),
    )
}

/** Glossy ball: base colour, darker bottom right, light spot top left. */
private fun DrawScope.glossyBall(
    centre: Offset,
    radius: Float,
    color: Color,
) {
    drawCircle(
        Brush.radialGradient(
            listOf(lighten(color, 0.45f), color, darken(color, 0.25f)),
            center = centre - Offset(radius * 0.35f, radius * 0.4f),
            radius = radius * 1.5f,
        ),
        radius,
        centre,
    )
    drawOval(
        Color.White.copy(alpha = 0.7f),
        topLeft = centre - Offset(radius * 0.55f, radius * 0.6f),
        size = Size(radius * 0.35f, radius * 0.22f),
    )
}

private fun DrawScope.drawLock() {
    val w = size.width
    val bodyTop = w * 0.42f
    val bodySize = Size(w * 0.56f, w * 0.44f)
    val left = (w - bodySize.width) / 2
    drawArc(
        darken(MINT, 0.1f),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(w * 0.32f, w * 0.18f),
        size = Size(w * 0.36f, w * 0.48f),
        style = Stroke(width = w * 0.07f),
    )
    drawRoundRect(
        Brush.linearGradient(
            listOf(lighten(GREEN, 0.4f), GREEN, darken(GREEN, 0.3f)),
            start = Offset(left, bodyTop),
            end = Offset(left + bodySize.width, bodyTop + bodySize.height),
        ),
        topLeft = Offset(left, bodyTop),
        size = bodySize,
        cornerRadius = CornerRadius(w * 0.1f),
    )
    drawOval(
        Color.White.copy(alpha = 0.55f),
        topLeft = Offset(left + w * 0.06f, bodyTop + w * 0.04f),
        size = Size(w * 0.16f, w * 0.06f),
    )
    glossyBall(Offset(w / 2, bodyTop + bodySize.height * 0.5f), w * 0.06f, darken(GREEN, 0.45f))
}

private fun DrawScope.drawTree() {
    val w = size.width
    drawRoundRect(
        Brush.horizontalGradient(listOf(lighten(BARK, 0.25f), BARK, darken(BARK, 0.3f))),
        topLeft = Offset(w * 0.45f, w * 0.52f),
        size = Size(w * 0.1f, w * 0.36f),
        cornerRadius = CornerRadius(w * 0.05f),
    )
    glossyBall(Offset(w * 0.36f, w * 0.48f), w * 0.17f, LEAF)
    glossyBall(Offset(w * 0.64f, w * 0.46f), w * 0.17f, GREEN)
    glossyBall(Offset(w * 0.5f, w * 0.3f), w * 0.21f, MINT)
    glossyBall(Offset(w * 0.78f, w * 0.18f), w * 0.06f, SUN)
}

private fun DrawScope.drawSeed() {
    val w = size.width
    glossyBall(Offset(w * 0.5f, w * 0.58f), w * 0.26f, MINT)
    drawLine(LEAF, Offset(w * 0.5f, w * 0.34f), Offset(w * 0.5f, w * 0.2f), strokeWidth = w * 0.03f)
    glossyBall(Offset(w * 0.6f, w * 0.18f), w * 0.08f, LEAF)
    glossyBall(Offset(w * 0.4f, w * 0.2f), w * 0.06f, GREEN)
}

private fun lighten(
    c: Color,
    f: Float,
) = androidx.compose.ui.graphics
    .lerp(c, Color.White, f)

private fun darken(
    c: Color,
    f: Float,
) = androidx.compose.ui.graphics
    .lerp(c, Color.Black, f)

private const val FLOAT_MS = 3600
private const val FLOAT_AMPLITUDE = 0.03f
private const val FLOAT_TILT = 2f
private const val SHADOW_BREATH = 0.12f
