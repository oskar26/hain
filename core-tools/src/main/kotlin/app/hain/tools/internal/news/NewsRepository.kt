package app.hain.tools.internal.news

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.GateResponse
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.URI
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/** What adding an own feed did (T-073). */
sealed interface AddFeedResult {
    data class Added(
        val feed: NewsFeed,
    ) : AddFeedResult

    /** Not an https address, or the answer was no RSS/Atom/RDF feed with items. */
    data object NotAFeed : AddFeedResult

    data object Duplicate : AddFeedResult

    /** Already [NewsRepository.MAX_OWN_FEEDS] own feeds. */
    data object Full : AddFeedResult

    data object Offline : AddFeedResult

    /** Network error or timeout. */
    data object Failed : AddFeedResult
}

/** What one refresh did: feeds asked, feeds that failed, and whether offline mode stopped it. */
data class RefreshResult(
    val fetched: Int,
    val failed: Int,
    val offline: Boolean = false,
)

/**
 * News headlines (T-071, NEWS.md §1–3): the three modes, the fixed standard mix plus own feeds,
 * conditional GET through the [NetworkGate] (purpose [Purpose.NEWS], so feeds have their own line in
 * the network log), dedup of the same headline within 24 h, 7 days of items, the CORRECTIV
 * "Faktencheck" filter and the broken-feed health count. Everything is kept in [store].
 */
