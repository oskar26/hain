package app.hain.tools.internal.news

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T-077: only an article whose text really is cut off gets the "Paywall" mark; a site never as a
 * whole. junge Welt asks for a subscription under every article, but most of its articles are free.
 */
class PaywallTest {
    private fun page(name: String): String =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("news/articles/$name")) { name }
            .use { it.readBytes().toString(Charsets.UTF_8) }

    private fun locked(name: String) = Readability.extract(page(name), "https://example.org/a").locked

    @Test fun freeJungeWeltArticleIsNotMarkedAlthoughThePageAsksForAnAbo() {
        val html = page("jungewelt_free.html")
        assertTrue(html.contains("Jetzt abonnieren") || html.contains("abonnieren"))
        assertFalse(locked("jungewelt_free.html"))
    }

    @Test fun jungeWeltArticleBehindTheLoginIsMarked() {
        assertTrue(locked("jungewelt_locked.html"))
    }

    @Test fun ndArticleIsNotMarked() {
        assertFalse(locked("nd_free.html"))
    }

    @Test fun teaserWithNotFreeSchemaIsMarked() {
        assertTrue(locked("plus_teaser.html"))
    }

    @Test fun notFreeSchemaWithTheWholeTextIsNotMarked() {
        // some sites say "not free" but still send the full article: then nothing is cut off
        val html = "<script type=\"application/ld+json\">{\"isAccessibleForFree\":\"False\"}</script>"
        assertFalse(Paywall.locked(html, "Ein langer Absatz. ".repeat(250)))
        assertTrue(Paywall.locked(html, "Nur der Anfang."))
    }

    @Test fun wallPhraseOnlyCountsForShortTexts() {
        val html = "<div class=\"wall\">Weiterlesen mit Abo</div>"
        assertTrue(Paywall.locked(html, "Kurzer Anreißer, dann ist Schluss."))
        assertFalse(Paywall.locked(html, "Ein vollständiger Artikel mit vielen Sätzen. ".repeat(60)))
    }

    @Test fun shortFreeArticleWithoutWallIsNotMarked() {
        val html = "<p>Kurzmeldung</p><a href=\"/abo\">Abo</a>"
        assertFalse(Paywall.locked(html, "Kurzmeldung in zwei Sätzen. Mehr nicht."))
    }

    @Test fun englishWallIsRecognised() {
        assertTrue(Paywall.locked("<p>Subscribe to continue reading</p>", "The first lines only."))
    }
}
