package app.hain.docs.ocr

/** A text box on the page picture, in pixels of the original picture. [score] = detection certainty. */
data class OcrBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val score: Float = 1f,
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
    val centerY: Float get() = (top + bottom) / 2f
}

/** One recognized text line: where it is, what it says and how sure the recognition is (0…1). */
data class OcrItem(
    val box: OcrBox,
    val text: String,
    val confidence: Float,
)

/** The text of one picture in reading order and the mean certainty of its lines (0 when empty). */
data class OcrResult(
    val text: String,
    val confidence: Float,
    val lines: Int,
)
