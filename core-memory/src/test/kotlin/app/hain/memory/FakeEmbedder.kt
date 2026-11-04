package app.hain.memory

import app.hain.memory.api.EmbedKind
import app.hain.memory.api.Embedder

/** Vectors by hand: each text gets the vector of the first key it contains (lower case), else none. */
class FakeEmbedder(
    private val vectors: Map<String, FloatArray>,
    var ready: Boolean = true,
    override val model: String = "fake",
) : Embedder {
    val calls = mutableListOf<Pair<String, EmbedKind>>()

    override fun ready(): Boolean = ready

    override suspend fun embed(
        text: String,
        kind: EmbedKind,
    ): FloatArray? {
        if (!ready) return null
        calls += text to kind
        val lower = text.lowercase()
        return vectors.entries.firstOrNull { lower.contains(it.key) }?.value ?: floatArrayOf(0f, 0f, 0f, 1f)
    }

    companion object {
        /** A unit vector at [angle] degrees in the first plane (cosine of two = cos of the angle between). */
        fun at(angle: Double): FloatArray {
            val r = Math.toRadians(angle)
            return floatArrayOf(Math.cos(r).toFloat(), Math.sin(r).toFloat(), 0f, 0f)
        }
    }
}
