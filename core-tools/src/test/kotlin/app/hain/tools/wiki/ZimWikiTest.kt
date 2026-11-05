package app.hain.tools.wiki

import app.hain.llm.api.ToolCall
import app.hain.tools.api.OfflineWiki
import app.hain.tools.api.ToolContext
import app.hain.tools.api.ToolRouter
import app.hain.tools.internal.net.HttpNetworkGate
import app.hain.tools.internal.wiki.WikiSummary
import app.hain.tools.internal.wiki.WikipediaTool
import app.hain.tools.internal.wiki.zim.ClusterCodec
import app.hain.tools.internal.wiki.zim.ZimArchive
import app.hain.tools.internal.wiki.zim.ZimText
import app.hain.tools.internal.wiki.zim.ZimWiki
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.ZonedDateTime

/** T-237: offline Wikipedia from a ZIM pack (a tiny uncompressed ZIM written by the test). */
class ZimWikiTest {
    private val noCodec = ClusterCodec { kind, _ -> throw IOException("no codec for $kind") }

    private val dom =
        "<html><body><table><tr><td><p>Infobox: 157 m hoch und sonst nichts weiter dazu</p></td></tr></table>" +
            "<p>Der <b>Kölner Dom</b> ist eine römisch-katholische Kirche in Köln.<sup>[1]</sup> " +
            "Er ist seit 1996 Welterbe &amp; 157&nbsp;m hoch.</p></body></html>"
    private val koeln = "<p><b>Köln</b> ist eine kreisfreie Stadt am Rhein mit rund einer Million Menschen.</p>"
    private val merkur = "<p><b>Merkur</b> steht für:</p><ul><li>Merkur (Planet)</li><li>Merkur (Mythologie)</li></ul>"

    private fun zim(): File {
        val file = File.createTempFile("pack", ".zim")
        file.deleteOnExit()
        file.writeBytes(
            TinyZim.build(
                listOf(
                    TinyZim.Article("Kölner_Dom", "Kölner Dom", dom),
                    TinyZim.Article("Köln", "", koeln),
                    TinyZim.Article("Merkur", "", merkur),
                ),
                redirects = mapOf("Koeln" to "Köln"),
            ),
        )
        return file
    }

    @Test
    fun findsArticlesByTitleAnyCaseAndFollowsRedirects() {
        val file = zim()
        val wiki = ZimWiki({ mapOf("de" to listOf(file)) }, noCodec)
        val dom = wiki.summary("kölner dom", "de")
        assertNotNull(dom)
        assertEquals("Kölner Dom", dom!!.title)
        // the infobox line before the lead is skipped, footnotes and entities are cleaned
        assertTrue(dom.extract, dom.extract.startsWith("Der Kölner Dom ist eine römisch-katholische Kirche in Köln."))
        assertTrue(dom.extract.contains("Welterbe & 157 m hoch."))
        assertFalse(dom.extract.contains("[1]"))
        assertEquals("https://de.wikipedia.org/wiki/K%C3%B6lner_Dom", dom.url)
        assertEquals("Köln", wiki.summary("Koeln", "de")?.title)
        assertNull(wiki.summary("Gibt es nicht", "de"))
        assertNull(wiki.summary("Köln", "en"))
        assertEquals(setOf("de"), wiki.languages())
    }

    @Test
    fun aRemovedPackIsClosedSoItsSpaceIsFreed() {
        val file = zim()
        var packs = mapOf("de" to listOf(file))
        val wiki = ZimWiki({ packs }, noCodec)
        assertNotNull(wiki.summary("Köln", "de"))
        assertEquals(1, wiki.openCount())
        // the pack is deleted in Modelle & Pakete: the next lookup closes the archive
        packs = emptyMap()
        assertNull(wiki.summary("Köln", "de"))
        assertEquals(0, wiki.openCount())
    }

    @Test
    fun aReplacedPackFileIsOpenedAgain() {
        val file = zim()
        val wiki = ZimWiki({ mapOf("de" to listOf(file)) }, noCodec)
        assertNotNull(wiki.summary("Köln", "de"))
        // a new download writes a different file at the same place
        val bonn = TinyZim.Article("Bonn", "", "<p><b>Bonn</b> ist eine Stadt am Rhein.</p>")
        file.writeBytes(TinyZim.build(listOf(bonn)))
        assertNull(wiki.summary("Köln", "de"))
        assertEquals("Bonn", wiki.summary("Bonn", "de")?.title)
        assertEquals(1, wiki.openCount())
    }

    @Test
    fun disambiguationPagesAreRecognised() {
        val wiki = ZimWiki({ mapOf("de" to listOf(zim())) }, noCodec)
        assertTrue(wiki.summary("Merkur", "de")!!.disambiguation)
    }

    @Test
    fun archiveReadsHeaderAndSortedDirectory() {
        ZimArchive(zim(), noCodec).use { z ->
            assertEquals(4, z.entryCount)
            assertTrue(z.search('C', "Köln") >= 0)
            assertTrue(z.search('C', "Bonn") < 0)
        }
    }

    @Test
    fun candidateTitles() {
        assertEquals(
            listOf("kölner_dom", "Kölner_dom", "Kölner_Dom"),
            ZimWiki.candidates("  kölner dom? "),
        )
        assertTrue(ZimWiki.candidates("").isEmpty())
    }

    @Test
    fun textCleaningKeepsUnicodeAndDropsTags() {
        assertEquals("Ab & c ä", ZimText.clean("<a href=\"x\" data='{\"a\":\">\"}'>Ab</a> &amp; c &#228;"))
    }

