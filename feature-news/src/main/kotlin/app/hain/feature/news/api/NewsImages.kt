package app.hain.feature.news.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.Purpose
import app.hain.tools.internal.net.HttpNetworkGate
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.LocalHainColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Pictures of the news (T-075): list cards, the big top story and the pictures inside an article.
 * Every picture goes through the [NetworkGate] (purpose [Purpose.IMAGES]: in the network log, the
 * orange dot, nothing in offline mode) and is kept on disk in [dir] (at most [MAX_DISK_BYTES], the
 * oldest go first) and, decoded and scaled down, in memory. No third-party image service.
 */
class NewsImages(
    private val gate: NetworkGate,
    private val dir: File,
    private val download: (String) -> ByteArray = ::httpGetImage,
) {
    private val memory =
        object : LruCache<String, ImageBitmap>(MAX_MEMORY_BYTES) {
            override fun sizeOf(
                key: String,
                value: ImageBitmap,
            ): Int = value.width * value.height * BYTES_PER_PIXEL
        }
    private val parallel = Semaphore(PARALLEL)

    /** The picture at [url] at most [maxWidth] pixels wide, or `null` (offline, error, not a picture). */
    @Suppress("TooGenericExceptionCaught", "SwallowedException", "ReturnCount")
    suspend fun load(
        url: String,
        maxWidth: Int = DEFAULT_WIDTH,
    ): ImageBitmap? {
        val key = "$maxWidth|$url"
        memory.get(key)?.let { return it }
        val bytes =
            try {
                bytes(url)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            } ?: return null
        val bitmap = withContext(Dispatchers.Default) { decode(bytes, maxWidth) } ?: return null
        memory.put(key, bitmap)
        return bitmap
    }

    /** Only the bytes, from disk or through the gate. */
    @Suppress("ReturnCount")
    suspend fun bytes(url: String): ByteArray? {
        if (!url.startsWith("https://")) return null
        val file = File(dir, key(url))
        withContext(Dispatchers.IO) { file.takeIf { it.isFile }?.let { runCatching { it.readBytes() }.getOrNull() } }
            ?.let { return it }
        return parallel.withPermit {
            var bytes = ByteArray(0)
            gate.stream(GateRequest(url, Purpose.IMAGES)) {
                bytes = withContext(Dispatchers.IO) { download(url) }
                bytes.size.toLong()
            }
            if (!looksLikeImage(bytes)) return@withPermit null
            withContext(Dispatchers.IO) {
                runCatching {
                    dir.mkdirs()
                    file.writeBytes(bytes)
                    trimDisk()
                }
            }
            bytes
        }
    }

    @Suppress("ReturnCount")
    private fun trimDisk() {
        val files = dir.listFiles()?.filter { it.isFile } ?: return
        var total = files.sumOf { it.length() }
        if (total <= MAX_DISK_BYTES) return
        files.sortedBy { it.lastModified() }.forEach { f ->
            if (total <= MAX_DISK_BYTES * KEEP_SHARE / PERCENT) return
            total -= f.length()
            f.delete()
        }
    }

    companion object {
        const val DEFAULT_WIDTH = 1080
        const val THUMB_WIDTH = 320
        const val MAX_DISK_BYTES = 48L * 1024 * 1024
        const val MAX_BYTES = 6 * 1024 * 1024
        private const val MAX_MEMORY_BYTES = 32 * 1024 * 1024
        private const val BYTES_PER_PIXEL = 4
        private const val PARALLEL = 4
        private const val KEEP_SHARE = 75
        private const val PERCENT = 100
        private const val MIN_IMAGE = 12

        /** File name for [url]: its SHA-1, so any address is a safe name. */
        fun key(url: String): String =
            MessageDigest
                .getInstance("SHA-1")
                .digest(url.toByteArray())
                .joinToString("") { "%02x".format(it) } + ".img"

        /** JPEG, PNG, GIF, WebP or AVIF/HEIF by their first bytes; an HTML error page is not a picture. */
        @Suppress("MagicNumber", "ReturnCount", "CyclomaticComplexMethod")
        fun looksLikeImage(bytes: ByteArray): Boolean {
            if (bytes.size < MIN_IMAGE) return false

            fun at(
                i: Int,
                v: Int,
            ) = bytes[i].toInt() and 0xFF == v
            val jpeg = at(0, 0xFF) && at(1, 0xD8)
            val png = at(0, 0x89) && at(1, 0x50) && at(2, 0x4E) && at(3, 0x47)
            val gif = at(0, 0x47) && at(1, 0x49) && at(2, 0x46)
            val riff = at(0, 0x52) && at(1, 0x49) && at(2, 0x46) && at(3, 0x46)
            val webp = riff && at(8, 0x57) && at(9, 0x45) && at(10, 0x42) && at(11, 0x50)
            val ftyp = at(4, 0x66) && at(5, 0x74) && at(6, 0x79) && at(7, 0x70)
            return jpeg || png || gif || webp || ftyp
        }

        /** Decodes [bytes] scaled down by a power of two to about [maxWidth] pixels. */
        fun decode(
            bytes: ByteArray,
            maxWidth: Int,
        ): ImageBitmap? =
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= maxWidth) sample *= 2
                val options =
                    BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)?.asImageBitmap()
            }.getOrNull()
    }
}

