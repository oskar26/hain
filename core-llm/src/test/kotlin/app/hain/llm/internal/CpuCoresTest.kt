package app.hain.llm.internal

import org.junit.Assert.assertEquals
import org.junit.Test

/** T-264: only the performance cores run the brain. */
class CpuCoresTest {
    /** [count] cores at [ghz] each, as kHz like `cpuinfo_max_freq`. */
    private fun cluster(
        count: Int,
        ghz: Double,
    ): List<Long> = List(count) { (ghz * KHZ_PER_GHZ).toLong() }

    @Test
    fun theEfficiencyClusterIsLeftOut() {
        // Snapdragon 8 Gen 2: 1× 3.2 GHz, 4× 2.8 GHz, 3× 2.0 GHz → 5
        assertEquals(5, CpuCores.threads(cluster(3, 2.0) + cluster(4, 2.8) + cluster(1, 3.2)))
        // Tensor G3: 1× 2.91, 4× 2.37, 4× 1.7 → 5
        assertEquals(5, CpuCores.threads(cluster(4, 1.7) + cluster(4, 2.37) + cluster(1, 2.91)))
        // 4× A76 + 4× A55 → 4
        assertEquals(4, CpuCores.threads(cluster(4, 1.8) + cluster(4, 2.4)))
    }

    @Test
    fun boundsAndFallbacks() {
        // only one fast core: still 2 threads
        assertEquals(2, CpuCores.threads(listOf(1800000L, 1800000, 1800000, 2800000)))
        // all cores alike: all minus two
        assertEquals(6, CpuCores.threads(List(8) { 2000000L }))
        assertEquals(2, CpuCores.threads(List(4) { 2000000L }))
        // unreadable clocks: all minus two, capped
        assertEquals(6, CpuCores.threads(List(10) { null }))
        assertEquals(2, CpuCores.threads(listOf(2000000L, null, 3000000L)))
    }

    private companion object {
        const val KHZ_PER_GHZ = 1_000_000
    }
}
