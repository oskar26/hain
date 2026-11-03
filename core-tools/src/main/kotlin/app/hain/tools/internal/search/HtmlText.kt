package app.hain.tools.internal.search

/** Title and readable text of one web page. */
data class PageText(
    val title: String,
    val text: String,
)

/**
 * Turns HTML into plain text for the brain (T-067), without a library: drops scripts, styles, menus,
 * headers and footers, keeps headings, paragraphs and list items, decodes the common entities and
 * cuts at a sentence end near [maxChars].
 */
object HtmlText {
    private val DROP =
        Regex(
            "<(script|style|noscript|svg|nav|header|footer|aside|form|iframe|template|button|select)\\b" +
                "[^>]*>.*?</\\1\\s*>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    private val COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val TITLE = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val OG_TITLE =
        Regex("<meta[^>]+property=[\"']og:title[\"'][^>]+content=[\"']([^\"']+)", RegexOption.IGNORE_CASE)
    private val BLOCK_END =
        Regex("</(p|div|li|h[1-6]|tr|section|article|blockquote|pre)\\s*>|<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val TAG = Regex("<[^>]+>")
    private val NUMERIC = Regex("&#(x?)([0-9a-fA-F]+);")
    private val SPACES = Regex("[ \\t\\u00A0]+")
    private val BLANK_LINES = Regex("\\n\\s*\\n+")
    private val ENTITIES =
        mapOf(
            "&amp;" to "&",
            "&lt;" to "<",
            "&gt;" to ">",
            "&quot;" to "\"",
            "&#39;" to "'",
            "&apos;" to "'",
            "&nbsp;" to " ",
            "&ndash;" to "–",
            "&mdash;" to "—",
            "&hellip;" to "…",
            "&auml;" to "ä",
            "&ouml;" to "ö",
            "&uuml;" to "ü",
            "&Auml;" to "Ä",
            "&Ouml;" to "Ö",
            "&Uuml;" to "Ü",
            "&szlig;" to "ß",
            "&euro;" to "€",
        )

    /** Shortest line that still counts as content (menus and buttons are shorter). */
    private const val MIN_LINE = 40

    fun extract(
        html: String,
        maxChars: Int,
    ): PageText {
        val title =
            (OG_TITLE.find(html)?.groupValues?.get(1) ?: TITLE.find(html)?.groupValues?.get(1))
                ?.let { decode(it).trim() }
                .orEmpty()
        val body = html.substringAfter("<body", html)
        val cleaned =
            body
                .replace(COMMENT, " ")
                .replace(DROP, " ")
                .replace(BLOCK_END, "\n")
                .replace(TAG, " ")
        val lines =
            decode(cleaned)
                .replace(SPACES, " ")
                .lines()
                .map { it.trim() }
                .filter { it.length >= MIN_LINE }
        val text = lines.joinToString("\n").replace(BLANK_LINES, "\n")
        return PageText(title, cut(text, maxChars))
    }

    /** Cuts [text] to at most [max] characters, at a sentence end when one is close. */
    fun cut(
        text: String,
        max: Int,
    ): String {
        if (text.length <= max) return text
        val head = text.take(max)
        val end = maxOf(head.lastIndexOf(". "), head.lastIndexOf(".\n"))
        return if (end > max * KEEP_SHARE) head.take(end + 1) else head.trimEnd() + "…"
    }

    fun decode(s: String): String {
        var out = s
        ENTITIES.forEach { (k, v) -> out = out.replace(k, v) }
        return NUMERIC.replace(out) { m ->
            val code = m.groupValues[2].toIntOrNull(if (m.groupValues[1].isEmpty()) DEC else HEX)
            if (code == null || code !in 1..MAX_CODE_POINT) m.value else String(Character.toChars(code))
        }
    }

    /** A sentence end in the last third is close enough to cut there. */
    private const val KEEP_SHARE = 2.0 / 3
    private const val DEC = 10
    private const val HEX = 16
    private const val MAX_CODE_POINT = 0x10FFFF
}
