package app.hain.llm.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThinkingPolicyTest {
    @Test
    fun fastAndThoroughIgnoreTheMessage() {
        assertFalse(ThinkingPolicy.decide("Warum ist der Himmel blau?", AnswerMode.FAST))
        assertTrue(ThinkingPolicy.decide("Hi", AnswerMode.THOROUGH))
    }

    @Test
    fun autoTurnsOnForQuestionWords() {
        listOf(
            "Warum ist der Himmel blau?",
            "Erklär mir Photosynthese",
            "Kannst du das erklären?",
            "Vergleiche Bus und Bahn",
            "Why is the sky blue?",
            "Please explain recursion",
            "Compare these two phones",
            "Mach einen Plan für Montag",
        ).forEach { assertTrue(it, ThinkingPolicy.decide(it, AnswerMode.AUTO)) }
    }

    @Test
    fun autoTurnsOnForArithmetic() {
        listOf("Was ist 17 * 23?", "12+5", "3 / 4 von 80", "Wie viel sind 15 % von 80?", "x = 4 - 2").forEach {
            assertTrue(it, ThinkingPolicy.decide(it, AnswerMode.AUTO))
        }
    }

    @Test
    fun autoTurnsOnForLongMessages() {
        assertTrue(ThinkingPolicy.decide("a".repeat(ThinkingPolicy.LONG_MESSAGE_CHARS + 1), AnswerMode.AUTO))
        assertFalse(ThinkingPolicy.decide("a ".repeat(ThinkingPolicy.LONG_MESSAGE_CHARS / 2), AnswerMode.AUTO))
    }

    @Test
    fun autoStaysOffForSmallTalk() {
        listOf(
            "Hallo Pim!",
            "Wie wird das Wetter morgen?",
            "Stell einen Wecker um 7:30",
            "Planet Erde",
            "Ich habe 3 Katzen",
        ).forEach { assertFalse(it, ThinkingPolicy.decide(it, AnswerMode.AUTO)) }
    }

    @Test
    fun answerModeKeysFallBackToAuto() {
        assertEquals(AnswerMode.FAST, AnswerMode.fromKey("fast"))
        assertEquals(AnswerMode.THOROUGH, AnswerMode.fromKey("thorough"))
        assertEquals(AnswerMode.AUTO, AnswerMode.fromKey(null))
        assertEquals(AnswerMode.AUTO, AnswerMode.fromKey("bogus"))
    }
}
