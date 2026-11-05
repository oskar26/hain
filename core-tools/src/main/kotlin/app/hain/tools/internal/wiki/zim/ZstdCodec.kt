package app.hain.tools.internal.wiki.zim

import com.github.luben.zstd.ZstdInputStream
import java.io.IOException
import java.io.InputStream

/** T-237: unpacks ZIM clusters with zstd (kind 5); very old xz packs (kind 4) are not offered. */
object ZstdCodec : ClusterCodec {
    private const val ZSTD = 5

    override fun open(
        kind: Int,
        input: InputStream,
    ): InputStream {
        if (kind != ZSTD) throw IOException("cluster compression $kind is not supported")
        return ZstdInputStream(input)
    }
}
