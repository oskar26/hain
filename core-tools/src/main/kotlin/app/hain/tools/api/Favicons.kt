package app.hain.tools.api

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.OfflineException
import app.hain.common.api.net.Purpose
import app.hain.tools.internal.net.HttpNetworkGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap

/**
 * Site icons for the source chips (T-064c). Every icon comes from the site itself
 * (`https://<host>/favicon.ico`, then `/apple-touch-icon.png`), never from a third-party icon
 * service, and always through the [NetworkGate] (purpose [Purpose.IMAGES]: in the network log, the
 * orange dot, blocked in offline mode). Icons are kept on disk in [dir], so a site is asked once:
 * a found icon for [KEEP_MS], a site without one for [MISS_MS]. The chat shows the first letter of
 * the site instead while nothing is there, and when the user switched "Quellen-Bilder" off.
 */
class Favicons(
    private val gate: NetworkGate,
    private val dir: File,
    private val clock: () -> Long = System::currentTimeMillis,
    private val download: (String) -> ByteArray = ::httpGet,
) {
    private val memory = ConcurrentHashMap<String, ByteArray>()
    private val lock = Mutex()

    /** The icon of the site [pageUrl] is on (PNG, ICO, GIF, JPEG or WebP bytes), or `null`. */
    suspend fun icon(pageUrl: String): ByteArray? {
        val host = FaviconRules.host(pageUrl) ?: return null
        // one request per site, even when several chips of the same site ask at once
        val bytes = memory[host] ?: lock.withLock { memory[host] ?: fromDisk(host) ?: fromSite(host) }
        return bytes.takeIf { it.isNotEmpty() }
    }

    /** The saved icon, an empty array for a site known to have none, `null` when nothing (fresh) is saved. */
    private suspend fun fromDisk(host: String): ByteArray? =
        withContext(Dispatchers.IO) {
            val file = file(host)
            if (!file.exists()) return@withContext null
            val saved = runCatching { file.readBytes() }.getOrNull() ?: return@withContext null
            if (saved.size < STAMP_BYTES) return@withContext null
            val age = clock() - ByteBuffer.wrap(saved, 0, STAMP_BYTES).long
            val bytes = saved.copyOfRange(STAMP_BYTES, saved.size)
            val fresh = if (bytes.isEmpty()) age < MISS_MS else age < KEEP_MS
            if (!fresh) return@withContext null
            memory[host] = bytes
            bytes
        }

    /** The icon, or an empty array when the site has none (remembered); offline mode remembers nothing. */
    private suspend fun fromSite(host: String): ByteArray {
        var found: ByteArray? = null
        for (url in FaviconRules.candidates(host)) {
            val bytes =
                try {
                    fetch(url)
                } catch (
                    @Suppress("SwallowedException") e: OfflineException,
                ) {
                    // offline mode: show the letter now, ask again once Hain may go online
                    return ByteArray(0)
                } catch (
                    @Suppress("SwallowedException") e: IOException,
                ) {
                    null // this address had nothing, try the next one
                } catch (
                    @Suppress("SwallowedException") e: IllegalArgumentException,
                ) {
                    null // not an https address
                }
            if (bytes != null && FaviconRules.isImage(bytes)) {
                found = bytes
                break
            }
        }
        return remember(host, found ?: ByteArray(0))
    }

    private suspend fun fetch(url: String): ByteArray {
        var bytes = ByteArray(0)
        gate.stream(GateRequest(url, Purpose.IMAGES)) {
            bytes = withContext(Dispatchers.IO) { download(url) }
            bytes.size.toLong()
        }
        return bytes
    }

    private suspend fun remember(
        host: String,
        bytes: ByteArray,
    ): ByteArray {
        memory[host] = bytes
        withContext(Dispatchers.IO) {
            runCatching {
                dir.mkdirs()
                // the time it was asked comes first, so the cache does not depend on file dates
                file(host).writeBytes(ByteBuffer.allocate(STAMP_BYTES).putLong(clock()).array() + bytes)
            }
        }
        return bytes
    }

    private fun file(host: String) = File(dir, "$host.icon")

    companion object {
        /** A found icon is asked for again after 30 days. */
        const val KEEP_MS = 30L * 24 * 60 * 60 * 1000

        /** A site without an icon is asked again after one day. */
        const val MISS_MS = 24L * 60 * 60 * 1000

        private const val STAMP_BYTES = Long.SIZE_BYTES
    }
}

