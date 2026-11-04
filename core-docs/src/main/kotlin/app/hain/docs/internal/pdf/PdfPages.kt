package app.hain.docs.internal.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Draws PDF pages with Android's own [PdfRenderer] (T-120): a small preview of the first page and,
 * for scanned pages, a 300 dpi picture for the text recognition (at most [MAX_SIDE] pixels on the
 * long side, about an A4 page at 300 dpi).
 */
class PdfPages(
    file: File,
) : AutoCloseable {
    private val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer = PdfRenderer(fd)

    val count: Int get() = renderer.pageCount

    /** Page [index] (0-based) at [dpi], white background, long side at most [maxSide]. */
    fun render(
        index: Int,
        dpi: Int = OCR_DPI,
        maxSide: Int = MAX_SIDE,
    ): Bitmap =
        renderer.openPage(index).use { page ->
            val scale = dpi / POINTS_PER_INCH
            var w = (page.width * scale).roundToInt().coerceAtLeast(1)
            var h = (page.height * scale).roundToInt().coerceAtLeast(1)
            val longest = max(w, h)
            if (longest > maxSide) {
                w = (w.toFloat() * maxSide / longest).roundToInt().coerceAtLeast(1)
                h = (h.toFloat() * maxSide / longest).roundToInt().coerceAtLeast(1)
            }
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        }

    /** Small PNG of page [index] for the file chip and "Meine Dateien". */
    fun thumbnail(index: Int = 0): ByteArray {
        val bitmap = render(index, THUMB_DPI, THUMB_SIDE)
        return try {
            png(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    override fun close() {
        renderer.close()
        fd.close()
    }

    companion object {
        const val OCR_DPI = 300
        const val MAX_SIDE = 2480
        private const val THUMB_DPI = 36
        private const val THUMB_SIDE = 160
        private const val POINTS_PER_INCH = 72f
        private const val PNG_QUALITY = 100

        /** PNG bytes of [bitmap]. */
        fun png(bitmap: Bitmap): ByteArray =
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)
                out.toByteArray()
            }
    }
}
