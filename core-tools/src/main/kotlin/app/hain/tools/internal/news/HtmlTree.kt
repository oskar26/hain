@file:Suppress("MagicNumber", "ReturnCount", "ComplexCondition") // a hand-written tokenizer: offsets of "<!--", "-->" …

package app.hain.tools.internal.news

import java.util.Locale

/**
 * A small, forgiving HTML tree for the reading view (T-076). Pages are untrusted: nothing is ever
 * executed, scripts and styles are dropped while parsing, and broken markup is closed the way a
 * browser roughly would. Only what the article extraction needs is kept (tags, attributes, text).
 */
internal sealed interface HtmlNode

internal class HtmlTextNode(
    val text: String,
) : HtmlNode

internal class HtmlElement(
    val tag: String,
    val attrs: Map<String, String>,
    val parent: HtmlElement?,
) : HtmlNode {
    val children = mutableListOf<HtmlNode>()

    fun attr(name: String): String? = attrs[name]?.trim()?.ifEmpty { null }

    /** `class` and `id` together, lower case, for the "looks like an article / looks like a menu" rules. */
    val classAndId: String by lazy { ((attrs["class"] ?: "") + " " + (attrs["id"] ?: "")).lowercase(Locale.ROOT) }

    fun elements(): Sequence<HtmlElement> = children.asSequence().filterIsInstance<HtmlElement>()

    /** Every element below this one, depth first. */
    fun descendants(): Sequence<HtmlElement> =
        sequence {
            val stack = ArrayDeque<HtmlElement>()
            elements().toList().asReversed().forEach { stack.addLast(it) }
            while (stack.isNotEmpty()) {
                val e = stack.removeLast()
                yield(e)
                e
                    .elements()
                    .toList()
                    .asReversed()
                    .forEach { stack.addLast(it) }
            }
        }

    /** Visible text below this element, whitespace collapsed. */
    fun text(): String {
        val out = StringBuilder()
        appendText(this, out)
        return out.toString().replace(SPACES, " ").trim()
    }

    /** Text inside links below this element (for the link density). */
    fun linkTextLength(): Int =
        (if (tag == "a") listOf(this) else descendants().filter { it.tag == "a" }.toList()).sumOf { it.text().length }

    private fun appendText(
        node: HtmlNode,
        out: StringBuilder,
    ) {
        when (node) {
            is HtmlTextNode -> out.append(node.text)
            is HtmlElement -> {
                if (node.tag == "br") out.append(' ')
                node.children.forEach { appendText(it, out) }
                if (node.tag in HtmlTree.BLOCKS) out.append(' ')
            }
        }
    }

    private companion object {
        val SPACES = Regex("""[ \t\r\n\u00A0]+""")
    }
}

internal object HtmlTree {
    /** Elements without an end tag. */
    private val VOID =
        setOf(
            "area",
            "base",
            "br",
            "col",
            "embed",
            "hr",
            "img",
            "input",
            "link",
            "meta",
            "param",
            "source",
            "track",
            "wbr",
        )

    /** Elements whose content is never text for the reader (dropped while parsing). */
    private val RAW = setOf("script", "style", "template", "svg", "math", "textarea", "title")

    /** Block elements: they end an open paragraph and separate words. */
    val BLOCKS =
        setOf(
            "p",
            "div",
            "section",
            "article",
            "main",
            "aside",
            "header",
            "footer",
            "nav",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "ul",
            "ol",
            "li",
            "table",
            "tr",
            "td",
            "th",
            "blockquote",
            "figure",
            "figcaption",
            "pre",
            "form",
            "dl",
            "dt",
            "dd",
            "hr",
            "picture",
        )

    private val TAG_NAME = Regex("""^[a-zA-Z][a-zA-Z0-9:-]*""")
    private val ATTR = Regex("""([^\s"'>/=]+)(?:\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s"'>]+)))?""")

