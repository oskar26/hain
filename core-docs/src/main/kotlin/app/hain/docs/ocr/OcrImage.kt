package app.hain.docs.ocr

import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The picture maths of the OCR pipeline (T-121), without Android, so it is tested on the JVM:
 * sizes for the detector and recognizer and the pixel → tensor step. Pixels are packed ARGB ints
 * (as `Bitmap.getPixels` gives them); the tensors are CHW in BGR order, as PaddleOCR trains on
 * OpenCV pictures.
 */
@Suppress("MagicNumber") // the normalisation values of the models' inference.yml
object OcrImage {
    /** Long side for the detector; text on an A4 photo stays readable, the phone stays quick. */
    const val DET_MAX_SIDE = 1280
    const val REC_HEIGHT = 48
    const val REC_MIN_WIDTH = 16
    const val REC_MAX_WIDTH = 3200
    const val ORI_WIDTH = 160
    const val ORI_HEIGHT = 80
    private const val STEP = 32
    private const val BYTE = 255f

    val DET_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
    val DET_STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    val REC_MEAN = floatArrayOf(0.5f, 0.5f, 0.5f)
    val REC_STD = floatArrayOf(0.5f, 0.5f, 0.5f)

    /** Detector input size: long side at most [maxSide], both sides multiples of 32 (at least 32). */
    fun detSize(
        width: Int,
        height: Int,
        maxSide: Int = DET_MAX_SIDE,
    ): Pair<Int, Int> {
        val scale = minOf(1f, maxSide.toFloat() / max(width, height))
        val w = max(STEP, (width * scale / STEP).roundToInt() * STEP)
        val h = max(STEP, (height * scale / STEP).roundToInt() * STEP)
        return w to h
    }

    /** Recognizer input width for a line of [width] × [height]: height 48, same aspect ratio. */
    fun recWidth(
        width: Int,
        height: Int,
    ): Int = ceil(REC_HEIGHT.toFloat() * width / max(1, height)).toInt().coerceIn(REC_MIN_WIDTH, REC_MAX_WIDTH)

    /** Packed ARGB pixels → CHW floats in B, G, R order, `(v / 255 − mean) / std` per channel. */
    fun chw(
        pixels: IntArray,
        mean: FloatArray,
        std: FloatArray,
    ): FloatArray {
        val plane = pixels.size
        val out = FloatArray(plane * 3)
        for (i in 0 until plane) {
            val c = pixels[i]
            out[i] = ((c and MASK) / BYTE - mean[0]) / std[0]
            out[plane + i] = (((c shr GREEN) and MASK) / BYTE - mean[1]) / std[1]
            out[2 * plane + i] = (((c shr RED) and MASK) / BYTE - mean[2]) / std[2]
        }
        return out
    }

    private const val MASK = 0xFF
    private const val GREEN = 8
    private const val RED = 16
}
