package app.hain.feature.chat.export

/**
 * The Markdown of a message as PDF blocks (T-054): paragraphs, headings, lists (also nested),
 * quotes, code fences, tables (as monospaced lines) and rules. Inline marks (bold, italic, code,
 * links) are taken out of the text; a link keeps its address in brackets.
 */
object MarkdownBlocks {
    @Suppress("CyclomaticComplexMethod", "LongMethod", "LoopWithTooManyJumpStatements", "NestedBlockDepth")
    fun parse(markdown: String): List<PdfBlock> {
        val out = mutableListOf<PdfBlock>()
        val lines = markdown.replace("\r\n", "\n").split('\n')
        val paragraph = StringBuilder()

        fun spaceBefore(): Float = if (out.isEmpty()) 0f else GAP

        fun flush() {
            if (paragraph.isBlank()) return
            out += PdfBlock.Text(PdfStyle.BODY, inline(paragraph.toString()), spaceBefore = spaceBefore())
            paragraph.clear()
        }

        var i = 0
        while (i < lines.size) {
            val raw = lines[i]
            val line = raw.trimEnd()
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith("```") || trimmed.startsWith("~~~") -> {
                    flush()
                    val fence = trimmed.take(FENCE)
                    val code = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].trimStart().startsWith(fence)) code += lines[i++]
                    out += PdfBlock.Code(code, spaceBefore = spaceBefore())
                }
                trimmed.isEmpty() -> flush()
                RULE.matches(trimmed) -> {
                    flush()
                    out += PdfBlock.Rule(spaceBefore = spaceBefore())
                }
                trimmed.startsWith("#") && HEADING.matches(trimmed) -> {
                    flush()
                    val text =
                        trimmed
                            .trimStart('#')
                            .trim()
                            .trimEnd('#')
                            .trim()
                    out +=
                        PdfBlock.Text(PdfStyle.HEADING, inline(text), keepWithNext = true, spaceBefore = spaceBefore())
                }
                trimmed.startsWith("|") -> {
                    flush()
                    val table = mutableListOf<String>()
                    while (i < lines.size && lines[i].trimStart().startsWith("|")) {
                        val row = lines[i].trim()
                        if (!TABLE_RULE.matches(row)) table += inline(row)
                        i++
                    }
                    out += PdfBlock.Code(table, spaceBefore = spaceBefore())
                    continue
                }
                trimmed.startsWith(">") -> {
                    flush()
                    val quote = StringBuilder()
                    while (i < lines.size && lines[i].trimStart().startsWith(">")) {
                        quote.append(lines[i].trimStart().removePrefix(">").trim()).append(' ')
                        i++
                    }
                    val text = inline(quote.toString())
                    out += PdfBlock.Text(PdfStyle.QUOTE, text, QUOTE_INDENT, spaceBefore = spaceBefore())
                    continue
                }
                BULLET.matches(trimmed) || NUMBER.matches(trimmed) -> {
                    flush()
                    val level = (line.length - trimmed.length) / LIST_STEP
                    val number = NUMBER.matchEntire(trimmed)
                    val (mark, text) =
                        if (number != null) {
                            "${number.groupValues[1]}." to number.groupValues[2]
                        } else {
                            "•" to trimmed.drop(2)
                        }
                    val item = StringBuilder(text)
                    // lines right below that are not a new block belong to the same item
                    while (i + 1 < lines.size && continues(lines[i + 1])) item.append(' ').append(lines[++i].trim())
                    val first = out.lastOrNull().let { it !is PdfBlock.Text || it.bullet == null }
                    out +=
                        PdfBlock.Text(
                            PdfStyle.BODY,
                            inline(item.toString()),
                            indent = level.coerceAtMost(MAX_LEVEL) * LIST_INDENT,
                            bullet = mark,
                            spaceBefore = if (first) spaceBefore() else ITEM_GAP,
                        )
                }
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(trimmed)
                }
            }
            i++
        }
        flush()
        return out
    }

    private fun continues(next: String): Boolean {
        val t = next.trim()
        return t.isNotEmpty() &&
            next.startsWith("  ") &&
            !BULLET.matches(t) &&
            !NUMBER.matches(t) &&
            !t.startsWith("```") &&
            !t.startsWith("#") &&
            !t.startsWith(">") &&
            !t.startsWith("|")
    }

    /** Takes out inline Markdown: **bold**, *italic*, `code`, [text](url) → "text (url)", ~~strike~~. */
    fun inline(text: String): String =
        text
            .replace(IMAGE, "$1")
            .replace(LINK) { m ->
                val label = m.groupValues[1]
                val url = m.groupValues[2]
                if (label == url || label.isBlank()) url else "$label ($url)"
            }.replace(CODE, "$1")
            .replace(BOLD, "$2")
            .replace(STRIKE, "$1")
            .replace(ITALIC, "$1$2")
            .replace(Regex("\\\\([\\\\`*_{}\\[\\]()#+\\-.!|>])"), "$1")
            .replace(Regex("\\s+"), " ")
            .trim()

    private const val GAP = 7f
    private const val ITEM_GAP = 3f
    private const val FENCE = 3
    private const val LIST_STEP = 2
    private const val LIST_INDENT = 14f
    private const val QUOTE_INDENT = 12f
    private const val MAX_LEVEL = 4
    private val RULE = Regex("^([-*_])(\\s*\\1){2,}$")
    private val HEADING = Regex("^#{1,6}\\s+.*")
    private val TABLE_RULE = Regex("^\\|?\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)*\\|?$")
    private val BULLET = Regex("^[-*+]\\s+.*")
    private val NUMBER = Regex("^(\\d{1,3})[.)]\\s+(.*)")
    private val IMAGE = Regex("!\\[([^\\]]*)]\\([^)]*\\)")
    private val LINK = Regex("\\[([^\\]]*)]\\(([^)\\s]+)\\)")
    private val CODE = Regex("`([^`]+)`")
    private val BOLD = Regex("(\\*\\*|__)(.+?)\\1")
    private val STRIKE = Regex("~~(.+?)~~")
    private val ITALIC =
        Regex(
            "(?<![\\w*])\\*(?!\\s)(.+?)(?<!\\s)\\*(?!\\w)" +
                "|(?<![\\w_])_(?!\\s)(.+?)(?<!\\s)_(?![\\w])",
        )
}
