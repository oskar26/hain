@file:Suppress("MagicNumber") // the unit table is numbers by nature

package app.hain.tools.internal.calc

/** Kinds of units; only units of the same kind convert into each other. */
enum class Dimension { LENGTH, MASS, VOLUME, SPEED, TIME, DATA, AREA, TEMPERATURE }

/** A unit with its factor to the base unit of its [dimension] (m, kg, l, m/s, s, byte, m²). */
data class UnitDef(
    val symbol: String,
    val dimension: Dimension,
    val factor: Double,
)

/** Result of a unit conversion. */
data class Conversion(
    val value: Double,
    val from: UnitDef,
    val result: Double,
    val to: UnitDef,
)

/**
 * Unit table and "5 km in m" conversions (T-063, TOOLS.md §5). Currency is not in v1.
 * Temperature (°C, °F, K) is converted with offsets, all other kinds with factors.
 */
object Units {
    private val table: Map<String, UnitDef> =
        buildMap {
            fun add(
                dimension: Dimension,
                factor: Double,
                vararg names: String,
            ) {
                val def = UnitDef(names.first(), dimension, factor)
                names.forEach { put(it.lowercase(), def) }
            }
            add(Dimension.LENGTH, 0.001, "mm", "millimeter", "millimetre")
            add(Dimension.LENGTH, 0.01, "cm", "zentimeter", "centimeter", "centimetre")
            add(Dimension.LENGTH, 1.0, "m", "meter", "metre", "meters", "metres")
            add(Dimension.LENGTH, 1000.0, "km", "kilometer", "kilometre", "kilometers", "kilometres")
            add(Dimension.LENGTH, 0.0254, "in", "inch", "inches", "zoll")
            add(Dimension.LENGTH, 0.3048, "ft", "foot", "feet", "fuß", "fuss")
            add(Dimension.LENGTH, 0.9144, "yd", "yard", "yards")
            add(Dimension.LENGTH, 1609.344, "mi", "mile", "miles", "meile", "meilen")
            add(Dimension.MASS, 1e-6, "mg", "milligramm", "milligram")
            add(Dimension.MASS, 0.001, "g", "gramm", "gram", "grams")
            add(Dimension.MASS, 1.0, "kg", "kilogramm", "kilogram", "kilo", "kilos")
            add(Dimension.MASS, 1000.0, "t", "tonne", "tonnen", "tonnes")
            add(Dimension.MASS, 0.028349523125, "oz", "ounce", "ounces", "unze")
            add(Dimension.MASS, 0.45359237, "lb", "lbs", "pound", "pounds", "pfund")
            add(Dimension.VOLUME, 0.001, "ml", "milliliter", "millilitre")
            add(Dimension.VOLUME, 0.01, "cl", "zentiliter", "centiliter")
            add(Dimension.VOLUME, 0.1, "dl", "deziliter", "deciliter")
            add(Dimension.VOLUME, 1.0, "l", "liter", "litre", "liters", "litres")
            add(Dimension.VOLUME, 3.785411784, "gal", "gallon", "gallons", "gallone", "gallonen")
            add(Dimension.VOLUME, 0.2365882365, "cup", "cups", "tasse", "tassen")
            add(Dimension.SPEED, 1.0, "m/s")
            add(Dimension.SPEED, 1 / 3.6, "km/h", "kmh", "kph")
            add(Dimension.SPEED, 0.44704, "mph")
            add(Dimension.SPEED, 0.514444, "kn", "knot", "knots", "knoten")
            add(Dimension.TIME, 1.0, "s", "sec", "sek", "sekunde", "sekunden", "second", "seconds")
            add(Dimension.TIME, 60.0, "min", "minute", "minuten", "minutes")
            add(Dimension.TIME, 3600.0, "h", "std", "stunde", "stunden", "hour", "hours")
            add(Dimension.TIME, 86400.0, "d", "tag", "tage", "tagen", "day", "days")
            add(Dimension.TIME, 604800.0, "woche", "wochen", "week", "weeks")
            add(Dimension.DATA, 1.0, "B", "byte", "bytes")
            add(Dimension.DATA, 1e3, "KB", "kilobyte")
            add(Dimension.DATA, 1e6, "MB", "megabyte")
            add(Dimension.DATA, 1e9, "GB", "gigabyte")
            add(Dimension.DATA, 1e12, "TB", "terabyte")
            add(Dimension.DATA, 1024.0, "KiB")
            add(Dimension.DATA, 1024.0 * 1024, "MiB")
            add(Dimension.DATA, 1024.0 * 1024 * 1024, "GiB")
            add(Dimension.AREA, 1.0, "m²", "m2", "qm", "quadratmeter")
            add(Dimension.AREA, 1e6, "km²", "km2", "quadratkilometer")
            add(Dimension.AREA, 1e4, "ha", "hektar", "hectare", "hectares")
            add(Dimension.TEMPERATURE, 1.0, "°C", "c", "celsius", "grad")
            add(Dimension.TEMPERATURE, 1.0, "°F", "f", "fahrenheit")
            add(Dimension.TEMPERATURE, 1.0, "K", "kelvin")
        }

    fun find(name: String): UnitDef? {
        val clean =
            name
                .trim()
                .removePrefix("°")
                .removePrefix("grad ")
                .trim()
        return table[name.trim().lowercase()] ?: table[clean.lowercase()]
    }

    private val CONNECTOR = Regex("\\s+(?:in|into|to|nach|zu|als|->|=|→)\\s+", RegexOption.IGNORE_CASE)
    private val GLUED = Regex("^(.*[\\d)])\\s*([°a-zA-Zäöüß][^\\d()]*)$")
    private val SPACES = Regex("\\s+")

    /** "5 km in m", "100 °F to °C", "3,5 kg in lb"; `null` when [text] is no conversion. */
    @Suppress("ReturnCount") // one early exit per part that does not fit
    fun parse(text: String): Conversion? {
        val parts = text.trim().removeSuffix("?").split(CONNECTOR)
        if (parts.size != 2) return null
        val target = find(parts[1]) ?: return null
        val left = parts[0].trim()
        val words = left.split(SPACES)
        val lastWord = words.last().takeIf { words.size >= 2 }?.let { find(it) }
        val (source, amountText) =
            if (lastWord != null) {
                lastWord to words.dropLast(1).joinToString(" ")
            } else {
                val match = GLUED.find(left) ?: return null
                (find(match.groupValues[2]) ?: return null) to match.groupValues[1]
            }
        if (source.dimension != target.dimension) return null
        val amount = runCatching { Calculator.evaluate(amountText) }.getOrNull() ?: return null
        return Conversion(amount, source, convert(amount, source, target), target)
    }

    fun convert(
        value: Double,
        from: UnitDef,
        to: UnitDef,
    ): Double {
        if (from.dimension != Dimension.TEMPERATURE) return value * from.factor / to.factor
        val celsius =
            when (from.symbol) {
                "°F" -> (value - FREEZING_F) * FIVE / NINE
                "K" -> value - ZERO_K
                else -> value
            }
        return when (to.symbol) {
            "°F" -> celsius * NINE / FIVE + FREEZING_F
            "K" -> celsius + ZERO_K
            else -> celsius
        }
    }

    private const val FREEZING_F = 32.0
    private const val ZERO_K = 273.15
    private const val FIVE = 5.0
    private const val NINE = 9.0
}
