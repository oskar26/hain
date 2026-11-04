package app.hain.feature.chat.internal

import app.hain.tools.api.NewsHeadline
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-072: the NewsCard of an answer is kept with it in `message.sources_json`. */
class NewsCardJsonTest {
    private val news =
        listOf(
            NewsHeadline("junge Welt", "Streik bei Ford", "https://www.jungewelt.de/a/1", 1_762_000_000_000, true),
            NewsHeadline("tagesschau", "Ohne Datum", null, null),
        )

    @Test fun newsSurvivesTheDatabase() {
        val json = ToolStepsJson.encode(emptyList(), emptyList(), news = news)
        assertNotNull(json)
        assertEquals(news, ToolStepsJson.decodeNews(json))
        val message = UiMessage(1, isUser = false, text = "x", news = news)
        assertEquals(news, message.toEntity(chatId = 2, createdAt = 3).toUi().news)
    }

    @Test fun answersWithoutNewsStayAsBefore() {
        assertTrue(ToolStepsJson.decodeNews(null).isEmpty())
        val json = ToolStepsJson.encode(emptyList(), listOf(SourceRef("A", "https://a.example")))
        assertTrue(ToolStepsJson.decodeNews(json).isEmpty())
    }
}
