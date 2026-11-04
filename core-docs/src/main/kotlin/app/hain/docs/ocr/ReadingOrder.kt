package app.hain.docs.ocr

/**
 * Puts recognized lines into reading order (T-121): top to bottom, left to right, and pages with
 * two columns column by column. Lines wider than most of the page (a title over both columns)
 * stay where they are and split the page into sections; inside a section a clear vertical gap
 * that no box crosses, with text on both sides, is a column gutter. Lines that sit side by side
 * at the same height become one line; a large vertical gap starts a new paragraph.
 */
object ReadingOrder {
    /** A box wider than this share of the text area spans both columns. */
    private const val SPAN_SHARE = 0.6f

    /** Each column needs at least this many lines. */
    private const val MIN_COLUMN_LINES = 3

    /** Both columns must share at least this much of their height. */
    private const val COLUMN_OVERLAP = 0.5f

    /** Two boxes are on one line when they overlap this much of the smaller height. */
    private const val SAME_LINE = 0.5f

    /** A gap above a line larger than this many line heights starts a paragraph. */
    private const val PARAGRAPH_GAP = 1.2f

    /** The text of [items] in reading order. */
    fun text(items: List<OcrItem>): String =
        blocks(items.filter { it.text.isNotBlank() })
            .joinToString("\n\n") { block -> join(lines(block)) }
            .trim()

    /**
     * The items in groups that are read one after the other: full-width lines and sections, each
     * section split into its left and right column when it has two.
     */
    internal fun blocks(items: List<OcrItem>): List<List<OcrItem>> {
        if (items.isEmpty()) return emptyList()
        val minX = items.minOf { it.box.left }
        val maxX = items.maxOf { it.box.right }
        val span = (maxX - minX).coerceAtLeast(1f)
        // (items, is a column) – plain parts next to each other are joined again at the end
        val parts = mutableListOf<Pair<List<OcrItem>, Boolean>>()
        val section = mutableListOf<OcrItem>()

        fun flush() {
            if (section.isEmpty()) return
            val split = columns(section.toList())
            if (split == null) {
                parts += section.toList() to false
            } else {
                parts += split.first to true
                parts += split.second to true
            }
            section.clear()
        }
        items.sortedBy { it.box.top }.forEach { item ->
            if (item.box.width >= SPAN_SHARE * span) {
                flush()
                parts += listOf(item) to false
            } else {
                section += item
            }
        }
        flush()
        val out = mutableListOf<List<OcrItem>>()
        var plain = mutableListOf<OcrItem>()
        parts.forEach { (part, column) ->
            if (column) {
                if (plain.isNotEmpty()) out += plain
                plain = mutableListOf()
                out += part
            } else {
                plain += part
            }
        }
        if (plain.isNotEmpty()) out += plain
        return out
    }

    /** Left and right column of [items], or `null` when they are not two columns. */
    @Suppress("ReturnCount")
    internal fun columns(items: List<OcrItem>): Pair<List<OcrItem>, List<OcrItem>>? {
        if (items.size < 2 * MIN_COLUMN_LINES) return null
        val gutter = gutter(items) ?: return null
        val left = items.filter { it.box.right <= gutter }
        val right = items.filter { it.box.left >= gutter }
        if (left.size < MIN_COLUMN_LINES || right.size < MIN_COLUMN_LINES) return null
        val overlap =
            minOf(left.maxOf { it.box.bottom }, right.maxOf { it.box.bottom }) -
                maxOf(left.minOf { it.box.top }, right.minOf { it.box.top })
        val smaller =
            minOf(
                left.maxOf { it.box.bottom } - left.minOf { it.box.top },
                right.maxOf { it.box.bottom } - right.minOf { it.box.top },
            )
        if (smaller <= 0f || overlap < COLUMN_OVERLAP * smaller) return null
        return left to right
    }

    /** The middle of the widest vertical strip no box crosses, inside the middle of the text area. */
    private fun gutter(items: List<OcrItem>): Float? {
        val minX = items.minOf { it.box.left }
        val maxX = items.maxOf { it.box.right }
        val lo = minX + (maxX - minX) * GUTTER_FROM
        val hi = minX + (maxX - minX) * GUTTER_TO
        val spans = items.map { it.box.left to it.box.right }.sortedBy { it.first }
        var best: Float? = null
        var bestWidth = 0f
        var reach = spans.first().second
        for (i in 1 until spans.size) {
            val (start, end) = spans[i]
            if (start > reach) {
                val gapMid = (reach + start) / 2f
                val width = start - reach
                if (gapMid in lo..hi && width > bestWidth) {
                    best = gapMid
                    bestWidth = width
                }
            }
            reach = maxOf(reach, end)
        }
        return best
    }

    /** [items] as lines from top to bottom; boxes at the same height join from left to right. */
    internal fun lines(items: List<OcrItem>): List<List<OcrItem>> {
        val lines = mutableListOf<MutableList<OcrItem>>()
        items.sortedBy { it.box.centerY }.forEach { item ->
            val line = lines.lastOrNull()
            if (line != null && sameLine(line, item)) line += item else lines += mutableListOf(item)
        }
        return lines.map { line -> line.sortedBy { it.box.left } }
    }

    private fun sameLine(
        line: List<OcrItem>,
        item: OcrItem,
    ): Boolean {
        val top = line.minOf { it.box.top }
        val bottom = line.maxOf { it.box.bottom }
        val overlap = minOf(bottom, item.box.bottom) - maxOf(top, item.box.top)
        val smaller = minOf(bottom - top, item.box.height)
        return smaller > 0f && overlap >= SAME_LINE * smaller
    }

    /** Lines joined by line breaks, with an empty line where the gap is large (a new paragraph). */
    private fun join(lines: List<List<OcrItem>>): String {
        if (lines.isEmpty()) return ""
        val heights = lines.map { l -> l.maxOf { it.box.bottom } - l.minOf { it.box.top } }.sorted()
        val typical = heights[heights.size / 2]
        val out = StringBuilder()
        var lastBottom = Float.NaN
        lines.forEach { line ->
            val top = line.minOf { it.box.top }
            if (out.isNotEmpty()) {
                out.append(if (top - lastBottom > PARAGRAPH_GAP * typical) "\n\n" else "\n")
            }
            out.append(line.joinToString(" ") { it.text.trim() })
            lastBottom = line.maxOf { it.box.bottom }
        }
        return out.toString()
    }

    private const val GUTTER_FROM = 0.25f
    private const val GUTTER_TO = 0.75f
}
