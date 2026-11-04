package app.hain.docs.pdf

import androidx.test.core.app.ApplicationProvider
import app.hain.docs.internal.pdf.PdfTextExtractor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import java.io.File

/** T-120: the text layer of the fixture `rechnung.pdf` (page 1 text, page 2 nearly empty, page 3 text). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class PdfTextExtractorTest {
    private lateinit var pdf: File

    @Before
    fun setUp() {
        PDFBoxResourceLoader.init(ApplicationProvider.getApplicationContext())
        val url = checkNotNull(javaClass.classLoader?.getResource("fixtures/rechnung.pdf"))
        pdf = File(url.toURI())
    }

    @Test
    fun readsEveryPageInOrder() {
        val pages = PdfTextExtractor.pages(pdf)
        assertEquals(3, pages.size)
        assertTrue(pages[0].contains("Rechnung Nr. 2026-117"))
        assertTrue(pages[0].contains("Größen"))
        assertTrue(pages[0].contains("Überprüfung der Straße"))
        assertTrue(pages[2].contains("Heizung im Keller"))
    }

    @Test
    fun pageWithAlmostNoTextCountsAsScan() {
        val pages = PdfTextExtractor.pages(pdf)
        assertFalse(PdfTextExtractor.needsOcr(pages[0]))
        assertTrue(PdfTextExtractor.needsOcr(pages[1]))
        assertFalse(PdfTextExtractor.needsOcr(pages[2]))
    }

    @Test
    fun reportsProgressPerPage() {
        val seen = mutableListOf<Pair<Int, Int>>()
        PdfTextExtractor.pages(pdf) { page, total -> seen += page to total }
        assertEquals(listOf(1 to 3, 2 to 3, 3 to 3), seen)
        assertEquals(3, PdfTextExtractor.pageCount(pdf))
    }

    @Test
    fun cleanUnifiesLineEndsAndBlankRuns() {
        assertEquals("a\n\nb", PdfTextExtractor.clean("a  \r\n\r\n\r\n\r\nb\u00A0"))
    }

    @Test
    fun fortyLettersAreTheLimit() {
        assertTrue(PdfTextExtractor.needsOcr("a".repeat(39) + " ... "))
        assertFalse(PdfTextExtractor.needsOcr("a".repeat(40)))
    }
}
