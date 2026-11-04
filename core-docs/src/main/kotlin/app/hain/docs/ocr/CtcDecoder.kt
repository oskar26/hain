package app.hain.docs.ocr

/**
 * Greedy CTC decoding of the recognizer output (T-121): per time step the most likely class;
 * repeats are merged and the blank (class 0) dropped. The certainty of a line is the mean of the
 * kept steps' probabilities, as in PaddleOCR.
 */
object CtcDecoder {
    data class Decoded(
        val text: String,
        val confidence: Float,
    )

    /**
     * [probs] is the flat `[steps × classes]` output of one line (softmax already applied by the
     * model). Classes beyond [classes] (a model/dictionary mismatch) are skipped, never guessed.
     */
    fun decode(
        probs: FloatArray,
        steps: Int,
        classes: List<String>,
    ): Decoded {
        if (steps <= 0 || probs.isEmpty()) return Decoded("", 0f)
        val count = probs.size / steps
        val text = StringBuilder()
        var sum = 0f
        var kept = 0
        var previous = -1
        for (t in 0 until steps) {
            val base = t * count
            var best = 0
            var bestP = probs[base]
            for (c in 1 until count) {
                val p = probs[base + c]
                if (p > bestP) {
                    best = c
                    bestP = p
                }
            }
            if (best != previous && best != BLANK && best < classes.size) {
                text.append(classes[best])
                sum += bestP
                kept++
            }
            previous = best
        }
        return Decoded(text.toString(), if (kept == 0) 0f else sum / kept)
    }

    private const val BLANK = 0
}
