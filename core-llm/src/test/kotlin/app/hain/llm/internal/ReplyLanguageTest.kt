package app.hain.llm.internal

import app.hain.common.api.AnswerLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

class ReplyLanguageTest {
    @Test
    fun detectsGerman() {
        listOf(
            "wie geht es dir",
            "Erklär mir Photosynthese",
            "Was kann ich mit Lauch kochen?",
            "kannst du mir bei Hausaufgaben helfen",
            "Gib mir drei Tipps zum Lernen",
            "Wer bist du?",
            "Hallo",
            "Größe?",
        ).forEach { assertEquals(it, ReplyLanguage.DE, ReplyLanguagePolicy.detect(it)) }
    }

    @Test
    fun detectsEnglish() {
        listOf(
            "Hello, how are you?",
            "What can I cook with leeks?",
            "Can you help me with my homework?",
            "Explain photosynthesis",
            "I'm tired today",
        ).forEach { assertEquals(it, ReplyLanguage.EN, ReplyLanguagePolicy.detect(it)) }
    }

    @Test
    fun unclearMessagesAreLeftToTheDevice() {
        listOf("Hi", "ok", "Photosynthese", "", "123").forEach { assertNull(it, ReplyLanguagePolicy.detect(it)) }
        assertEquals(
            ReplyLanguage.DE,
            ReplyLanguagePolicy.replyLanguage(AnswerLanguage.AUTO, "Hi", ReplyLanguage.DE),
        )
        assertEquals(
            ReplyLanguage.EN,
            ReplyLanguagePolicy.replyLanguage(AnswerLanguage.AUTO, "Hi", ReplyLanguage.EN),
        )
    }

    @Test
    fun fixedSettingWinsOverTheMessage() {
        assertEquals(
            ReplyLanguage.DE,
            ReplyLanguagePolicy.replyLanguage(AnswerLanguage.DE, "How are you?", ReplyLanguage.EN),
        )
        assertEquals(
            ReplyLanguage.EN,
            ReplyLanguagePolicy.replyLanguage(AnswerLanguage.EN, "wie geht es dir", ReplyLanguage.DE),
        )
        assertEquals(
            ReplyLanguage.EN,
            ReplyLanguagePolicy.replyLanguage(AnswerLanguage.AUTO, "How are you?", ReplyLanguage.DE),
        )
    }

    @Test
    fun promptLanguageFollowsSettingThenDevice() {
        assertEquals(ReplyLanguage.DE, ReplyLanguagePolicy.promptLanguage(AnswerLanguage.AUTO, ReplyLanguage.DE))
        assertEquals(ReplyLanguage.EN, ReplyLanguagePolicy.promptLanguage(AnswerLanguage.AUTO, ReplyLanguage.EN))
        assertEquals(ReplyLanguage.DE, ReplyLanguagePolicy.promptLanguage(AnswerLanguage.DE, ReplyLanguage.EN))
        assertEquals(ReplyLanguage.EN, ReplyLanguagePolicy.promptLanguage(AnswerLanguage.EN, ReplyLanguage.DE))
        assertEquals(ReplyLanguage.DE, ReplyLanguage.fromLocale(Locale.GERMANY))
        assertEquals(ReplyLanguage.DE, ReplyLanguage.fromLocale(Locale.forLanguageTag("de-AT")))
        assertEquals(ReplyLanguage.EN, ReplyLanguage.fromLocale(Locale.FRANCE))
    }

    @Test
    fun rulesAndHintsAreInThePromptLanguage() {
        val german = ReplyLanguagePolicy.rule(ReplyLanguage.DE, AnswerLanguage.DE)
        assertTrue(german.startsWith("Antworte immer nur auf Deutsch"))
        assertTrue(ReplyLanguagePolicy.rule(ReplyLanguage.DE, AnswerLanguage.AUTO).contains("mische nie"))
        assertTrue(ReplyLanguagePolicy.rule(ReplyLanguage.EN, AnswerLanguage.EN).startsWith("Always answer in English"))
        assertTrue(ReplyLanguagePolicy.rule(ReplyLanguage.EN, AnswerLanguage.AUTO).contains("never mix"))
        assertEquals("Antworte auf Deutsch.", ReplyLanguagePolicy.turnHint(ReplyLanguage.DE))
        assertEquals("Answer in English.", ReplyLanguagePolicy.turnHint(ReplyLanguage.EN))
    }

    @Test
    fun germanPromptIsGermanAndEndsWithTheLanguageRule() {
        val file =
            generateSequence(File("").absoluteFile) { it.parentFile }
                .map { File(it, "prompts/system_full.de.txt") }
                .first { it.exists() }
        val now = ZonedDateTime.of(2026, 11, 3, 7, 0, 0, 0, ZoneId.of("Europe/Berlin"))
        val rule = ReplyLanguagePolicy.rule(ReplyLanguage.DE, AnswerLanguage.DE)
        val filled =
            SystemPromptFiller.fill(
                file.readText(),
                SystemPromptVars.of(now, Locale.GERMANY, "DE").copy(languageRule = rule),
            )
        assertFalse(filled, Regex("""\{[a-z_]+\}""").containsMatchIn(filled))
        assertTrue(filled.startsWith("Du bist Pim"))
        assertTrue(filled.contains("Jetzt: Dienstag, 3. November 2026, 07:00 (Europe/Berlin)."))
        assertTrue(filled, filled.trimEnd().endsWith("# Sprache\n$rule"))
        assertFalse("no English headings", filled.contains("# Context") || filled.contains("You are"))
    }
}
