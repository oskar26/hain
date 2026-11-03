package app.hain.ui.pim

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hain.ui.theme.LocalHainColors

/** Pim counts as "dark" below this background luminance (dark eye #0E1513). */
private const val DARK_BG_LUMINANCE = 0.5f

/**
 * Pim v2, drawn with Canvas paths — no bitmaps, no legacy PNG
 * (assets/mascot/legacy_mascot_v1.png is retired).
 *
 * @param state one of the nine base states (static drawing; animations come
 *   with T-163 and are driven by pim_motion.json)
 * @param size rendered edge length; the leaf vein only appears from 64 dp
 */
@Composable
fun Pim(
    state: PimState,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val pose = poseFor(state)
    PimCanvas(pose = { pose }, size = size, modifier = modifier)
}

/**
 * Draws the pose [pose] returns, flat and calm; contact shadow when [shadowLift] is set. [size] is
 * Pim's own size (T-089): the canvas shows the 108-unit frame around him, not the old 144-unit
 * viewBox, so a 30 dp Pim really looks 30 dp. Hops and fx may draw a little outside the canvas.
 * [colors] overrides the palette (the 2048 easter egg makes him glow).
 */
@Composable
internal fun PimCanvas(
    pose: () -> PimPose,
    size: Dp,
    modifier: Modifier = Modifier,
    shadowLift: Float? = null,
    colors: (() -> PimColors)? = null,
) {
    val palette = if (LocalHainColors.current.bg.luminance() < DARK_BG_LUMINANCE) DarkPimColors else LightPimColors
    val showVein = size >= 64.dp
    // [pose] is read while drawing, so an animated Pim only redraws and never recomposes.
    Canvas(modifier = modifier.size(size)) {
        val now = pose()
        val fit = this.size.minDimension / PimGeometry.FRAME_SIZE
        val origin = PimGeometry.FRAME_TOP_LEFT - Offset(PimGeometry.VIEW_BOX_X, PimGeometry.VIEW_BOX_Y)
        scale(fit, fit, Offset.Zero) {
            translate(-origin.x, -origin.y) {
                if (shadowLift != null) drawContactShadow(-now.y + shadowLift, now.sx)
                drawPim(now, colors?.invoke() ?: palette, showVein)
            }
        }
    }
}

@Preview(name = "Light — 4 poses")
@Preview(name = "Dark — 4 poses", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
internal fun PimPosesPreview() {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(PimState.IDLE, PimState.HAPPY, PimState.THINKING, PimState.SLEEPING).forEach { state ->
            Pim(state = state, size = 72.dp)
        }
    }
}

// Glyph path data for the fx accents, viewBox-local around the fx anchor
// (hand-drawn here; the state fx of pim_motion.json select which one shows).
private const val SPARKLE_PATH =
    "M0 -7 C0.5 -3 3 -0.5 7 0 C3 0.5 0.5 3 0 7 " +
        "C-0.5 3 -3 0.5 -7 0 C-3 -0.5 -0.5 -3 0 -7 Z"
private const val HEART_PATH = "M0 5 C-7 0 -5 -6 0 -2.5 C5 -6 7 0 0 5 Z"
private const val DROP_PATH = "M0 -6 C4 -1 4.5 2 0 4.5 C-4.5 2 -4 -1 0 -6 Z"
private const val QUESTION_PATH =
    "M-3.5 -4 C-3.5 -8 3.5 -8 3.5 -4 C3.5 -1.5 0 -2 0 1 " +
        "M-1.3 5 C-1.3 3.9 1.3 3.9 1.3 5 C1.3 6.1 -1.3 6.1 -1.3 5 Z"
private const val BANG_PATH = "M0 -6 L0 1 M-1.3 5 C-1.3 3.9 1.3 3.9 1.3 5 C1.3 6.1 -1.3 6.1 -1.3 5 Z"
private const val ZZZ_PATH =
    "M0 0 L6 0 L0 3 L6 3 " +
        "M5 4.5 L9.5 4.5 L5 7.1 L9.5 7.1 " +
        "M9.5 9 L12.5 9 L9.5 11.1 L12.5 11.1"

internal fun DrawScope.drawFx(
    pose: PimPose,
    colors: PimColors,
) {
    if (pose.fx == PimFx.NONE) return
    val color = colors.leaf.copy(alpha = pose.fxA.coerceIn(0f, 1f))
    withTransform({ translate(PimGeometry.FX_ANCHOR.x + pose.fxX, PimGeometry.FX_ANCHOR.y + pose.fxY) }) {
        when (pose.fx) {
            PimFx.ZZZ -> drawGlyph(ZZZ_PATH, color, filled = false)
            PimFx.BANG -> drawGlyph(BANG_PATH, color, filled = false)
            PimFx.QUESTION -> drawGlyph(QUESTION_PATH, color, filled = false)
            PimFx.SPARKLE -> drawGlyph(SPARKLE_PATH, color, filled = true)
            PimFx.HEART -> drawGlyph(HEART_PATH, color, filled = true)
            PimFx.DROP -> drawGlyph(DROP_PATH, color, filled = true)
            PimFx.NONE -> Unit
        }
    }
}

private fun DrawScope.drawGlyph(
    pathData: String,
    color: Color,
    filled: Boolean,
) {
    val path = svgPathToPathPublic(pathData)
    if (filled) {
        drawPath(path, color)
    } else {
        drawPath(path, color, style = Stroke(width = PimGeometry.STROKE_W, cap = StrokeCap.Round))
    }
}

