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
            ReplyLanguagePolicy.replyLanguage(AnswerLanguage.AUTO, listOf("Hi"), MessageLanguage.GERMAN).prompt,
        )
        assertEquals(
            ReplyLanguage.EN,
            ReplyLanguagePolicy.replyLanguage(AnswerLanguage.AUTO, listOf("Hi"), MessageLanguage.ENGLISH).prompt,
        )
    }

    @Test
    fun fixedSettingWinsOverTheMessage() {
        assertEquals(
            ReplyLanguage.DE,
            target(AnswerLanguage.DE, "How are you?", MessageLanguage.ENGLISH),
        )
        assertEquals(
            ReplyLanguage.EN,
            target(AnswerLanguage.EN, "wie geht es dir", MessageLanguage.GERMAN),
        )
        assertEquals(
            ReplyLanguage.EN,
            target(AnswerLanguage.AUTO, "How are you?", MessageLanguage.GERMAN),
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
        assertEquals("Antworte auf Deutsch.", ReplyLanguagePolicy.turnHint(ReplyLanguage.DE, MessageLanguage.GERMAN))
        assertEquals("Answer in English.", ReplyLanguagePolicy.turnHint(ReplyLanguage.EN, MessageLanguage.ENGLISH))
        val turkish = checkNotNull(ReplyLanguagePolicy.detectLanguage("Merhaba, nasılsın?"))
        assertEquals("Antworte auf Türkisch.", ReplyLanguagePolicy.turnHint(ReplyLanguage.DE, turkish))
        assertEquals("Answer in Turkish.", ReplyLanguagePolicy.turnHint(ReplyLanguage.EN, turkish))
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

    @Test
    fun detectsOtherLanguages() {
        mapOf(
            "Salut, comment ça va ?" to "fr",
            "¿Qué puedo cocinar con puerros?" to "es",
            "Ciao, come stai?" to "it",
            "Merhaba, nasılsın?" to "tr",
            "Hoi, hoe gaat het met je?" to "nl",
            "Cześć, jak się masz?" to "pl",
            "Olá, tudo bem com você?" to "pt",
            "Привет, как дела?" to "ru",
            "Привіт, як справи? Їжак" to "uk",
            "こんにちは、元気ですか" to "ja",
            "你好，你好吗" to "zh",
            "안녕하세요" to "ko",
            "مرحبا كيف حالك" to "ar",
        ).forEach { (text, tag) -> assertEquals(text, tag, ReplyLanguagePolicy.detectLanguage(text)?.tag) }
    }

    @Test
    fun shortMessagesKeepTheLanguageOfTheChat() {
        val target =
            ReplyLanguagePolicy.replyLanguage(
                AnswerLanguage.AUTO,
                listOf("Salut, comment ça va ?", "ok"),
                MessageLanguage.GERMAN,
            )
        assertEquals("fr", target.tag)
        assertEquals("Französisch", target.nameDe)
        assertEquals("fr", MessageLanguage.fromLocale(Locale.FRANCE).tag)
        assertEquals("French", MessageLanguage.fromLocale(Locale.FRANCE).nameEn)
    }

    private fun target(
        setting: AnswerLanguage,
        message: String,
        device: MessageLanguage,
    ): ReplyLanguage = ReplyLanguagePolicy.replyLanguage(setting, listOf(message), device).prompt
}
