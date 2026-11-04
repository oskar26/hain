package app.hain.docs.ocr

/**
 * Turns the detector's probability map into text boxes (T-121), like PaddleOCR's DBPostProcess with
 * the values of the model's `inference.yml`: pixels above [thresh] form regions (4-neighbours);
 * a region whose mean probability is below [boxThresh] or that is thinner than [minSide] is
 * dropped; the rest grow by `area × unclip / perimeter` (DB's "unclip") and are scaled back to the
 * original picture. Boxes are axis-aligned: pages are photographed or scanned upright, and a
 * line upside down is turned by the orientation model before it is read.
 */
class DbPostProcess(
    private val thresh: Float = THRESH,
    private val boxThresh: Float = BOX_THRESH,
    private val unclip: Float = UNCLIP,
    private val minSide: Int = MIN_SIDE,
    private val maxCandidates: Int = MAX_CANDIDATES,
) {
    /**
     * [prob] is the `[mapH × mapW]` map for a picture of [origW] × [origH] pixels. Returns the boxes
     * from top to bottom, then left to right.
     */
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "LongMethod", "LoopWithTooManyJumpStatements")
    fun boxes(
        prob: FloatArray,
        mapW: Int,
        mapH: Int,
        origW: Int,
        origH: Int,
    ): List<OcrBox> {
        require(prob.size >= mapW * mapH) { "map too small" }
        val label = IntArray(mapW * mapH)
        val stack = IntArray(mapW * mapH)
        val out = mutableListOf<OcrBox>()
        val sx = origW.toFloat() / mapW
        val sy = origH.toFloat() / mapH
        var next = 0
        for (start in 0 until mapW * mapH) {
            if (label[start] != 0 || prob[start] <= thresh) continue
            next++
            var top = 0
            stack[top++] = start
            label[start] = next
            var x0 = mapW
            var y0 = mapH
            var x1 = -1
            var y1 = -1
            var sum = 0.0
            var n = 0
            while (top > 0) {
                val p = stack[--top]
                val x = p % mapW
                val y = p / mapW
                sum += prob[p]
                n++
                if (x < x0) x0 = x
                if (x > x1) x1 = x
                if (y < y0) y0 = y
                if (y > y1) y1 = y
                if (x > 0) top = push(p - 1, prob, label, stack, top, next)
                if (x < mapW - 1) top = push(p + 1, prob, label, stack, top, next)
                if (y > 0) top = push(p - mapW, prob, label, stack, top, next)
                if (y < mapH - 1) top = push(p + mapW, prob, label, stack, top, next)
            }
            val w = x1 - x0 + 1
            val h = y1 - y0 + 1
            val score = (sum / n).toFloat()
            if (score < boxThresh || minOf(w, h) < minSide) continue
            val off = w.toFloat() * h * unclip / (2f * (w + h))
            out +=
                OcrBox(
                    left = ((x0 - off) * sx).coerceIn(0f, origW.toFloat()),
                    top = ((y0 - off) * sy).coerceIn(0f, origH.toFloat()),
                    right = ((x1 + 1 + off) * sx).coerceIn(0f, origW.toFloat()),
                    bottom = ((y1 + 1 + off) * sy).coerceIn(0f, origH.toFloat()),
                    score = score,
                )
            if (out.size >= maxCandidates) break
        }
        return out.sortedWith(compareBy<OcrBox> { it.top }.thenBy { it.left })
    }

    @Suppress("LongParameterList") // hot loop: no objects per pixel
    private fun push(
        p: Int,
        prob: FloatArray,
        label: IntArray,
        stack: IntArray,
        top: Int,
        id: Int,
    ): Int {
        if (label[p] != 0 || prob[p] <= thresh) return top
        label[p] = id
        stack[top] = p
        return top + 1
    }

    companion object {
        const val THRESH = 0.3f
        const val BOX_THRESH = 0.6f
        const val UNCLIP = 1.5f
        const val MIN_SIDE = 3
        const val MAX_CANDIDATES = 1000
    }
}
