package app.hain.docs.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecDictionaryTest {
    private val yaml = checkNotNull(javaClass.classLoader?.getResource("fixtures/rec_latin.yml")).readText()

    @Test
    fun readsTheLatinModelDictionary() {
        val dict = RecDictionary.parseYaml(yaml)
        assertEquals(836, dict.size)
        assertEquals("0", dict[0])
        assertTrue(dict.containsAll(listOf("ä", "ö", "ü", "Ä", "Ö", "Ü", "ß", "€", "'", "\"", "#", ":")))
    }

    @Test
    fun classesHaveBlankFirstAndSpaceLast() {
        val classes = RecDictionary.classes(listOf("a", "b"))
        assertEquals(listOf("", "a", "b", " "), classes)
    }

    @Test
    fun yamlScalars() {
        assertEquals("'", RecDictionary.value("''''"))
        assertEquals("#", RecDictionary.value("'#'"))
        assertEquals("\"", RecDictionary.value("'\"'"))
        assertEquals("\\", RecDictionary.value("\"\\\\\""))
        assertEquals("é", RecDictionary.value("\"\\u00e9\""))
        assertEquals("ß", RecDictionary.value("ß"))
    }

    @Test
    fun stopsAtTheEndOfTheList() {
        val dict = RecDictionary.parseYaml("PostProcess:\n  character_dict:\n  - a\n  - 'b'\n  name: x\n")
        assertEquals(listOf("a", "b"), dict)
    }
}
