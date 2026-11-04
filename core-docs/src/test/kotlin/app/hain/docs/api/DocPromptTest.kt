package app.hain.docs.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DocPromptTest {
    private fun doc(
        id: Long,
        tokens: Int,
        pages: Int = 2,
    ) = DocAttachment(id, "Datei$id.pdf", DocKind.PDF, pages, tokens)

    @Test
    fun smallDocumentsGoInWholeUntilTheBudgetIsUsed() {
        val plan = DocPrompt.plan(listOf(doc(1, 1000), doc(2, 1800), doc(3, 500), doc(1, 1000)))
        assertEquals(listOf(1L, 2L), plan.inline.map { it.id })
        assertEquals(listOf(3L), plan.search.map { it.id })
    }

    @Test
    fun bigDocumentIsSearched() {
        val plan = DocPrompt.plan(listOf(doc(1, 3001)))
        assertTrue(plan.inline.isEmpty())
        assertEquals(1, plan.search.size)
    }

    @Test
    fun wholeDocumentHasPageMarks() {
        val text =
            DocPrompt.inline(
                "Rechnung \"März\".pdf",
                listOf(DocPage(1, "Betrag 249,90 €"), DocPage(2, ""), DocPage(3, "Heizung")),
            )
        assertTrue(text.startsWith("<document name=\"Rechnung 'März'.pdf\">"))
        assertTrue("[S. 1]\nBetrag 249,90 €" in text)
        assertFalse("[S. 2]" in text)
        assertTrue("[S. 3]\nHeizung" in text)
        assertTrue(text.endsWith("</document>"))
    }

    @Test
    fun onePageHasNoPageMark() {
        val text = DocPrompt.inline("notiz.txt", listOf(DocPage(1, "Einkaufen")))
        assertEquals("<document name=\"notiz.txt\">\nEinkaufen\n</document>", text)
    }

    @Test
    fun longDocumentOnlyNamesTheTool() {
        val note = DocPrompt.searchNote(doc(5, 9000, pages = 40))
        assertTrue("pages=\"40\"" in note)
        assertTrue("search_document" in note)
    }

    @Test
    fun tokensAreRoughlyFourCharacters() {
        val info = DocInfo(1, "a", DocKind.TEXT, 1, chars = 4001, addedAt = 0)
        assertEquals(1001, info.tokens)
    }
}
