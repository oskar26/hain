package app.hain.docs.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class CtcDecoderTest {
    private val classes = listOf("", "a", "b", " ")

    /** One-hot-ish rows: [best] gets [p], the rest share what is left. */
    private fun rows(vararg steps: Pair<Int, Float>): FloatArray =
        steps
            .flatMap { (best, p) ->
                List(classes.size) { c -> if (c == best) p else (1f - p) / (classes.size - 1) }
            }.toFloatArray()

    @Test
    fun repeatsMergeAndBlanksDrop() {
        val probs = rows(1 to 0.9f, 1 to 0.9f, 0 to 0.9f, 1 to 0.8f, 2 to 0.7f, 3 to 0.9f, 2 to 0.6f)
        val decoded = CtcDecoder.decode(probs, 7, classes)
        assertEquals("aab b", decoded.text)
        assertEquals((0.9f + 0.8f + 0.7f + 0.9f + 0.6f) / 5, decoded.confidence, 1e-5f)
    }

    @Test
    fun onlyBlanksGiveEmptyText() {
        val decoded = CtcDecoder.decode(rows(0 to 0.9f, 0 to 0.9f), 2, classes)
        assertEquals("", decoded.text)
        assertEquals(0f, decoded.confidence, 0f)
    }

    @Test
    fun classOutsideTheDictionaryIsSkipped() {
        val decoded = CtcDecoder.decode(floatArrayOf(0f, 0.1f, 0.9f), 1, listOf("", "a"))
        assertEquals("", decoded.text)
    }

    @Test
    fun noStepsNoText() {
        assertEquals("", CtcDecoder.decode(FloatArray(0), 0, classes).text)
    }
}
