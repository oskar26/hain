package app.hain.feature.news.api

import app.hain.tools.internal.news.Article
import app.hain.tools.internal.news.ArticleBlock
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-078: questions about the open article, cut to what fits into the brain's context. */
class ArticleAskTest {
    private fun article(paragraphs: Int) =
        Article(
            url = "https://example.org/haushalt",
            title = "Bundestag beschließt Haushalt",
            siteName = "tagesschau",
            byline = "Anna Beispiel",
            leadImage = null,
            blocks =
                (1..paragraphs).map { i ->
                    ArticleBlock.Paragraph("Absatz $i: Der Bundestag hat heute lange über den Haushalt gestritten.")
                } + ArticleBlock.Image("https://example.org/bild.jpg"),
            locked = false,
        )

    @Test fun budgetFollowsTheContextAndStaysInItsBounds() {
        // 4096 tokens: what is left after instructions, question and answer, about 3 characters each
        assertEquals((4096 - ArticleAsk.RESERVED_TOKENS) * ArticleAsk.CHARS_PER_TOKEN, ArticleAsk.budgetChars(4096))
        // a bigger context gives more article text, but never more than the cap
        assertTrue(ArticleAsk.budgetChars(8192) > ArticleAsk.budgetChars(4096))
        assertEquals(ArticleAsk.MAX_ARTICLE_CHARS, ArticleAsk.budgetChars(131_072))
        // a tiny context still sends a useful piece of the article
        assertEquals(ArticleAsk.MIN_ARTICLE_CHARS, ArticleAsk.budgetChars(512))
    }

    @Test fun longTextIsCutAtAParagraphEnd() {
        val text = (1..40).joinToString("\n\n") { "Absatz $it mit etwas Text, der den Haushalt erklärt." }
        val cut = ArticleAsk.trimmed(text, 600)
        assertTrue(cut.length <= 600 + 2)
        assertTrue(cut.endsWith("erklärt. …"))
        // short text stays as it is
        assertEquals("Kurz.", ArticleAsk.trimmed("Kurz.", 600))
    }

    @Test fun withoutParagraphsItCutsAtASentence() {
        val text = "Das ist ein Satz über den Haushalt. ".repeat(40).trim()
        val cut = ArticleAsk.trimmed(text, 300)
        assertTrue(cut.length <= 302)
        assertTrue(cut.endsWith("Haushalt. …"))
    }

    @Test fun userTextHasTitleSourceTextAndTheQuestionLast() {
        val text = ArticleAsk.userText(article(3), "Worum geht es?", 10_000)
        assertTrue(text.startsWith("Bundestag beschließt Haushalt\ntagesschau · Anna Beispiel"))
        assertTrue(text.contains("Absatz 3:"))
        assertTrue(text.endsWith("---\nWorum geht es?"))
    }

    @Test fun theBrainNeverGetsMoreThanTheBudget() =
        runTest {
            var sent = ""
            val small = 2048
            val ask =
                ArticleAsk(BrainCall { _, user -> "<think>hm</think> Es geht um den Haushalt.".also { sent = user } }) {
                    small
                }
            val result = ask.ask(article(400), "  Worum geht es?  ", "de")
            assertEquals(AskResult.Answer("Es geht um den Haushalt."), result)
            val articlePart = sent.substringAfter("\n\n").substringBefore("\n\n---\n")
            assertTrue(articlePart.length <= ArticleAsk.budgetChars(small) + 2)
            assertTrue(sent.endsWith("Worum geht es?"))
        }

    @Test fun noBrainEmptyQuestionAndErrorsAreSaidPlainly() =
        runTest {
            assertEquals(AskResult.NoBrain, ArticleAsk(BrainCall { _, _ -> null }).ask(article(2), "Was?", "de"))
            assertEquals(AskResult.Failed, ArticleAsk(BrainCall { _, _ -> "x" }).ask(article(2), "   ", "de"))
            val broken = ArticleAsk(BrainCall { _, _ -> error("brain crashed") })
            assertEquals(AskResult.Failed, broken.ask(article(2), "Was?", "de"))
            val onlyThinking = ArticleAsk(BrainCall { _, _ -> "<think>…</think>" })
            assertEquals(AskResult.Failed, onlyThinking.ask(article(2), "Was?", "en"))
        }

    @Test fun promptAndHandOffInTheUsersLanguage() {
        assertTrue(ArticleAsk.systemPrompt("de").contains("Antworte auf Deutsch"))
        assertTrue(ArticleAsk.systemPrompt("en").contains("Answer in English"))
        val handOff = ArticleAsk.chatHandOff(article(1), "Worum geht es?", "Um den Haushalt.", "de")
        assertTrue(handOff.contains("https://example.org/haushalt"))
        assertTrue(handOff.contains("Ich habe gefragt: Worum geht es?"))
        assertTrue(handOff.contains("Du hast geantwortet: Um den Haushalt."))
    }
}