    /** Parses [html] into a tree under a synthetic `#root` element. The `<title>` text is kept in [titleOut]. */
    @Suppress("CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
    fun parse(
        html: String,
        titleOut: StringBuilder? = null,
    ): HtmlElement {
        val root = HtmlElement("#root", emptyMap(), null)
        var current = root
        var i = 0
        val n = html.length
        val text = StringBuilder()

        fun flushText() {
            if (text.isNotEmpty()) {
                current.children += HtmlTextNode(Entities.decode(text.toString()))
                text.setLength(0)
            }
        }
        while (i < n) {
            val c = html[i]
            if (c != '<') {
                text.append(c)
                i++
                continue
            }
            when {
                html.startsWith("<!--", i) -> {
                    val end = html.indexOf("-->", i + 4)
                    i = if (end < 0) n else end + 3
                }
                html.startsWith("<![CDATA[", i) -> {
                    val end = html.indexOf("]]>", i)
                    text.append(html, i + 9, if (end < 0) n else end)
                    i = if (end < 0) n else end + 3
                }
                i + 1 < n && (html[i + 1] == '!' || html[i + 1] == '?') -> {
                    val end = html.indexOf('>', i)
                    i = if (end < 0) n else end + 1
                }
                i + 1 < n && html[i + 1] == '/' -> {
                    val end = html.indexOf('>', i)
                    val name =
                        TAG_NAME.find(html.substring(i + 2, if (end < 0) n else end))?.value?.lowercase(Locale.ROOT)
                    i = if (end < 0) n else end + 1
                    if (name != null) {
                        flushText()
                        var e: HtmlElement? = current
                        while (e != null && e.tag != name) e = e.parent
                        if (e != null && e !== root) current = e.parent ?: root
                    }
                }
                else -> {
                    val end = tagEnd(html, i)
                    if (end < 0) {
                        text.append(c)
                        i++
                        continue
                    }
                    val inside = html.substring(i + 1, end)
                    val name = TAG_NAME.find(inside)?.value?.lowercase(Locale.ROOT)
                    if (name == null) {
                        text.append(c)
                        i++
                        continue
                    }
                    flushText()
                    i = end + 1
                    if (name in RAW) {
                        val close = html.indexOf("</$name", i, ignoreCase = true)
                        val stop = if (close < 0) n else close
                        if (name == "title" && titleOut != null && titleOut.isEmpty()) {
                            titleOut.append(Entities.decode(html.substring(i, stop)).trim())
                        }
                        val closeEnd = if (close < 0) n else html.indexOf('>', close).let { if (it < 0) n else it + 1 }
                        i = closeEnd
                        continue
                    }
                    val attrs = attributes(inside.substring(name.length))
                    // a new block ends an open paragraph, a new list item the open one
                    if (name in BLOCKS && current.tag == "p") current = current.parent ?: root
                    if (name == "li") {
                        var e: HtmlElement? = current
                        while (e != null && e.tag != "li" && e.tag != "ul" && e.tag != "ol") e = e.parent
                        if (e != null && e.tag == "li") current = e.parent ?: root
                    }
                    val element = HtmlElement(name, attrs, current)
                    current.children += element
                    val selfClosing = inside.endsWith("/")
                    if (name !in VOID && !selfClosing) current = element
                }
            }
        }
        flushText()
        return root
    }

    /** The `>` that ends the tag starting at [start], skipping `>` inside quoted attribute values. */
    private fun tagEnd(
        html: String,
        start: Int,
    ): Int {
        var quote = 0.toChar()
        var i = start + 1
        while (i < html.length) {
            val c = html[i]
            when {
                quote != 0.toChar() -> if (c == quote) quote = 0.toChar()
                c == '"' || c == '\'' -> quote = c
                c == '>' -> return i
                c == '<' && i == start + 1 -> return -1
            }
            i++
        }
        return -1
    }

    private fun attributes(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        val out = LinkedHashMap<String, String>()
        ATTR.findAll(raw).forEach { m ->
            val key = m.groupValues[1].lowercase(Locale.ROOT)
            if (key == "/" || key.isEmpty() || out.containsKey(key)) return@forEach
            val value = m.groups[2]?.value ?: m.groups[3]?.value ?: m.groups[4]?.value ?: ""
            out[key] = Entities.decode(value)
        }
        return out
    }
}

/** HTML character references: numeric ones and the named ones that turn up in news pages. */
internal object Entities {
    private val ENTITY = Regex("""&(#x[0-9a-fA-F]+|#[0-9]+|[a-zA-Z][a-zA-Z0-9]*);?""")
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
            "copy" to "©",
            "eacute" to "é",
            "egrave" to "è",
            "aacute" to "á",
            "agrave" to "à",
            "ccedil" to "ç",
            "oacute" to "ó",
            "iacute" to "í",
            "uacute" to "ú",
            "ntilde" to "ñ",
            "times" to "×",
            "thinsp" to " ",
            "ensp" to " ",
            "emsp" to " ",
            "zwnj" to "",
            "zwj" to "",
            "lrm" to "",
            "rlm" to "",
            "bull" to "•",
            "rarr" to "→",
            "larr" to "←",
            "trade" to "™",
            "reg" to "®",
            "sect" to "§",
            "frac12" to "½",
            "plusmn" to "±",
            "minus" to "−",
            "prime" to "′",
            "Prime" to "″",
        )

    fun decode(text: String): String {
        if ('&' !in text) return text
        return ENTITY.replace(text) { m ->
            val name = m.groupValues[1]
            when {
                name.startsWith("#x") || name.startsWith("#X") -> cp(name.drop(2).toIntOrNull(HEX))
                name.startsWith("#") -> cp(name.drop(1).toIntOrNull())
                else -> NAMED[name]
            } ?: m.value
        }
    }

    private fun cp(value: Int?): String? {
        val valid = value?.takeIf { Character.isValidCodePoint(it) && it != 0 } ?: return null
        return String(Character.toChars(valid))
    }
}
