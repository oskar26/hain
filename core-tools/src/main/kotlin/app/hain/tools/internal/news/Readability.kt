@file:Suppress(
    "MagicNumber",
    "ReturnCount",
    "LoopWithTooManyJumpStatements",
    "CyclomaticComplexMethod",
    "ComplexCondition",
    "NestedBlockDepth",
) // scoring weights and small hand-written scanners, kept in one readable place

package app.hain.tools.internal.news

import java.net.URI
import java.util.Locale
import kotlin.math.min

/** One part of an article in the reading view (T-076). */
sealed interface ArticleBlock {
    data class Heading(
        val text: String,
    ) : ArticleBlock

    data class Paragraph(
        val text: String,
    ) : ArticleBlock

    data class Quote(
        val text: String,
    ) : ArticleBlock

    /** One list entry; [number] is set in a numbered list. */
    data class ListItem(
        val text: String,
        val number: Int? = null,
    ) : ArticleBlock

    /** A picture or chart of the article, an https address, with its caption if there is one. */
    data class Image(
        val url: String,
        val caption: String? = null,
    ) : ArticleBlock
}

/**
 * The readable part of an article page (T-076). [locked] = the text is cut off or behind a login
 * or subscription wall (T-077; see [Paywall]); [textLength] counts only the article's own text.
 */
data class Article(
    val url: String,
    val title: String,
    val siteName: String?,
    val byline: String?,
    val leadImage: String?,
    val blocks: List<ArticleBlock>,
    val locked: Boolean,
) {
    val textLength: Int get() = text.length

    /** Plain text of the article, paragraphs separated by blank lines (for Pim's context). */
    val text: String by lazy {
        blocks
            .mapNotNull {
                when (it) {
                    is ArticleBlock.Heading -> it.text
                    is ArticleBlock.Paragraph -> it.text
                    is ArticleBlock.Quote -> it.text
                    is ArticleBlock.ListItem -> "• " + it.text
                    is ArticleBlock.Image -> null
                }
            }.joinToString("\n\n")
    }
}

/**
 * Readability-like extraction of the main article from a news page (T-076), own code: scores the
 * paragraphs' containers by text length, commas, class names and link density (the idea of Mozilla's
 * Readability), takes the best container and its fitting siblings and turns them into
 * [ArticleBlock]s with their images. Menus, comments, share buttons, forms and ads are dropped.
 * Pages without a good container fall back to the article text in the page's JSON-LD.
 */
@Suppress("TooManyFunctions") // one small function per extraction step
object Readability {
    /** Pages bigger than this are cut before parsing (some news pages are several MB of markup). */
    const val MAX_HTML = 1_500_000
    private const val MIN_PARAGRAPH = 25
    private const val MAX_IMAGES = 30
    private const val SIBLING_SHARE = 0.2
    private const val MIN_SIBLING_SCORE = 10.0
    private const val CLASS_WEIGHT = 25.0
    private const val ARTICLE_BONUS = 25.0
    private const val CHARS_PER_POINT = 100
    private const val MAX_LENGTH_POINTS = 3
    private const val LINK_DENSITY_CUT = 0.5
    private const val LONG_PARAGRAPH = 80
    private const val MIN_IMAGE_SIDE = 120
    private const val MIN_JSON_LD_BODY = 400

