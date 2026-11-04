package app.hain.docs.ocr

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.Purpose
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** Opens the download stream of one address (a fake in tests). */
fun interface PackSource {
    @Throws(IOException::class)
    fun open(url: String): InputStream
}

/**
 * The text recognition pack (T-121): PP-OCRv5 mobile detection, the Latin recognizer and the small
 * line orientation model, ONNX exports from PaddlePaddle's own Hugging Face repos (Apache-2.0),
 * pinned to a commit and checked by SHA-256. About 13.9 MB, downloaded only after the user says
 * yes ("Texterkennung laden?"), always through the [NetworkGate] (network log, offline mode).
 */
class OcrPack(
    private val dir: File,
    private val gate: NetworkGate,
    private val source: PackSource = HttpPackSource(),
    private val files: List<PackFile> = FILES,
) {
    data class PackFile(
        val name: String,
        val url: String,
        val sha256: String,
        val size: Long,
    )

    val totalBytes: Long get() = files.sumOf { it.size }

    /** All files are there with the right size (checked by SHA-256 when they were saved). */
    fun installed(): Boolean = files.all { File(dir, it.name).length() == it.size }

    fun file(name: String): File = File(dir, name)

    /**
     * Downloads what is missing. [onProgress] gets the bytes done and the total. Throws
     * [IOException] on network or checksum trouble (the broken file is deleted).
     */
    suspend fun download(onProgress: (done: Long, total: Long) -> Unit = { _, _ -> }) {
        dir.mkdirs()
        var done = files.filter { File(dir, it.name).length() == it.size }.sumOf { it.size }
        onProgress(done, totalBytes)
        for (f in files) {
            val target = File(dir, f.name)
            if (target.length() == f.size) continue
            val part = File(dir, f.name + ".part")
            val start = done
            gate.stream(GateRequest(f.url, Purpose.MODEL_DOWNLOAD, userStarted = true)) {
                save(f, part) { n -> onProgress(start + n, totalBytes) }
            }
            if (!part.renameTo(target)) throw IOException("could not save ${f.name}")
            done += f.size
            onProgress(done, totalBytes)
        }
    }

    /** "Pack entfernen": deletes the models; the documents and their text stay. */
    fun remove() {
        files.forEach { f ->
            File(dir, f.name).delete()
            File(dir, f.name + ".part").delete()
        }
    }

    @Suppress("NestedBlockDepth")
    private fun save(
        f: PackFile,
        part: File,
        onBytes: (Long) -> Unit,
    ): Long {
        val digest = MessageDigest.getInstance("SHA-256")
        var n = 0L
        source.open(f.url).use { input ->
            FileOutputStream(part).use { out ->
                val buf = ByteArray(BUFFER)
                var r = input.read(buf)
                while (r >= 0) {
                    out.write(buf, 0, r)
                    digest.update(buf, 0, r)
                    n += r
                    if (n > f.size) break
                    onBytes(n)
                    r = input.read(buf)
                }
            }
        }
        val sha = digest.digest().joinToString("") { "%02x".format(it) }
        if (n != f.size || sha != f.sha256) {
            part.delete()
            throw IOException("checksum mismatch for ${f.name}")
        }
        return n
    }

    companion object {
        const val DIR = "ocr"
        const val DET = "det.onnx"
        const val REC = "rec_latin.onnx"
        const val REC_YML = "rec_latin.yml"
        const val ORI = "textline_ori.onnx"
        private const val BUFFER = 64 * 1024
        private const val HF = "https://huggingface.co/PaddlePaddle"
        private const val DET_COMMIT = "e6f4fa85f00e168c862bc462aebca69eef9b3d3d"
        private const val REC_COMMIT = "89d3a50e2c27e2e7cceeab0e944c25c807d5db4f"
        private const val ORI_COMMIT = "aea1f18d97338aaf84b463f90192f2820524a00a"
        private const val DET_REPO = "$HF/PP-OCRv5_mobile_det_onnx/resolve/$DET_COMMIT"
        private const val REC_REPO = "$HF/latin_PP-OCRv5_mobile_rec_onnx/resolve/$REC_COMMIT"
        private const val ORI_REPO = "$HF/PP-LCNet_x0_25_textline_ori_onnx/resolve/$ORI_COMMIT"

        val FILES =
            listOf(
                PackFile(
                    DET,
                    "$DET_REPO/inference.onnx",
                    "a431985659dc921974177a95adcfbb90fd9e51989a5e04d70d0b75f597b6e61d",
                    4_826_518,
                ),
                PackFile(
                    REC,
                    "$REC_REPO/inference.onnx",
                    "7888113072263cb471b93f66dd5e2ad70548dc526fa1ace760d0d973dd121498",
                    8_042_023,
                ),
                PackFile(
                    REC_YML,
                    "$REC_REPO/inference.yml",
                    "0bbe984570f597af3638e50bdf2e8276f3ab26a61966096538b3b0d1849f5c84",
                    6_817,
                ),
                PackFile(
                    ORI,
                    "$ORI_REPO/inference.onnx",
                    "94a6a0a0425f2b5f08b5df72086f2d72abe40f1d22f6d12d2cd83674f11f2ff3",
                    1_019_454,
                ),
            )
    }
}

/** Plain [HttpURLConnection] that follows Hugging Face's redirect to its file servers. */
class HttpPackSource : PackSource {
    override fun open(url: String): InputStream {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = TIMEOUT_MS
        conn.readTimeout = TIMEOUT_MS
        conn.instanceFollowRedirects = true
        conn.setRequestProperty("User-Agent", "Hain/0.1 (Android)")
        val code = conn.responseCode
        if (code != HttpURLConnection.HTTP_OK) {
            conn.disconnect()
            throw IOException("HTTP $code")
        }
        return conn.inputStream
    }

    private companion object {
        const val TIMEOUT_MS = 20_000
    }
}
