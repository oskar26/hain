package app.hain.docs.ocr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.graphics.Matrix
import java.nio.FloatBuffer
import kotlin.math.roundToInt

/**
 * PP-OCRv5 mobile on ONNX Runtime (T-121, own pipeline, decision in STATUS.md): detection
 * ([DbPostProcess]), one box per text line, orientation check (0°/180°), Latin recognition
 * ([CtcDecoder]) and [ReadingOrder]. Runs on the CPU with [threads] threads; one page of
 * A4 takes about 2–5 s on a mid-range phone. Not thread-safe: one page at a time.
 */
class OrtOcrEngine(
    pack: OcrPack,
    threads: Int = THREADS,
) : AutoCloseable {
    private val env = OrtEnvironment.getEnvironment()
    private val options = OrtSession.SessionOptions().apply { setIntraOpNumThreads(threads) }
    private val det = env.createSession(pack.file(OcrPack.DET).absolutePath, options)
    private val rec = env.createSession(pack.file(OcrPack.REC).absolutePath, options)
    private val ori = env.createSession(pack.file(OcrPack.ORI).absolutePath, options)
    private val classes =
        RecDictionary.classes(RecDictionary.parseYaml(pack.file(OcrPack.REC_YML).readText()))
    private val post = DbPostProcess()

    /** The text of [picture] in reading order. */
    fun read(picture: Bitmap): OcrResult {
        val src =
            if (picture.config == Bitmap.Config.ARGB_8888) picture else picture.copy(Bitmap.Config.ARGB_8888, false)
        val (w, h) = OcrImage.detSize(src.width, src.height)
        val prob = run(det, scaled(src, w, h), OcrImage.DET_MEAN, OcrImage.DET_STD).first
        val boxes = post.boxes(prob, w, h, src.width, src.height)
        val items =
            boxes.mapNotNull { box ->
                val line = crop(src, box) ?: return@mapNotNull null
                val upright = upright(line)
                val rw = OcrImage.recWidth(upright.width, upright.height)
                val input = scaled(upright, rw, OcrImage.REC_HEIGHT)
                val (out, shape) = run(rec, input, OcrImage.REC_MEAN, OcrImage.REC_STD)
                val decoded = CtcDecoder.decode(out, shape[1].toInt(), classes)
                decoded
                    .takeIf { it.text.isNotBlank() && it.confidence >= MIN_LINE_CONFIDENCE }
                    ?.let { OcrItem(box, it.text, it.confidence) }
            }
        val chars = items.sumOf { it.text.length }.coerceAtLeast(1)
        val confidence = items.sumOf { (it.confidence * it.text.length).toDouble() }.toFloat() / chars
        return OcrResult(ReadingOrder.text(items), if (items.isEmpty()) 0f else confidence, items.size)
    }

    /** Vertical lines are turned a quarter, upside-down lines (orientation model) half way. */
    private fun upright(line: Bitmap): Bitmap {
        val turned = if (line.height >= VERTICAL * line.width) rotate(line, QUARTER_LEFT) else line
        val small = scaled(turned, OcrImage.ORI_WIDTH, OcrImage.ORI_HEIGHT)
        val (p, _) = run(ori, small, OcrImage.DET_MEAN, OcrImage.DET_STD)
        return if (p.size >= 2 && p[1] > p[0] && p[1] >= UPSIDE_DOWN) rotate(turned, HALF) else turned
    }

    private fun crop(
        src: Bitmap,
        box: OcrBox,
    ): Bitmap? {
        val l = box.left.toInt().coerceIn(0, src.width - 1)
        val t = box.top.toInt().coerceIn(0, src.height - 1)
        val w = (box.right.roundToInt() - l).coerceIn(1, src.width - l)
        val h = (box.bottom.roundToInt() - t).coerceIn(1, src.height - t)
        return if (w < MIN_PIXELS || h < MIN_PIXELS) null else Bitmap.createBitmap(src, l, t, w, h)
    }

    private fun rotate(
        b: Bitmap,
        degrees: Float,
    ): Bitmap = Bitmap.createBitmap(b, 0, 0, b.width, b.height, Matrix().apply { postRotate(degrees) }, true)

    private fun scaled(
        b: Bitmap,
        w: Int,
        h: Int,
    ): Bitmap = if (b.width == w && b.height == h) b else Bitmap.createScaledBitmap(b, w, h, true)

    /** Runs [session] on [b]; returns the first output as floats and its shape. */
    private fun run(
        session: OrtSession,
        b: Bitmap,
        mean: FloatArray,
        std: FloatArray,
    ): Pair<FloatArray, LongArray> {
        val pixels = IntArray(b.width * b.height)
        b.getPixels(pixels, 0, b.width, 0, 0, b.width, b.height)
        val input = OcrImage.chw(pixels, mean, std)
        val shape = longArrayOf(1, CHANNELS, b.height.toLong(), b.width.toLong())
        return OnnxTensor.createTensor(env, FloatBuffer.wrap(input), shape).use { tensor ->
            session.run(mapOf(session.inputNames.first() to tensor)).use { result ->
                val out = result[0] as OnnxTensor
                val buf = out.floatBuffer
                val floats = FloatArray(buf.remaining())
                buf.get(floats)
                floats to out.info.shape
            }
        }
    }

    override fun close() {
        det.close()
        rec.close()
        ori.close()
        options.close()
    }

    private companion object {
        const val THREADS = 4
        const val CHANNELS = 3L
        const val MIN_PIXELS = 4
        const val VERTICAL = 1.5f
        const val QUARTER_LEFT = -90f
        const val HALF = 180f
        const val UPSIDE_DOWN = 0.9f
        const val MIN_LINE_CONFIDENCE = 0.3f
    }
}
