package app.hain.tools.wiki

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.GateResponse
import app.hain.tools.api.ToolContext
import app.hain.tools.internal.net.HttpNetworkGate
import app.hain.tools.internal.wiki.WikiParser
import app.hain.tools.internal.wiki.WikipediaTool
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZonedDateTime

class WikipediaToolTest {
    private val search = """["Kölner Dom",["Kölner Dom"],[""],["https://de.wikipedia.org/wiki/K%C3%B6lner_Dom"]]"""
    private val summary =
        """{"type":"standard","title":"Kölner Dom","extract":"Der Kölner Dom ist eine römisch-katholische Kirche.",""" +
            """"content_urls":{"desktop":{"page":"https://de.wikipedia.org/wiki/K%C3%B6lner_Dom"}}}"""

    @Test
    fun parsers() {
        assertEquals("Kölner Dom", WikiParser.firstTitle(search))
        assertNull(WikiParser.firstTitle("""["x",[],[],[]]"""))
        val s = WikiParser.summary(summary)!!
        assertEquals("Kölner Dom", s.title)
        assertTrue(s.extract.startsWith("Der Kölner Dom"))
        assertNull(WikiParser.summary("{}"))
    }

    @Test
    fun urls() {
        assertEquals(
            "https://de.wikipedia.org/api/rest_v1/page/summary/K%C3%B6lner_Dom",
            WikipediaTool.summaryUrl("Kölner Dom", "de"),
        )
        assertTrue(WikipediaTool.searchUrl("Ada Lovelace", "en").startsWith("https://en.wikipedia.org/w/api.php?"))
    }

    @Test
    fun runsInAppLanguage() =
        runTest {
            val seen = mutableListOf<GateRequest>()
            val gate =
                HttpNetworkGate(transport = { r ->
                    seen += r
                    GateResponse(200, if (r.url.contains("opensearch")) search else summary)
                })
            val ctx = ToolContext("de", ZonedDateTime.now(), false)
            val r = WikipediaTool(gate).run(mapOf("topic" to "Kölner Dom"), ctx)
            assertEquals("Kölner Dom", r.label)
            assertEquals("Wikipedia: Kölner Dom", r.sources.single().title)
            assertTrue(seen.all { it.url.startsWith("https://de.wikipedia.org/") })
            assertTrue(r.forModel.contains("\"extract\""))
        }
}
