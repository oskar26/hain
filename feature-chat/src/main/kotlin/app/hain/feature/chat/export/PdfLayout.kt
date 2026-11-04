package app.hain.feature.chat.export

import app.hain.feature.chat.internal.Chat

/** Text styles of the chat PDF (T-054); the renderer maps each to a font, size and colour. */
enum class PdfStyle { TITLE, META, ROLE_USER, ROLE_HAIN, HEADING, BODY, QUOTE, CODE, SOURCE, FOOTER }

/** One piece of the document before it is broken into lines and pages. */
sealed interface PdfBlock {
    val spaceBefore: Float

    data class Text(
        val style: PdfStyle,
        val text: String,
        val indent: Float = 0f,
        /** "•" or "1." in front of the first line (lists, sources). */
        val bullet: String? = null,
        /** Never the last thing on a page (titles, roles, headings). */
        val keepWithNext: Boolean = false,
        override val spaceBefore: Float = 0f,
    ) : PdfBlock

    /** Code keeps its spaces and line breaks; long lines wrap by character. */
    data class Code(
        val lines: List<String>,
        override val spaceBefore: Float = 0f,
    ) : PdfBlock

    /** A hairline; [accent] = the green line under the title. */
    data class Rule(
        val accent: Boolean = false,
        override val spaceBefore: Float = 0f,
    ) : PdfBlock
}

/** Something placed on a page; [top] is measured from the top edge of the page in points. */
sealed interface PdfItem {
    val top: Float

    data class Line(
        val style: PdfStyle,
        val text: String,
        val x: Float,
        override val top: Float,
        val height: Float,
        val bullet: String? = null,
        val bulletX: Float = x,
    ) : PdfItem

    /** Grey background behind one line of code. */
    data class CodeBox(
        val x: Float,
        override val top: Float,
        val width: Float,
        val height: Float,
    ) : PdfItem

    data class Rule(
        val x: Float,
        override val top: Float,
        val width: Float,
        val accent: Boolean,
    ) : PdfItem
}

data class PdfPage(
    val number: Int,
    val items: List<PdfItem>,
)

/** Page size and spacing in PDF points (A4 by default). */
data class PdfMetrics(
    val pageWidth: Float = A4_WIDTH,
    val pageHeight: Float = A4_HEIGHT,
    val margin: Float = MARGIN,
    /** Room kept free at the bottom for the page number. */
    val footer: Float = FOOTER,
    val bulletIndent: Float = BULLET_INDENT,
    val codePadding: Float = CODE_PADDING,
    val lineHeight: (PdfStyle) -> Float = ::defaultLineHeight,
) {
    val contentWidth: Float get() = pageWidth - 2 * margin
    val contentBottom: Float get() = pageHeight - margin - footer

    companion object {
        const val A4_WIDTH = 595f
        const val A4_HEIGHT = 842f
        const val MARGIN = 56f
        const val FOOTER = 24f
        const val BULLET_INDENT = 14f
        const val CODE_PADDING = 6f
    }
}

/** Line heights that go with the sizes in [ChatPdf]. */
@Suppress("MagicNumber") // a small table of type sizes in points
fun defaultLineHeight(style: PdfStyle): Float =
    when (style) {
        PdfStyle.TITLE -> 28f
        PdfStyle.HEADING -> 19f
        PdfStyle.ROLE_USER, PdfStyle.ROLE_HAIN -> 16f
        PdfStyle.BODY, PdfStyle.QUOTE -> 16f
        PdfStyle.CODE -> 13.5f
        PdfStyle.META, PdfStyle.SOURCE, PdfStyle.FOOTER -> 13f
    }

/**
 * Turns a chat into pages (T-054), without Android: [paginate] only needs a way to measure text, so
 * the page breaks can be tested on the JVM. [ChatPdf] draws the result with android.graphics.pdf.
 */
object PdfLayout {
    /** The chat as blocks: title, date line, green hairline, then each message under its role. */
    fun blocks(
        chat: Chat,
        labels: ExportLabels,
    ): List<PdfBlock> =
        buildList {
            add(PdfBlock.Text(PdfStyle.TITLE, chat.title.ifBlank { labels.untitled }, keepWithNext = true))
            if (labels.exportedLine.isNotBlank()) add(PdfBlock.Text(PdfStyle.META, labels.exportedLine))
            add(PdfBlock.Rule(accent = true, spaceBefore = GAP_SMALL))
            ChatMarkdown.exportable(chat.messages).forEach { message ->
                val role = if (message.isUser) PdfStyle.ROLE_USER else PdfStyle.ROLE_HAIN
                val label = if (message.isUser) labels.you else labels.hain
                add(PdfBlock.Text(role, label, keepWithNext = true, spaceBefore = GAP_MESSAGE))
                addAll(MarkdownBlocks.parse(message.text))
                if (message.stopped && labels.stopped.isNotBlank()) {
                    add(PdfBlock.Text(PdfStyle.META, labels.stopped, spaceBefore = GAP_SMALL))
                }
                if (message.sources.isNotEmpty()) {
                    add(PdfBlock.Text(PdfStyle.META, labels.sources, keepWithNext = true, spaceBefore = GAP_PARAGRAPH))
                    message.sources.forEach { source ->
                        add(PdfBlock.Text(PdfStyle.SOURCE, "${source.title.trim()} · ${source.url}", bullet = "•"))
                    }
                }
            }
        }

