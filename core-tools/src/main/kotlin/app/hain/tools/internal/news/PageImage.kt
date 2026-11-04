package app.hain.tools.internal.news

import java.util.Locale

/**
 * The picture an article page names for sharing (`og:image`, `twitter:image`), for list cards whose
 * feed has no picture (T-075). Only the page's head is looked at.
 */
object PageImage {
    private const val HEAD_CHARS = 300_000
    private val META = Regex("""<meta\b[^>]*>""", RegexOption.IGNORE_CASE)
    private val ATTR = Regex("""([a-zA-Z:-]+)\s*=\s*(?:"([^"]*)"|'([^']*)')""")
    private val KEYS = listOf("og:image", "og:image:url", "og:image:secure_url", "twitter:image", "twitter:image:src")

    /** An https address of the page's picture, or `null`. */
    @Suppress("MagicNumber") // regex group numbers
    fun find(
        html: String,
        pageUrl: String,
    ): String? {
        val head = html.take(HEAD_CHARS).substringBefore("</head>")
        val found = mutableMapOf<String, String>()
        META.findAll(head).forEach { tag ->
            val attrs =
                ATTR.findAll(tag.value).associate { m ->
                    val value = m.groups[2]?.value ?: m.groups[3]?.value ?: ""
                    m.groupValues[1].lowercase(Locale.ROOT) to value
                }
            val key = (attrs["property"] ?: attrs["name"])?.lowercase(Locale.ROOT) ?: return@forEach
            val content = attrs["content"]?.trim()?.takeIf { it.isNotEmpty() } ?: return@forEach
            if (key in KEYS && key !in found) found[key] = Entities.decode(content)
        }
        return KEYS.firstNotNullOfOrNull { key -> found[key]?.let { Readability.absolute(pageUrl, it) } }
    }
}