@Suppress("TooManyFunctions") // one small function per news action
class NewsRepository(
    private val gate: NetworkGate,
    private val store: NewsStore,
    val standardFeeds: List<NewsFeed>,
    private val parser: FeedParser = FeedParser(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val refreshMs: () -> Long = { DEFAULT_REFRESH_MS },
) {
    private val mutex = Mutex()
    private val pageImageLimit = Semaphore(PAGE_IMAGE_PARALLEL)
    private val snapshot = MutableStateFlow(NewsSnapshot())
    private var loaded = false

    /** The current snapshot; [items] turns it into the list the user sees. */
    val state: StateFlow<NewsSnapshot> = snapshot.asStateFlow()

    /** Reads the stored snapshot once; every other call does this first. */
    suspend fun load() {
        mutex.withLock { ensureLoaded() }
    }

    /** Feeds of [mode]. */
    fun feeds(
        mode: NewsMode,
        s: NewsSnapshot = snapshot.value,
    ): List<NewsFeed> {
        val standard = if (mode.usesStandard) standardFeeds else emptyList()
        return standard + if (mode.usesOwn) s.ownFeeds else emptyList()
    }

    /**
     * The headlines of the current mode, newest first, the same headline only once (first one within
     * 24 h wins) and none older than 7 days. [lang] `de`/`en` keeps feeds of that language and own feeds.
     */
    fun items(
        s: NewsSnapshot = snapshot.value,
        lang: String? = null,
    ): List<NewsItem> {
        val ids = feeds(s.mode, s).map { it.id }.toSet()
        val since = clock() - KEEP_MS
        val wanted =
            s.items.filter { item ->
                item.feedId in ids && item.time >= since && (lang == null || item.lang == null || item.lang == lang)
            }
        return dedup(wanted).sortedByDescending { it.time }.map { item -> withPageFacts(item, s) }
    }

    /** The page's picture when the feed had none (T-075) and the "really cut off" mark (T-077). */
    private fun withPageFacts(
        item: NewsItem,
        s: NewsSnapshot,
    ): NewsItem {
        val link = item.link ?: return item
        val image = item.imageUrl ?: s.pageImages[link]?.ifEmpty { null }
        val locked = link in s.locked
        val same = image == item.imageUrl && locked == item.paywall
        return if (same) item else item.copy(imageUrl = image, paywall = locked)
    }

    /**
     * What the reading view found out about the article at [link] (T-076/T-077): whether its text is
     * cut off, and the page's picture for the list when the feed had none.
     */
    suspend fun markRead(
        link: String,
        locked: Boolean,
        image: String?,
    ) {
        update { s ->
            val known = !s.pageImages[link].isNullOrEmpty()
            val images = if (image != null && !known) s.pageImages + (link to image) else s.pageImages
            s.copy(locked = if (locked) s.locked + link else s.locked - link, pageImages = images)
        }
    }

    /**
     * Looks up the picture of an article whose feed names none (T-075): reads the page's `og:image`
     * once through the gate (purpose IMAGES, at most [PAGE_IMAGE_PARALLEL] at a time) and remembers
     * it, or that there is none. Offline or on errors nothing is remembered, so it is asked again later.
     */
    @Suppress("TooGenericExceptionCaught", "SwallowedException", "ReturnCount")
    suspend fun lookUpPageImage(link: String) {
        if (!link.startsWith("https://")) return
        if (link in snapshot.value.pageImages) return
        val found =
            pageImageLimit.withPermit {
                if (link in snapshot.value.pageImages) return
                try {
                    val response = gate.fetch(GateRequest(link, Purpose.IMAGES))
                    if (!response.ok) "" else PageImage.find(response.body, link).orEmpty()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    null
                }
            } ?: return
        update { s -> s.copy(pageImages = s.pageImages + (link to found)) }
    }

    /** Own feeds that stopped answering with a feed (shown in the own-feeds list only). */
    fun brokenOwnFeeds(s: NewsSnapshot = snapshot.value): Set<String> =
        s.ownFeeds
            .filter { s.feedStates[it.id]?.broken == true }
            .map { it.id }
            .toSet()

    suspend fun setMode(mode: NewsMode) {
        update { it.copy(mode = mode) }
    }

    suspend fun renameFeed(
        id: String,
        name: String,
    ) {
        val clean = name.trim().take(MAX_NAME)
        if (clean.isEmpty()) return
        update { s -> s.copy(ownFeeds = s.ownFeeds.map { if (it.id == id) it.copy(name = clean) else it }) }
    }

    suspend fun deleteFeed(id: String) {
        update { s ->
            s.copy(
                ownFeeds = s.ownFeeds.filterNot { it.id == id },
                feedStates = s.feedStates - id,
                items = s.items.filterNot { it.feedId == id },
            )
        }
    }

    /**
     * Adds an own feed after checking that [rawUrl] answers with a feed that has items; [name] or the
     * feed's own title becomes its name. Its first items are kept right away.
     */
    @Suppress("ReturnCount", "SwallowedException")
    suspend fun addFeed(
        rawUrl: String,
        name: String? = null,
    ): AddFeedResult {
        val url = normalizeUrl(rawUrl) ?: return AddFeedResult.NotAFeed
        val current = mutex.withLock { ensureLoaded() }
        if (current.ownFeeds.size >= MAX_OWN_FEEDS) return AddFeedResult.Full
        if ((current.ownFeeds + standardFeeds).any { sameUrl(it.url, url) }) return AddFeedResult.Duplicate
        val response =
            try {
                gate.fetch(GateRequest(url, Purpose.NEWS))
            } catch (e: OfflineException) {
                return AddFeedResult.Offline
            } catch (e: IOException) {
                return AddFeedResult.Failed
            } catch (e: IllegalArgumentException) {
                return AddFeedResult.NotAFeed
            }
        val parsed = (parse(response) as? FeedParseResult.Success)?.feed
        if (!response.ok || parsed == null || parsed.items.isEmpty()) return AddFeedResult.NotAFeed
        val title =
            name?.trim()?.takeIf { it.isNotEmpty() }
                ?: parsed.title?.takeIf { it.isNotBlank() }
                ?: hostOf(url)
        val feed =
            NewsFeed(id = OWN_PREFIX + clock().toString(BASE), name = title.take(MAX_NAME), url = url, own = true)
        val now = clock()
        update { s ->
            s.copy(
                ownFeeds = s.ownFeeds + feed,
                feedStates = s.feedStates + (feed.id to validators(response, FeedState(lastFetched = now))),
                items = merge(s.items, feed, parsed, now),
            )
        }
        return AddFeedResult.Added(feed)
    }

    /**
     * Fetches the feeds of the current mode, all at the same time; with [force] = false only those
     * not fetched within the refresh interval. Offline mode stops it and keeps the cached items.
     */
    @Suppress("ReturnCount", "SwallowedException")
    suspend fun refresh(force: Boolean = false): RefreshResult {
        val start = mutex.withLock { ensureLoaded() }
        val now = clock()
        val due =
            feeds(start.mode, start).filter { feed ->
                force || now - (start.feedStates[feed.id]?.lastFetched ?: 0) >= refreshMs()
            }
        if (due.isEmpty()) return RefreshResult(0, 0)
        val failed = AtomicInteger(0)
        try {
            // T-075: every feed on its own, each shown as soon as it is there; a slow or broken feed
            // neither holds the others back nor stops them (time limit per feed)
            coroutineScope {
                due.forEach { feed ->
                    launch {
                        val old = start.feedStates[feed.id]
                        val outcome =
                            withTimeoutOrNull(FEED_TIMEOUT_MS) { fetchOne(feed, old) }
                                ?: Outcome(feed, old ?: FeedState(), null, failed = true)
                        if (outcome.failed) failed.incrementAndGet()
                        update { s ->
                            val items = outcome.parsed?.let { merge(s.items, feed, it, now) } ?: s.items
                            s.copy(feedStates = s.feedStates + (feed.id to outcome.state), items = items)
                        }
                    }
                }
            }
        } catch (e: OfflineException) {
            return RefreshResult(0, 0, offline = true)
        }
        update { s -> s.copy(items = prune(s.items, s, now), lastRefresh = now, pageImages = prunePages(s)) }
        return RefreshResult(due.size, failed.get())
    }

    /** True when the current mode's items are older than the refresh interval. */
    fun stale(): Boolean = clock() - snapshot.value.lastRefresh >= refreshMs()

    /**
     * Headlines for the `news` tool (T-072): refreshes first when stale (offline: the cache), then
     * the best matches for [query] in title and teaser, newest first among equals; no query = newest.
     */
    suspend fun search(
        query: String?,
        lang: String? = null,
        limit: Int = TOOL_ITEMS,
    ): List<NewsItem> {
        load()
        if (stale()) refresh()
        val pool = items(lang = lang?.takeIf { it == "de" || it == "en" })
        val terms = terms(query)
        if (terms.isEmpty()) return pool.take(limit)
        return pool
            .map { it to score(it, terms) }
            .filter { it.second > 0 }
            .sortedWith(compareByDescending<Pair<NewsItem, Int>> { it.second }.thenByDescending { it.first.time })
            .take(limit)
            .map { it.first }
    }

    private class Outcome(
        val feed: NewsFeed,
        val state: FeedState,
        val parsed: ParsedFeed?,
        val failed: Boolean,
    )

    @Suppress("TooGenericExceptionCaught", "SwallowedException", "ReturnCount")
    private suspend fun fetchOne(
        feed: NewsFeed,
        old: FeedState?,
    ): Outcome {
        val before = old ?: FeedState()
        val headers =
            listOfNotNull(
                before.etag?.let { "If-None-Match" to it },
                before.lastModified?.let { "If-Modified-Since" to it },
            ).toMap()
        val now = clock()
        val response =
            try {
                gate.fetch(GateRequest(feed.url, Purpose.NEWS, headers = headers))
            } catch (e: CancellationException) {
                throw e
            } catch (e: OfflineException) {
                throw e
            } catch (e: Exception) {
                // a network error says nothing about the feed itself: no health count
                return Outcome(feed, before, null, failed = true)
            }
        if (response.notModified) return Outcome(feed, before.copy(failures = 0, lastFetched = now), null, false)
        val parsed = if (response.ok) (parse(response) as? FeedParseResult.Success)?.feed else null
        return if (parsed == null || parsed.items.isEmpty()) {
            Outcome(feed, before.copy(failures = before.failures + 1, lastFetched = now), null, failed = true)
        } else {
            Outcome(feed, validators(response, before.copy(failures = 0, lastFetched = now)), parsed, false)
        }
    }

    private fun parse(response: GateResponse): FeedParseResult {
        val bytes = response.body.byteInputStream(Charsets.UTF_8)
        return parser.parse(bytes)
    }

    private fun validators(
        response: GateResponse,
        state: FeedState,
    ): FeedState =
        state.copy(
            etag = response.header("etag") ?: state.etag,
            lastModified = response.header("last-modified") ?: state.lastModified,
        )

    /** New items of [feed] replace older copies of the same guid; the first time seen is kept. */
    private fun merge(
        items: List<NewsItem>,
        feed: NewsFeed,
        parsed: ParsedFeed,
        now: Long,
    ): List<NewsItem> {
        val old = items.filter { it.feedId == feed.id }.associateBy { it.guid }
        val fresh =
            parsed.items
                .filter { item -> feed.category == null || item.categories.any { it.contains(feed.category, true) } }
                .map { f ->
                    NewsItem(
                        feedId = feed.id,
                        source = feed.name,
                        title = f.title,
                        link = f.link,
                        guid = f.guid,
                        // junge Welt dates articles with tomorrow's paper: never in the future
                        published = f.published?.toEpochMilli()?.coerceAtMost(now),
                        fetchedAt = old[f.guid]?.fetchedAt ?: now,
                        summary = f.summary,
                        imageUrl = f.imageUrl,
                        lang = feed.lang,
                    )
                }
        val freshIds = fresh.map { it.guid }.toSet()
        val kept = old.values.filter { it.guid !in freshIds }
        val ofFeed = (fresh + kept).sortedByDescending { it.time }.take(MAX_ITEMS_PER_FEED)
        return items.filterNot { it.feedId == feed.id } + ofFeed
    }

    private fun prune(
        items: List<NewsItem>,
        s: NewsSnapshot,
        now: Long,
    ): List<NewsItem> {
        val feedIds = (standardFeeds.map { it.id } + s.ownFeeds.map { it.id }).toSet()
        return items.filter { it.time >= now - KEEP_MS && it.feedId in feedIds }
    }

    /** Page pictures and marks only for links that are still in the list. */
    private fun prunePages(s: NewsSnapshot): Map<String, String> {
        val links = s.items.mapNotNull { it.link }.toSet()
        return s.pageImages.filterKeys { it in links }
    }

    private suspend fun update(change: (NewsSnapshot) -> NewsSnapshot) {
        mutex.withLock {
            val next = change(ensureLoaded())
            snapshot.value = next
            runCatching { store.save(next) }
        }
    }

    /** Must hold [mutex]. */
    private suspend fun ensureLoaded(): NewsSnapshot {
        if (!loaded) {
            loaded = true
            runCatching { store.load() }.getOrNull()?.let { snapshot.value = it }
        }
        return snapshot.value
    }

    companion object {
        const val MAX_OWN_FEEDS = 50
        const val TOOL_ITEMS = 8
        const val DEFAULT_REFRESH_MS = 30 * 60 * 1000L
        const val KEEP_MS = 7 * 24 * 60 * 60 * 1000L
        const val DEDUP_MS = 24 * 60 * 60 * 1000L

        /** One feed may take this long; then it counts as failed for this refresh (T-075). */
        const val FEED_TIMEOUT_MS = 15_000L
        const val PAGE_IMAGE_PARALLEL = 3
        private const val MIX_WINDOW = 12
        private const val MIX_MEMORY = 6
        private const val MAX_ITEMS_PER_FEED = 150
        private const val MAX_NAME = 60
        private const val MIN_TERM = 3
        private const val TITLE_WEIGHT = 3
        private const val BASE = 36
        private const val OWN_PREFIX = "own-"
        private val PUNCTUATION = Regex("""[^\p{L}\p{N} ]""")
        private val SPACES = Regex("""\s+""")

        /** Title for dedup: lower case, no punctuation, single spaces. */
        fun normalizeTitle(title: String): String =
            title
                .lowercase(Locale.ROOT)
                .replace(PUNCTUATION, " ")
                .replace(SPACES, " ")
                .trim()

        /** Keeps the first (oldest) of the same headline within 24 h. */
        fun dedup(items: List<NewsItem>): List<NewsItem> {
            val seen = mutableMapOf<String, MutableList<Long>>()
            return items.sortedBy { it.time }.filter { item ->
                val key = normalizeTitle(item.title)
                val times = seen.getOrPut(key) { mutableListOf() }
                val duplicate = times.any { item.time - it < DEDUP_MS }
                if (!duplicate) times += item.time
                !duplicate
            }
        }

        /**
         * The list as the user sees it (T-075): newest first, but sources take turns when others are
         * close behind (looks [MIX_WINDOW] items ahead), so a feed that publishes 30 articles at once
         * does not fill the whole first screen.
         */
        fun mixed(items: List<NewsItem>): List<NewsItem> {
            val rest = items.toMutableList()
            val out = ArrayList<NewsItem>(items.size)
            while (rest.isNotEmpty()) {
                // the newest item of the source seen least among the last few, within the next MIX_WINDOW
                val recent = out.takeLast(MIX_MEMORY).groupingBy { it.feedId }.eachCount()
                val window = rest.take(MIX_WINDOW)
                val least = window.minOf { recent[it.feedId] ?: 0 }
                val pick = window.indexOfFirst { (recent[it.feedId] ?: 0) == least }
                out += rest.removeAt(pick)
            }
            return out
        }

        /** https address of a feed the user typed; `http://` becomes `https://`, no scheme gets one. */
        @Suppress("ReturnCount")
        fun normalizeUrl(raw: String): String? {
            val text = raw.trim()
            if (text.isEmpty() || text.any { it.isWhitespace() }) return null
            val url =
                when {
                    text.startsWith("https://", ignoreCase = true) -> text
                    text.startsWith("http://", ignoreCase = true) -> "https://" + text.substring("http://".length)
                    "://" in text -> return null
                    else -> "https://$text"
                }
            val host = runCatching { URI(url).host }.getOrNull()
            return url.takeIf { host != null && '.' in host }
        }

        private fun sameUrl(
            a: String,
            b: String,
        ) = a.trimEnd('/').equals(b.trimEnd('/'), ignoreCase = true)

        private fun hostOf(url: String): String = url.substringAfter("://").substringBefore('/').removePrefix("www.")

        internal fun terms(query: String?): List<String> =
            normalizeTitle(query.orEmpty())
                .split(' ')
                .filter { it.length >= MIN_TERM || (it.isNotEmpty() && it.all(Char::isDigit)) }
                .distinct()

        internal fun score(
            item: NewsItem,
            terms: List<String>,
        ): Int {
            val title = normalizeTitle(item.title)
            val summary = normalizeTitle(item.summary)
            return terms.sumOf { t ->
                (if (title.contains(t)) TITLE_WEIGHT else 0) + (if (summary.contains(t)) 1 else 0)
            }
        }
    }
}