    /** A pack that knows only "Kölner Dom"; the tool must answer from it without going online. */
    private class OnePage : OfflineWiki {
        override fun languages() = setOf("de")

        override fun summary(
            topic: String,
            lang: String,
        ) = if (lang == "de" && topic.contains("Dom")) {
            WikiSummary("Kölner Dom", "Der Kölner Dom ist eine Kirche.", "https://de.wikipedia.org/wiki/K_Dom")
        } else {
            null
        }
    }

    @Test
    fun wikipediaToolUsesThePackEvenInOfflineMode() =
        runTest {
            var calls = 0
            val gate =
                HttpNetworkGate(transport = { _ ->
                    calls++
                    throw IOException("no network in this test")
                })
            val tool = WikipediaTool(gate, OnePage())
            assertFalse("with a pack the tool works without internet", tool.online)
            val router = ToolRouter(listOf(tool))
            val ctx = ToolContext("de", ZonedDateTime.now(), offline = true)
            val result = router.run(ToolCall("c1", "wikipedia", mapOf("topic" to "Kölner Dom")), ctx)
            assertNull(result.error)
            assertEquals("Kölner Dom", result.label)
            assertTrue(
                result.sources
                    .single()
                    .title
                    .contains("offline"),
            )
            assertEquals(0, calls)
            assertTrue(router.schemas(offline = true).any { it.name == "wikipedia" })
        }
}

/** Writes a minimal ZIM 6 file: one uncompressed cluster, namespace C, a few redirects. */
internal object TinyZim {
    data class Article(
        val url: String,
        val title: String,
        val html: String,
    )

    private sealed interface Item {
        val url: String
    }

    private data class Content(
        override val url: String,
        val title: String,
        val blob: Int,
    ) : Item

    private data class Redirect(
        override val url: String,
        val target: String,
    ) : Item

    @Suppress("LongMethod") // the whole file layout in one place, in file order
    fun build(
        articles: List<Article>,
        redirects: Map<String, String> = emptyMap(),
    ): ByteArray {
        val items =
            (articles.mapIndexed { i, a -> Content(a.url, a.title, i) } + redirects.map { Redirect(it.key, it.value) })
                .sortedWith { a, b -> compare("C/${a.url}", "C/${b.url}") }
        val index = items.mapIndexed { i, item -> item.url to i }.toMap()
        val mime = "text/html\u0000\u0000".toByteArray()
        val entries =
            items.map { item ->
                val out = ByteArrayOutputStream()
                when (item) {
                    is Content -> {
                        out.write(le(0, 2))
                        out.write(byteArrayOf(0, 'C'.code.toByte()))
                        out.write(le(0, 4))
                        out.write(le(0, 4))
                        out.write(le(item.blob.toLong(), 4))
                        out.write(item.url.toByteArray() + 0 + item.title.toByteArray() + 0)
                    }
                    is Redirect -> {
                        out.write(le(0xFFFF, 2))
                        out.write(byteArrayOf(0, 'C'.code.toByte()))
                        out.write(le(0, 4))
                        out.write(le(index.getValue(item.target).toLong(), 4))
                        out.write(item.url.toByteArray() + 0 + 0)
                    }
                }
                out.toByteArray()
            }
        val blobs = articles.map { it.html.toByteArray() }
        val cluster = ByteArrayOutputStream()
        cluster.write(1) // no compression, 4-byte offsets
        var offset = 4L * (blobs.size + 1)
        cluster.write(le(offset, 4))
        blobs.forEach {
            offset += it.size
            cluster.write(le(offset, 4))
        }
        blobs.forEach { cluster.write(it) }
        val mimeAt = 80L
        val urlPtrAt = mimeAt + mime.size
        var at = urlPtrAt + 8L * entries.size
        val entryAt = entries.map { e -> at.also { at += e.size } }
        val clusterPtrAt = at
        val clusterAt = clusterPtrAt + 8
        val checksumAt = clusterAt + cluster.size()
        val out = ByteArrayOutputStream()
        out.write(le(ZimArchive.MAGIC.toLong(), 4))
        out.write(le(6, 2))
        out.write(le(1, 2))
        out.write(ByteArray(16))
        out.write(le(entries.size.toLong(), 4))
        out.write(le(1, 4))
        out.write(le(urlPtrAt, 8))
        out.write(le(-1, 8))
        out.write(le(clusterPtrAt, 8))
        out.write(le(mimeAt, 8))
        out.write(le(0, 4))
        out.write(le(0xFFFFFFFFL, 4))
        out.write(le(checksumAt, 8))
        out.write(mime)
        entryAt.forEach { out.write(le(it, 8)) }
        entries.forEach { out.write(it) }
        out.write(le(clusterAt, 8))
        out.write(cluster.toByteArray())
        out.write(ByteArray(16))
        return out.toByteArray()
    }

    private fun compare(
        a: String,
        b: String,
    ): Int {
        val x = a.toByteArray()
        val y = b.toByteArray()
        for (i in 0 until minOf(x.size, y.size)) {
            val d = (x[i].toInt() and 0xFF) - (y[i].toInt() and 0xFF)
            if (d != 0) return d
        }
        return x.size - y.size
    }

    private fun le(
        value: Long,
        size: Int,
    ): ByteArray {
        val b =
            ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(value)
                .array()
        return b.copyOf(size)
    }

    private fun le(
        value: Int,
        size: Int,
    ): ByteArray = le(value.toLong(), size)
}