    /** Breaks [blocks] into lines that fit [metrics] and those into pages. Never returns no page. */
    @Suppress("CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth")
    fun paginate(
        blocks: List<PdfBlock>,
        metrics: PdfMetrics,
        measure: (PdfStyle, String) -> Float,
    ): List<PdfPage> {
        val pages = mutableListOf<PdfPage>()
        var items = mutableListOf<PdfItem>()
        var y = metrics.margin
        val bottom = metrics.contentBottom

        fun newPage() {
            if (items.isEmpty()) return
            pages += PdfPage(pages.size + 1, items)
            items = mutableListOf()
            y = metrics.margin
        }

        fun space(gap: Float) {
            if (items.isNotEmpty()) y += gap
        }

        blocks.forEachIndexed { index, block ->
            when (block) {
                is PdfBlock.Text -> {
                    val lh = metrics.lineHeight(block.style)
                    val bulletRoom = if (block.bullet != null) metrics.bulletIndent else 0f
                    val x = metrics.margin + block.indent + bulletRoom
                    val lines =
                        wrap(block.text, metrics.contentWidth - block.indent - bulletRoom) {
                            measure(block.style, it)
                        }
                    space(block.spaceBefore)
                    if (block.keepWithNext) {
                        val next = blocks.getOrNull(index + 1)
                        val nextHeight =
                            next?.let { it.spaceBefore + firstLineHeight(it, metrics) } ?: 0f
                        if (y + lines.size * lh + nextHeight > bottom) newPage()
                    }
                    lines.forEachIndexed { i, line ->
                        if (y + lh > bottom) newPage()
                        items +=
                            PdfItem.Line(
                                block.style,
                                line,
                                x,
                                y,
                                lh,
                                bullet = if (i == 0) block.bullet else null,
                                bulletX = metrics.margin + block.indent,
                            )
                        y += lh
                    }
                }
                is PdfBlock.Code -> {
                    val lh = metrics.lineHeight(PdfStyle.CODE)
                    val pad = metrics.codePadding
                    val inner = metrics.contentWidth - 2 * pad
                    val lines =
                        block.lines
                            .flatMap { wrapCode(it, inner) { s -> measure(PdfStyle.CODE, s) } }
                            .ifEmpty { listOf("") }
                    space(block.spaceBefore)
                    if (y + pad + lh > bottom) newPage()
                    y += pad
                    lines.forEachIndexed { i, line ->
                        if (y + lh > bottom) {
                            newPage()
                            y += pad
                        }
                        val first = i == 0 || items.none { it is PdfItem.CodeBox }
                        val boxTop = if (first) y - pad else y
                        val last = i == lines.lastIndex
                        val boxHeight = lh + (y - boxTop) + if (last) pad else 0f
                        items += PdfItem.CodeBox(metrics.margin, boxTop, metrics.contentWidth, boxHeight)
                        items += PdfItem.Line(PdfStyle.CODE, line, metrics.margin + pad, y, lh)
                        y += lh
                    }
                    y += pad
                }
                is PdfBlock.Rule -> {
                    space(block.spaceBefore)
                    if (y + RULE_ROOM > bottom) newPage()
                    items += PdfItem.Rule(metrics.margin, y, metrics.contentWidth, block.accent)
                    y += RULE_ROOM
                }
            }
        }
        newPage()
        return pages.ifEmpty { listOf(PdfPage(1, emptyList())) }
    }

    private fun firstLineHeight(
        block: PdfBlock,
        metrics: PdfMetrics,
    ): Float =
        when (block) {
            is PdfBlock.Text -> metrics.lineHeight(block.style)
            is PdfBlock.Code -> metrics.lineHeight(PdfStyle.CODE) + 2 * metrics.codePadding
            is PdfBlock.Rule -> RULE_ROOM
        }

    /**
     * Word wrap: as many words per line as fit into [maxWidth]; a word longer than a whole line is
     * cut by character. Whitespace runs become one space. Empty text → no line.
     */
    fun wrap(
        text: String,
        maxWidth: Float,
        width: (String) -> Float,
    ): List<String> {
        val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val lines = mutableListOf<String>()
        var current = ""
        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            when {
                width(candidate) <= maxWidth -> current = candidate
                width(word) <= maxWidth -> {
                    lines += current
                    current = word
                }
                else -> {
                    if (current.isNotEmpty()) lines += current
                    val pieces = wrapCode(word, maxWidth, width)
                    lines += pieces.dropLast(1)
                    current = pieces.last()
                }
            }
        }
        if (current.isNotEmpty()) lines += current
        return lines
    }

    /** Character wrap that keeps spaces (code); tabs become four spaces. An empty line stays one line. */
    fun wrapCode(
        line: String,
        maxWidth: Float,
        width: (String) -> Float,
    ): List<String> {
        val text = line.replace("\t", "    ").trimEnd()
        if (text.isEmpty()) return listOf("")
        val out = mutableListOf<String>()
        var start = 0
        while (start < text.length) {
            var end = start + 1
            while (end < text.length && width(text.substring(start, end + 1)) <= maxWidth) end++
            out += text.substring(start, end)
            start = end
        }
        return out
    }

    private const val GAP_SMALL = 6f
    private const val GAP_PARAGRAPH = 8f
    private const val GAP_MESSAGE = 18f
    private const val RULE_ROOM = 10f
}
