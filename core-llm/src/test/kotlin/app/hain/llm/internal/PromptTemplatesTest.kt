package app.hain.llm.internal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PromptTemplatesTest {
    @Test
    fun sectionsAndComments() {
        val t =
            PromptTemplates.parse(
                "// comment\n[tools]\n# Werkzeuge\n- a\n\n" +
                    "[nudge]\n// hidden\nGo.\n[offline]\nOff.\n",
            )
        assertEquals("# Werkzeuge\n- a", t[PromptTemplates.TOOLS])
        assertEquals("Go.", t[PromptTemplates.NUDGE])
        assertEquals("", t["missing"])
        assertEquals("# Werkzeuge\n- a", t.toolRules(offline = false))
        assertEquals("Off.", t.toolRules(offline = true))
    }

    @Test
    fun examplesFollowTheRulesOnline() {
        val t = PromptTemplates.parse("[tools]\nR\n[examples]\nE\n")
        assertEquals("R\n\nE", t.toolRules(offline = false))
    }

    @Test
    fun shippedFilesHaveAllSectionsAndFitTheBudget() {
        for (lang in listOf("de", "en")) {
            val text = File("src/main/assets/prompts/tools.$lang.txt").readText()
            val t = PromptTemplates.parse(text)
            listOf(PromptTemplates.TOOLS, PromptTemplates.NUDGE, PromptTemplates.OFFLINE)
                .forEach { assertTrue("$lang: $it", t[it].isNotBlank()) }
            assertFalse(lang, t.toolRules(offline = false).contains("//"))
            // ctx 2048: rules + examples ≈ chars / 3.5 tokens; keep them under ~450 tokens
            assertTrue("$lang: ${t.toolRules(false).length}", t.toolRules(offline = false).length < 1600)
            assertTrue(lang, t[PromptTemplates.NUDGE].length < 140)
        }
    }

    @Test
    fun rulesNameEveryToolAndNoQuotedReplies() {
        val tools = listOf("weather", "wikipedia", "web_search", "read_page", "calculate", "set_alarm", "set_timer")
        for (lang in listOf("de", "en")) {
            val text = File("src/main/assets/prompts/tools.$lang.txt").readText()
            val rules = PromptTemplates.parse(text).toolRules(false)
            tools.forEach { assertTrue("$lang: $it", rules.contains(it)) }
            // host eval 4d: a quoted reply ("vorbereitet, tipp auf die Karte") was copied instead of calling the tool
            assertFalse(lang, rules.contains("„") || rules.contains("\""))
        }
    }
}