internal fun DrawScope.drawEye(
    center: Offset,
    pose: PimPose,
    colors: PimColors,
    sizeScale: Float,
) {
    val stroke = Stroke(width = PimGeometry.STROKE_W, cap = StrokeCap.Round)
    when (pose.eyes) {
        PimEyes.PILL, PimEyes.WIDE -> drawPillEye(center, pose, colors, sizeScale)
        PimEyes.HAPPY -> {
            val p =
                Path().apply {
                    moveTo(center.x - EYE_ARC_HALF_W, center.y + EYE_ARC_CORNER_Y)
                    cubicTo(
                        center.x - EYE_ARC_MID_X,
                        center.y + EYE_ARC_TOP_Y,
                        center.x + EYE_ARC_MID_X,
                        center.y + EYE_ARC_TOP_Y,
                        center.x + EYE_ARC_HALF_W,
                        center.y + EYE_ARC_CORNER_Y,
                    )
                }
            drawPath(p, colors.eye, style = stroke)
        }
        PimEyes.CLOSED -> drawClosedEye(center, colors, stroke)
        PimEyes.SQUEEZE -> drawSqueezedEye(center, colors, stroke)
    }
}

internal fun DrawScope.drawPillEye(
    center: Offset,
    pose: PimPose,
    colors: PimColors,
    sizeScale: Float,
) {
    val wide = if (pose.eyes == PimEyes.WIDE) EYE_WIDE_FACTOR else 1f
    val w = PimGeometry.EYE_W * sizeScale * wide
    val h = PimGeometry.EYE_H * pose.eyeH * sizeScale * wide
    drawRoundRect(
        color = colors.eye,
        topLeft = Offset(center.x - w / 2, center.y - h / 2),
        size = Size(w, h),
        cornerRadius = CornerRadius(w / 2, w / 2),
    )
}

internal fun DrawScope.drawClosedEye(
    center: Offset,
    colors: PimColors,
    stroke: Stroke,
) {
    val p =
        Path().apply {
            moveTo(center.x - EYE_ARC_HALF_W_CLOSED, center.y + EYE_CLOSED_TOP_Y)
            cubicTo(
                center.x - EYE_ARC_MID_X_CLOSED,
                center.y + EYE_CLOSED_MID_Y,
                center.x + EYE_ARC_MID_X_CLOSED,
                center.y + EYE_CLOSED_MID_Y,
                center.x + EYE_ARC_HALF_W_CLOSED,
                center.y + EYE_CLOSED_TOP_Y,
            )
        }
    drawPath(p, colors.eye, style = stroke)
}

internal fun DrawScope.drawSqueezedEye(
    center: Offset,
    colors: PimColors,
    stroke: Stroke,
) {
    val leftChevron =
        Path().apply {
            moveTo(center.x - EYE_SQUEEZE_HALF_W, center.y - EYE_SQUEEZE_HALF_H)
            lineTo(center.x - EYE_SQUEEZE_INNER_X, center.y)
            lineTo(center.x - EYE_SQUEEZE_HALF_W, center.y + EYE_SQUEEZE_HALF_H)
        }
    drawPath(leftChevron, colors.eye, style = stroke)
    val rightChevron =
        Path().apply {
            moveTo(center.x + EYE_SQUEEZE_HALF_W, center.y - EYE_SQUEEZE_HALF_H)
            lineTo(center.x + EYE_SQUEEZE_INNER_X, center.y)
            lineTo(center.x + EYE_SQUEEZE_HALF_W, center.y + EYE_SQUEEZE_HALF_H)
        }
    drawPath(rightChevron, colors.eye, style = stroke)
}

// Eye shape geometry, relative to the eye centre (viewBox units, from
// tools/pim_gen.py's rendered shapes).
internal const val EYE_WIDE_FACTOR = 1.35f
internal const val EYE_ARC_HALF_W = 6.5f
internal const val EYE_ARC_MID_X = 2.5f
internal const val EYE_ARC_TOP_Y = -6f
internal const val EYE_ARC_CORNER_Y = 2f
internal const val EYE_ARC_HALF_W_CLOSED = 6f
internal const val EYE_ARC_MID_X_CLOSED = 2f
internal const val EYE_CLOSED_TOP_Y = -1f
internal const val EYE_CLOSED_MID_Y = 2.5f
internal const val EYE_SQUEEZE_HALF_W = 6f
internal const val EYE_SQUEEZE_INNER_X = 1f
internal const val EYE_SQUEEZE_HALF_H = 4f

/** Flat contact shadow under Pim (Lichtung: a quiet grey oval); shrinks while he is in the air. */
internal fun DrawScope.drawContactShadow(
    lift: Float,
    widthScale: Float,
) {
    val fade = (1f - lift * SHADOW_LIFT_FADE).coerceIn(0f, 1f)
    val w = SHADOW_W * widthScale * fade
    val h = SHADOW_H * fade
    translate(-PimGeometry.VIEW_BOX_X, -PimGeometry.VIEW_BOX_Y) {
        drawOval(
            color = Color.Black.copy(alpha = SHADOW_ALPHA * fade),
            topLeft = SHADOW_CENTRE - Offset(w / 2, h / 2),
            size = Size(w, h),
        )
    }
}

private val SHADOW_CENTRE = Offset(x = 60f, y = 109f)
private const val SHADOW_W = 44f
private const val SHADOW_H = 5f
private const val SHADOW_ALPHA = 0.07f
private const val SHADOW_LIFT_FADE = 0.03f

/** A still frame of [pose] for the screenshot frame tests. */
@Composable
internal fun PimCanvasForTest(
    pose: PimPose,
    size: Dp,
) {
    PimCanvas(pose = { pose }, size = size, shadowLift = 0f)
}