    private val UNLIKELY =
        Regex(
            "comment|disqus|footer|header-nav|menu|navigation|navbar|sidebar|share|sharing|social|related|" +
                "recommend|promo|advert|anzeige|newsletter|cookie|consent|banner|breadcrumb|subscribe|" +
                "teaser-list|more-stories|outbrain|taboola|popup|modal|skip|pagination|tags|toolbar|login",
        )
    private val MAYBE = Regex("article|body|content|main|story|text|column|entry|post")
    private val POSITIVE = Regex("article|body|content|entry|main|page|post|text|blog|story|paragraph|richtext")
    private val NEGATIVE =
        Regex(
            "hidden|comment|com-|contact|foot|footer|footnote|masthead|media-credit|meta|outbrain|promo|" +
                "related|scroll|share|shoutbox|sidebar|sponsor|shopping|tags|tool|widget|teaser|newsletter|ad-|" +
                "advert|abo-|paywall|login|subscribe",
        )
    private val DROP_TAGS =
        setOf(
            "nav",
            "aside",
            "footer",
            "form",
            "button",
            "iframe",
            "noscript",
            "select",
            "input",
            "object",
            "embed",
            "canvas",
            "video",
            "audio",
            "dialog",
            "menu",
        )
    private val SCORE_TAGS = setOf("p", "pre", "td", "blockquote")
    private const val ANCESTOR_LEVELS = 5
    private const val CLIMB_LEVELS = 8
    private const val CLIMB_ROUNDS = 3
    private const val CLIMB_RATIO = 1.6
    private const val CLIMB_MAX_LINKS = 0.35
    private val TOP_TAGS = setOf("#root", "html", "body")
    private const val ANCESTOR_FALLOFF = 3.0
    private const val MIN_SHORT_TEXT = 16
    private const val SENTENCE_END = ".!?:“”\"»«)"
    private val NOISE_WORDS = setOf("anzeige", "werbung", "advertisement", "ad", "video", "audio", "foto", "thema")
    private const val ARTICLE_BODY_KEY = "\"articleBody\""
    private val SRCSET_ENTRY = Regex("""\s*(\S+)(?:\s+(\d+)w)?""")

    /** The article of [html] loaded from [url]; [Paywall] decides [Article.locked]. */
    fun extract(
        html: String,
        url: String,
    ): Article {
        val page = html.take(MAX_HTML)
        val titleTag = StringBuilder()
        val root = HtmlTree.parse(page, titleTag)
        val meta = meta(root)
        val title =
            (meta["og:title"] ?: meta["twitter:title"] ?: titleTag.toString().ifEmpty { null } ?: firstH1(root))
                ?.let { clean(it) }
                .orEmpty()
        val lead = (meta["og:image"] ?: meta["twitter:image"] ?: meta["twitter:image:src"])?.let { absolute(url, it) }
        val byline = meta["author"]?.let { clean(it) }?.takeIf { it.length in 2..80 && !it.startsWith("http") }
        dropClutter(root)
        var blocks = bestContainer(root)?.let { blocks(it, url, title) }.orEmpty()
        val textLength = blocks.filterNot { it is ArticleBlock.Image }.sumOf { textOf(it).length }
        val jsonBody = jsonLdBody(page)
        if (jsonBody != null && jsonBody.length > textLength * 2 && jsonBody.length >= MIN_JSON_LD_BODY) {
            blocks = blocks.filterIsInstance<ArticleBlock.Image>().take(1) + paragraphsOf(jsonBody)
        }
        // the lead picture once at the top, not again inside the text
        val leadKey = lead?.let(::imageKey)
        blocks = blocks.filterNot { it is ArticleBlock.Image && leadKey != null && imageKey(it.url) == leadKey }
        val article =
            Article(
                url = url,
                title = title,
                siteName = meta["og:site_name"]?.let { clean(it) },
                byline = byline,
                leadImage = lead,
                blocks = blocks,
                locked = false,
            )
        return article.copy(locked = Paywall.locked(page, article.text))
    }

    // ── page metadata ───────────────────────────────────────────────────────────

    private fun meta(root: HtmlElement): Map<String, String> {
        val out = mutableMapOf<String, String>()
        root.descendants().filter { it.tag == "meta" }.forEach { m ->
            val key = (m.attr("property") ?: m.attr("name") ?: m.attr("itemprop"))?.lowercase(Locale.ROOT)
            val value = m.attr("content")
            if (key != null && value != null && key !in out) out[key] = value
        }
        return out
    }

    private fun firstH1(root: HtmlElement): String? =
        root
            .descendants()
            .firstOrNull { it.tag == "h1" }
            ?.text()
            ?.ifEmpty { null }

