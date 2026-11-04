package app.hain.tools.internal.news

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.io.IOException

/** Where the news feature keeps its [NewsSnapshot] between app starts. */
interface NewsStore {
    suspend fun load(): NewsSnapshot?

    suspend fun save(snapshot: NewsSnapshot)
}

/** Keeps the snapshot in memory only (tests, or when the app's files cannot be written). */
class MemoryNewsStore(
    private var saved: NewsSnapshot? = null,
) : NewsStore {
    override suspend fun load(): NewsSnapshot? = saved

    override suspend fun save(snapshot: NewsSnapshot) {
        saved = snapshot
    }
}

/**
 * One JSON file in the app's private files (T-071). Written to a temp file and renamed, so a crash
 * never leaves half a file. Headlines are public texts; nothing about the user is in it except the
 * mode and their own feed addresses.
 */
class FileNewsStore(
    private val file: File,
) : NewsStore {
    override suspend fun load(): NewsSnapshot? =
        withContext(Dispatchers.IO) {
            if (!file.isFile) return@withContext null
            runCatching { NewsJson.decode(file.readText()) }.getOrNull()
        }

    override suspend fun save(snapshot: NewsSnapshot) {
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            val tmp = File(file.parentFile, file.name + ".tmp")
            tmp.writeText(NewsJson.encode(snapshot))
            if (!tmp.renameTo(file)) {
                file.delete()
                if (!tmp.renameTo(file)) throw IOException("cannot write ${file.name}")
            }
        }
    }
}

/** [NewsSnapshot] ⇄ JSON text; unknown or broken entries are skipped. */
@Suppress("TooManyFunctions") // one reader and writer per kind of entry
object NewsJson {
    fun encode(s: NewsSnapshot): String =
        buildJsonObject {
            put("version", VERSION)
            put("mode", s.mode.key)
            put("lastRefresh", s.lastRefresh)
            putJsonArray("ownFeeds") { s.ownFeeds.forEach { add(feed(it)) } }
            putJsonObject("feedStates") {
                s.feedStates.forEach { (id, st) ->
                    putJsonObject(id) {
                        st.etag?.let { put("etag", it) }
                        st.lastModified?.let { put("lastModified", it) }
                        put("failures", st.failures)
                        put("lastFetched", st.lastFetched)
                    }
                }
            }
            putJsonArray("items") { s.items.forEach { add(item(it)) } }
        }.toString()

    fun decode(text: String): NewsSnapshot {
        val root = Json.parseToJsonElement(text).jsonObject
        val states =
            (root["feedStates"] as? JsonObject).orEmpty().mapNotNull { (id, el) ->
                val o = el as? JsonObject ?: return@mapNotNull null
                id to
                    FeedState(
                        etag = o.text("etag"),
                        lastModified = o.text("lastModified"),
                        failures = o.int("failures") ?: 0,
                        lastFetched = o.long("lastFetched") ?: 0,
                    )
            }
        return NewsSnapshot(
            mode = NewsMode.fromKey(root.text("mode")),
            ownFeeds = (root["ownFeeds"] as? JsonArray).orEmpty().mapNotNull { readFeed(it, own = true) },
            feedStates = states.toMap(),
            items = (root["items"] as? JsonArray).orEmpty().mapNotNull(::readItem),
            lastRefresh = root.long("lastRefresh") ?: 0,
        )
    }

    /** The fixed standard list from `assets/news/standard_feeds.json`. */
    fun standardFeeds(text: String): List<NewsFeed> {
        val root = Json.parseToJsonElement(text).jsonObject
        return (root["feeds"] as? JsonArray).orEmpty().mapNotNull { readFeed(it, own = false) }
    }

    private fun feed(f: NewsFeed): JsonObject =
        buildJsonObject {
            put("id", f.id)
            put("name", f.name)
            put("url", f.url)
            f.lang?.let { put("lang", it) }
            put("paywall", f.paywall)
            f.category?.let { put("category", it) }
        }

    @Suppress("ReturnCount")
    private fun readFeed(
        el: JsonElement,
        own: Boolean,
    ): NewsFeed? {
        val o = el as? JsonObject ?: return null
        val id = o.text("id") ?: return null
        val url = o.text("url") ?: return null
        return NewsFeed(
            id = id,
            name = o.text("name") ?: id,
            url = url,
            lang = o.text("lang"),
            paywall = o.bool("paywall") == true,
            own = own,
            category = o.text("category"),
        )
    }

    private fun item(i: NewsItem): JsonObject =
        buildJsonObject {
            put("feed", i.feedId)
            put("source", i.source)
            put("title", i.title)
            i.link?.let { put("link", it) }
            put("guid", i.guid)
            i.published?.let { put("published", it) }
            put("fetchedAt", i.fetchedAt)
            put("summary", i.summary)
            i.imageUrl?.let { put("image", it) }
            if (i.paywall) put("paywall", true)
            i.lang?.let { put("lang", it) }
        }

    @Suppress("ReturnCount")
    private fun readItem(el: JsonElement): NewsItem? {
        val o = el as? JsonObject ?: return null
        return NewsItem(
            feedId = o.text("feed") ?: return null,
            source = o.text("source").orEmpty(),
            title = o.text("title") ?: return null,
            link = o.text("link"),
            guid = o.text("guid") ?: return null,
            published = o.long("published"),
            fetchedAt = o.long("fetchedAt") ?: 0,
            summary = o.text("summary").orEmpty(),
            imageUrl = o.text("image"),
            paywall = o.bool("paywall") == true,
            lang = o.text("lang"),
        )
    }

    private const val VERSION = 1

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

    private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
}
