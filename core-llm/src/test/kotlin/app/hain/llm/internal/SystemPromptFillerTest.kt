package app.hain.llm.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class SystemPromptFillerTest {
    private val now = ZonedDateTime.of(2026, 10, 28, 14, 5, 0, 0, ZoneId.of("Europe/Berlin"))
    private val template =
        "Help. In danger call {emergency_number}.\n\n# Context\n" +
            "Now: {weekday}, {date}, {time} ({timezone}). Place: {city}.\n" +
            "About the user: {memory_snippets}\n{user_custom_instructions}\n\n\nEnd\n"

    @Test
    fun germanDateAndTime() {
        val vars = SystemPromptVars.of(now, Locale.GERMANY, "DE").copy(city = "Köln")
        assertEquals("Mittwoch", vars.weekday)
        assertEquals("28. Oktober 2026", vars.date)
        assertEquals("14:05", vars.time)
        val filled = SystemPromptFiller.fill(template, vars)
        assertTrue(filled, filled.contains("Now: Mittwoch, 28. Oktober 2026, 14:05 (Europe/Berlin). Place: Köln."))
        assertTrue(filled.contains("call 112."))
    }

    @Test
    fun englishDateAndTime() {
        val vars = SystemPromptVars.of(now, Locale.UK, "GB")
        assertEquals("Wednesday", vars.weekday)
        assertEquals("28 October 2026", vars.date)
        assertEquals("14:05", vars.time)
        assertTrue(SystemPromptFiller.fill(template, vars).contains("call 999."))
    }

    @Test
    fun missingValuesAreRemovedAndBlankLinesCollapsed() {
        val filled = SystemPromptFiller.fill(template, SystemPromptVars())
        assertFalse(filled, filled.contains("{"))
        assertFalse(filled, filled.contains("\n\n\n"))
        // T-262: an empty labelled value goes with its label
        assertFalse(filled, filled.contains("Place:"))
        assertFalse(filled, filled.contains("About the user:"))
        assertTrue(filled, filled.contains("(Europe/Berlin).") || filled.contains("()."))
        assertTrue(filled.endsWith("End\n"))
    }

    @Test
    fun emergencyNumbers() {
        assertEquals("911", EmergencyNumbers.forCountry("US"))
        assertEquals("911", EmergencyNumbers.forCountry("ca"))
        assertEquals("999", EmergencyNumbers.forCountry("GB"))
        assertEquals("112", EmergencyNumbers.forCountry("DE"))
        assertEquals("112", EmergencyNumbers.forCountry("AT"))
        assertEquals("112", EmergencyNumbers.forCountry(null))
    }

    @Test
    fun fillsTheRealSystemPrompt() {
        val file =
            generateSequence(java.io.File("").absoluteFile) { it.parentFile }
                .map { java.io.File(it, "prompts/system_full.en.txt") }
                .first { it.exists() }
        val vars = SystemPromptVars.of(now, Locale.GERMANY, "DE").copy(city = "Köln", memorySnippets = "mag Grün")
        val filled = SystemPromptFiller.fill(file.readText(), vars)
        assertFalse(filled, Regex("""\{[a-z_]+\}""").containsMatchIn(filled))
        assertTrue(filled.contains("mag Grün"))
    }
}
