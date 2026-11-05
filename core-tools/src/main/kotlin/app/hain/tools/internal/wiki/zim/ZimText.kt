package app.hain.tools.internal.wiki.zim

/**
 * T-237: the readable intro of a Kiwix article page: the text of its first paragraphs, without
 * tags, footnote marks ("[1]") or HTML entities, at most [MAX] characters.
 */
object ZimText {
    const val MAX = 1200

    private val PARAGRAPH = Regex("<p\\b[^>]*>(.*?)</p>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val DROP =
        Regex(
            "<(sup|style|script|table)\\b[^>]*>.*?</\\1>",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
        )

    // a tag, also when an attribute value holds a ">" (Kiwix keeps data-mw JSON in attributes)
    private val TAG = Regex("<(?:[^>\"']|\"[^\"]*\"|'[^']*')*>")
    private val SPACE = Regex("\\s+")
    private val FOOTNOTE = Regex("\\[\\d+]")
    private val NUMERIC = Regex("&#(x?)([0-9a-fA-F]+);")
    private val NAMED =
        mapOf(
            "&amp;" to "&",
            "&lt;" to "<",
            "&gt;" to ">",
            "&quot;" to "\"",
            "&apos;" to "'",
            "&#39;" to "'",
            "&nbsp;" to " ",
            "&shy;" to "",
            "&ndash;" to "–",
            "&mdash;" to "—",
        )

    fun intro(html: String): String {
        val body = DROP.replace(html, " ")
        val paragraphs = PARAGRAPH.findAll(body).toList()
        // the lead starts with the bold title ("Der <b>Kölner Dom</b> …"); skip infobox lines before it
        val lead = paragraphs.indexOfFirst { "<b>" in it.groupValues[1] || "<b " in it.groupValues[1] }
        val out = StringBuilder()
        val texts =
            (if (lead > 0) paragraphs.drop(lead) else paragraphs)
                .mapIndexed { i, m -> clean(m.groupValues[1]) to (i == 0 && lead >= 0) }
                // short lines are noise, except the lead itself ("Merkur steht für:")
                .filter { (text, isLead) -> text.isNotEmpty() && (text.length >= MIN_PARAGRAPH || isLead) }
                .map { it.first }
        for (text in texts) {
            if (out.length >= MAX) break
            if (out.isNotEmpty()) out.append("\n\n")
            out.append(text)
        }
        return out.toString().take(MAX).trim()
    }

    fun clean(fragment: String): String {
        var t = TAG.replace(fragment, "")
        NAMED.forEach { (k, v) -> t = t.replace(k, v) }
        t =
            NUMERIC.replace(t) { m ->
                val code = m.groupValues[2].toIntOrNull(if (m.groupValues[1].isEmpty()) DEC else HEX)
                code?.let { String(Character.toChars(it)) } ?: m.value
            }
        return SPACE.replace(FOOTNOTE.replace(t.replace(SOFT_HYPHEN, ""), ""), " ").trim()
    }

    /** A page that only lists meanings ("Merkur steht für:", "Mercury may refer to:"). */
    fun isDisambiguation(
        html: String,
        intro: String,
    ): Boolean {
        val head = intro.take(HEAD).lowercase()
        return "steht für" in head ||
            "may refer to" in head ||
            "bezeichnet:" in head ||
            "Begriffsklärungsseite" in html ||
            "disambiguation" in html.take(DISAMBIG_SCAN).lowercase()
    }

    private const val SOFT_HYPHEN = "\u00AD"
    private const val MIN_PARAGRAPH = 25
    private const val DEC = 10
    private const val HEX = 16
    private const val HEAD = 200
    private const val DISAMBIG_SCAN = 3000
}
