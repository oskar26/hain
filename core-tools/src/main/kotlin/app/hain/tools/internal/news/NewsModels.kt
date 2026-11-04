package app.hain.tools.internal.news

/** The only news choice the user makes (NEWS.md §1, ADR-14): three modes, nothing else. */
enum class NewsMode(
    val key: String,
) {
    /** The fixed standard mix (§2), the default. */
    STANDARD("standard"),

    /** The standard mix plus the user's own feeds. */
    STANDARD_PLUS_OWN("standard_plus_own"),

    /** Only the user's own feeds. */
    OWN_ONLY("own_only"),
    ;

    val usesStandard: Boolean get() = this != OWN_ONLY
    val usesOwn: Boolean get() = this != STANDARD

    companion object {
        fun fromKey(key: String?): NewsMode = entries.firstOrNull { it.key == key } ?: STANDARD
    }
}

/**
 * One feed. Standard feeds come from `assets/news/standard_feeds.json`; own feeds are added by
 * the user (max [NewsRepository.MAX_OWN_FEEDS]).
 *
 * @property lang `de` or `en`; `null` for own feeds (shown for every language).
 * @property category only items whose category contains this text are kept (CORRECTIV: "Faktencheck").
 */
data class NewsFeed(
    val id: String,
    val name: String,
    val url: String,
    val lang: String? = null,
    val paywall: Boolean = false,
    val own: Boolean = false,
    val category: String? = null,
)

/**
 * Fetch bookkeeping of one feed: validators for the conditional GET and the health count. A feed
 * whose answer was no feed or had no items [BROKEN_AFTER] times in a row is broken (§3).
 */
data class FeedState(
    val etag: String? = null,
    val lastModified: String? = null,
    val failures: Int = 0,
    val lastFetched: Long = 0,
) {
    val broken: Boolean get() = failures >= BROKEN_AFTER

    companion object {
        const val BROKEN_AFTER = 3
    }
}

/** One headline as Hain keeps it (7 days). [published] falls back to [fetchedAt] for sorting. */
data class NewsItem(
    val feedId: String,
    val source: String,
    val title: String,
    val link: String?,
    val guid: String,
    val published: Long?,
    val fetchedAt: Long,
    val summary: String,
    val imageUrl: String? = null,
    val paywall: Boolean = false,
    val lang: String? = null,
) {
    val time: Long get() = published ?: fetchedAt
}

/** Everything the news feature keeps on the phone: the mode, own feeds, feed states and the items. */
data class NewsSnapshot(
    val mode: NewsMode = NewsMode.STANDARD,
    val ownFeeds: List<NewsFeed> = emptyList(),
    val feedStates: Map<String, FeedState> = emptyMap(),
    val items: List<NewsItem> = emptyList(),
    val lastRefresh: Long = 0,
)
