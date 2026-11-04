package app.hain.tools.internal.net

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.GateResponse
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.NetworkLogEntry
import app.hain.common.api.net.NetworkLogStore
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/** Sends one request and returns the answer; swapped for a fake in tests. */
fun interface Transport {
    @Throws(IOException::class)
    fun send(request: GateRequest): GateResponse
}

/**
 * [NetworkGate] on [HttpURLConnection] (T-060). Checks offline mode, sets the Hain user agent,
 * logs host, purpose and size (full address only with [detailedLog]) and keeps the log 7 days.
 * Only https addresses pass.
 */
class HttpNetworkGate(
    private val offline: () -> Boolean = { false },
    private val detailedLog: () -> Boolean = { false },
    private val userAgent: String = DEFAULT_USER_AGENT,
    private val clock: () -> Long = System::currentTimeMillis,
    private val transport: Transport = UrlConnectionTransport(),
    private val store: NetworkLogStore? = null,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO),
) : NetworkGate {
    private val entries = MutableStateFlow<List<NetworkLogEntry>>(emptyList())
    private val running = MutableStateFlow(0)
    override val log: StateFlow<List<NetworkLogEntry>> = entries.asStateFlow()
    override val active: StateFlow<Int> = running.asStateFlow()

    init {
        store?.let { saved ->
            scope.launch {
                runCatching {
                    saved.prune(clock() - KEEP_MS)
                    val old = saved.load(clock() - KEEP_MS)
                    entries.update { fresh -> (fresh + old).sortedByDescending { it.at }.take(MAX_ENTRIES) }
                }
            }
        }
    }

    override suspend fun fetch(request: GateRequest): GateResponse {
        val uri = runCatching { URI(request.url) }.getOrNull()
        val host = uri?.host
        require(uri != null && host != null && uri.scheme == "https") { "only https addresses" }
        val allowedOffline = request.purpose == Purpose.MODEL_DOWNLOAD && request.userStarted
        if (offline() && !allowedOffline) throw OfflineException()
        val headers = mapOf("User-Agent" to userAgent) + request.headers
        running.update { it + 1 }
        try {
            val response = withContext(Dispatchers.IO) { transport.send(request.copy(headers = headers)) }
            record(
                host,
                request,
                response.body
                    .toByteArray()
                    .size
                    .toLong(),
                response.code,
            )
            return response
        } catch (e: IOException) {
            record(host, request, 0, null)
            throw e
        } finally {
            running.update { it - 1 }
        }
    }

    override fun clearLog() {
        entries.value = emptyList()
        store?.let { scope.launch { runCatching { it.clear() } } }
    }

    private fun record(
        host: String,
        request: GateRequest,
        bytes: Long,
        code: Int?,
    ) {
        val now = clock()
        val entry = NetworkLogEntry(now, host, request.purpose, bytes, code, request.url.takeIf { detailedLog() })
        store?.let { scope.launch { runCatching { it.append(entry) } } }
        entries.update { list -> (listOf(entry) + list.filter { now - it.at < KEEP_MS }).take(MAX_ENTRIES) }
    }

    companion object {
        const val DEFAULT_USER_AGENT = "Hain/0.1 (https://github.com/oskar26/hain)"
        const val KEEP_MS = 7 * 24 * 60 * 60 * 1000L
        const val MAX_ENTRIES = 500
    }
}

/** Plain [HttpURLConnection], 10 s timeouts, answers up to 2 MB. */
class UrlConnectionTransport : Transport {
    override fun send(request: GateRequest): GateResponse {
        val connection = URL(request.url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = request.method
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.instanceFollowRedirects = true
            request.headers.forEach { (k, v) -> connection.setRequestProperty(k, v) }
            request.body?.let { body ->
                connection.doOutput = true
                connection.outputStream.use { it.write(body.toByteArray()) }
            }
            val code = connection.responseCode
            val stream = if (code >= ERROR_CODES) connection.errorStream else connection.inputStream
            val bytes = stream?.use { readCapped(it) } ?: ByteArray(0)
            return GateResponse(code, String(bytes, Charsets.UTF_8))
        } finally {
            connection.disconnect()
        }
    }

    private fun readCapped(input: java.io.InputStream): ByteArray {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER)
        while (out.size() < MAX_BYTES) {
            val n = input.read(buffer, 0, minOf(BUFFER, MAX_BYTES - out.size()))
            if (n < 0) break
            out.write(buffer, 0, n)
        }
        return out.toByteArray()
    }

    private companion object {
        const val BUFFER = 8192
        const val TIMEOUT_MS = 10_000
        const val ERROR_CODES = 400
        const val MAX_BYTES = 2 * 1024 * 1024
    }
}
