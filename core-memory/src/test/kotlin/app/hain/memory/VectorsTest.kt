package app.hain.memory

import app.hain.memory.internal.Vectors
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class VectorsTest {
    @Test
    fun cosineOfSameOppositeAndOrthogonalVectors() {
        val a = floatArrayOf(1f, 2f, 3f)
        assertEquals(1.0, Vectors.cosine(a, floatArrayOf(2f, 4f, 6f)), 1e-6)
        assertEquals(-1.0, Vectors.cosine(a, floatArrayOf(-1f, -2f, -3f)), 1e-6)
        assertEquals(0.0, Vectors.cosine(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f)), 1e-6)
        assertEquals(0.0, Vectors.cosine(floatArrayOf(), a), 0.0)
        assertEquals(0.0, Vectors.cosine(floatArrayOf(0f, 0f, 0f), a), 0.0)
    }

    @Test
    fun bytesRoundTripLittleEndian() {
        val v = floatArrayOf(1f, -0.5f, 0.25f, 3.75f)
        val bytes = Vectors.toBytes(v)
        assertEquals(16, bytes.size)
        // 1.0f = 0x3F800000, little endian
        assertArrayEquals(byteArrayOf(0, 0, 0x80.toByte(), 0x3f), bytes.copyOfRange(0, 4))
        assertArrayEquals(v, Vectors.fromBytes(bytes), 0f)
    }

    @Test
    fun relevanceSpreadsTheMeasuredRange() {
        // measured with LFM2.5-Embedding: unrelated ≤ 0.08, fitting 0.15…0.35
        assertEquals(0.0, Vectors.relevance(-0.1), 0.0)
        assertEquals(0.0, Vectors.relevance(0.05), 1e-9)
        assertEquals(0.4, Vectors.relevance(0.15), 1e-9)
        assertEquals(1.0, Vectors.relevance(0.34), 0.05)
        assertEquals(1.0, Vectors.relevance(0.9), 0.0)
    }
}
