package app.hain.ui.pim

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath

/*
 * Glossy 3D Pim (T-011, Oskar's reference 7): the body is no longer one flat mint but a soft jelly —
 * pale and bright at the top left, saturated green at the bottom right, a warm inner glow near the
 * belly, a thin light rim and a crisp specular spot. Light always comes from the top left.
 */
private val LIGHT_CENTRE = Offset(x = 46f, y = 48f)
private const val BODY_RADIUS = 78f
private val BODY_PALE = Color(color = 0xFFDDF9EA)
private val BODY_MID = Color(color = 0xFF86DDAC)
private val BODY_DEEP = Color(color = 0xFF27A56C)
private const val MID_STOP = 0.42f
private val SOFT_PALE = Color(color = 0xFFB8EBCF)
private val SOFT_DEEP = Color(color = 0xFF4DBE88)
private val GLOW_CENTRE = Offset(x = 62f, y = 92f)
private const val GLOW_RADIUS = 30f
private const val GLOW_ALPHA = 0.38f
private const val RIM_ALPHA = 0.35f
private const val RIM_WIDTH = 2f
private val SPEC_CENTRE = Offset(x = 43f, y = 47f)
private val SPEC_SIZE = Size(width = 12f, height = 7f)
private const val SPEC_ALPHA = 0.9f

/** Paints the glossy body (replaces the flat fill when Pim is big enough to show it). */
internal fun DrawScope.drawGlossyBody(
    body: Path,
    soft: Boolean = false,
) {
    if (soft) {
        // style B (STYLE_OPTIONS.md): a calm matte gradient, no jelly glow, no spot, no rim
        drawPath(
            body,
            Brush.radialGradient(
                0f to SOFT_PALE,
                1f to SOFT_DEEP,
                center = LIGHT_CENTRE,
                radius = BODY_RADIUS,
            ),
        )
        return
    }
    drawPath(
        body,
        Brush.radialGradient(
            0f to BODY_PALE,
            MID_STOP to BODY_MID,
            1f to BODY_DEEP,
            center = LIGHT_CENTRE,
            radius = BODY_RADIUS,
        ),
    )
    clipPath(body) {
        // inner glow: light scattered inside the jelly, strongest just above the bottom
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White.copy(alpha = GLOW_ALPHA), Color.Transparent),
                center = GLOW_CENTRE,
                radius = GLOW_RADIUS,
            ),
            radius = GLOW_RADIUS,
            center = GLOW_CENTRE,
        )
        drawOval(
            color = Color.White.copy(alpha = SPEC_ALPHA),
            topLeft = SPEC_CENTRE - Offset(SPEC_SIZE.width / 2, SPEC_SIZE.height / 2),
            size = SPEC_SIZE,
        )
    }
    // thin light rim along the edge
    drawPath(body, Color.White.copy(alpha = RIM_ALPHA), style = Stroke(width = RIM_WIDTH))
}

/** Glossy eyes are white ovals with a soft green shadow (reference 7). */
internal val GLOSSY_EYE = Color.White
internal val GLOSSY_EYE_SHADOW = Color(color = 0x33125C3B)
internal val GLOSSY_EYE_OFFSET = Offset(x = 0.6f, y = 1.4f)
internal const val GLOSSY_EYE_SCALE = 1.22f

/** Soft contact shadow under Pim; shrinks while he is in the air. */
private val SHADOW_CENTRE = Offset(x = 60f, y = 110f)
private const val SHADOW_W = 50f
private const val SHADOW_H = 7f
private const val SHADOW_ALPHA = 0.18f
private const val SHADOW_LIFT_FADE = 0.03f

internal fun DrawScope.drawContactShadow(
    lift: Float,
    widthScale: Float,
) {
    val fade = (1f - lift * SHADOW_LIFT_FADE).coerceIn(0f, 1f)
    val w = SHADOW_W * widthScale * fade
    val h = SHADOW_H * fade
    translateViewBox {
        drawOval(
            brush =
                Brush.radialGradient(
                    listOf(Color.Black.copy(alpha = SHADOW_ALPHA * fade), Color.Transparent),
                    center = SHADOW_CENTRE,
                    radius = w / 2,
                ),
            topLeft = SHADOW_CENTRE - Offset(w / 2, h / 2),
            size = Size(w, h),
        )
    }
}

private inline fun DrawScope.translateViewBox(block: DrawScope.() -> Unit) {
    drawContext.transform.translate(-PimGeometry.VIEW_BOX_X, -PimGeometry.VIEW_BOX_Y)
    block()
    drawContext.transform.translate(PimGeometry.VIEW_BOX_X, PimGeometry.VIEW_BOX_Y)
}
