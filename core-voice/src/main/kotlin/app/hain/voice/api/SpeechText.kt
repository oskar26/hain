package app.hain.voice.api

/** Words read instead of parts that make no sense spoken (strings from the app, DE or EN). */
data class SpeechLabels(
    val codeSkipped: String,
    val formulaSkipped: String,
    val tableSkipped: String = "",
)

/**
 * Turns an answer (Markdown) into plain sentences for read aloud (VOICE.md "Text prep", T-141):
 * code blocks become "Codeblock übersprungen", links keep their text, bare URLs keep only the
 * domain, list items and headings become their own sentences, markers like `**`, `#` or `|` go away.
 */
object SpeechText {
    private val FENCE = Regex("^\\s*(```|~~~)")
    private val DISPLAY_MATH = Regex("\\$\\$[\\s\\S]*?\\$\\$|\\\\\\[[\\s\\S]*?\\\\]")
    private val INLINE_MATH = Regex("(?<![\\\\\\w])\\$([^$\\n]{1,80})\\$")
    private val IMAGE = Regex("!\\[([^\\]]*)]\\([^)]*\\)")
    private val LINK = Regex("\\[([^\\]]+)]\\((?:[^()]|\\([^)]*\\))*\\)")
    private val AUTOLINK = Regex("<(https?://[^>\\s]+)>")
    private val URL = Regex("\\bhttps?://([^\\s/?#)\\]]+)[^\\s)\\]]*|\\bwww\\.([^\\s/?#)\\]]+)[^\\s)\\]]*")
    private val CITATION = Regex("\\[(\\d{1,3}(?:\\s*[,–-]\\s*\\d{1,3})*)]")
    private val HTML = Regex("</?[a-zA-Z][^>]{0,200}>")
    private val INLINE_CODE = Regex("`+([^`]+)`+")
    private val BOLD = Regex("(\\*\\*|__)(?=\\S)(.+?)(?<=\\S)\\1")
    private val ITALIC_STAR = Regex("(?<![\\w*])\\*(?=\\S)([^*\\n]+?)(?<=\\S)\\*(?![\\w*])")
    private val ITALIC_UNDER = Regex("(?<![\\w_])_(?=\\S)([^_\\n]+?)(?<=\\S)_(?![\\w_])")
    private val STRIKE = Regex("~~(.+?)~~")
    private val HEADING = Regex("^\\s{0,3}#{1,6}\\s+(.*?)\\s*#*\\s*$")
    private val QUOTE = Regex("^\\s*(>\\s?)+")
    private val BULLET = Regex("^\\s*([-*+•]|\\d{1,3}[.)])\\s+(\\[[ xX]]\\s+)?")
    private val RULE = Regex("^\\s*([-*_]\\s*){3,}$")
    private val TABLE_SEPARATOR = Regex("^\\s*\\|?\\s*:?-{2,}:?\\s*(\\|\\s*:?-{2,}:?\\s*)*\\|?\\s*$")
    private val SPACES = Regex("[ \\t\\u00A0]+")
    private val END_PUNCT = Regex("[.!?…:;]$")

    /**
     * Plain text, one block per line (headings, list items and paragraphs each on their own). One branch
     * per kind of line: fence, heading, list, table, quote, paragraph.
     */
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements")
    fun fromMarkdown(
        markdown: String,
        labels: SpeechLabels,
    ): String {
        val blocks = mutableListOf<String>()
        val paragraph = StringBuilder()

        fun flush() {
            val text = inline(paragraph.toString())
            if (text.isNotBlank()) blocks += sentenceEnd(text)
            paragraph.clear()
        }
        var inCode = false
        var tableNoted = false
        val source = DISPLAY_MATH.replace(markdown.replace("\r\n", "\n")) { "\n\u0000MATH\u0000\n" }
        for (raw in source.lines()) {
            if (FENCE.containsMatchIn(raw)) {
                if (!inCode) {
                    flush()
                    blocks += sentenceEnd(labels.codeSkipped)
                }
                inCode = !inCode
                continue
            }
            if (inCode) continue
            val line = raw.trimEnd()
            when {
                line.isBlank() -> flush().also { tableNoted = false }
                line == "\u0000MATH\u0000" -> {
                    flush()
                    blocks += sentenceEnd(labels.formulaSkipped)
                }
                RULE.matches(line) || TABLE_SEPARATOR.matches(line) -> flush()
                line.trimStart().startsWith("|") -> {
                    flush()
                    if (labels.tableSkipped.isNotBlank()) {
                        if (!tableNoted) blocks += sentenceEnd(labels.tableSkipped)
                        tableNoted = true
                    } else {
                        tableRow(line)?.let { blocks += sentenceEnd(it) }
                    }
                }
                HEADING.matches(line) -> {
                    flush()
                    val text = inline(HEADING.find(line)!!.groupValues[1])
                    if (text.isNotBlank()) blocks += sentenceEnd(text)
                }
                BULLET.containsMatchIn(QUOTE.replace(line, "")) -> {
                    flush()
                    paragraph.append(BULLET.replace(QUOTE.replace(line, ""), ""))
                }
                else -> {
                    if (paragraph.isNotEmpty()) paragraph.append(' ')
                    paragraph.append(QUOTE.replace(line, "").trim())
                }
            }
        }
        flush()
        return blocks.joinToString("\n")
    }

