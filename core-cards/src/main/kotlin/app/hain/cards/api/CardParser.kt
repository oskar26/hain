package app.hain.cards.api

/**
 * Line parser for model cards (CARDS.md §2, T-130). Pure Kotlin, streaming-tolerant: it reads the
 * body of a "```card" block (without the fences) line by line, may be called again and again on a
 * growing text, ignores lines it does not know and returns null when nothing usable is left — the
 * caller then shows the block as plain text. Model output is untrusted: everything is capped and
 * stays plain text.
 *
 * Lenient on purpose (small brain): English and German keywords, items with `-`, `*`, `•` or `1.`,
 * check marks before or after the text, a table or list without its keyword line.
 */
object CardParser {
    const val MAX_LINES = 30
    const val MAX_ITEMS = 20
    const val MAX_ROWS = 20
    const val MAX_COLUMNS = 5
    const val MAX_BUTTONS = 3
    const val MAX_TEXT = 200
    const val MAX_POINTS = 12
    const val MAX_LABEL = 24
    const val MAX_UNIT = 8

    private enum class Mode { NONE, LIST, CHECKLIST, TABLE, CHART }

    private val itemPrefix = Regex("""^(?:[-*•–]|\d{1,2}[.)])\s+""")
    private val separatorCell = Regex("""^:?-{2,}:?$""")
    private val checkBefore = Regex("""^(?:\[([ xX✓✔])]|([☐☑✅✔✓]))\s*""")
    private val checkAfter = Regex("""\s*(?:\[([ xX✓✔])]|([☐☑✅✔✓]))$""")
    private val attribute =
        Regex("""(\w+)\s*=\s*(?:"((?:[^"\\]|\\.)*)"|„([^“”"]*)[“”"]|'([^']*)'|(\S+))""")
    private const val QUOTED = """"((?:[^"\\]|\\.)*)"|„([^“”"]*)[“”"]|'([^']*)'"""
    private val askAction =
        Regex("""action\s*=\s*ask\s*:\s*(?:$QUOTED|"((?:[^"\\]|\\.)*)$|(\S.*)$)""")

    /** "Mo: 7,5 h", "Januar = 120", "Köln 9 °C": label, number, optional unit. */
    private val chartPoint =
        Regex("""^(.+?)\s*(?:[:=]\s*|\s)([-−]?\d{1,9}(?:[.,]\d{1,4})?)\s*([^\d\s][^\d]{0,7})?$""")
    private val controlChars = Regex("""[\u0000-\u0008\u000B-\u001F\u007F]""")

    /** Parses the body of one card block; null when it holds no block at all. */
    fun parse(body: String): ModelCard? {
        val builder = Builder()
        body
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(MAX_LINES)
            .forEach { builder.line(it) }
        return builder.build()
    }

    private class Builder {
        var title: String? = null
        val blocks = mutableListOf<CardBlock>()
        var mode = Mode.NONE
        val items = mutableListOf<String>()
        val checks = mutableListOf<CheckItem>()
        val rows = mutableListOf<List<String>>()
        var buttons = 0
        val chart = ChartDraft()

        fun line(line: String) {
            val first = line.substringBefore(' ').trimEnd(':').lowercase()
            val keyword = KEYWORDS[first]
            val rest = line.substringAfter(' ', "").trim()
            when {
                first in IGNORED -> Unit // `Image src=…`: not shown yet (CARDS.md §2), never a chart point
                keyword == Keyword.CARD -> title = attributes(rest)["title"]?.let(::clean) ?: title
                keyword == Keyword.PROGRESS -> progress(rest)
                keyword == Keyword.BUTTON -> button(line)
                keyword == Keyword.CHART -> {
                    switchTo(Mode.CHART)
                    chart.start(attributes(rest), rest)
                }
                keyword != null && rest.isEmpty() -> switchTo(keyword.mode)
                line.startsWith("|") -> row(line)
                mode == Mode.CHART -> chart.add(line.replaceFirst(itemPrefix, ""))
                itemPrefix.containsMatchIn(line) -> item(line.replaceFirst(itemPrefix, ""))
                else -> Unit // unknown line: ignored (CARDS.md §3)
            }
        }

        fun switchTo(next: Mode) {
            flush()
            mode = next
        }

