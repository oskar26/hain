package app.hain.tools.net

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.GateResponse
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import app.hain.tools.internal.net.HttpNetworkGate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

class HttpNetworkGateTest {
    private val sent = mutableListOf<GateRequest>()
    private var now = 1_000L

    private fun gate(
        offline: Boolean = false,
        detail: Boolean = false,
        fail: Boolean = false,
    ) = HttpNetworkGate(
        offline = { offline },
        detailedLog = { detail },
        clock = { now },
        transport = { r ->
            sent += r
            if (fail) throw IOException("down")
            GateResponse(200, "hello")
        },
    )

    @Test
    fun logsHostAndPurposeWithoutQuery() =
        runTest {
            val g = gate()
            val res = g.fetch(GateRequest("https://api.open-meteo.com/v1/forecast?q=secret", Purpose.WEATHER))
            assertEquals("hello", res.body)
            val entry = g.log.value.single()
            assertEquals("api.open-meteo.com", entry.host)
            assertEquals(Purpose.WEATHER, entry.purpose)
            assertEquals(5L, entry.bytes)
            assertNull(entry.url)
            assertEquals(0, g.active.value)
        }

    @Test
    fun detailedLogKeepsUrl() =
        runTest {
            val g = gate(detail = true)
            g.fetch(GateRequest("https://de.wikipedia.org/w/api.php?x=1", Purpose.WIKI))
            assertEquals(
                "https://de.wikipedia.org/w/api.php?x=1",
                g.log.value
                    .single()
                    .url,
            )
        }

    @Test
    fun setsUserAgent() =
        runTest {
            gate().fetch(GateRequest("https://de.wikipedia.org/", Purpose.WIKI))
            assertTrue(sent.single().headers["User-Agent"]!!.startsWith("Hain/"))
        }

    @Test
    fun offlineBlocksEverythingButUserDownloads() =
        runTest {
            val g = gate(offline = true)
            try {
                g.fetch(GateRequest("https://api.open-meteo.com/", Purpose.WEATHER))
                fail("should be blocked")
            } catch (_: OfflineException) {
                assertTrue(sent.isEmpty())
            }
            g.fetch(GateRequest("https://huggingface.co/x", Purpose.MODEL_DOWNLOAD, userStarted = true))
            assertEquals(1, sent.size)
        }

    @Test(expected = IllegalArgumentException::class)
    fun onlyHttps() =
        runTest {
            gate().fetch(GateRequest("http://example.org/", Purpose.OTHER))
        }

    @Test
    fun failedRequestIsLoggedToo() =
        runTest {
            val g = gate(fail = true)
            runCatching { g.fetch(GateRequest("https://example.org/", Purpose.OTHER)) }
            assertNull(
                g.log.value
                    .single()
                    .code,
            )
        }

    @Test
    fun oldEntriesDropAfterSevenDays() =
        runTest {
            val g = gate()
            g.fetch(GateRequest("https://a.org/", Purpose.OTHER))
            now += HttpNetworkGate.KEEP_MS + 1
            g.fetch(GateRequest("https://b.org/", Purpose.OTHER))
            assertEquals(listOf("b.org"), g.log.value.map { it.host })
            g.clearLog()
            assertTrue(g.log.value.isEmpty())
        }
}
