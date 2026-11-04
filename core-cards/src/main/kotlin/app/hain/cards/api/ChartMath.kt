package app.hain.cards.api

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** The pure maths of a card chart (0.1.6.1): a calm axis with round ends, numbers in the phone's language. */
object ChartMath {
    /** The axis from [low] to [high]; it always holds 0, so bars grow from a real baseline. */
    data class Range(
        val low: Double,
        val high: Double,
    ) {
        val span: Double get() = high - low

        /** Where [value] sits between [low] (0) and [high] (1). */
        fun fraction(value: Double): Float = if (span <= 0.0) 0f else ((value - low) / span).toFloat().coerceIn(0f, 1f)
    }

    /** A range with round ends (1, 2, 2.5, 5 × 10ⁿ) around all [values] and 0. */
    fun range(values: List<Double>): Range {
        val top = maxOf(0.0, values.maxOrNull() ?: 0.0)
        val bottom = minOf(0.0, values.minOrNull() ?: 0.0)
        return if (top == 0.0 && bottom == 0.0) {
            Range(0.0, 1.0)
        } else {
            Range(if (bottom < 0.0) -nice(-bottom) else 0.0, if (top > 0.0) nice(top) else 0.0)
        }
    }

    /** The smallest round number ≥ [value] (> 0): 1, 2, 2.5, 5 or 10 times a power of ten. */
    fun nice(value: Double): Double {
        val power = 10.0.pow(floor(log10(value)))
        return STEPS.first { it * power >= value * (1 - EPSILON) } * power
    }

    /** "7", "7,5" (German) or "7.5" (English): at most two decimals, none for whole numbers. */
    fun format(
        value: Double,
        locale: Locale,
    ): String {
        val format = NumberFormat.getNumberInstance(locale)
        format.maximumFractionDigits = if (abs(value - Math.rint(value)) < EPSILON) 0 else 2
        return format.format(value)
    }

    /** "7,5 h": the value and, when there is one, its unit. */
    fun withUnit(
        value: Double,
        unit: String?,
        locale: Locale,
    ): String = listOfNotNull(format(value, locale), unit).joinToString(" ")

    /** Index of the highest value (the first one when several are equal). */
    fun peak(points: List<ChartPoint>): Int = points.indices.maxByOrNull { points[it].value } ?: 0

    /** "Mo 7,5 h, Di 6 h" — every point for TalkBack, in the phone's language. */
    fun spoken(
        points: List<ChartPoint>,
        unit: String?,
        locale: Locale,
    ): String = points.joinToString(", ") { "${it.label} ${withUnit(it.value, unit, locale)}" }

    @Suppress("MagicNumber") // the round steps of an axis
    private val STEPS = listOf(1.0, 2.0, 2.5, 5.0, 10.0)
    private const val EPSILON = 1e-9
}
