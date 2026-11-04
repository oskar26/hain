package app.hain.docs.ephemeral

import app.hain.docs.api.DocInfo
import app.hain.docs.api.DocKind
import app.hain.docs.api.DocPage
import app.hain.docs.internal.ephemeral.EphemeralDocs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EphemeralDocsTest {
    private val info = DocInfo(id = 99, name = "Zeugnis.pdf", kind = DocKind.PDF, pages = 2, chars = 20, addedAt = 1)
    private val pages = listOf(DocPage(1, "Mathe: 2"), DocPage(2, "Deutsch: 1"))

    @Test
    fun keptFilesGetNegativeIdsAndKeepTheirPages() {
        val docs = EphemeralDocs()
        val a = docs.add(info, pages)
        val b = docs.add(info.copy(name = "b.txt"), listOf(DocPage(1, "b")))
        assertTrue(a.id < 0 && b.id < 0)
        assertTrue(a.id != b.id)
        assertTrue(EphemeralDocs.isEphemeral(a.id))
        assertFalse(EphemeralDocs.isEphemeral(info.id))
        assertEquals(pages, docs.pages(a.id))
        assertEquals(listOf("Zeugnis.pdf"), docs.info(listOf(a.id, 5L)).map { it.name })
    }

    @Test
    fun searchFindsTheTextWithItsPage() {
        val docs = EphemeralDocs()
        val a = docs.add(info, pages)
        val chunks = docs.chunks(listOf(a.id))
        assertEquals(listOf(1, 2), chunks.map { it.page })
        assertTrue(chunks.all { it.document == "Zeugnis.pdf" })
    }

    @Test
    fun clearForgetsEverything() {
        val docs = EphemeralDocs()
        val a = docs.add(info, pages)
        docs.add(info, pages)
        docs.remove(a.id)
        assertEquals(1, docs.size)
        docs.clear()
        assertEquals(0, docs.size)
        assertTrue(docs.pages(a.id).isEmpty())
        assertTrue(docs.chunks(listOf(a.id)).isEmpty())
    }
}
