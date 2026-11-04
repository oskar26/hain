package app.hain.cards.api

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.cards.R
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.isHainDark
import java.util.Locale

/**
 * A bar or line chart in an answer card (0.1.6.1), Lichtung style: own Compose canvas, no library.
 * Hairline grid (top, middle, baseline), bars or a 2 dp line in Hain green with the highest value
 * full green and the rest soft, values above the points when they fit, labels underneath. The chart
 * grows in once (600 ms, still with "reduce motion"). TalkBack reads one sentence with every value.
 */
@Composable
internal fun CardChart(block: CardBlock.Chart) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val locale = LocalConfiguration.current.locales[0] ?: Locale.getDefault()
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val grow by animateFloatAsState(
        if (shown) 1f else 0f,
        hainTween(HainMotion.PAGE_MS * 2, LocalReduceMotion.current),
        label = "card-chart",
    )
    val points = block.points
    val peak = remember(points) { ChartMath.peak(points) }
    val look =
        ChartLook(
            accent = colors.accent,
            soft = colors.accent.copy(alpha = SOFT_ALPHA),
            fill = colors.accent.copy(alpha = FILL_ALPHA),
            grid = colors.border,
            // the tile under the chart (calmTile), so the dots sit in a clean ring
            dotRing = if (isHainDark()) colors.surface else colors.bg,
            values = type.label.copy(color = colors.textMuted),
        )
    val measurer = rememberTextMeasurer()
    val description = chartDescription(block, peak, locale)
    Column(
        Modifier.fillMaxWidth().clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.xs),
    ) {
        block.title?.let { BasicText(it, style = type.bodySmall.copy(color = colors.text)) }
        Canvas(Modifier.fillMaxWidth().height(CHART_HEIGHT.dp)) {
            val range = ChartMath.range(points.map { it.value })
            val plot = Plot(range, VALUE_SPACE.dp.toPx(), size.height, points.size, size.width)
            grid(plot, look.grid)
            val labels = points.map { ChartMath.format(it.value, locale) }
            if (block.type == ChartType.LINE) line(points, plot, grow, look) else bars(points, plot, grow, peak, look)
            values(points, labels, plot, grow, block.type, peak, measurer, look.values)
        }
        ChartLabels(points)
        block.unit?.let {
            BasicText(stringResource(R.string.card_chart_unit, it), style = type.label.copy(color = colors.textMuted))
        }
    }
}

/** One sentence for TalkBack: kind and title, how many values, every value, the highest one. */
@Composable
private fun chartDescription(
    block: CardBlock.Chart,
    peak: Int,
    locale: Locale,
): String {
    val points = block.points
    val kind = stringResource(if (block.type == ChartType.LINE) R.string.card_chart_line else R.string.card_chart_bar)
    return stringResource(
        R.string.card_chart_description,
        listOfNotNull(block.title, kind).joinToString(", "),
        pluralStringResource(R.plurals.card_chart_values, points.size, points.size),
        ChartMath.spoken(points, block.unit, locale),
        "${points[peak].label} ${ChartMath.withUnit(points[peak].value, block.unit, locale)}",
    )
}

