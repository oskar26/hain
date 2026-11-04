package app.hain.docs.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DbPostProcessTest {
    private fun map(
        w: Int,
        h: Int,
        vararg rects: IntArray,
        value: Float = 0.9f,
    ): FloatArray {
        val m = FloatArray(w * h)
        rects.forEach { r ->
            for (y in r[1] until r[3]) for (x in r[0] until r[2]) m[y * w + x] = value
        }
        return m
    }

    @Test
    fun twoRegionsBecomeTwoBoxesTopToBottom() {
        val prob = map(64, 32, intArrayOf(4, 20, 40, 26), intArrayOf(4, 4, 30, 10))
        val boxes = DbPostProcess().boxes(prob, 64, 32, 64, 32)
        assertEquals(2, boxes.size)
        assertTrue(boxes[0].top < boxes[1].top)
    }

    @Test
    fun boxGrowsByUnclipAndScalesBack() {
        // region 20 × 6 px: offset = 20 * 6 * 1.5 / (2 * 26) ≈ 3.46 px on the map
        val prob = map(64, 32, intArrayOf(10, 10, 30, 16))
        val box = DbPostProcess().boxes(prob, 64, 32, 128, 64).single()
        val off = 20f * 6 * 1.5f / (2f * 26)
        assertEquals((10 - off) * 2, box.left, 1e-3f)
        assertEquals((16 + off) * 2, box.bottom, 1e-3f)
        assertEquals(0.9f, box.score, 1e-5f)
    }

    @Test
    fun weakOrThinRegionsAreDropped() {
        val weak = map(32, 32, intArrayOf(2, 2, 20, 10), value = 0.5f)
        assertTrue(DbPostProcess().boxes(weak, 32, 32, 32, 32).isEmpty())
        val thin = map(32, 32, intArrayOf(2, 2, 20, 4))
        assertTrue(DbPostProcess().boxes(thin, 32, 32, 32, 32).isEmpty())
    }

    @Test
    fun boxesStayInsideThePicture() {
        val prob = map(32, 32, intArrayOf(0, 0, 32, 8))
        val box = DbPostProcess().boxes(prob, 32, 32, 32, 32).single()
        assertEquals(0f, box.left, 0f)
        assertEquals(0f, box.top, 0f)
        assertEquals(32f, box.right, 0f)
    }
}
