package app.hain.feature.chat.export

import app.hain.feature.chat.internal.Chat
import app.hain.feature.chat.internal.UiMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfLayoutTest {
    /** Every character is 5 points wide: 483 points of A4 content width hold 96 characters. */
    private val measure: (PdfStyle, String) -> Float = { _, text -> text.length * CHAR }
    private val metrics = PdfMetrics()

    private fun lines(pages: List<PdfPage>) = pages.flatMap { p -> p.items.filterIsInstance<PdfItem.Line>() }

    @Test
    fun wrapKeepsWordsTogetherAndCutsOnlyTooLongWords() {
        val width: (String) -> Float = { it.length * CHAR }
        assertEquals(listOf("eins zwei", "drei"), PdfLayout.wrap("eins  zwei\ndrei", 9 * CHAR, width))
        assertEquals(listOf("abcde", "fghij", "k"), PdfLayout.wrap("abcdefghijk", 5 * CHAR, width))
        assertEquals(emptyList<String>(), PdfLayout.wrap("   ", 5 * CHAR, width))
    }

    @Test
    fun codeWrapsByCharacterAndKeepsIndent() {
        val width: (String) -> Float = { it.length * CHAR }
        assertEquals(listOf("    val", " x = 1"), PdfLayout.wrapCode("\tval x = 1", 7 * CHAR, width))
        assertEquals(listOf(""), PdfLayout.wrapCode("   ", 7 * CHAR, width))
    }

    @Test
    fun shortChatIsOnePageWithTitleRolesAndAccentRule() {
        val chat =
            Chat(
                1,
                "Hallo",
                listOf(UiMessage(2, true, "Hi"), UiMessage(3, false, "Hallo **du**")),
                updatedAt = 1,
            )
        val pages = PdfLayout.paginate(PdfLayout.blocks(chat, ExportLabels(you = "Du")), metrics, measure)
        assertEquals(1, pages.size)
        val texts = lines(pages).map { it.style to it.text }
        assertEquals(
            listOf(
                PdfStyle.TITLE to "Hallo",
                PdfStyle.ROLE_USER to "Du",
                PdfStyle.BODY to "Hi",
                PdfStyle.ROLE_HAIN to "Hain",
                PdfStyle.BODY to "Hallo du",
            ),
            texts,
        )
        assertTrue(pages[0].items.any { it is PdfItem.Rule && it.accent })
    }

    @Test
    fun longAnswerBreaksIntoPagesAndNothingLeavesThePage() {
        val paragraph = List(PARAGRAPH_WORDS) { "Wort$it" }.joinToString(" ")
        val text = List(PARAGRAPHS) { paragraph }.joinToString("\n\n")
        val chat = Chat(1, "Lang", listOf(UiMessage(2, true, "Frage"), UiMessage(3, false, text)), updatedAt = 1)
        val pages = PdfLayout.paginate(PdfLayout.blocks(chat, ExportLabels()), metrics, measure)
        assertTrue(pages.size >= 3)
        assertEquals((1..pages.size).toList(), pages.map { it.number })
        pages.forEach { page ->
            page.items.filterIsInstance<PdfItem.Line>().forEach { line ->
                assertTrue(line.top >= metrics.margin)
                assertTrue(line.top + line.height <= metrics.contentBottom)
                assertTrue(measure(line.style, line.text) <= metrics.contentWidth)
            }
        }
        // every word is there exactly once, in order
        val words = lines(pages).filter { it.style == PdfStyle.BODY }.flatMap { it.text.split(" ") }.drop(1)
        assertEquals(PARAGRAPH_WORDS * PARAGRAPHS, words.size)
        assertEquals("Wort0", words.first())
    }

    @Test
    fun roleLabelIsNeverAloneAtTheBottomOfAPage() {
        val filler = List(FILLER_LINES) { "Zeile $it" }.joinToString("\n\n")
        val messages = listOf(UiMessage(2, true, filler), UiMessage(3, false, "Antwort"))
        val pages = PdfLayout.paginate(PdfLayout.blocks(Chat(1, "T", messages, 1), ExportLabels()), metrics, measure)
        pages.forEach { page ->
            val last = page.items.filterIsInstance<PdfItem.Line>().last()
            assertTrue(last.style != PdfStyle.ROLE_HAIN && last.style != PdfStyle.ROLE_USER)
        }
        val hainPage = pages.first { p -> p.items.any { it is PdfItem.Line && it.style == PdfStyle.ROLE_HAIN } }
        assertTrue(hainPage.items.any { it is PdfItem.Line && it.text == "Antwort" })
    }

    @Test
    fun longCodeBlockContinuesOnTheNextPageWithItsBackground() {
        val code = List(CODE_LINES) { "line $it" }.joinToString("\n")
        val chat = Chat(1, "Code", listOf(UiMessage(2, false, "```\n$code\n```")), updatedAt = 1)
        val pages = PdfLayout.paginate(PdfLayout.blocks(chat, ExportLabels()), metrics, measure)
        assertTrue(pages.size >= 2)
        val codeLines = lines(pages).filter { it.style == PdfStyle.CODE }
        assertEquals(CODE_LINES, codeLines.size)
        pages.filter { p -> p.items.any { it is PdfItem.Line && it.style == PdfStyle.CODE } }.forEach { page ->
            val boxes = page.items.filterIsInstance<PdfItem.CodeBox>()
            val lineCount = page.items.count { it is PdfItem.Line && it.style == PdfStyle.CODE }
            assertEquals(lineCount, boxes.size)
            boxes.forEach { assertTrue(it.top + it.height <= metrics.contentBottom + metrics.codePadding) }
        }
    }

    @Test
    fun emptyChatStillGivesOnePage() {
        val pages = PdfLayout.paginate(emptyList(), metrics, measure)
        assertEquals(1, pages.size)
    }

    private companion object {
        const val CHAR = 5f
        const val PARAGRAPH_WORDS = 120
        const val PARAGRAPHS = 12
        const val FILLER_LINES = 41
        const val CODE_LINES = 120
    }
}
