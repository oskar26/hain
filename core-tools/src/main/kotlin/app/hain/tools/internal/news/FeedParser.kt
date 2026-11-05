package app.hain.tools.internal.news

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/** Feed formats the parser understands (NEWS.md §3). */
enum class FeedFormat { RSS2, ATOM, RDF }

/**
 * One news item. Text fields are plain text (HTML stripped, entities decoded).
 *
 * @property guid stable id: RSS `guid`, Atom `id`, RDF `rdf:about`; falls back to [link].
 * @property summary teaser, at most [FeedParser.MAX_SUMMARY_CHARS] chars.
 * @property categories RSS `category` / Atom `category@term` / `dc:subject` (used e.g. for the CORRECTIV filter).
 */
data class FeedItem(
    val title: String,
    val link: String?,
    val guid: String,
    val published: Instant?,
    val summary: String,
    val imageUrl: String?,
    val categories: List<String>,
)

data class ParsedFeed(
    val format: FeedFormat,
    val title: String?,
    val items: List<FeedItem>,
)

/** Result of parsing one HTTP body. [NotAFeed] counts towards the feed's `broken` health status. */
sealed interface FeedParseResult {
    data class Success(
        val feed: ParsedFeed,
    ) : FeedParseResult

    data class NotAFeed(
        val reason: String,
    ) : FeedParseResult
}

/**
 * Small own feed parser for RSS 2.0, Atom and RSS 1.0/RDF on top of XmlPullParser (NEWS.md §3).
 * Feed content is untrusted: no DTDs, no entity expansion beyond XML's built-ins, links kept as text only.
 *
 * @param newParser creates a pull parser; Android's built-in one by default, injectable for JVM tests.
 */
