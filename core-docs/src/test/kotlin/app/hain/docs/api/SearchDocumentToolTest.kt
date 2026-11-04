package app.hain.docs.api

import app.hain.docs.internal.search.DocSearch
import app.hain.docs.internal.search.SearchChunk
import app.hain.tools.api.BadArgumentsException
import app.hain.tools.api.ToolContext
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class SearchDocumentToolTest {
    private val chunks =
        listOf(
            SearchChunk("Mietvertrag.pdf", 1, "Der Mietvertrag beginnt am 1. März. Die Miete beträgt 820 Euro."),
            SearchChunk("Mietvertrag.pdf", 2, "Haustiere sind nach Absprache erlaubt."),
            SearchChunk("Mietvertrag.pdf", 3, "Die Heizung wird jedes Jahr im Oktober gewartet."),
            SearchChunk("Mietvertrag.pdf", 4, "Kündigungsfrist: drei Monate zum Monatsende."),
            SearchChunk("Rechnung.pdf", 1, "Heizungswartung 2026: 249,90 Euro, fällig am 15.11.2026."),
        )

    private val tool = SearchDocumentTool { ids -> if (ids.isEmpty()) emptyList() else chunks }

    private fun ctx(docs: List<Long> = listOf(1L)): ToolContext {
        val now = ZonedDateTime.now()
        return ToolContext("de", now, offline = true, documents = docs)
    }

    @Test
    fun bestPassagesComeWithTheirPage() =
        runTest {
            val result = tool.run(mapOf("query" to "Wann wird die Heizung gewartet?"), ctx())
            val results =
                Json
                    .parseToJsonElement(result.forModel)
                    .jsonObject
                    .getValue("results")
                    .jsonArray
            val first = results.first().jsonObject
            assertEquals(3, first.getValue("page").jsonPrimitive.int)
            assertTrue(results.size <= DocSearch.TOP)
        }

    @Test
    fun documentNameNarrowsTheSearch() =
        runTest {
            val result = tool.run(mapOf("query" to "Heizung", "document" to "rechnung"), ctx())
            val results =
                Json
                    .parseToJsonElement(result.forModel)
                    .jsonObject
                    .getValue("results")
                    .jsonArray
            assertTrue(
                results.all {
                    it.jsonObject
                        .getValue("document")
                        .jsonPrimitive.content == "Rechnung.pdf"
                },
            )
        }

    @Test
    fun nothingFitsGivesANoteNotAnError() =
        runTest {
            val result = tool.run(mapOf("query" to "Raumschiff"), ctx())
            assertEquals(null, result.error)
            assertTrue("note" in result.forModel)
        }

    @Test
    fun noDocumentAttached() =
        runTest {
            val result = tool.run(mapOf("query" to "Heizung"), ctx(emptyList()))
            assertEquals("no_document", result.error)
        }

    @Test(expected = BadArgumentsException::class)
    fun queryIsRequired() =
        runTest {
            tool.run(emptyMap(), ctx())
            Unit
        }

    @Test
    fun offlineTool() {
        assertEquals(false, tool.online)
        assertEquals("search_document", tool.schema.name)
    }

    @Test
    fun atMostFourBest() {
        val many = List(10) { SearchChunk("a", it + 1, "Heizung Wartung Termin $it") }
        assertEquals(4, DocSearch.best(many, "Heizung").size)
    }
}