        fun flush() {
            when (mode) {
                Mode.LIST -> if (items.isNotEmpty()) blocks += CardBlock.Bullets(items.toList())
                Mode.CHECKLIST -> if (checks.isNotEmpty()) blocks += CardBlock.Checklist(checks.toList())
                Mode.TABLE -> table()?.let { blocks += it }
                Mode.CHART -> chart.build()?.let { blocks += it }
                Mode.NONE -> Unit
            }
            items.clear()
            checks.clear()
            rows.clear()
            chart.clear()
            mode = Mode.NONE
        }

        fun item(raw: String) {
            if (mode == Mode.NONE || mode == Mode.TABLE) {
                val marked = checkBefore.containsMatchIn(raw) || checkAfter.containsMatchIn(raw)
                switchTo(if (marked) Mode.CHECKLIST else Mode.LIST)
            }
            if (mode == Mode.CHECKLIST) {
                if (checks.size >= MAX_ITEMS) return
                checkItem(raw)?.let { checks += it }
            } else {
                if (items.size >= MAX_ITEMS) return
                clean(raw).takeIf { it.isNotEmpty() }?.let { items += it }
            }
        }

        fun row(line: String) {
            if (mode != Mode.TABLE) switchTo(Mode.TABLE)
            val cells =
                line
                    .trim()
                    .removePrefix("|")
                    .removeSuffix("|")
                    .split("|")
                    .map { clean(it) }
            if (cells.all { it.isEmpty() || separatorCell.matches(it) }) return
            if (rows.size > MAX_ROWS) return
            rows += cells.take(MAX_COLUMNS)
        }

        fun table(): CardBlock.Table? {
            val header = rows.firstOrNull()?.takeIf { cells -> cells.any { it.isNotEmpty() } } ?: return null
            val width = header.size
            val body = rows.drop(1).map { cells -> List(width) { cells.getOrElse(it) { "" } } }
            return CardBlock.Table(header, body)
        }

        fun progress(rest: String) {
            flush()
            val attrs = attributes(rest)
            val raw = (attrs["value"] ?: rest.substringBefore(' ')).trim().removeSuffix("%").replace(',', '.')
            val number = raw.toDoubleOrNull() ?: return
            val percent = if (number > 0 && number < 1 && raw.contains('.')) number * PERCENT else number
            val label = attrs["label"]?.let(::clean)?.takeIf { it.isNotEmpty() }
            blocks += CardBlock.Progress(percent.toInt().coerceIn(0, PERCENT.toInt()), label)
        }

        @Suppress("ReturnCount") // each missing piece drops the button
        fun button(line: String) {
            flush()
            if (buttons >= MAX_BUTTONS) return
            val attrs = attributes(line)
            val label = attrs["label"]?.let(::clean)?.takeIf { it.isNotEmpty() } ?: return
            val action =
                when (attrs["action"]?.lowercase()) {
                    "copy" -> CardAction.Copy
                    "share" -> CardAction.Share
                    else -> askText(line)?.let { CardAction.Ask(it) }
                } ?: return
            buttons++
            blocks += CardBlock.Button(label, action)
        }

        fun build(): ModelCard? {
            flush()
            return if (blocks.isEmpty()) null else ModelCard(title?.takeIf { it.isNotEmpty() }, blocks.toList())
        }
    }

    /**
     * A chart while it is read (0.1.6.1): `Chart type=bar|line title="…" unit="…"` (German `Diagramm
     * typ=balken|linie titel=… einheit=…`), then "Mo: 7,5" lines; anything else inside is ignored.
     */
    private class ChartDraft {
        val points = mutableListOf<ChartPoint>()
        var type = ChartType.BAR
        var title: String? = null
        var unit: String? = null
        var pointUnit: String? = null

        fun start(
            attrs: Map<String, String>,
            rest: String,
        ) {
            val kind = (attrs["type"] ?: attrs["typ"] ?: rest.substringBefore(' ')).lowercase()
            type = if (kind in LINE_WORDS) ChartType.LINE else ChartType.BAR
            title = (attrs["title"] ?: attrs["titel"])?.let(::clean)?.takeIf { it.isNotEmpty() }
            unit = (attrs["unit"] ?: attrs["einheit"])?.let(::clean)?.take(MAX_UNIT)?.takeIf { it.isNotEmpty() }
            pointUnit = null
        }