@Suppress("TooManyFunctions") // one small function per feed format detail
class FeedParser(
    private val newParser: () -> XmlPullParser = { Xml.newPullParser() },
) {
    fun parse(body: InputStream): FeedParseResult =
        try {
            interpret(readTree(body))
        } catch (e: XmlPullParserException) {
            FeedParseResult.NotAFeed("xml: ${e.message}")
        } catch (e: IOException) {
            FeedParseResult.NotAFeed("io: ${e.message}")
        }

    private fun interpret(root: Node?): FeedParseResult =
        when {
            root == null -> FeedParseResult.NotAFeed("empty document")
            root.name == "rss" -> {
                val channel = root.child("channel")
                success(
                    FeedFormat.RSS2,
                    channel?.childText("title"),
                    channel?.children("item").orEmpty().map(::rssItem),
                )
            }
            root.name == "feed" && root.ns == NS_ATOM ->
                success(FeedFormat.ATOM, root.childText("title"), root.children("entry").map(::atomItem))
            root.name == "RDF" ->
                success(FeedFormat.RDF, root.child("channel")?.childText("title"), root.children("item").map(::rssItem))
            else -> FeedParseResult.NotAFeed("root element <${root.name}>")
        }

    private fun success(
        format: FeedFormat,
        title: String?,
        items: List<FeedItem?>,
    ): FeedParseResult {
        val feed = ParsedFeed(format, title?.let(TextCleaner::plain), items.filterNotNull())
        return FeedParseResult.Success(feed)
    }

    private fun rssItem(item: Node): FeedItem? {
        val title = TextCleaner.plain(item.childText("title").orEmpty())
        val link = item.childText("link")?.trim()?.takeIf { it.isNotEmpty() }
        val guid =
            item.childText("guid")?.trim()?.takeIf { it.isNotEmpty() }
                ?: item.attr(NS_RDF, "about")
                ?: link
        val description = item.childText("description") ?: item.childText("encoded").orEmpty()
        val published = Dates.parse(item.childText("pubDate") ?: item.childText("date"))
        val categories = (item.children("category") + item.children("subject")).map { it.text.trim() }
        return if (title.isEmpty() || guid == null) {
            null
        } else {
            FeedItem(
                title = title,
                link = link,
                guid = guid,
                published = published,
                summary = TextCleaner.summary(description),
                imageUrl = image(item) ?: htmlImage(description + item.childText("encoded").orEmpty(), link),
                categories = categories.filter { it.isNotEmpty() },
            )
        }
    }

    private fun atomItem(entry: Node): FeedItem? {
        val title = TextCleaner.plain(entry.childText("title").orEmpty())
        val links = entry.children("link")
        val link =
            (links.firstOrNull { it.attr(null, "rel") in setOf(null, "alternate") } ?: links.firstOrNull())
                ?.attr(null, "href")
        val guid = entry.childText("id")?.trim()?.takeIf { it.isNotEmpty() } ?: link
        val summary = entry.childText("summary") ?: entry.childText("content").orEmpty()
        val published = Dates.parse(entry.childText("published") ?: entry.childText("updated"))
        val categories =
            entry.children("category").mapNotNull { it.attr(null, "term") ?: it.text.trim().ifEmpty { null } }
        val atomImage = links.firstOrNull { it.attr(null, "rel") == "enclosure" && it.isImage() }?.attr(null, "href")
        return if (title.isEmpty() || guid == null) {
            null
        } else {
            val picture = atomImage ?: image(entry) ?: htmlImage(entry.childText("content").orEmpty() + summary, link)
            FeedItem(title, link, guid, published, TextCleaner.summary(summary), picture, categories)
        }
    }

    /** `media:content` (image), `media:thumbnail`, image `enclosure`, also inside `media:group`. */
    private fun image(item: Node): String? {
        val media = item.descendants().filter { it.ns == NS_MEDIA }
        val content = media.firstOrNull { it.name == "content" && it.isImage() }?.attr(null, "url")
        val thumbnail = media.firstOrNull { it.name == "thumbnail" }?.attr(null, "url")
        val enclosure = item.children("enclosure").firstOrNull { it.isImage() }?.attr(null, "url")
        return (content ?: thumbnail ?: enclosure)?.trim()?.takeIf { it.startsWith("http") }?.let(::https)
    }

    /** T-075: the first real picture inside the item's HTML (taz, tagesschau, heise put it there). */
    private fun htmlImage(
        html: String,
        link: String?,
    ): String? {
        if ("<img" !in html) return null
        return HTML_IMG
            .findAll(html)
            .filterNot { tag -> TINY.containsMatchIn(tag.value) }
            .mapNotNull { tag -> HTML_SRC.find(tag.value)?.let { it.groups[1]?.value ?: it.groups[2]?.value } }
            .map { Entities.decode(it.trim()) }
            .filterNot { it.startsWith("data:") || it.substringBefore('?').endsWith(".gif", ignoreCase = true) }
            .firstNotNullOfOrNull { src -> Readability.absolute(link ?: src, src) }
    }

    private fun https(url: String): String {
        val plain = url.startsWith("http://")
        return if (plain) "https://" + url.removePrefix("http://") else url
    }

    private fun Node.isImage(): Boolean {
        val type = attr(null, "type").orEmpty()
        val medium = attr(null, "medium").orEmpty()
        val url = (attr(null, "url") ?: attr(null, "href")).orEmpty().substringBefore('?').lowercase(Locale.ROOT)
        return type.startsWith("image/") ||
            medium == "image" ||
            (type.isEmpty() && medium.isEmpty() && IMAGE_EXTENSIONS.any { url.endsWith(it) })
    }

    private fun readTree(body: InputStream): Node? {
        val parser = newParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        parser.setInput(body, null)
        val stack = ArrayDeque<Node>()
        var root: Node? = null
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val node = Node(parser.name, parser.namespace.orEmpty(), readAttributes(parser))
                    stack.lastOrNull()?.kids?.add(node)
                    if (root == null) root = node
                    stack.addLast(node)
                }
                XmlPullParser.END_TAG -> stack.removeLastOrNull()
                XmlPullParser.TEXT -> stack.lastOrNull()?.textBuilder?.append(parser.text)
            }
            event = parser.next()
        }
        return root
    }

    private fun readAttributes(parser: XmlPullParser): Map<String, String> =
        (0 until parser.attributeCount).associate { i ->
            val key = attrKey(parser.getAttributeNamespace(i).orEmpty(), parser.getAttributeName(i))
            key to parser.getAttributeValue(i)
        }

    /** Minimal element tree; feeds are small (< 1 MB), so a tree keeps the format logic simple. */
    private class Node(
        val name: String,
        val ns: String,
        val attributes: Map<String, String>,
    ) {
        val kids = mutableListOf<Node>()
        val textBuilder = StringBuilder()
        val text: String get() = textBuilder.toString()

        fun child(name: String): Node? = kids.firstOrNull { it.name == name }

        fun children(name: String): List<Node> = kids.filter { it.name == name }

        fun childText(name: String): String? = child(name)?.text

        fun attr(
            ns: String?,
            name: String,
        ): String? = attributes[attrKey(ns.orEmpty(), name)]?.trim()?.ifEmpty { null }

        fun descendants(): Sequence<Node> = kids.asSequence().flatMap { sequenceOf(it) + it.descendants() }
    }

    companion object {
        const val MAX_SUMMARY_CHARS = 500
        private const val NS_ATOM = "http://www.w3.org/2005/Atom"
        private const val NS_RDF = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
        private const val NS_MEDIA = "http://search.yahoo.com/mrss/"
        private val HTML_IMG = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
        private val HTML_SRC = Regex("""\ssrc\s*=\s*(?:"([^"]+)"|'([^']+)')""", RegexOption.IGNORE_CASE)
        private val TINY = Regex("""\s(?:width|height)\s*=\s*["']?[0-9]["'\s/>]""", RegexOption.IGNORE_CASE)
        private val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".avif")

        private fun attrKey(
            ns: String,
            name: String,
        ) = if (ns.isEmpty()) name else "{$ns}$name"
    }
}

