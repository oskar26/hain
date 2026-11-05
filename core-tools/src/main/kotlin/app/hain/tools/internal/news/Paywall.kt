package app.hain.tools.internal.news

import java.util.Locale

/**
 * Is an article really cut off (T-077)? A news site is never marked as "paywall" as a whole (junge
 * Welt, nd, ZEIT … all have free articles); only an article whose text Hain could not read in full.
 * Signals, each only together with a short article text:
 * - the page itself says so in its schema.org data (`"isAccessibleForFree": false`),
 * - the page shows a subscription or login wall ("Weiterlesen mit …", "Zugang zu allen Artikeln",
 *   "Subscribe to continue" …).
 * A long, complete text is never marked, even when the page asks for a subscription or donation
 * somewhere (junge Welt's "Jetzt abonnieren" under every free article).
 */
object Paywall {
    /** Below this many characters of article text a page that says "not free" counts as cut off. */
    const val NOT_FREE_MAX_TEXT = 3_000

    /** Below this many characters a subscription wall phrase counts. */
    const val WALL_MAX_TEXT = 1_500

    private val NOT_FREE = Regex(""""?isAccessibleForFree"?\s*:\s*"?(false|no)\b""", RegexOption.IGNORE_CASE)

    private val WALL =
        listOf(
            "weiterlesen mit",
            "jetzt weiterlesen",
            "mit z+ weiterlesen",
            "nur für abonnent",
            "exklusiv für abonnent",
            "zugang zu allen artikeln",
            "erhalten sie zugang zu allen",
            "sie haben bereits ein abo",
            "um diesen artikel zu lesen",
            "dieser artikel ist nur",
            "artikel freischalten",
            "subscribe to continue",
            "subscribe to read",
            "to continue reading",
            "subscribers only",
            "for subscribers",
            "already a subscriber",
            "sign in to read",
        )

    /** `true` when the article text of [html] ([articleText], already extracted) is cut off or locked. */
    @Suppress("ReturnCount")
    fun locked(
        html: String,
        articleText: String,
    ): Boolean {
        val length = articleText.trim().length
        if (length < NOT_FREE_MAX_TEXT && NOT_FREE.containsMatchIn(html)) return true
        if (length >= WALL_MAX_TEXT) return false
        val page = html.lowercase(Locale.ROOT)
        return WALL.any { it in page }
    }
}
