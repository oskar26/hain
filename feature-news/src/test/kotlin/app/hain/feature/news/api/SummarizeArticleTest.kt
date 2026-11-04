package app.hain.feature.news.api

import app.hain.common.api.net.GateResponse
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-074: fetch → readable text → 5 points from the brain; paywall, offline and no brain. */
class SummarizeArticleTest {
    private val paragraph =
        "Die Beschäftigten im Werk legen am Mittwoch die Arbeit nieder, um höhere Löhne zu fordern. "
    private val article =
        "<html><head><title>Streik</title></head><body>" + "<p>$paragraph</p>".repeat(12) + "</body></html>"
    private val five = "- Eins\n- Zwei\n- Drei\n- Vier\n- Fünf\n- Sechs"

    @Test fun fivePointsFromTheBrainThroughTheGate() =
        runTest {
            val gate = FakeGate { GateResponse(200, article) }
            var asked: Pair<String, String>? = null
            val summary =
                SummarizeArticle(gate) { system, user ->
                    asked = system to user
                    five
                }
            val result = summary.summarize("https://example.org/a", "Streik", "de")
            assertEquals(SummaryResult.Bullets(listOf("Eins", "Zwei", "Drei", "Vier", "Fünf")), result)
            assertEquals(Purpose.NEWS, gate.requests.single().purpose)
            assertTrue(asked!!.first.contains("5"))
            assertTrue(asked!!.second.startsWith("Streik\n\n"))
            assertTrue(asked!!.second.length <= SummarizeArticle.MAX_CHARS + 20)
        }

    @Test fun paywallAnswerIsNotReadable() =
        runTest {
            val brainCalls = mutableListOf<String>()
            val summary = SummarizeArticle(FakeGate { GateResponse(403, "") }) { _, u -> five.also { brainCalls += u } }
            assertEquals(SummaryResult.NotReadable, summary.summarize("https://example.org/a", "T", "de"))
            assertTrue(brainCalls.isEmpty())
        }

    @Test fun subscriptionWallIsNotReadable() =
        runTest {
            val wall = "<p>Jetzt abonnieren und weiterlesen mit dem Digital-Abo für nur 1 €.</p>"
            val locked = article.replace("</body>", "$wall</body>")
            val summary = SummarizeArticle(FakeGate { GateResponse(200, locked) }) { _, _ -> five }
            assertEquals(SummaryResult.NotReadable, summary.summarize("https://example.org/a", "T", "de"))
        }

    @Test fun offlineAndNoBrain() =
        runTest {
            val offline = SummarizeArticle(FakeGate { throw OfflineException() }) { _, _ -> five }
            assertEquals(SummaryResult.Offline, offline.summarize("https://example.org/a", "T", "de"))
            val noBrain = SummarizeArticle(FakeGate { GateResponse(200, article) }) { _, _ -> null }
            assertEquals(SummaryResult.NoBrain, noBrain.summarize("https://example.org/a", "T", "en"))
            val noLink = SummarizeArticle(FakeGate()) { _, _ -> five }
            assertEquals(SummaryResult.NotReadable, noLink.summarize(null, "T", "de"))
        }

    @Test fun bulletsDropThinkingAndNumbers() {
        val raw = "<think>hmm</think>\n1. **Erstens**\n2) Zweitens\n• Drittens"
        assertEquals(listOf("Erstens", "Zweitens", "Drittens"), SummarizeArticle.bullets(raw))
    }
}
