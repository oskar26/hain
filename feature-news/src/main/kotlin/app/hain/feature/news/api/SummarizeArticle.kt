package app.hain.feature.news.api

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import app.hain.tools.internal.search.HtmlText
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * Asks the local brain once: [system] instructions and the [user] text, thinking off. Returns the
 * raw answer, or `null` when no brain is installed or it could not answer.
 */
fun interface BrainCall {
    suspend fun complete(
        system: String,
        user: String,
    ): String?
}

/** What summarising one article gave (T-074). */
sealed interface SummaryResult {
    data class Bullets(
        val points: List<String>,
    ) : SummaryResult

    /** Paywall, login, blocked or no readable text: "Artikel nicht frei lesbar" + open link. */
    data object NotReadable : SummaryResult

    data object Offline : SummaryResult

    data object NoBrain : SummaryResult

    data object Failed : SummaryResult
}

/**
 * "Zusammenfassen" on a headline (T-074, NEWS.md §5), only when the user taps it: fetches the article
 * through the [NetworkGate] (purpose NEWS), keeps its readable text (about 2,500 tokens) and asks the
 * brain for 5 bullet points in [lang]. Nothing is stored. Paywalls are never bypassed: no archive
 * sites, no cookie tricks; a page that is not freely readable says so.
 */
class SummarizeArticle(
    private val gate: NetworkGate,
    private val brain: BrainCall,
) {
    @Suppress("ReturnCount", "TooGenericExceptionCaught", "SwallowedException")
    suspend fun summarize(
        url: String?,
        title: String,
        lang: String,
    ): SummaryResult {
        if (url == null || !url.startsWith("https://")) return SummaryResult.NotReadable
        val text =
            try {
                val response = gate.fetch(GateRequest(url, Purpose.NEWS))
                if (!response.ok) return SummaryResult.NotReadable
                HtmlText.extract(response.body.take(MAX_HTML), MAX_CHARS).text
            } catch (e: OfflineException) {
                return SummaryResult.Offline
            } catch (e: IOException) {
                return SummaryResult.Failed
            } catch (e: IllegalArgumentException) {
                return SummaryResult.NotReadable
            }
        if (text.length < MIN_TEXT || looksLocked(text)) return SummaryResult.NotReadable
        val answer =
            try {
                brain.complete(systemPrompt(lang), "$title\n\n$text")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                return SummaryResult.Failed
            }
        if (answer == null) return SummaryResult.NoBrain
        val points = bullets(answer)
        return if (points.isEmpty()) SummaryResult.Failed else SummaryResult.Bullets(points)
    }

    companion object {
        /** About 2,500 tokens of article text (NEWS.md §5). */
        const val MAX_CHARS = 9_000
        const val POINTS = 5
        private const val MAX_HTML = 800_000
        private const val MIN_TEXT = 400
        private const val LOCK_ZONE = 600
        private val BULLET = Regex("""^\s*(?:[-•*–]|\d+[.)])\s*""")
        private val LOCK_WORDS =
            listOf(
                "jetzt abonnieren",
                "abo abschließen",
                "weiterlesen mit",
                "nur für abonnenten",
                "subscribe to continue",
                "subscribe to read",
                "already a subscriber",
                "bitte melden sie sich an",
            )

        fun systemPrompt(lang: String): String =
            if (lang == "de") {
                "Fasse den Artikel in genau $POINTS kurzen Stichpunkten auf Deutsch zusammen. " +
                    "Jeder Stichpunkt beginnt mit \"- \". Nur Fakten aus dem Artikel, keine eigene Meinung, " +
                    "keine Einleitung."
            } else {
                "Summarise the article in exactly $POINTS short bullet points in English. " +
                    "Each bullet starts with \"- \". Only facts from the article, no opinion, no intro."
            }

        /** The bullet points of the brain's answer (thinking dropped), at most [POINTS]. */
        fun bullets(raw: String): List<String> {
            val lines =
                raw
                    .substringAfter("</think>")
                    .lines()
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            val marked = lines.filter { BULLET.containsMatchIn(it) }
            val picked = marked.ifEmpty { lines.filter { it.length > 1 } }
            return picked
                .map { it.replace(BULLET, "").replace("**", "").trim() }
                .filter { it.isNotEmpty() }
                .take(POINTS)
        }

        /** A page that only shows its start and a subscription wall. */
        fun looksLocked(text: String): Boolean {
            val tail = text.takeLast(LOCK_ZONE).lowercase()
            return LOCK_WORDS.any { it in tail }
        }
    }
}