/** Date formats seen in feeds: RFC 822/1123 (RSS) and ISO 8601 (Atom, `dc:date`). */
internal object Dates {
    private val RFC_822_LENIENT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm[:ss] [XX][zzz]", Locale.ENGLISH)
    private val RFC_822_NO_DAY = DateTimeFormatter.ofPattern("d MMM yyyy HH:mm[:ss] [XX][zzz]", Locale.ENGLISH)

    fun parse(raw: String?): Instant? {
        val text = raw?.trim()?.replace(Regex("""\s+"""), " ")?.takeIf { it.isNotEmpty() } ?: return null
        val attempts =
            listOf<(String) -> Instant>(
                { ZonedDateTime.parse(it, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant() },
                { OffsetDateTime.parse(it).toInstant() },
                { Instant.parse(it) },
                { ZonedDateTime.parse(it, RFC_822_LENIENT).toInstant() },
                { ZonedDateTime.parse(it, RFC_822_NO_DAY).toInstant() },
            )
        return attempts.firstNotNullOfOrNull { attempt ->
            try {
                attempt(text)
            } catch (ignored: DateTimeParseException) {
                null
            }
        }
    }
}

/** Plain text from feed HTML: tags removed, entities decoded, whitespace collapsed. */
internal object TextCleaner {
    private val SCRIPT_STYLE = Regex("""(?is)<(script|style)\b.*?</\1>""")
    private val BLOCK_TAGS = Regex("""(?i)<\s*(br|/p|/div|/li|/h[1-6])\b[^>]*>""")
    private val TAGS = Regex("""<[^>]*>""")
    private val ENTITY = Regex("""&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z][a-zA-Z0-9]*);""")
    private val SPACES = Regex("""[ \t\u00A0\r\n]+""")
    private const val SOFT_HYPHEN = "\u00AD"
    private const val ELLIPSIS = "…"
    private const val HEX = 16
    private val NAMED =
        mapOf(
            "amp" to "&",
            "lt" to "<",
            "gt" to ">",
            "quot" to "\"",
            "apos" to "'",
            "nbsp" to " ",
            "ndash" to "–",
            "mdash" to "—",
            "hellip" to "…",
            "laquo" to "«",
            "raquo" to "»",
            "bdquo" to "„",
            "ldquo" to "“",
            "rdquo" to "”",
            "lsquo" to "‘",
            "rsquo" to "’",
            "sbquo" to "‚",
            "auml" to "ä",
            "ouml" to "ö",
            "uuml" to "ü",
            "Auml" to "Ä",
            "Ouml" to "Ö",
            "Uuml" to "Ü",
            "szlig" to "ß",
            "euro" to "€",
            "shy" to "",
            "middot" to "·",
            "deg" to "°",
        )

    fun plain(html: String): String {
        val noTags = html.replace(SCRIPT_STYLE, " ").replace(BLOCK_TAGS, " ").replace(TAGS, " ")
        return decode(noTags).replace(SOFT_HYPHEN, "").replace(SPACES, " ").trim()
    }

    fun summary(html: String): String {
        val text = plain(html)
        if (text.length <= FeedParser.MAX_SUMMARY_CHARS) return text
        val cut = text.take(FeedParser.MAX_SUMMARY_CHARS - ELLIPSIS.length)
        val atWord = cut.substringBeforeLast(' ').takeIf { it.length > cut.length / 2 } ?: cut
        return atWord.trimEnd(',', ';', ':', ' ') + ELLIPSIS
    }

    private fun decode(text: String): String =
        ENTITY.replace(text) { match ->
            val name = match.groupValues[1]
            when {
                name.startsWith("#x") || name.startsWith("#X") -> codePoint(name.drop(2).toIntOrNull(HEX))
                name.startsWith("#") -> codePoint(name.drop(1).toIntOrNull())
                else -> NAMED[name]
            } ?: match.value
        }

    private fun codePoint(cp: Int?): String? {
        val valid = cp?.takeIf { Character.isValidCodePoint(it) } ?: return null
        return String(Character.toChars(valid))
    }
}
