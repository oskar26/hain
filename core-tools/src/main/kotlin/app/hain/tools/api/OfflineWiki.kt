package app.hain.tools.api

import app.hain.tools.internal.wiki.WikiSummary

/**
 * T-237: Wikipedia from an installed knowledge pack (Settings → Modelle & Pakete → Wissen). When a
 * pack for the language is there, the `wikipedia` tool answers from it first — without internet,
 * also in offline mode — and goes online only when the pack has nothing.
 */
interface OfflineWiki {
    /** Languages with an installed pack ("de", "en"). */
    fun languages(): Set<String>

    /** The intro of the article about [topic] in [lang], or `null` when the pack has none. */
    fun summary(
        topic: String,
        lang: String,
    ): WikiSummary?
}
