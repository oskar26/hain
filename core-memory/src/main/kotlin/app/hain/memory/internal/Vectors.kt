package app.hain.memory.internal

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

/** Vector helpers for the Kotlin cosine search (T-110; no sqlite-vec needed for a few thousand facts). */
object Vectors {
    /**
     * LFM2.5-Embedding-350M gives low cosines between a question and a fact (measured on the host,
     * 04.11., query:/document: prefixes): unrelated pairs −0.15…0.08, fitting ones 0.15…0.35
     * ("Wie heißt meine Schwester?" ↔ "Schwester heißt Rosa" = 0.34). [relevance] spreads that
     * range over 0..1 so the hybrid score of MEMORY.md §3 works with its 0.35 threshold.
     */
    const val FLOOR = 0.05
    const val SPAN = 0.25

    /** Cosine of two vectors (any length; 0 when one is empty or zero). */
    @Suppress("ReturnCount")
    fun cosine(
        a: FloatArray,
        b: FloatArray,
    ): Double {
        val n = minOf(a.size, b.size)
        if (n == 0) return 0.0
        var dot = 0.0
        var na = 0.0
        var nb = 0.0
        for (i in 0 until n) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        if (na == 0.0 || nb == 0.0) return 0.0
        return dot / (sqrt(na) * sqrt(nb))
    }

    /** Question ↔ fact cosine as relevance 0..1 (see [FLOOR], [SPAN]). */
    fun relevance(cosine: Double): Double = ((cosine - FLOOR) / SPAN).coerceIn(0.0, 1.0)

    /** Little-endian float32, as stored in `memory_fact_vec.vector`. */
    fun toBytes(vector: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(vector.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        vector.forEach { buffer.putFloat(it) }
        return buffer.array()
    }

    fun fromBytes(bytes: ByteArray): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        return FloatArray(buffer.remaining()).also { buffer.get(it) }
    }

    /** Which text a vector was made from (an edited fact needs a new one). */
    fun textHash(text: String): Int = text.trim().hashCode()
}
