// Bubble positions are fractions of the cloud; naming each one would hide the picture.
@file:Suppress("MagicNumber")

package app.hain.feature.onboarding.api

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hain.ui.components.Hain3D
import app.hain.ui.components.Hain3DIcon
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.GlowHue
import kotlin.math.PI
import kotlin.math.sin

/**
 * A shiny, see-through 3D bubble (T-011, Wabi reference): faint tint inside, a rainbow shimmer on
 * the rim, a bright light edge top left and a crisp highlight. [content] floats inside.
 */
@Composable
internal fun GlassBubble(
    size: Dp,
    hue: GlowHue,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawBubbleBack(hue) }
        content()
        Canvas(Modifier.fillMaxSize()) { drawBubbleFront(hue) }
    }
}

/** Behind the motif: the tinted inside and a soft shadow-side. */
private fun DrawScope.drawBubbleBack(hue: GlowHue) {
    val r = size.minDimension / 2
    val c = center
    drawCircle(
        Brush.radialGradient(
            0f to Color.White.copy(alpha = 0.10f),
            0.7f to hue.light.copy(alpha = 0.35f),
            1f to hue.core.copy(alpha = 0.32f),
            center = c - Offset(r * 0.25f, r * 0.3f),
            radius = r * 1.25f,
        ),
        r,
        c,
    )
}

/** In front of the motif: rainbow rim, light edge and the specular highlight. */
private fun DrawScope.drawBubbleFront(hue: GlowHue) {
    val r = size.minDimension / 2
    val c = center
    // iridescent rim like a soap bubble
    drawCircle(
        Brush.sweepGradient(
            listOf(
                IRIS_PINK.copy(alpha = 0.55f),
                IRIS_CYAN.copy(alpha = 0.45f),
                IRIS_LIME.copy(alpha = 0.35f),
                hue.core.copy(alpha = 0.45f),
                IRIS_PINK.copy(alpha = 0.55f),
            ),
            c,
        ),
        r - r * 0.04f,
        c,
        style = Stroke(width = r * 0.08f),
    )
    // bright edge top left, fading round to the bottom right
    drawCircle(
        Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.95f), Color.White.copy(alpha = 0f)),
            start = c - Offset(r, r),
            end = c + Offset(r * 0.3f, r * 0.3f),
        ),
        r - 1f,
        c,
        style = Stroke(width = r * 0.05f),
    )
    // specular highlight
    drawOval(
        Color.White.copy(alpha = 0.85f),
        topLeft = c - Offset(r * 0.62f, r * 0.66f),
        size = Size(r * 0.42f, r * 0.24f),
    )
    drawCircle(Color.White.copy(alpha = 0.7f), r * 0.05f, c - Offset(r * 0.18f, r * 0.72f))
}

private val IRIS_PINK = Color(color = 0xFFFF8FC8)
private val IRIS_CYAN = Color(color = 0xFF7FE3FF)
private val IRIS_LIME = Color(color = 0xFFD9FF8A)

/** One bubble in the cloud: where (fractions of the cloud), how big, what floats inside. */
private data class CloudBubble(
    val x: Float,
    val y: Float,
    val size: Float,
    val hue: GlowHue,
    val icon: Hain3D?,
)

private val CLOUD =
    listOf(
        CloudBubble(0.04f, 0.62f, 0.13f, GlowHue.LILAC, null),
        CloudBubble(0.93f, 0.66f, 0.10f, GlowHue.SKY, null),
        CloudBubble(0.50f, 0.04f, 0.09f, GlowHue.SUN, null),
        CloudBubble(0.27f, 0.17f, 0.25f, GlowHue.SUN, Hain3D.WEATHER),
        CloudBubble(0.75f, 0.17f, 0.24f, GlowHue.SKY, Hain3D.BOOK),
        CloudBubble(0.14f, 0.50f, 0.27f, GlowHue.MINT, Hain3D.LEAF),
        CloudBubble(0.87f, 0.48f, 0.25f, GlowHue.ROSE, Hain3D.HEART),
        CloudBubble(0.50f, 0.45f, 0.40f, GlowHue.TEAL, Hain3D.TREE),
    )

/**
 * The welcome cloud (T-011, Wabi reference): shiny bubbles with Hain's motifs — weather, books,
 * a leaf, a heart and the forest in the middle — that bob gently. Still with reduced motion.
 */
@Composable
internal fun BubbleCloud(modifier: Modifier = Modifier) {
    val reduce = LocalReduceMotion.current
    val phase: State<Float> =
        if (reduce) {
            remember { mutableFloatStateOf(0f) }
        } else {
            rememberInfiniteTransition(label = "cloud").animateFloat(
                0f,
                1f,
                infiniteRepeatable(tween(BOB_MS, easing = LinearEasing), RepeatMode.Restart),
                label = "cloud-bob",
            )
        }
    BoxWithConstraints(modifier.fillMaxWidth().height(CLOUD_HEIGHT)) {
        val w = maxWidth
        val h = maxHeight
        CLOUD.forEachIndexed { i, b ->
            val d = w * b.size
            GlassBubble(
                size = d,
                hue = b.hue,
                modifier =
                    Modifier
                        .offset(x = w * b.x - d / 2, y = h * b.y)
                        .graphicsLayer {
                            val a = (phase.value + i * PHASE_STEP) * 2f * PI.toFloat()
                            translationY = sin(a) * d.toPx() * BOB_SHARE
                        },
            ) {
                b.icon?.let { Hain3DIcon(it, size = d * ICON_SHARE) }
            }
        }
    }
}

private const val BOB_MS = 5200
private const val PHASE_STEP = 0.13f
private const val BOB_SHARE = 0.03f
private const val ICON_SHARE = 0.56f
private val CLOUD_HEIGHT = 270.dp
