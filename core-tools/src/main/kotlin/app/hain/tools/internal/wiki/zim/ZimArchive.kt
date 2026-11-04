package app.hain.tools.internal.wiki.zim

import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Unpacks a compressed cluster (zstd since ZIM 6; xz in very old files). */
fun interface ClusterCodec {
    /** [input] = the compressed bytes of one cluster; [kind] = 4 (xz) or 5 (zstd). */
    @Throws(IOException::class)
    fun open(
        kind: Int,
        input: InputStream,
    ): InputStream
}

/**
 * T-237: reads articles from an offline Wikipedia pack (a Kiwix **ZIM** file, format 5/6, own code
 * after the open spec at wiki.openzim.org/wiki/ZIM_file_format). Only what Pim needs: find an
 * article by its title (the URL list is sorted, so a binary search, no index in memory) and give
 * its HTML. Clusters are unpacked by [codec] (zstd on the phone); the last two stay in memory.
 *
 * Thread-safe: one file handle, all reads are synchronized.
 */
class ZimArchive(
    file: File,
    private val codec: ClusterCodec,
) : Closeable {
    private val raf = RandomAccessFile(file, "r")
    private val length = raf.length()

    /** Number of directory entries. */
    val entryCount: Int
    private val clusterCount: Int
    private val urlPtrPos: Long
    private val clusterPtrPos: Long
    private val checksumPos: Long
    private val mimeTypes: List<String>

    /** One entry of the directory: an article, an image, a redirect … */
    data class Entry(
        val namespace: Char,
        val url: String,
        val title: String,
        val mime: String?,
        val cluster: Int = -1,
        val blob: Int = -1,
        /** Index of the target entry when this is a redirect. */
        val redirect: Int = -1,
    ) {
        val isRedirect: Boolean get() = redirect >= 0

        /** Title as shown (an empty title means: the URL with spaces). */
        val shownTitle: String get() = title.ifEmpty { url.replace('_', ' ') }
    }

    init {
        val head = read(0, HEADER_SIZE)
        if (head.int(0) != MAGIC) throw IOException("not a ZIM file")
        val major = head.short(MAJOR_AT)
        if (major != V5 && major != V6) throw IOException("unsupported ZIM version $major")
        entryCount = head.int(ENTRIES_AT)
        clusterCount = head.int(CLUSTERS_AT)
        urlPtrPos = head.long(URL_PTR_AT)
        clusterPtrPos = head.long(CLUSTER_PTR_AT)
        val mimePos = head.long(MIME_AT)
        checksumPos = head.long(CHECKSUM_AT).takeIf { it in 1 until length } ?: length
        mimeTypes = readMimeTypes(mimePos)
    }

    /** Entry number [index] of the URL-ordered directory. */
    fun entry(index: Int): Entry {
        require(index in 0 until entryCount) { "no entry $index" }
        val at = read(urlPtrPos + POINTER * index.toLong(), POINTER).long(0)
        val bytes = read(at, minOf(ENTRY_READ.toLong(), length - at).toInt())
        val mime = bytes.short(0)
        val namespace = (bytes[NS_AT].toInt() and BYTE).toChar()
        val redirect = mime == REDIRECT_MIME
        var p = if (redirect) REDIRECT_STRINGS_AT else CONTENT_STRINGS_AT
        val url = bytes.cString(p).also { p += it.second }.first
        val title = bytes.cString(p).first
        return if (redirect) {
            Entry(namespace, url, title, null, redirect = bytes.int(REDIRECT_INDEX_AT))
        } else {
            Entry(
                namespace,
                url,
                title,
                mimeTypes.getOrNull(mime),
                cluster = bytes.int(CLUSTER_AT),
                blob = bytes.int(BLOB_AT),
            )
        }
    }

    /**
     * Index of the entry [namespace]/[url] (exact), or `-(insertion point) - 1` like
     * [java.util.Collections.binarySearch], so a caller can look at the neighbours.
     */
    fun search(
        namespace: Char,
        url: String,
    ): Int {
        val key = "$namespace/$url".toByteArray(Charsets.UTF_8)
        var low = 0
        var high = entryCount - 1
        while (low <= high) {
            val mid = (low + high) ushr 1
            val e = entry(mid)
            val cmp = compareBytes("${e.namespace}/${e.url}".toByteArray(Charsets.UTF_8), key)
            when {
                cmp < 0 -> low = mid + 1
                cmp > 0 -> high = mid - 1
                else -> return mid
            }
        }
        return -(low + 1)
    }

    /** Follows redirects (at most a few) to the entry that holds content. */
    fun resolve(index: Int): Entry? {
        var e: Entry? = entry(index)
        var hops = 0
        while (e != null && e.isRedirect && hops < MAX_REDIRECTS) {
            e = e.redirect.takeIf { it in 0 until entryCount }?.let(::entry)
            hops++
        }
        return e?.takeUnless { it.isRedirect }
    }

    /** The bytes of a content [entry]. */
    fun content(entry: Entry): ByteArray {
        require(!entry.isRedirect && entry.cluster in 0 until clusterCount) { "no content" }
        return blob(entry.cluster, entry.blob)
    }

    private val cache = LinkedHashMap<Int, Cluster>(CACHE + 1, 1f, true)

    private class Cluster(
        val data: ByteArray,
        val extended: Boolean,
    ) {
        private val size get() = if (extended) LONG else INT

        fun blobs(): Int = (offset(0) / size).toInt() - 1

        fun offset(i: Int): Long =
            if (extended) {
                ByteBuffer.wrap(data, i * LONG, LONG).order(ByteOrder.LITTLE_ENDIAN).long
            } else {
                ByteBuffer
                    .wrap(data, i * INT, INT)
                    .order(ByteOrder.LITTLE_ENDIAN)
                    .int
                    .toLong() and UINT
            }

        fun blob(i: Int): ByteArray {
            require(i in 0 until blobs()) { "no blob $i" }
            val from = offset(i).toInt()
            val to = offset(i + 1).toInt()
            return data.copyOfRange(from, to)
        }
    }

    private fun blob(
        cluster: Int,
        blob: Int,
    ): ByteArray = synchronized(this) { cluster(cluster) }.blob(blob)

    private fun cluster(index: Int): Cluster {
        cache[index]?.let { return it }
        val start = read(clusterPtrPos + POINTER * index.toLong(), POINTER).long(0)
        val end =
            if (index + 1 < clusterCount) {
                read(clusterPtrPos + POINTER * (index + 1).toLong(), POINTER).long(0)
            } else {
                checksumPos
            }
        val raw = read(start, (end - start).toInt())
        val info = raw[0].toInt() and BYTE
        val kind = info and KIND_MASK
        val extended = info and EXTENDED != 0
        val body = ByteArrayInputStream(raw, 1, raw.size - 1)
        val data =
            when (kind) {
                0, 1 -> raw.copyOfRange(1, raw.size)
                else -> codec.open(kind, body).use { it.readBytes() }
            }
        val c = Cluster(data, extended)
        cache[index] = c
        if (cache.size > CACHE) cache.remove(cache.keys.first())
        return c
    }

    private fun readMimeTypes(at: Long): List<String> {
        val bytes = read(at, minOf(MIME_READ.toLong(), length - at).toInt())
        val list = mutableListOf<String>()
        var p = 0
        while (p < bytes.size) {
            val (s, used) = bytes.cString(p)
            if (s.isEmpty()) break
            list += s
            p += used
        }
        return list
    }

    private fun read(
        at: Long,
        size: Int,
    ): ByteArray =
        synchronized(this) {
            val out = ByteArray(size)
            raf.seek(at)
            raf.readFully(out)
            out
        }

    override fun close() = raf.close()

    companion object {
        const val MAGIC = 72_173_914
        private const val V5 = 5
        private const val V6 = 6
        private const val HEADER_SIZE = 80
        private const val MAJOR_AT = 4
        private const val ENTRIES_AT = 24
        private const val CLUSTERS_AT = 28
        private const val URL_PTR_AT = 32
        private const val CLUSTER_PTR_AT = 48
        private const val MIME_AT = 56
        private const val CHECKSUM_AT = 72
        private const val POINTER = 8
        private const val ENTRY_READ = 2048
        private const val MIME_READ = 4096
        private const val NS_AT = 3
        private const val CLUSTER_AT = 8
        private const val BLOB_AT = 12
        private const val REDIRECT_INDEX_AT = 8
        private const val CONTENT_STRINGS_AT = 16
        private const val REDIRECT_STRINGS_AT = 12
        private const val REDIRECT_MIME = 0xFFFF
        private const val MAX_REDIRECTS = 5
        private const val CACHE = 2
        private const val KIND_MASK = 0x0F
        private const val EXTENDED = 0x10
        private const val BYTE = 0xFF
        private const val INT = 4
        private const val LONG = 8
        private const val UINT = 0xFFFF_FFFFL
        private const val SHORT_MASK = 0xFFFF

        private fun ByteArray.int(at: Int): Int = ByteBuffer.wrap(this, at, INT).order(ByteOrder.LITTLE_ENDIAN).int

        private fun ByteArray.long(at: Int): Long = ByteBuffer.wrap(this, at, LONG).order(ByteOrder.LITTLE_ENDIAN).long

        private fun ByteArray.short(at: Int): Int =
            ByteBuffer
                .wrap(this, at, 2)
                .order(ByteOrder.LITTLE_ENDIAN)
                .short
                .toInt() and SHORT_MASK

        /** Zero-terminated UTF-8 string at [at] and the bytes it used (with the zero). */
        private fun ByteArray.cString(at: Int): Pair<String, Int> {
            var end = at
            while (end < size && this[end] != 0.toByte()) end++
            return String(this, at, end - at, Charsets.UTF_8) to (end - at + 1)
        }

        /** Unsigned byte order, as the ZIM URL list is sorted. */
        internal fun compareBytes(
            a: ByteArray,
            b: ByteArray,
        ): Int {
            val n = minOf(a.size, b.size)
            for (i in 0 until n) {
                val d = (a[i].toInt() and BYTE) - (b[i].toInt() and BYTE)
                if (d != 0) return d
            }
            return a.size - b.size
        }
    }
}
