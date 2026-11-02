package app.hain.models.api

/** What the phone offers (T-081): memory in GB and free storage in bytes. */
data class DeviceProfile(
    val ramGb: Int,
    val freeBytes: Long,
)

/** How well a pack fits this phone. */
enum class Fit { GOOD, TIGHT, NO_SPACE, TOO_SMALL }

/** Pure rules, no Android calls, so they are easy to test (MODELS.md). */
object DeviceCheck {
    private const val SPACE_MARGIN_PERCENT = 110L
    private const val PERCENT = 100L

    fun fit(
        model: ModelInfo,
        device: DeviceProfile,
    ): Fit =
        when {
            device.ramGb < model.minRamGb - 1 -> Fit.TOO_SMALL
            device.freeBytes < model.totalSize * SPACE_MARGIN_PERCENT / PERCENT -> Fit.NO_SPACE
            device.ramGb < model.minRamGb -> Fit.TIGHT
            else -> Fit.GOOD
        }
}
