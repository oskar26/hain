package app.hain.feature.chat.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownBlocksTest {
    @Test
    fun paragraphsHeadingsListsQuotesCodeAndRules() {
        val md =
            """
            ## Plan
            Erst **das** und
            dann `jenes`.

            - eins
            - zwei
              - tiefer
            1. erster

            > Zitat
            > weiter

            ```
            a  b
            ```
            ---
            """.trimIndent()
        val blocks = MarkdownBlocks.parse(md)
        val heading = blocks[0] as PdfBlock.Text
        assertEquals(PdfStyle.HEADING to "Plan", heading.style to heading.text)
        assertTrue(heading.keepWithNext)
        assertEquals("Erst das und dann jenes.", (blocks[1] as PdfBlock.Text).text)
        val items = blocks.filterIsInstance<PdfBlock.Text>().filter { it.bullet != null }
        assertEquals(listOf("eins", "zwei", "tiefer", "erster"), items.map { it.text })
        assertEquals(listOf("•", "•", "•", "1."), items.map { it.bullet })
        assertTrue(items[2].indent > items[1].indent)
        val quote = blocks.filterIsInstance<PdfBlock.Text>().single { it.style == PdfStyle.QUOTE }
        assertEquals("Zitat weiter", quote.text)
        assertEquals(listOf("a  b"), blocks.filterIsInstance<PdfBlock.Code>().single().lines)
        assertTrue(blocks.last() is PdfBlock.Rule)
    }

    @Test
    fun tablesBecomeMonospacedRowsWithoutTheDividerLine() {
        val blocks = MarkdownBlocks.parse("| A | B |\n|---|:-:|\n| 1 | 2 |")
        assertEquals(listOf("| A | B |", "| 1 | 2 |"), (blocks.single() as PdfBlock.Code).lines)
    }

    @Test
    fun inlineMarksAreTakenOutAndLinksKeepTheirAddress() {
        assertEquals("fett kursiv code", MarkdownBlocks.inline("**fett** *kursiv* `code`"))
        assertEquals("Hain (https://hain.app)", MarkdownBlocks.inline("[Hain](https://hain.app)"))
        assertEquals("https://a.de", MarkdownBlocks.inline("[https://a.de](https://a.de)"))
        assertEquals("2 * 3 * 4", MarkdownBlocks.inline("2 * 3 * 4"))
        assertEquals("snake_case_name", MarkdownBlocks.inline("snake_case_name"))
    }

    @Test
    fun unclosedCodeFenceKeepsTheRestAsCode() {
        val blocks = MarkdownBlocks.parse("Text\n```\nx = 1\ny = 2")
        assertEquals(listOf("x = 1", "y = 2"), (blocks.last() as PdfBlock.Code).lines)
    }
}
