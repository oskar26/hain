package app.hain.feature.news.api

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import app.hain.tools.internal.news.Article
import app.hain.tools.internal.news.Readability
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** What opening an article in the reading view gave (T-076). */
sealed interface ReaderResult {
    data class Ready(
        val article: Article,
    ) : ReaderResult

    /** The page had no readable article text (video page, live ticker, blocked). */
    data object NotReadable : ReaderResult

    data object Offline : ReaderResult

    data object Failed : ReaderResult
}

/**
 * Loads an article for the reading view (T-076): the page through the [NetworkGate] (purpose
 * NEWS, only when the user opens it), then the readable part with its pictures ([Readability]).
 * Paywalls are never bypassed: a cut-off article is shown as far as the page itself shows it, with
 * the hint and "Auf Website öffnen". The last [KEEP] articles stay in memory while the screen is open.
 * The page is taken apart on [parsing] (a background thread by default).
 */
class ArticleReader(
    private val gate: NetworkGate,
    private val parsing: CoroutineDispatcher = Dispatchers.Default,
) {
    private val cache = LinkedHashMap<String, Article>()

    @Suppress("ReturnCount", "TooGenericExceptionCaught", "SwallowedException")
    suspend fun read(url: String?): ReaderResult {
        if (url == null || !url.startsWith("https://")) return ReaderResult.NotReadable
        synchronized(cache) { cache[url] }?.let { return ReaderResult.Ready(it) }
        val html =
            try {
                val response = gate.fetch(GateRequest(url, Purpose.NEWS))
                if (!response.ok) return ReaderResult.NotReadable
                response.body
            } catch (e: CancellationException) {
                throw e
            } catch (e: OfflineException) {
                return ReaderResult.Offline
            } catch (e: IOException) {
                return ReaderResult.Failed
            } catch (e: IllegalArgumentException) {
                return ReaderResult.NotReadable
            }
        val article = withContext(parsing) { runCatching { Readability.extract(html, url) }.getOrNull() }
        if (article == null || (article.textLength < MIN_TEXT && !article.locked)) return ReaderResult.NotReadable
        synchronized(cache) {
            cache[url] = article
            if (cache.size > KEEP) cache.remove(cache.keys.first())
        }
        return ReaderResult.Ready(article)
    }

    companion object {
        /** Less text than this (and no paywall) is no article: the reader offers the website instead. */
        const val MIN_TEXT = 280
        const val KEEP = 12
    }
}