/** The pure rules of [Favicons]: which site, which addresses, is it a picture, which letter. */
object FaviconRules {
    /** Icons bigger than this are not loaded (a favicon is a few KB). */
    const val MAX_BYTES = 64 * 1024

    /** The host of an https or http page address, lower case and safe as a file name; `null` otherwise. */
    fun host(pageUrl: String): String? {
        val uri = runCatching { URI(pageUrl.trim()) }.getOrNull()
        val host =
            uri
                ?.host
                ?.lowercase()
                ?.trimEnd('.')
                .takeIf { uri?.scheme?.lowercase() in WEB }
        return host?.takeIf { it.isNotEmpty() && it.length <= MAX_HOST && HOST.matches(it) && ".." !in it }
    }

    /** Where to look, in this order; https only (the gate lets nothing else through). */
    fun candidates(host: String): List<String> = ICON_PATHS.map { "https://$host$it" }

    /** `true` for PNG, ICO, GIF, JPEG and WebP. An HTML error page or an SVG is not an icon. */
    fun isImage(bytes: ByteArray): Boolean {
        if (bytes.size < MIN_IMAGE || bytes.size > MAX_BYTES) return false
        val head = bytes.copyOfRange(0, MIN_IMAGE).joinToString("") { "%02X".format(it) }
        return SIGNATURES.any { head.startsWith(it) } || (head.startsWith(RIFF) && head.startsWith(WEBP, WEBP_AT))
    }

    /**
     * The letter shown instead of the icon: first letter of the site's own name, so "de.wikipedia.org"
     * is "W", "www.tagesschau.de" is "T" and "bbc.co.uk" is "B"; "?" if there is none.
     */
    fun letter(pageUrl: String): String {
        val labels = host(pageUrl)?.split('.')?.filter { it.isNotEmpty() } ?: return "?"
        val tail = labels.asReversed()
        val name =
            when {
                tail.size < 2 -> tail.firstOrNull()
                tail.size > 2 && tail[1].length <= SHORT_SECOND_LEVEL -> tail[2]
                else -> tail[1]
            }
        return name?.firstOrNull { it.isLetterOrDigit() }?.uppercase() ?: "?"
    }

    private val HOST = Regex("[a-z0-9.-]+")

    private val WEB = setOf("https", "http")
    private val ICON_PATHS = listOf("/favicon.ico", "/apple-touch-icon.png")

    /** First bytes (hex) of PNG, ICO, GIF and JPEG; WebP is "RIFF", four size bytes, then "WEBP". */
    private val SIGNATURES = listOf("89504E47", "00000100", "47494638", "FFD8FF")
    private const val RIFF = "52494646"
    private const val WEBP = "57454250"
    private const val MAX_HOST = 253
    private const val SHORT_SECOND_LEVEL = 3
    private const val MIN_IMAGE = 12
    private const val WEBP_AT = 16 // hex digits: after "RIFF" and the four size bytes
}

/** One small GET straight from the site, 5 s timeouts, at most [FaviconRules.MAX_BYTES]. */
private fun httpGet(url: String): ByteArray {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = ICON_TIMEOUT_MS
        connection.readTimeout = ICON_TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", HttpNetworkGate.DEFAULT_USER_AGENT)
        val code = connection.responseCode
        if (code !in HTTP_OK until HTTP_REDIRECT) throw IOException("HTTP $code")
        return connection.inputStream.use { readCapped(it) }
    } finally {
        connection.disconnect()
    }
}

private fun readCapped(input: InputStream): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(ICON_BUFFER)
    while (true) {
        val n = input.read(buffer)
        if (n < 0) break
        out.write(buffer, 0, n)
        if (out.size() > FaviconRules.MAX_BYTES) throw IOException("icon too big")
    }
    return out.toByteArray()
}

private const val ICON_TIMEOUT_MS = 5_000
private const val ICON_BUFFER = 4096
private const val HTTP_OK = 200
private const val HTTP_REDIRECT = 300