    /** Splits prepared text into sentences that are spoken one after another. */
    @Suppress("NestedBlockDepth", "CyclomaticComplexMethod") // a small scanner over each line
    fun sentences(
        text: String,
        maxChars: Int = MAX_SENTENCE,
    ): List<String> {
        val out = mutableListOf<String>()
        for (block in text.lines()) {
            val line = block.trim()
            if (line.isEmpty()) continue
            var start = 0
            var i = 0
            while (i < line.length) {
                val c = line[i]
                if (c in ENDERS && isSentenceEnd(line, i)) {
                    var end = i + 1
                    while (end < line.length && (line[end] in CLOSERS || line[end] in ENDERS)) end++
                    out += line.substring(start, end).trim()
                    start = end
                    i = end
                    continue
                }
                i++
            }
            if (start < line.length) {
                line
                    .substring(start)
                    .trim()
                    .takeIf { it.isNotEmpty() }
                    ?.let { out += it }
            }
        }
        return out.flatMap { split(it, maxChars) }.filter { s -> s.any { it.isLetterOrDigit() } }
    }

    /** Both steps: Markdown in, sentences out. */
    fun speakable(
        markdown: String,
        labels: SpeechLabels,
    ): List<String> = sentences(fromMarkdown(markdown, labels))

    private fun inline(text: String): String {
        var s = text
        s = IMAGE.replace(s) { it.groupValues[1] }
        s = LINK.replace(s) { it.groupValues[1] }
        s = AUTOLINK.replace(s) { it.groupValues[1] }
        s = URL.replace(s) { m -> domain(m.groupValues[1].ifEmpty { m.groupValues[2] }) }
        s = CITATION.replace(s, "")
        s = HTML.replace(s, " ")
        s = INLINE_CODE.replace(s) { it.groupValues[1] }
        s = INLINE_MATH.replace(s) { it.groupValues[1].replace("\\", "") }
        s = BOLD.replace(s) { it.groupValues[2] }
        s = ITALIC_STAR.replace(s) { it.groupValues[1] }
        s = ITALIC_UNDER.replace(s) { it.groupValues[1] }
        s = STRIKE.replace(s) { it.groupValues[1] }
        s =
            s
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
        s = s.replace(" ,", ",").replace(" .", ".")
        return SPACES.replace(s, " ").trim()
    }

    private fun domain(host: String): String = host.removePrefix("www.").trimEnd('.', ',', ';', ':')

    private fun tableRow(line: String): String? {
        val cells =
            line
                .trim()
                .trim('|')
                .split('|')
                .map { inline(it) }
                .filter { it.isNotBlank() }
        return cells.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    private fun sentenceEnd(text: String): String = if (END_PUNCT.containsMatchIn(text)) text else "$text."

    @Suppress("ReturnCount", "ComplexCondition", "CyclomaticComplexMethod") // the rules for a full stop
    private fun isSentenceEnd(
        line: String,
        i: Int,
    ): Boolean {
        val next = line.getOrNull(i + 1)
        if (next != null && next !in CLOSERS && !next.isWhitespace() && next != line[i]) return false
        if (line[i] == '.') {
            // 3.5, z.B., Dr. Müller, usw. — not the end of a sentence
            if (next != null && next.isDigit()) return false
            val word = line.substring(0, i).takeLastWhile { !it.isWhitespace() }.lowercase()
            if (word in ABBREVIATIONS || (word.length == 1 && word[0].isLetter())) return false
            if (word.isNotEmpty() && word.all { it.isDigit() } && word.length <= 2) return false // "1. Mai"
        }
        val after = line.substring(i + 1).trimStart { it in CLOSERS || it in ENDERS }.trimStart()
        return after.isEmpty() || !after[0].isLowerCase()
    }

    /** Very long sentences are cut at a comma, semicolon or dash so the voice can start sooner. */
    private fun split(
        sentence: String,
        maxChars: Int,
    ): List<String> {
        if (sentence.length <= maxChars) return listOf(sentence)
        val out = mutableListOf<String>()
        var rest = sentence
        while (rest.length > maxChars) {
            val window = rest.substring(0, maxChars)
            val cut =
                maxOf(window.lastIndexOf(", "), window.lastIndexOf("; "), window.lastIndexOf(" – "))
                    .takeIf { it > maxChars / MIN_CUT_PART }
                    ?: window.lastIndexOf(' ').takeIf { it > 0 }
                    ?: maxChars
            out += rest.substring(0, cut + 1).trim()
            rest = rest.substring(cut + 1).trim()
        }
        if (rest.isNotEmpty()) out += rest
        return out
    }

    private const val MAX_SENTENCE = 280
    private const val MIN_CUT_PART = 3
    private const val CLOSERS = "\"'”“»«)]"
    private const val ENDERS = ".!?…"
    private val ABBREVIATIONS =
        setOf(
            "z.b",
            "z. b",
            "d.h",
            "u.a",
            "usw",
            "bzw",
            "ca",
            "dr",
            "prof",
            "nr",
            "str",
            "vgl",
            "evtl",
            "ggf",
            "inkl",
            "etc",
            "mr",
            "mrs",
            "ms",
            "vs",
            "e.g",
            "i.e",
            "no",
            "st",
            "bspw",
            "s",
            "jh",
            "mio",
            "mrd",
        )
}
