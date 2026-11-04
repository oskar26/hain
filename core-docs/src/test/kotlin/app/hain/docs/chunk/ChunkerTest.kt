package app.hain.docs.chunk

import app.hain.docs.api.DocPage
import app.hain.docs.internal.chunk.Chunker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChunkerTest {
    @Test
    fun shortPageIsOneChunkWithItsPage() {
        val chunks = Chunker.chunk(listOf(DocPage(1, "Kurz."), DocPage(2, "  "), DocPage(3, "Auch kurz.", ocr = true)))
        assertEquals(listOf(1, 3), chunks.map { it.page })
        assertEquals(listOf(0, 1), chunks.map { it.index })
        assertTrue(chunks[1].ocr)
    }

    @Test
    fun longPageIsCutAtParagraphsAndNeverAcrossPages() {
        val paragraph = "Das ist ein Satz über die Heizung. ".repeat(20).trim()
        val page = List(6) { paragraph }.joinToString("\n\n")
        val chunks = Chunker.chunk(listOf(DocPage(1, page), DocPage(2, paragraph)))
        assertTrue(chunks.count { it.page == 1 } > 1)
        assertTrue(chunks.all { it.text.length <= Chunker.MAX_CHARS })
        assertEquals(paragraph, chunks.last().text)
    }

    @Test
    fun piecesJoinedGiveThePageAgain() {
        val page = List(8) { i -> "Absatz $i: " + "Wort ".repeat(60).trim() }.joinToString("\n\n")
        val pieces = Chunker.pieces(page, 500)
        assertEquals(page, pieces.joinToString("\n\n"))
    }

    @Test
    fun veryLongSentenceIsCutAtSpaces() {
        val sentence = "wort ".repeat(1000).trim()
        val pieces = Chunker.pieces(sentence, 300)
        assertTrue(pieces.size > 1)
        assertTrue(pieces.all { it.length <= 300 })
        assertEquals(1000, pieces.sumOf { p -> p.split(' ').size })
    }
}
