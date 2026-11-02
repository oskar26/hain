package app.hain.ui.pim

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath

/*
 * "Light 3D touch" (docs/design/REFERENCES_3D.md): a soft shade at the bottom,
 * a light spot top left and a small specular dot. Drawn inside the body only,
 * before the eyes, so Pim keeps his flat, friendly face.
 */
private val SHADE_CENTRE = Offset(x = 68f, y = 104f)
private const val SHADE_RADIUS = 52f
private const val SHADE_ALPHA = 0.22f
private val LIGHT_CENTRE = Offset(x = 46f, y = 50f)
private const val LIGHT_RADIUS = 34f
private const val LIGHT_ALPHA = 0.55f
private val SPEC_CENTRE = Offset(x = 44f, y = 48f)
private val SPEC_SIZE = Size(width = 9f, height = 6f)
private const val SPEC_ALPHA = 0.85f
private val SHADE_COLOR = Color(color = 0xFF0B5A3A)

internal fun DrawScope.drawBodyGloss(body: Path) {
    clipPath(body) {
        drawCircle(
            brush =
                Brush.radialGradient(
                    listOf(SHADE_COLOR.copy(alpha = SHADE_ALPHA), Color.Transparent),
                    center = SHADE_CENTRE,
                    radius = SHADE_RADIUS,
                ),
            radius = SHADE_RADIUS,
            center = SHADE_CENTRE,
        )
        drawCircle(
            brush =
                Brush.radialGradient(
                    listOf(Color.White.copy(alpha = LIGHT_ALPHA), Color.Transparent),
                    center = LIGHT_CENTRE,
                    radius = LIGHT_RADIUS,
                ),
            radius = LIGHT_RADIUS,
            center = LIGHT_CENTRE,
        )
        drawOval(
            color = Color.White.copy(alpha = SPEC_ALPHA),
            topLeft = SPEC_CENTRE - Offset(SPEC_SIZE.width / 2, SPEC_SIZE.height / 2),
            size = SPEC_SIZE,
        )
    }
}

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