/** The labels under the slots; with many points only every second one. */
@Composable
private fun ChartLabels(points: List<ChartPoint>) {
    val muted = LocalHainColors.current.textMuted
    val style = LocalHainTypography.current.label.copy(color = muted, textAlign = TextAlign.Center)
    val every = if (points.size > DENSE) 2 else 1
    Row(Modifier.fillMaxWidth()) {
        points.forEachIndexed { i, point ->
            BasicText(
                if (i % every == 0) point.label else "",
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Colours and text style of one chart, read once from the theme. */
private class ChartLook(
    val accent: Color,
    val soft: Color,
    val fill: Color,
    val grid: Color,
    val dotRing: Color,
    val values: TextStyle,
)

/** Where values sit: one equal slot per point, [top] kept free for the value labels. */
private class Plot(
    val range: ChartMath.Range,
    val top: Float,
    val height: Float,
    val slots: Int,
    val width: Float,
) {
    val slot: Float get() = width / slots

    fun x(index: Int): Float = slot * (index + HALF)

    fun y(value: Double): Float = top + (height - top) * (1f - range.fraction(value))

    /** [value] while the chart grows in: from the baseline (0) to its place. */
    fun grown(
        value: Double,
        grow: Float,
    ): Float {
        val base = y(0.0)
        return base + (y(value) - base) * grow
    }
}

private fun DrawScope.grid(
    plot: Plot,
    color: Color,
) {
    val hairline = HAIRLINE.dp.toPx()
    listOf(plot.range.high, (plot.range.low + plot.range.high) / 2, 0.0).distinct().forEach { value ->
        val y = plot.y(value)
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = hairline)
    }
}

private fun DrawScope.bars(
    points: List<ChartPoint>,
    plot: Plot,
    grow: Float,
    peak: Int,
    look: ChartLook,
) {
    val width = minOf(plot.slot * BAR_SHARE, BAR_MAX.dp.toPx())
    val radius = CornerRadius(BAR_RADIUS.dp.toPx())
    val base = plot.y(0.0)
    points.forEachIndexed { i, point ->
        val y = plot.grown(point.value, grow)
        val top = minOf(y, base)
        val heightPx = maxOf(kotlin.math.abs(base - y), if (point.value != 0.0) MIN_BAR.dp.toPx() * grow else 0f)
        drawRoundRect(
            color = if (i == peak) look.accent else look.soft,
            topLeft = Offset(plot.x(i) - width / 2, if (y <= base) base - heightPx else top),
            size = Size(width, heightPx),
            cornerRadius = radius,
        )
    }
}

private fun DrawScope.line(
    points: List<ChartPoint>,
    plot: Plot,
    grow: Float,
    look: ChartLook,
) {
    val at = points.mapIndexed { i, point -> Offset(plot.x(i), plot.grown(point.value, grow)) }
    val stroke = Path()
    at.forEachIndexed { i, o -> if (i == 0) stroke.moveTo(o.x, o.y) else stroke.lineTo(o.x, o.y) }
    val base = plot.y(0.0)
    val area =
        Path().apply {
            moveTo(at.first().x, base)
            at.forEach { lineTo(it.x, it.y) }
            lineTo(at.last().x, base)
            close()
        }
    drawPath(area, look.fill)
    drawPath(
        stroke,
        look.accent,
        style = Stroke(width = LINE_WIDTH.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    at.forEach { o ->
        drawCircle(look.dotRing, radius = DOT_RING.dp.toPx(), center = o)
        drawCircle(look.accent, radius = DOT.dp.toPx(), center = o)
    }
}

/** Values above the points, only where they fit their slot (a line chart: peak, first and last at least). */
@Suppress("LongParameterList") // all of it describes one drawing pass
private fun DrawScope.values(
    points: List<ChartPoint>,
    labels: List<String>,
    plot: Plot,
    grow: Float,
    type: ChartType,
    peak: Int,
    measurer: TextMeasurer,
    style: TextStyle,
) {
    val gap = VALUE_GAP.dp.toPx()
    val alpha = grow.coerceIn(0f, 1f)
    val crowded = points.size > DENSE
    points.forEachIndexed { i, point ->
        val keep = !crowded || i == peak || i == 0 || i == points.lastIndex
        if (!keep && type == ChartType.LINE) return@forEachIndexed
        val layout = measurer.measure(labels[i], style, maxLines = 1)
        if (layout.size.width > plot.slot && i != peak) return@forEachIndexed
        val y = minOf(plot.grown(point.value, grow), plot.y(0.0))
        val top = (y - layout.size.height - gap).coerceAtLeast(0f)
        drawText(layout, topLeft = Offset(plot.x(i) - layout.size.width / 2f, top), alpha = alpha)
    }
}

private const val CHART_HEIGHT = 148
private const val VALUE_SPACE = 18
private const val VALUE_GAP = 3
private const val HAIRLINE = 1
private const val BAR_SHARE = 0.56f
private const val BAR_MAX = 28
private const val BAR_RADIUS = 4
private const val MIN_BAR = 2
private const val LINE_WIDTH = 2
private const val DOT = 3.5f
private const val DOT_RING = 5.5f
private const val SOFT_ALPHA = 0.42f
private const val FILL_ALPHA = 0.10f
private const val HALF = 0.5f
private const val DENSE = 8
