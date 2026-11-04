package app.hain.docs.ocr

import org.junit.Assert.assertEquals
import org.junit.Test

class OcrImageTest {
    @Test
    fun detSizeKeepsLongSideAndMultiplesOf32() {
        assertEquals(896 to 1280, OcrImage.detSize(2480, 3508))
        assertEquals(1248 to 704, OcrImage.detSize(1240, 700))
        assertEquals(32 to 32, OcrImage.detSize(5, 5))
    }

    @Test
    fun recWidthKeepsAspectRatio() {
        assertEquals(480, OcrImage.recWidth(300, 30))
        assertEquals(16, OcrImage.recWidth(2, 40))
        assertEquals(3200, OcrImage.recWidth(100_000, 10))
    }

    @Test
    fun chwIsBgrAndNormalized() {
        // one pixel: R = 255, G = 0, B = 51
        val px = intArrayOf((0xFF shl 24) or (0xFF shl 16) or 51)
        val out = OcrImage.chw(px, OcrImage.REC_MEAN, OcrImage.REC_STD)
        assertEquals((51 / 255f - 0.5f) / 0.5f, out[0], 1e-5f)
        assertEquals(-1f, out[1], 1e-5f)
        assertEquals(1f, out[2], 1e-5f)
    }
}
