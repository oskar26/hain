package app.hain.feature.news.api

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.GateResponse
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.NetworkLogEntry
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** T-075: news pictures only through the gate, kept on disk, never an HTML page as a picture. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NewsImagesTest {
    @get:Rule
    val folder = TemporaryFolder()

    /** Records what goes through the gate; [offline] behaves like offline mode. */
    private class StreamGate(
        var offline: Boolean = false,
    ) : NetworkGate {
        val streamed = mutableListOf<GateRequest>()
        override val log: StateFlow<List<NetworkLogEntry>> = MutableStateFlow(emptyList())
        override val active: StateFlow<Int> = MutableStateFlow(0)

        override suspend fun fetch(request: GateRequest): GateResponse = GateResponse(404, "")

        override suspend fun stream(
            request: GateRequest,
            transfer: suspend () -> Long,
        ): Long {
            if (offline) throw OfflineException()
            streamed += request
            return transfer()
        }

        override fun clearLog() = Unit
    }

    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0x0D, 0x49)
    private val jpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()) + ByteArray(12)

    @Test fun picturesAreRecognisedByTheirFirstBytes() {
        assertTrue(NewsImages.looksLikeImage(png))
        assertTrue(NewsImages.looksLikeImage(jpeg))
        assertTrue(NewsImages.looksLikeImage("GIF89a".toByteArray() + ByteArray(8)))
        assertTrue(NewsImages.looksLikeImage("RIFF\u0000\u0000\u0000\u0000WEBPVP8 ".toByteArray()))
        assertTrue(NewsImages.looksLikeImage(byteArrayOf(0, 0, 0, 0x1C) + "ftypavif".toByteArray() + ByteArray(4)))
        assertFalse(NewsImages.looksLikeImage("<!DOCTYPE html><html>".toByteArray()))
        assertFalse(NewsImages.looksLikeImage(ByteArray(4)))
    }

    @Test fun fileNamesAreSafeAndStable() {
        val a = NewsImages.key("https://example.org/a b/ä.jpg?x=1")
        assertEquals(a, NewsImages.key("https://example.org/a b/ä.jpg?x=1"))
        assertTrue(a.matches(Regex("[0-9a-f]{40}\\.img")))
        assertFalse(a == NewsImages.key("https://example.org/b.jpg"))
    }

    @Test fun umlautsAndSpacesArePercentEncoded() {
        val encoded = asciiUrl("https://jungewelt.de/img/Größe neu.jpg")
        assertEquals("https://jungewelt.de/img/Gr%C3%B6%C3%9Fe%20neu.jpg", encoded)
        assertEquals("https://example.org/a.jpg?w=320", asciiUrl("https://example.org/a.jpg?w=320"))
    }

    @Test fun aPictureGoesThroughTheGateOnceAndIsThenReadFromDisk() =
        runBlocking {
            val gate = StreamGate()
            var downloads = 0
            val dir = folder.newFolder("news-images")
            val images =
                NewsImages(gate, dir) {
                    downloads++
                    png
                }
            val url = "https://example.org/bild.png"
            assertArrayEquals(png, images.bytes(url))
            assertEquals(listOf(GateRequest(url, Purpose.IMAGES)), gate.streamed)
            assertTrue(dir.resolve(NewsImages.key(url)).isFile)
            // second time: from disk, no new request
            assertArrayEquals(png, NewsImages(gate, dir) { error("no second download") }.bytes(url))
            assertEquals(1, downloads)
            assertEquals(1, gate.streamed.size)
        }

    @Test fun noPictureWithoutHttpsOfflineOrForAnErrorPage() =
        runBlocking {
            val gate = StreamGate()
            val dir = folder.newFolder("news-images")
            val html = NewsImages(gate, dir) { "<html>Not found</html>".toByteArray() }
            assertNull(html.bytes("https://example.org/missing.jpg"))
            assertFalse(dir.resolve(NewsImages.key("https://example.org/missing.jpg")).exists())
            assertNull(html.bytes("http://example.org/plain.jpg"))
            gate.offline = true
            val offline = NewsImages(gate, dir) { png }
            assertNull(offline.load("https://example.org/bild.png"))
            assertEquals(1, gate.streamed.size)
        }
}