    /** The longest `"articleBody"` string in the page's JSON-LD (scanned by hand: bodies can be long). */
    private fun jsonLdBody(html: String): String? {
        var best: String? = null
        var from = 0
        while (true) {
            val keyAt = html.indexOf(ARTICLE_BODY_KEY, from)
            if (keyAt < 0) break
            var i = keyAt + ARTICLE_BODY_KEY.length
            while (i < html.length && (html[i] == ' ' || html[i] == ':' || html[i] == '\n')) i++
            from = i
            if (i >= html.length || html[i] != '"') continue
            val start = i + 1
            var j = start
            while (j < html.length && html[j] != '"') j += if (html[j] == '\\') 2 else 1
            val raw = html.substring(start, minOf(j, html.length))
            if (raw.length > (best?.length ?: 0)) best = raw
            from = j + 1
        }
        val body = best?.let(::unescapeJson) ?: return null
        return (if ('<' in body) HtmlTree.parse(body).text() else body.trim()).takeIf { it.isNotEmpty() }
    }

    @Suppress("MagicNumber")
    private fun unescapeJson(s: String): String {
        val out = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val e = s[i + 1]) {
                    'n' -> out.append('\n')
                    't' -> out.append(' ')
                    'r' -> Unit
                    'u' ->
                        if (i + 5 < s.length) {
                            s.substring(i + 2, i + 6).toIntOrNull(16)?.let { out.append(it.toChar()) }
                            i += 4
                        }
                    else -> out.append(e)
                }
                i += 2
            } else {
                out.append(c)
                i++
            }
        }
        return Entities.decode(out.toString())
    }

    private fun paragraphsOf(text: String): List<ArticleBlock> =
        text
            .split(Regex("""\n\s*\n|\n"""))
            .map { clean(it) }
            .filter { it.isNotEmpty() }
            .map { ArticleBlock.Paragraph(it) }

    // ── cleaning and scoring ────────────────────────────────────────────────────

    /** Removes menus, forms, comments, ads and other parts that are never the article. */
    internal fun dropClutter(root: HtmlElement) {
        // a "page with sidebar" wrapper still holds the article: never drop what contains it
        val holders = HashSet<HtmlElement>()
        root
            .descendants()
            .filter { it.tag == "article" || it.tag == "main" || it.attr("itemprop") == "articleBody" }
            .forEach { a ->
                var e = a.parent
                while (e != null && holders.add(e)) e = e.parent
            }
        val doomed =
            root
                .descendants()
                .filter { e ->
                    e.tag in DROP_TAGS ||
                        e.attr("hidden") != null ||
                        e.attr("aria-hidden") == "true" ||
                        e.attr("role") in setOf("navigation", "complementary", "dialog", "banner", "contentinfo") ||
                        (e.tag != "body" && e.tag != "article" && e.tag != "main" && e !in holders && unlikely(e))
                }.toList()
        doomed.forEach { e -> e.parent?.children?.remove(e) }
    }

    private fun unlikely(e: HtmlElement): Boolean {
        val ci = e.classAndId
        if (ci.isBlank()) return false
        return UNLIKELY.containsMatchIn(ci) && !MAYBE.containsMatchIn(ci)
    }

    private fun classWeight(e: HtmlElement): Double {
        val ci = e.classAndId
        var weight = 0.0
        if (NEGATIVE.containsMatchIn(ci)) weight -= CLASS_WEIGHT
        if (POSITIVE.containsMatchIn(ci)) weight += CLASS_WEIGHT
        return weight
    }

    @Suppress("MagicNumber")
    private fun tagWeight(e: HtmlElement): Double =
        when (e.tag) {
            "article", "main" -> ARTICLE_BONUS
            "div", "section" -> 5.0
            "pre", "td", "blockquote" -> 3.0
            "address", "ol", "ul", "dl", "dd", "dt", "li", "form" -> -3.0
            "h1", "h2", "h3", "h4", "h5", "h6", "th" -> -5.0
            else -> 0.0
        } + (if (e.attr("itemprop") == "articleBody") ARTICLE_BONUS else 0.0)

    internal fun bestContainer(root: HtmlElement): HtmlElement? {
        val scores = HashMap<HtmlElement, Double>()
        val paragraphChars = HashMap<HtmlElement, Int>()

        fun init(e: HtmlElement) = scores.getOrPut(e) { tagWeight(e) + classWeight(e) }
        root.descendants().filter { it.tag in SCORE_TAGS }.forEach { p ->
            val text = p.text()
            if (text.length < MIN_PARAGRAPH) return@forEach
            val points = 1.0 + text.count { it == ',' } + min(text.length / CHARS_PER_POINT, MAX_LENGTH_POINTS)
            var holder = p.parent
            repeat(CLIMB_LEVELS) {
                val h = holder ?: return@repeat
                paragraphChars[h] = (paragraphChars[h] ?: 0) + text.length
                holder = h.parent
            }
            // like Readability: the parent gets all points, the grandparent half, further up less
            var ancestor = p.parent
            var level = 0
            while (ancestor != null && ancestor.tag != "#root" && level < ANCESTOR_LEVELS) {
                val divider = if (level < 2) level + 1.0 else level * ANCESTOR_FALLOFF
                scores[ancestor] = init(ancestor) + points / divider
                ancestor = ancestor.parent
                level++
            }
        }
        if (scores.isEmpty()) return root.descendants().firstOrNull { it.tag == "body" } ?: root
        val ranked =
            scores.entries
                .map { (e, s) -> e to s * (1 - linkDensity(e)) }
                .sortedByDescending { it.second }
        val (first, topScore) = ranked.first()
        val top = climb(first, paragraphChars)
        // one level up when the article is split into several equally good blocks
        val parent = top.parent
        val wrapper =
            if (parent != null && parent.tag != "#root" && ranked.count { it.first.parent === parent } >= 3) {
                parent
            } else {
                top
            }
        val threshold = maxOf(MIN_SIBLING_SCORE, topScore * SIBLING_SHARE)
        val siblings =
            wrapper.parent
                ?.elements()
                ?.toList()
                .orEmpty()
        if (wrapper === top && siblings.size > 1) {
            val keep =
                siblings.filter { s ->
                    s === top ||
                        (scores[s] ?: 0.0) >= threshold ||
                        (s.tag == "p" && s.text().length > LONG_PARAGRAPH && linkDensity(s) < LINK_DENSITY_CUT / 2)
                }
            if (keep.size > 1) {
                val merged = HtmlElement("div", emptyMap(), wrapper.parent)
                merged.children += keep
                return merged
            }
        }
        return wrapper
    }

    /**
     * Pages that wrap every paragraph in its own box (BBC) make one box the winner: go up to the
     * nearest ancestor that holds clearly more of the article's paragraphs.
     */
    private fun climb(
        start: HtmlElement,
        chars: Map<HtmlElement, Int>,
    ): HtmlElement {
        var best = start
        repeat(CLIMB_ROUNDS) {
            var a = best.parent
            var level = 0
            while (a != null && a.tag !in TOP_TAGS && level < CLIMB_LEVELS) {
                if ((chars[a] ?: 0) >= (chars[best] ?: 0) * CLIMB_RATIO && linkDensity(a) < CLIMB_MAX_LINKS) {
                    best = a
                    return@repeat
                }
                a = a.parent
                level++
            }
            return best
        }
        return best
    }

    private fun linkDensity(e: HtmlElement): Double {
        val length = e.text().length
        return if (length == 0) 0.0 else e.linkTextLength().toDouble() / length
    }

    // ── blocks ──────────────────────────────────────────────────────────────────

    private fun blocks(
        container: HtmlElement,
        url: String,
        title: String,
    ): List<ArticleBlock> {
        val out = mutableListOf<ArticleBlock>()
        walk(container, url, out)
        val titleKey = key(title)
        return out
            .filterIndexed { i, b ->
                // the headline again at the top of the text
                !(i < 3 && (b is ArticleBlock.Heading || b is ArticleBlock.Paragraph) && key(textOf(b)) == titleKey) &&
                    !noise(b)
            }.distinctBy { if (it is ArticleBlock.Image) imageKey(it.url) else it.hashCode().toString() }
            .let { blocks ->
                var images = 0
                blocks.filter { b -> b !is ArticleBlock.Image || images++ < MAX_IMAGES }
            }
    }

    @Suppress("CyclomaticComplexMethod")
    private fun walk(
        e: HtmlElement,
        url: String,
        out: MutableList<ArticleBlock>,
    ) {
        when (e.tag) {
            "h1", "h2", "h3", "h4", "h5", "h6" ->
                e.text().takeIf { it.isNotEmpty() }?.let { out += ArticleBlock.Heading(clean(it)) }
            "p", "pre" -> {
                images(e, url).forEach { out += it }
                paragraph(e)?.let { out += ArticleBlock.Paragraph(it) }
            }
            "blockquote" -> paragraph(e)?.let { out += ArticleBlock.Quote(it) }
            "ul", "ol" -> list(e, url, out)
            "figure", "picture" -> figure(e, url)?.let { out += it }
            "img" -> image(e, url, null)?.let { out += it }
            "table" -> table(e, out)
            "figcaption" -> Unit
            else -> {
                val hasBlocks = e.elements().any { it.tag in HtmlTree.BLOCKS || it.tag == "img" || it.tag == "br" }
                if (!hasBlocks) {
                    paragraph(e)?.let { out += ArticleBlock.Paragraph(it) }
                } else {
                    walkMixed(e, url, out)
                }
            }
        }
    }

    /** A div with text and blocks mixed: loose text between blocks becomes its own paragraph. */
    private fun walkMixed(
        e: HtmlElement,
        url: String,
        out: MutableList<ArticleBlock>,
    ) {
        val loose = StringBuilder()

        fun flush() {
            val text = clean(loose.toString())
            if (text.length >= MIN_PARAGRAPH) out += ArticleBlock.Paragraph(text)
            loose.setLength(0)
        }
        e.children.forEach { child ->
            when {
                child is HtmlTextNode -> loose.append(child.text)
                child is HtmlElement && child.tag == "br" -> flush()
                child is HtmlElement && (child.tag in HtmlTree.BLOCKS || child.tag == "img") -> {
                    flush()
                    walk(child, url, out)
                }
                child is HtmlElement -> loose.append(' ').append(child.text()).append(' ')
            }
        }
        flush()
    }

    private fun paragraph(e: HtmlElement): String? {
        val text = clean(e.text())
        if (text.isEmpty()) return null
        val dense = linkDensity(e) > LINK_DENSITY_CUT && text.length < LONG_PARAGRAPH * 2
        return text.takeUnless { dense }
    }

    private fun list(
        e: HtmlElement,
        url: String,
        out: MutableList<ArticleBlock>,
    ) {
        val items = e.elements().filter { it.tag == "li" }.toList()
        if (items.isEmpty() || linkDensity(e) > LINK_DENSITY_CUT) return
        items.forEachIndexed { i, li ->
            images(li, url).forEach { out += it }
            val text = clean(li.text())
            if (text.isNotEmpty()) out += ArticleBlock.ListItem(text, if (e.tag == "ol") i + 1 else null)
        }
    }

    private fun table(
        e: HtmlElement,
        out: MutableList<ArticleBlock>,
    ) {
        e.descendants().filter { it.tag == "tr" }.forEach { row ->
            val cells =
                row
                    .elements()
                    .filter { it.tag == "td" || it.tag == "th" }
                    .map { clean(it.text()) }
                    .toList()
            val line = cells.filter { it.isNotEmpty() }.joinToString(" · ")
            if (line.isNotEmpty()) out += ArticleBlock.Paragraph(line)
        }
    }

    private fun figure(
        e: HtmlElement,
        url: String,
    ): ArticleBlock.Image? {
        val caption =
            e
                .descendants()
                .firstOrNull { it.tag == "figcaption" }
                ?.text()
                ?.let { clean(it) }
                ?.ifEmpty { null }
        val img = (sequenceOf(e) + e.descendants()).firstOrNull { it.tag == "img" }
        val source = e.descendants().firstOrNull { it.tag == "source" && it.attr("srcset") != null }
        return (img?.let { image(it, url, caption) })
            ?: source?.attr("srcset")?.let { set -> fromSrcset(set)?.let { absolute(url, it) } }?.let {
                ArticleBlock.Image(it, caption)
            }
    }

    private fun images(
        e: HtmlElement,
        url: String,
    ): List<ArticleBlock.Image> =
        e
            .descendants()
            .filter { it.tag == "img" }
            .mapNotNull { image(it, url, null) }
            .toList()

    @Suppress("ReturnCount")
    private fun image(
        img: HtmlElement,
        url: String,
        caption: String?,
    ): ArticleBlock.Image? {
        val width = img.attr("width")?.toIntOrNull()
        val height = img.attr("height")?.toIntOrNull()
        if ((width != null && width < MIN_IMAGE_SIDE) || (height != null && height < MIN_IMAGE_SIDE)) return null
        val raw =
            img.attr("data-src")
                ?: img.attr("data-lazy-src")
                ?: img.attr("data-original")
                ?: (img.attr("srcset") ?: img.attr("data-srcset"))?.let(::fromSrcset)
                ?: img.attr("src")
                ?: return null
        if (raw.startsWith("data:")) return null
        val src = absolute(url, raw) ?: return null
        val lower = src.lowercase(Locale.ROOT).substringBefore('?')
        if (lower.endsWith(".svg") || lower.endsWith(".gif") || "pixel" in lower || "tracking" in lower) return null
        val alt = img.attr("alt")?.let { clean(it) }?.ifEmpty { null }
        return ArticleBlock.Image(src, caption ?: alt?.takeIf { it.length > MIN_PARAGRAPH })
    }

    /** The largest picture up to 1600 px wide from a `srcset`. */
    @Suppress("MagicNumber")
    internal fun fromSrcset(set: String): String? {
        val entries =
            set.split(',').mapNotNull { part ->
                SRCSET_ENTRY.find(part)?.let { m -> m.groupValues[1] to (m.groupValues[2].toIntOrNull() ?: 0) }
            }
        if (entries.isEmpty()) return null
        val fitting = entries.filter { it.second <= 1600 }.ifEmpty { entries }
        return fitting.maxByOrNull { it.second }?.first
    }

    // ── helpers ─────────────────────────────────────────────────────────────────

    /** An https address for [raw] on the page [base]; http becomes https, anything else `null`. */
    fun absolute(
        base: String,
        raw: String,
    ): String? {
        val trimmed = raw.trim().replace(" ", "%20")
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("https://")) return trimmed
        if (trimmed.startsWith("http://")) return "https://" + trimmed.removePrefix("http://")
        val resolved =
            runCatching {
                if (trimmed.startsWith("//")) "https:$trimmed" else URI(base).resolve(trimmed).toString()
            }.getOrNull() ?: return null
        return when {
            resolved.startsWith("https://") -> resolved
            resolved.startsWith("http://") -> "https://" + resolved.removePrefix("http://")
            else -> null
        }
    }

    private fun imageKey(url: String): String = url.substringBefore('?').substringAfterLast('/').lowercase(Locale.ROOT)

    private fun textOf(b: ArticleBlock): String =
        when (b) {
            is ArticleBlock.Heading -> b.text
            is ArticleBlock.Paragraph -> b.text
            is ArticleBlock.Quote -> b.text
            is ArticleBlock.ListItem -> b.text
            is ArticleBlock.Image -> ""
        }

    /** "Anzeige", "|", "Video", a lone date: short bits that are page furniture, not text. */
    private fun noise(b: ArticleBlock): Boolean {
        val text = (b as? ArticleBlock.Paragraph)?.text ?: return false
        if (text.lowercase(Locale.ROOT) in NOISE_WORDS) return true
        return text.length < MIN_SHORT_TEXT && text.last() !in SENTENCE_END
    }

    private fun key(s: String) = s.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }

    private fun clean(s: String): String =
        s
            .replace("\u00AD", "")
            .replace(Regex("""[ \t\r\n\u00A0]+"""), " ")
            .trim()
}