/** The pictures of the news screen; `null` = no pictures (screenshots, "Bilder" off). */
val LocalNewsImages = staticCompositionLocalOf<NewsImages?> { null }

/**
 * A news picture filling [modifier]'s size: a quiet surface while it loads or when there is none,
 * then the picture fades in (no fade with "reduce motion"). Decoration only for TalkBack unless
 * [description] is given.
 */
@Composable
fun NewsPicture(
    url: String?,
    modifier: Modifier = Modifier,
    maxWidth: Int = NewsImages.DEFAULT_WIDTH,
    description: String? = null,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val images = LocalNewsImages.current
    val bitmap by produceState<ImageBitmap?>(null, url, images, maxWidth) {
        value = if (url != null && images != null) images.load(url, maxWidth) else null
    }
    val shown by animateFloatAsState(
        if (bitmap != null) 1f else 0f,
        hainTween(HainMotion.PAGE_MS),
        label = "news-picture",
    )
    Box(modifier.background(LocalHainColors.current.surface)) {
        bitmap?.let {
            Image(
                it,
                contentDescription = description,
                contentScale = contentScale,
                modifier = Modifier.matchParentSize().alpha(shown),
            )
        }
    }
}

/** One GET straight from the picture's site, 10 s timeouts, at most [NewsImages.MAX_BYTES]. */
private fun httpGetImage(url: String): ByteArray {
    val connection = URL(asciiUrl(url)).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", HttpNetworkGate.DEFAULT_USER_AGENT)
        connection.setRequestProperty("Accept", "image/avif,image/webp,image/jpeg,image/png,image/*")
        val code = connection.responseCode
        if (code !in HTTP_OK until HTTP_REDIRECT) throw IOException("HTTP $code")
        return connection.inputStream.use { readCapped(it) }
    } finally {
        connection.disconnect()
    }
}

/** Umlauts and spaces in a picture address (junge Welt, some CMS) percent-encoded as UTF-8. */
internal fun asciiUrl(url: String): String {
    val out = StringBuilder()
    url.toByteArray(Charsets.UTF_8).forEach { b ->
        val c = b.toInt() and BYTE_MASK
        if (c <= ASCII_MAX && c > ' '.code) out.append(c.toChar()) else out.append("%%%02X".format(c))
    }
    return out.toString()
}

private fun readCapped(input: InputStream): ByteArray {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(BUFFER)
    var total = 0
    while (true) {
        val n = input.read(buffer)
        if (n < 0) break
        total += n
        if (total > NewsImages.MAX_BYTES) throw IOException("picture too big")
        out.write(buffer, 0, n)
    }
    return out.toByteArray()
}

private const val TIMEOUT_MS = 10_000
private const val HTTP_OK = 200
private const val HTTP_REDIRECT = 300
private const val BUFFER = 16 * 1024
private const val BYTE_MASK = 0xFF
private const val ASCII_MAX = 0x7E
