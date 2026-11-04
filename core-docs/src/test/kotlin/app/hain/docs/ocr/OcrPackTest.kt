package app.hain.docs.ocr

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.GateResponse
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.NetworkLogEntry
import app.hain.common.api.net.Purpose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.security.MessageDigest

class OcrPackTest {
    @get:Rule val tmp = TemporaryFolder()

    private class FakeGate : NetworkGate {
        val requests = mutableListOf<GateRequest>()
        override val log: StateFlow<List<NetworkLogEntry>> = MutableStateFlow(emptyList())
        override val active: StateFlow<Int> = MutableStateFlow(0)

        override suspend fun fetch(request: GateRequest): GateResponse = error("not used")

        override suspend fun stream(
            request: GateRequest,
            transfer: suspend () -> Long,
        ): Long {
            requests += request
            return transfer()
        }

        override fun clearLog() = Unit
    }

    private fun sha(b: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(b)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private val a = "model a".toByteArray()
    private val b = "dictionary b".toByteArray()
    private val files =
        listOf(
            OcrPack.PackFile("a.onnx", "https://huggingface.co/x/a", sha(a), a.size.toLong()),
            OcrPack.PackFile("b.yml", "https://huggingface.co/x/b", sha(b), b.size.toLong()),
        )

    @Test
    fun downloadsThroughTheGateAndChecksEveryFile() =
        runTest {
            val gate = FakeGate()
            val content = mapOf("https://huggingface.co/x/a" to a, "https://huggingface.co/x/b" to b)
            val pack = OcrPack(tmp.root, gate, { url -> content.getValue(url).inputStream() }, files)
            assertFalse(pack.installed())
            var last = 0L to 0L
            pack.download { done, total -> last = done to total }
            assertTrue(pack.installed())
            assertEquals(pack.totalBytes to pack.totalBytes, last)
            assertEquals(2, gate.requests.size)
            assertTrue(gate.requests.all { it.purpose == Purpose.MODEL_DOWNLOAD && it.userStarted })
            pack.remove()
            assertFalse(pack.installed())
        }

    @Test
    fun wrongFileIsDeletedAndReported() =
        runTest {
            val pack = OcrPack(tmp.root, FakeGate(), { "x".repeat(7).toByteArray().inputStream() }, files.take(1))
            val failure = runCatching { pack.download() }.exceptionOrNull()
            assertTrue(failure is IOException)
            assertFalse(pack.installed())
            assertFalse(tmp.root.resolve("a.onnx.part").exists())
        }

    @Test
    fun officialPackIsPinnedAndAboutFourteenMegabytes() {
        assertEquals(4, OcrPack.FILES.size)
        assertTrue(
            OcrPack.FILES.all { it.url.startsWith("https://huggingface.co/PaddlePaddle/") && "/resolve/" in it.url },
        )
        assertTrue(OcrPack.FILES.all { it.sha256.length == 64 })
        assertEquals(13_894_812L, OcrPack.FILES.sumOf { it.size })
    }
}
