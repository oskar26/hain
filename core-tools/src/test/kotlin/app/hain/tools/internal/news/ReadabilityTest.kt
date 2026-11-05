package app.hain.tools.internal.news

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * T-076: the reading view's article extraction on saved pages (resources/news/articles: two real
 * junge Welt pages and one nd page from 04.11.2026, two made-up pages for the box layout and a
 * subscription teaser).
 */
class ReadabilityTest {
    private fun page(name: String): String =
        requireNotNull(javaClass.classLoader?.getResourceAsStream("news/articles/$name")) { name }
            .use { it.readBytes().toString(Charsets.UTF_8) }

    private fun paragraphs(article: Article) = article.blocks.filterIsInstance<ArticleBlock.Paragraph>()

    @Test fun jungeWeltArticleIsReadInFullWithoutMenusOrAboBox() {
        val url = "https://www.jungewelt.de/artikel/532785.zwischenwahlen-in-den-usa-trump-in-der-midterm-crisis.html"
        val article = Readability.extract(page("jungewelt_free.html"), url)
        assertEquals("Zwischenwahlen in den USA: Trump in der Midterm-Crisis", article.title)
        assertEquals("https://www.jungewelt.de/img/1100/229220.jpg", article.leadImage)
        assertTrue(article.textLength > 3_500)
        assertTrue(paragraphs(article).first().text.startsWith("MAGA-Land ist abgebrannt."))
        assertFalse(article.text.contains("Passwort vergessen"))
        assertFalse(article.text.contains("Anzeige"))
        assertFalse(article.locked)
    }

    @Test fun ndArticleKeepsAllParagraphs() {
        val article = Readability.extract(page("nd_free.html"), "https://www.nd-aktuell.de/artikel/1203659.html")
        assertEquals("Hinters Licht geführt", article.title)
        assertTrue(paragraphs(article).size >= 8)
        assertTrue(article.textLength > 4_000)
        assertFalse(article.locked)
    }

    @Test fun boxPerParagraphLayoutStillFindsTheWholeArticle() {
        val article = Readability.extract(page("boxes.html"), "https://www.example.com/news/storm")
        val texts = paragraphs(article).map { it.text }
        assertEquals(4, texts.size)
        assertTrue(texts.first().startsWith("Rivers across the region"))
        assertTrue(texts.last().startsWith("Forecasters expect"))
        assertTrue(texts.none { "Comment" in it || "Most read" in it || "Story A" in it })
    }

    @Test fun imagesComeWithCaptionAndTheBestSizeButNoTrackingPixel() {
        val article = Readability.extract(page("boxes.html"), "https://www.example.com/news/storm")
        val images = article.blocks.filterIsInstance<ArticleBlock.Image>()
        assertEquals(1, images.size)
        assertEquals("https://www.example.com/storm/flooded-street-960.jpg", images.single().url)
        assertEquals("A flooded street in the old town, early on Tuesday.", images.single().caption)
        // the picture sits between the first and the second paragraph, as on the page
        assertTrue(article.blocks[1] is ArticleBlock.Image)
        // protocol-relative og:image becomes https, entities decoded
        assertEquals("https://images.example.com/storm/lead.jpg?width=1200&q=80", article.leadImage)
    }

    @Test fun srcsetPicksTheLargestPictureUpTo1600Pixels() {
        assertEquals("b.jpg", Readability.fromSrcset("a.jpg 480w, b.jpg 1200w, c.jpg 2400w"))
        assertEquals("only.jpg", Readability.fromSrcset("only.jpg"))
        assertNull(Readability.fromSrcset(""))
    }

    @Test fun addressesBecomeHttpsAndOtherSchemesAreDropped() {
        val base = "https://www.example.com/a/b.html"
        assertEquals("https://www.example.com/a/c.jpg", Readability.absolute(base, "c.jpg"))
        assertEquals("https://cdn.example.com/x.jpg", Readability.absolute(base, "//cdn.example.com/x.jpg"))
        assertEquals("https://old.example.com/x.jpg", Readability.absolute(base, "http://old.example.com/x.jpg"))
        assertNull(Readability.absolute(base, "javascript:alert(1)"))
        assertNull(Readability.absolute(base, ""))
    }

    @Test fun scriptsStylesAndCommentsNeverBecomeText() {
        val html =
            "<html><head><title>T</title><style>p{color:red}</style></head><body><article>" +
                "<script>var x = '<p>not text, but a script with a long sentence inside it</p>';</script>" +
                "<!-- <p>a comment that should never show up in the article</p> -->" +
                "<p>Erster Absatz mit genug Text, damit er als Absatz zählt, und noch ein paar Wörter.</p>" +
                "<p>Zweiter Absatz &amp; mehr, mit &bdquo;Anführungszeichen&ldquo; und einem Komma, bitte.</p>" +
                "</article></body></html>"
        val article = Readability.extract(html, "https://example.org/x")
        assertEquals(2, paragraphs(article).size)
        val second = paragraphs(article)[1].text
        assertEquals("Zweiter Absatz & mehr, mit „Anführungszeichen“ und einem Komma, bitte.", second)
        assertFalse(article.text.contains("script"))
        assertFalse(article.text.contains("comment"))
    }

    @Test fun brokenMarkupDoesNotCrash() {
        val html = "<div><p>Offen <b>fett <i>kursiv</p><p>Noch ein Absatz, ohne Ende <a href='x'>Link"
        val article = Readability.extract(html, "https://example.org/x")
        assertNotNull(article)
        assertTrue(article.text.contains("Noch ein Absatz"))
    }

    @Test fun jsonLdArticleBodyHelpsWhenThePageHasNoParagraphs() {
        val body = "Satz eins des Artikels, mit Komma. ".repeat(20)
        val html =
            "<html><head><title>Nur JSON</title><script type=\"application/ld+json\">" +
                "{\"@type\":\"NewsArticle\",\"articleBody\":\"$body\\nZweiter Absatz.\"}</script></head>" +
                "<body><div id=\"app\"></div></body></html>"
        val article = Readability.extract(html, "https://example.org/x")
        assertTrue(article.textLength >= body.trim().length)
        assertEquals(2, paragraphs(article).size)
    }
}
