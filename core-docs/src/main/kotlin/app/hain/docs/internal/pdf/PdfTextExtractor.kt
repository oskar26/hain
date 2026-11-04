package app.hain.docs.internal.pdf

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File
import java.io.IOException

/** The PDF is protected with a password we do not have. */
class LockedPdfException : IOException("pdf needs a password")

/**
 * Reads the text layer of a PDF page by page (T-120) with pdfbox-android (Apache-2.0); Android's
 * PdfRenderer can only draw pages, not read their text. A page with fewer than [MIN_CHARS]
 * characters counts as a picture (a scan) and is left for the text recognition (T-121).
 */
object PdfTextExtractor {
    /** Below this many letters/digits a page is treated as a scanned picture (DOCS_OCR.md). */
    const val MIN_CHARS = 40

    /** One text per page, in page order. Throws [LockedPdfException] or [IOException]. */
    fun pages(
        file: File,
        onPage: (page: Int, total: Int) -> Unit = { _, _ -> },
    ): List<String> =
        open(file).use { doc ->
            val total = doc.numberOfPages
            val stripper =
                PDFTextStripper().apply {
                    sortByPosition = true
                    lineSeparator = "\n"
                    paragraphEnd = "\n"
                }
            (1..total).map { page ->
                onPage(page, total)
                stripper.startPage = page
                stripper.endPage = page
                clean(stripper.getText(doc))
            }
        }

    /** Number of pages without reading any text. */
    fun pageCount(file: File): Int = open(file).use { it.numberOfPages }

    /** `true` when [text] has too little real text to be a text page. */
    fun needsOcr(text: String): Boolean = text.count { it.isLetterOrDigit() } < MIN_CHARS

    /** Unifies line ends, drops trailing spaces and runs of more than one empty line. */
    fun clean(text: String): String =
        text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace('\u00A0', ' ')
            .lines()
            .joinToString("\n") { it.trimEnd() }
            .replace(BLANK_RUN, "\n\n")
            .trim()

    @Suppress("SwallowedException") // the password exception becomes our own, clearer one
    private fun open(file: File): PDDocument =
        try {
            PDDocument.load(file)
        } catch (e: InvalidPasswordException) {
            throw LockedPdfException()
        }

    private val BLANK_RUN = Regex("\n{3,}")
}
