package app.hain.feature.chat.api

import app.hain.ui.components.Hain3D
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** T-091: big, one-line suggestion pools in both languages, rotating three at a time. */
class SuggestionsTest {
    private fun pool(values: String): List<String> {
        val file =
            generateSequence(File("").absoluteFile) { it.parentFile }
                .map { File(it, "feature-chat/src/main/res/$values/strings.xml") }
                .plus(sequenceOf(File("src/main/res/$values/strings.xml")))
                .first { it.exists() }
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val arrays = doc.getElementsByTagName("string-array")
        val array =
            (0 until arrays.length)
                .map { arrays.item(it) }
                .single { it.attributes.getNamedItem("name").nodeValue == "chat_suggestions" }
        val items = array.childNodes
        return (0 until items.length)
            .map { items.item(it) }
            .filter { it.nodeName == "item" }
            .map { it.textContent.replace("\\'", "'") }
    }

    @Test
    fun `at least 60 short one-line suggestions per language, all with a known icon`() {
        listOf("values", "values-de").forEach { lang ->
            val raw = pool(lang)
            assertTrue("$lang has ${raw.size}", raw.size >= 60)
            val parsed = parseSuggestions(raw.toTypedArray())
            assertEquals(raw.size, parsed.size)
            assertTrue(raw.all { it.substringBefore('|') in KNOWN })
            parsed.forEach { assertTrue("too long for one line: ${it.text}", it.text.length <= 32) }
            assertEquals(parsed.size, parsed.map { it.text }.toSet().size)
        }
    }

    @Test
    fun `rotation shows the next three each round and wraps around`() {
        val pool = parseSuggestions(Array(7) { "leaf|Tipp $it" })
        assertEquals("Tipp 0", suggestionAt(pool, 0, 0).text)
        assertEquals("Tipp 3", suggestionAt(pool, 1, 0).text)
        assertEquals("Tipp 2", suggestionAt(pool, 3, 0).text)
        assertNotEquals(suggestionAt(pool, 0, 1), suggestionAt(pool, 1, 1))
        assertEquals(Hain3D.SPARKLES, parseSuggestions(arrayOf("unknown|Hallo")).single().icon)
    }

    private companion object {
        val KNOWN =
            setOf(
                "sparkles",
                "leaf",
                "bubble",
                "recycle",
                "heart",
                "bulb",
                "weather",
                "book",
                "phone",
                "calendar",
                "gift",
                "calc",
                "globe",
            )
    }
}
