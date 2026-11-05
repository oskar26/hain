package app.hain.llm.internal

import java.io.File

/**
 * T-264 (Oskar: ~20 t/s instead of 29): llama.cpp is as fast as its slowest thread, so the brain runs
 * only on the performance cores. Phones have two or three clusters; the cluster with the lowest top
 * clock (the efficiency cores, e.g. 4× A55 at 2.0 GHz) is left out. When all cores clock the same, or
 * the clocks cannot be read, it falls back to "all cores minus two". Pure apart from [read].
 */
object CpuCores {
    const val MIN_THREADS = 2
    const val MAX_THREADS = 6

    /** Max clock of each core in kHz (`cpuinfo_max_freq`), `null` where it cannot be read. */
    fun read(count: Int = Runtime.getRuntime().availableProcessors()): List<Long?> =
        (0 until count).map { i ->
            runCatching {
                File("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
                    .readText()
                    .trim()
                    .toLong()
            }.getOrNull()
        }

    /** Threads for the brain: the cores faster than the slowest cluster, within [MIN_THREADS]..[MAX_THREADS]. */
    fun threads(maxFreqs: List<Long?>): Int {
        val known = maxFreqs.filterNotNull().filter { it > 0 }
        val fallback = (maxFreqs.size - 2).coerceIn(MIN_THREADS, MAX_THREADS)
        val slowest = known.minOrNull()
        val fast = known.count { slowest != null && it > slowest }
        return if (known.size != maxFreqs.size || fast == 0) fallback else fast.coerceIn(MIN_THREADS, MAX_THREADS)
    }
}
