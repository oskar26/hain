package app.hain.tools.favicon

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.GateResponse
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.NetworkLogEntry
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import app.hain.tools.api.FaviconRules
import app.hain.tools.api.Favicons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

/** T-064c: site icons for the source chips, through the gate, cached on disk. */
class FaviconsTest {
    @get:Rule val folder = TemporaryFolder()

    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 1, 2)
    private val ico = byteArrayOf(0, 0, 1, 0, 1, 0, 16, 16, 0, 0, 1, 0, 32, 0)

    /** Gate that only records the streamed requests; [offline] makes it refuse like offline mode. */
    private class StreamGate(
        var offline: Boolean = false,
    ) : NetworkGate {
        val requests = mutableListOf<GateRequest>()
        override val log: StateFlow<List<NetworkLogEntry>> = MutableStateFlow(emptyList())
        override val active: StateFlow<Int> = MutableStateFlow(0)

        override suspend fun fetch(request: GateRequest): GateResponse = error("icons use stream")

        override suspend fun stream(
            request: GateRequest,
            transfer: suspend () -> Long,
        ): Long {
            if (offline) throw OfflineException()
            requests += request
            return transfer()
        }

        override fun clearLog() {
            requests.clear()
        }
    }

    @Test
    fun hostsAreCheckedAndSafe() {
        assertEquals("de.wikipedia.org", FaviconRules.host("https://de.wikipedia.org/wiki/K%C3%B6ln"))
        assertEquals("www.tagesschau.de", FaviconRules.host("HTTPS://WWW.Tagesschau.de/inland"))
        assertEquals("example.com", FaviconRules.host("http://example.com"))
        assertNull(FaviconRules.host("javascript:alert(1)"))
        assertNull(FaviconRules.host("file:///etc/passwd"))
        assertNull(FaviconRules.host("kein link"))
        assertNull(FaviconRules.host("https://"))
    }

    @Test
    fun onlyTheSiteItselfIsAsked() {
        assertEquals(
            listOf("https://example.com/favicon.ico", "https://example.com/apple-touch-icon.png"),
            FaviconRules.candidates("example.com"),
        )
    }

    @Test
    fun picturesAreRecognised() {
        assertTrue(FaviconRules.isImage(png))
        assertTrue(FaviconRules.isImage(ico))
        assertTrue(FaviconRules.isImage("GIF89a.........".toByteArray()))
        assertTrue(FaviconRules.isImage(byteArrayOf(-1, -40, -1, -32, 0, 16, 74, 70, 73, 70, 0, 1)))
        assertTrue(FaviconRules.isImage("RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray()))
        assertFalse(FaviconRules.isImage("<!doctype html><html>".toByteArray()))
        assertFalse(FaviconRules.isImage("<svg xmlns=\"http://www.w3.org/2000/svg\">".toByteArray()))
        assertFalse(FaviconRules.isImage(ByteArray(0)))
        assertFalse(FaviconRules.isImage(png + ByteArray(FaviconRules.MAX_BYTES)))
    }

    @Test
    fun theLetterSkipsWww() {
        assertEquals("T", FaviconRules.letter("https://www.tagesschau.de/x"))
        assertEquals("W", FaviconRules.letter("https://de.wikipedia.org/wiki/Hain"))
        assertEquals("1", FaviconRules.letter("https://1und1.de"))
        assertEquals("B", FaviconRules.letter("https://www.bbc.co.uk/news"))
        assertEquals("?", FaviconRules.letter("nix"))
    }

    @Test
    fun anIconIsLoadedOnceThroughTheGate() {
        runTest {
            val gate = StreamGate()
            var downloads = 0
            val icons =
                Favicons(gate, folder.root, clock = { 1_000L }) {
                    downloads++
                    png
                }
            assertArrayEquals(png, icons.icon("https://example.com/a"))
            assertArrayEquals(png, icons.icon("https://example.com/b"))
            assertEquals(1, downloads)
            assertEquals(Purpose.IMAGES, gate.requests.single().purpose)
            assertEquals("https://example.com/favicon.ico", gate.requests.single().url)
        }
    }

    @Test
    fun theDiskCacheSurvivesARestart() {
        runTest {
            Favicons(StreamGate(), folder.root, clock = { 1_000L }) { ico }.icon("https://example.com")
            val gate = StreamGate()
            val again = Favicons(gate, folder.root, clock = { 2_000L }) { error("no second request") }
            assertArrayEquals(ico, again.icon("https://example.com/other"))
            assertTrue(gate.requests.isEmpty())
        }
    }

    @Test
    fun anHtmlPageIsNoIconSoTheTouchIconIsTried() {
        runTest {
            val gate = StreamGate()
            val icons =
                Favicons(gate, folder.root) { url ->
                    if (url.endsWith(".ico")) "<html>404</html>".toByteArray() else png
                }
            assertArrayEquals(png, icons.icon("https://example.com"))
            assertEquals(2, gate.requests.size)
        }
    }

    @Test
    fun aSiteWithoutIconIsAskedAgainAfterADay() {
        runTest {
            var now = 0L
            var downloads = 0
            val gate = StreamGate()
            val dead = { _: String ->
                downloads++
                throw IOException("404")
            }
            Favicons(gate, folder.root, clock = { now }, download = dead).icon("https://nothing.example")
            assertEquals(2, downloads)
            now = Favicons.MISS_MS / 2
            assertNull(Favicons(gate, folder.root, clock = { now }, download = dead).icon("https://nothing.example"))
            assertEquals(2, downloads)
            now = Favicons.MISS_MS + 1
            Favicons(gate, folder.root, clock = { now }, download = dead).icon("https://nothing.example")
            assertEquals(4, downloads)
        }
    }

    @Test
    fun offlineShowsTheLetterAndAsksLater() {
        runTest {
            val gate = StreamGate(offline = true)
            val icons = Favicons(gate, folder.root) { png }
            assertNull(icons.icon("https://example.com"))
            gate.offline = false
            assertArrayEquals(png, icons.icon("https://example.com"))
        }
    }

    @Test
    fun badAddressesAreNeverFetched() {
        runTest {
            val gate = StreamGate()
            val icons = Favicons(gate, folder.root) { error("must not load") }
            assertNull(icons.icon("javascript:alert(1)"))
            assertTrue(gate.requests.isEmpty())
        }
    }
}