        fun add(raw: String) {
            val match = chartPoint.find(clean(raw)).takeIf { points.size < MAX_POINTS }
            val label =
                match
                    ?.groupValues
                    ?.get(1)
                    ?.trim()
                    ?.trimEnd(':', '=')
                    ?.trim()
                    ?.take(MAX_LABEL)
                    .orEmpty()
            val number =
                match
                    ?.groupValues
                    ?.get(2)
                    ?.replace('−', '-')
                    ?.replace(',', '.')
                    ?.toDoubleOrNull()
                    ?.takeIf { it.isFinite() }
            if (match == null || label.isEmpty() || number == null) return
            if (pointUnit == null) {
                pointUnit =
                    match.groupValues[UNIT_GROUP]
                        .trim()
                        .take(MAX_UNIT)
                        .takeIf { it.isNotEmpty() }
            }
            points += ChartPoint(label, number)
        }

        fun build(): CardBlock.Chart? {
            if (points.isEmpty()) return null
            val shown = if (type == ChartType.LINE && points.size < MIN_LINE_POINTS) ChartType.BAR else type
            return CardBlock.Chart(shown, points.toList(), title, unit ?: pointUnit)
        }

        fun clear() {
            points.clear()
        }
    }

    private fun checkItem(raw: String): CheckItem? {
        var text = raw
        var checked = false
        checkBefore.find(text)?.let { match ->
            checked = isTicked(match)
            text = text.substring(match.range.last + 1)
        }
        checkAfter.find(text)?.let { match ->
            checked = checked || isTicked(match)
            text = text.substring(0, match.range.first)
        }
        val cleaned = clean(text)
        return if (cleaned.isEmpty()) null else CheckItem(cleaned, checked)
    }

    private fun isTicked(match: MatchResult): Boolean {
        val box = match.groupValues[1]
        val glyph = match.groupValues[2]
        return box.isNotBlank() || (glyph.isNotEmpty() && glyph != "☐")
    }

    @Suppress("ReturnCount")
    private fun askText(line: String): String? {
        val match = askAction.find(line) ?: return null
        val text = match.groupValues.drop(1).firstOrNull { it.isNotEmpty() } ?: return null
        return clean(text.replace("\\\"", "\"")).takeIf { it.isNotEmpty() }
    }

    /** `key="value"`, `key=„value“`, `key='value'` or `key=value`, keys lower-cased. */
    internal fun attributes(text: String): Map<String, String> =
        attribute.findAll(text).associate { match ->
            val value =
                match.groupValues
                    .drop(2)
                    .firstOrNull { it.isNotEmpty() }
                    .orEmpty()
            match.groupValues[1].lowercase() to value.replace("\\\"", "\"")
        }

    /** Plain, capped text: no control characters, no markdown bold markers, no stray backticks. */
    internal fun clean(text: String): String =
        text
            .replace(controlChars, "")
            .replace("**", "")
            .replace("`", "")
            .trim()
            .take(MAX_TEXT)

    /** Keyword lines in English and German; list, checklist and table switch the [Mode]. */
    private enum class Keyword(
        val mode: Mode,
    ) {
        CARD(Mode.NONE),
        LIST(Mode.LIST),
        CHECKLIST(Mode.CHECKLIST),
        TABLE(Mode.TABLE),
        PROGRESS(Mode.NONE),
        BUTTON(Mode.NONE),
        CHART(Mode.CHART),
    }

    private val KEYWORDS =
        mapOf(
            "card" to Keyword.CARD,
            "karte" to Keyword.CARD,
            "list" to Keyword.LIST,
            "liste" to Keyword.LIST,
            "checklist" to Keyword.CHECKLIST,
            "checkliste" to Keyword.CHECKLIST,
            "todo" to Keyword.CHECKLIST,
            "todos" to Keyword.CHECKLIST,
            "table" to Keyword.TABLE,
            "tabelle" to Keyword.TABLE,
            "progress" to Keyword.PROGRESS,
            "fortschritt" to Keyword.PROGRESS,
            "button" to Keyword.BUTTON,
            "knopf" to Keyword.BUTTON,
            "chart" to Keyword.CHART,
            "diagramm" to Keyword.CHART,
            "grafik" to Keyword.CHART,
        )
    private val IGNORED = setOf("image", "bild", "img")
    private val LINE_WORDS = setOf("line", "linie", "linien", "liniendiagramm", "kurve")
    private const val PERCENT = 100.0
    private const val MIN_LINE_POINTS = 2
    private const val UNIT_GROUP = 3
}
