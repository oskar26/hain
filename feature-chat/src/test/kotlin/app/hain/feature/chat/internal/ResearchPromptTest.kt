package app.hain.feature.chat.internal

import app.hain.llm.internal.AnswerMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-275: the research lines ask for a direct answer with backed facts, no talk about sources. */
class ResearchPromptTest {
    @Test
    fun finalAndThoroughLinesAskForADirectAnswerWithoutMetaTalk() {
        listOf("de", "en").forEach { lang ->
            val final = ChatEngine.finalHint(lang)
            val thorough = ChatEngine.researchHint(AnswerMode.THOROUGH, lang).orEmpty()
            assertTrue(final.contains("[1]"))
            assertTrue(final.contains("direkt") || final.contains("directly"))
            listOf("nenne Widersprüche", "sag, wenn Quellen", "point out contradictions", "say when sources")
                .forEach { assertFalse(it, final.contains(it) || thorough.contains(it)) }
        }
    }
}
