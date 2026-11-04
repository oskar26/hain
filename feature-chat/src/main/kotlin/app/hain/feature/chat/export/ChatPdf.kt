package app.hain.feature.chat.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.res.ResourcesCompat
import app.hain.feature.chat.internal.Chat
import java.io.OutputStream

/**
 * Draws a chat as an A4 PDF with android.graphics.pdf (T-054, own code, no library): title, date
 * line and a green hairline, then each message under its role ("Hain" in Hain-Grün), paragraphs,
 * lists, code on a grey band in a monospaced font, and on every page a quiet footer with the page
 * number. The page breaks come from [PdfLayout].
 */
class ChatPdf(
    private val context: Context,
) {
    /** Writes the PDF to [out] (left open) and returns the number of pages. */
    fun write(
        chat: Chat,
        labels: ExportLabels,
        out: OutputStream,
    ): Int {
        val paints = Paints(fonts())
        val metrics = PdfMetrics()
        val pages =
            PdfLayout.paginate(PdfLayout.blocks(chat, labels), metrics) { style, text ->
                paints[style].measureText(text)
            }
        val document = PdfDocument()
        try {
            pages.forEach { page ->
                val info =
                    PdfDocument.PageInfo
                        .Builder(metrics.pageWidth.toInt(), metrics.pageHeight.toInt(), page.number)
                        .create()
                val pdfPage = document.startPage(info)
                draw(pdfPage.canvas, page, pages.size, labels, metrics, paints)
                document.finishPage(pdfPage)
            }
            document.writeTo(out)
        } finally {
            document.close()
        }
        return pages.size
    }

    @Suppress("LongParameterList") // one page: canvas, its items, the page count and the look
    private fun draw(
        canvas: Canvas,
        page: PdfPage,
        pageCount: Int,
        labels: ExportLabels,
        metrics: PdfMetrics,
        paints: Paints,
    ) {
        page.items.forEach { item ->
            when (item) {
                is PdfItem.CodeBox ->
                    canvas.drawRect(item.x, item.top, item.x + item.width, item.top + item.height, paints.codeBox)
                is PdfItem.Rule -> {
                    val paint = if (item.accent) paints.accentRule else paints.hairline
                    canvas.drawLine(item.x, item.top, item.x + item.width, item.top, paint)
                }
                is PdfItem.Line -> {
                    val paint = paints[item.style]
                    val baseline = baseline(paint, item.top, item.height)
                    if (item.style == PdfStyle.QUOTE) {
                        canvas.drawRect(
                            item.x - QUOTE_BAR_GAP,
                            item.top,
                            item.x - QUOTE_BAR_GAP + QUOTE_BAR,
                            item.top + item.height,
                            paints.quoteBar,
                        )
                    }
                    item.bullet?.let { canvas.drawText(it, item.bulletX, baseline, paint) }
                    canvas.drawText(item.text, item.x, baseline, paint)
                }
            }
        }
        // footer: hairline, "Hain" in green on the left, page x of n on the right
        val footerTop = metrics.pageHeight - metrics.margin
        canvas.drawLine(metrics.margin, footerTop, metrics.pageWidth - metrics.margin, footerTop, paints.hairline)
        val footer = paints[PdfStyle.FOOTER]
        val baseline = baseline(footer, footerTop + FOOTER_GAP, defaultLineHeight(PdfStyle.FOOTER))
        canvas.drawText(labels.hain, metrics.margin, baseline, paints.footerBrand)
        val number = labels.pageOf.format(page.number, pageCount)
        canvas.drawText(number, metrics.pageWidth - metrics.margin - footer.measureText(number), baseline, footer)
    }

    private fun baseline(
        paint: Paint,
        top: Float,
        height: Float,
    ): Float {
        val fm = paint.fontMetrics
        return top + (height - (fm.descent - fm.ascent)) / 2f - fm.ascent
    }

    private fun fonts(): Fonts {
        fun load(
            id: Int,
            fallback: Typeface,
        ): Typeface = runCatching { ResourcesCompat.getFont(context, id) }.getOrNull() ?: fallback
        return Fonts(
            regular = load(app.hain.ui.R.font.hankengrotesk_regular, Typeface.DEFAULT),
            semibold = load(app.hain.ui.R.font.hankengrotesk_semibold, Typeface.DEFAULT_BOLD),
            mono = load(app.hain.ui.R.font.jetbrainsmono_regular, Typeface.MONOSPACE),
        )
    }

    private class Fonts(
        val regular: Typeface,
        val semibold: Typeface,
        val mono: Typeface,
    )

    /** Font, size and colour per style; the Lichtung colours on white paper. */
    private class Paints(
        fonts: Fonts,
    ) {
        private fun text(
            face: Typeface,
            size: Float,
            color: Int,
        ) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = face
            textSize = size
            this.color = color
        }

        private val styles: Map<PdfStyle, Paint> =
            mapOf(
                PdfStyle.TITLE to text(fonts.semibold, SIZE_TITLE, INK),
                PdfStyle.META to text(fonts.regular, SIZE_SMALL, MUTED),
                PdfStyle.ROLE_USER to text(fonts.semibold, SIZE_ROLE, INK),
                PdfStyle.ROLE_HAIN to text(fonts.semibold, SIZE_ROLE, ACCENT),
                PdfStyle.HEADING to text(fonts.semibold, SIZE_HEADING, INK),
                PdfStyle.BODY to text(fonts.regular, SIZE_BODY, INK),
                PdfStyle.QUOTE to text(fonts.regular, SIZE_BODY, MUTED),
                PdfStyle.CODE to text(fonts.mono, SIZE_CODE, INK),
                PdfStyle.SOURCE to text(fonts.regular, SIZE_SMALL, MUTED),
                PdfStyle.FOOTER to text(fonts.regular, SIZE_FOOTER, MUTED),
            )

        operator fun get(style: PdfStyle): Paint = styles.getValue(style)

        val footerBrand = text(fonts.semibold, SIZE_FOOTER, ACCENT)
        val codeBox = Paint().apply { color = CODE_BG }
        val quoteBar = Paint().apply { color = ACCENT_SOFT }
        val hairline =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = LINE
                strokeWidth = HAIRLINE
            }
        val accentRule =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ACCENT
                strokeWidth = ACCENT_LINE
            }
    }

    private companion object {
        val INK = Color.rgb(0x16, 0x1A, 0x18)
        val MUTED = Color.rgb(0x6B, 0x72, 0x6E)
        val ACCENT = Color.rgb(0x16, 0xA3, 0x6B)
        val ACCENT_SOFT = Color.rgb(0xB9, 0xE4, 0xD1)
        val CODE_BG = Color.rgb(0xF2, 0xF5, 0xF3)
        val LINE = Color.rgb(0xE1, 0xE5, 0xE2)
        const val SIZE_TITLE = 20f
        const val SIZE_HEADING = 13f
        const val SIZE_ROLE = 10.5f
        const val SIZE_BODY = 11f
        const val SIZE_CODE = 9f
        const val SIZE_SMALL = 9f
        const val SIZE_FOOTER = 8.5f
        const val HAIRLINE = 0.5f
        const val ACCENT_LINE = 1.5f
        const val QUOTE_BAR = 2f
        const val QUOTE_BAR_GAP = 8f
        const val FOOTER_GAP = 6f
    }
}
