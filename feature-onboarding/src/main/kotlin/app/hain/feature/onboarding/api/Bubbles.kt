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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hain.ui.components.Hain3D
import app.hain.ui.components.Hain3DIcon
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.HainStyle
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainStyle
import app.hain.ui.theme.isHainDark
import kotlin.math.PI
import kotlin.math.sin

/**
 * A calm bubble (STYLE_OPTIONS.md, replaces the shiny soap bubble of T-011). Style A: a page-coloured
 * disc with a hairline. Style B: a frosted white disc with a faint mint wash and a light edge.
 * [hue] is no longer drawn. [content] floats inside.
 */
@Suppress("UnusedParameter") // hue stays for source compatibility
@Composable
internal fun GlassBubble(
    size: Dp,
    hue: GlowHue,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val c = LocalHainColors.current
    val a = LocalHainStyle.current == HainStyle.A
    val dark = isHainDark()
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = this.size.minDimension / 2
            if (a) {
                drawCircle(if (dark) c.surface else c.bg, r)
                drawCircle(c.border, r - 0.5.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
            } else {
                drawCircle(
                    Brush.radialGradient(
                        0f to Color.White.copy(alpha = if (dark) 0.10f else 0.85f),
                        1f to c.accentSoft.copy(alpha = if (dark) 0.35f else 0.7f),
                        center = center - Offset(r * 0.3f, r * 0.35f),
                        radius = r * 1.4f,
                    ),
                    r,
                )
                drawCircle(c.glassEdge, r - 0.5.dp.toPx(), style = Stroke(width = 1.dp.toPx()))
            }
        }
        content()
    }
}

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
    val colors = LocalHainColors.current
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
                b.icon?.let {
                    val tint = if (b.icon == Hain3D.TREE) colors.accent else colors.textMuted
                    Hain3DIcon(it, size = d * ICON_SHARE, tint = tint)
                }
            }
        }
    }
}

private const val BOB_MS = 5200
private const val PHASE_STEP = 0.13f
private const val BOB_SHARE = 0.03f
private const val ICON_SHARE = 0.4f
private val CLOUD_HEIGHT = 270.dp
