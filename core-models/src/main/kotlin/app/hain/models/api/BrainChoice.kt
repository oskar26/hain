package app.hain.models.api

/** How big a brain feels, for a name in plain language instead of "LFM3 1.7B" (T-055). */
enum class BrainSize {
    SMALL,
    MEDIUM,
    LARGE,
    ;

    companion object {
        private const val SMALL_MAX_BYTES = 1_500_000_000L
        private const val MEDIUM_MAX_BYTES = 3_000_000_000L

        fun of(bytes: Long): BrainSize =
            when {
                bytes <= SMALL_MAX_BYTES -> SMALL
                bytes <= MEDIUM_MAX_BYTES -> MEDIUM
                else -> LARGE
            }
    }
}

/** One row of the brain picker in the top bar (T-055). */
data class BrainOption(
    val model: ModelInfo,
    val installed: Boolean,
    /** The brain DeviceCheck thinks fits this phone best. */
    val recommended: Boolean,
    /** The brain Hain uses (or loads) right now. */
    val active: Boolean,
    val fit: Fit,
) {
    val size: BrainSize get() = BrainSize.of(model.totalSize)

    /** T-240: the optional "Großes Gehirn" for strong phones. */
    val strong: Boolean get() = model.isStrong
}

/**
 * Pure rules for the brain picker (T-055), no Android calls so they are easy to test: which brain is
 * in use, which one is recommended for this phone, and what the menu lists.
 */
object BrainChoice {
    const val KIND_BRAIN = "brain"

    fun brains(catalog: List<ModelInfo>): List<ModelInfo> = catalog.filter { it.kind == KIND_BRAIN }

    /**
     * The brain to load: the chosen one when it is on the phone, else the default brain when it is,
     * else the first brain on the phone, else the default brain (so onboarding can fetch it).
     */
    fun active(
        catalog: List<ModelInfo>,
        selectedId: String?,
        installed: (ModelInfo) -> Boolean,
    ): ModelInfo {
        val brains = brains(catalog)
        require(brains.isNotEmpty()) { "catalog has no brain" }
        val fallback = brains.firstOrNull { it.isDefault } ?: brains.first()
        return brains.firstOrNull { it.id == selectedId && installed(it) }
            ?: fallback.takeIf(installed)
            ?: brains.firstOrNull(installed)
            ?: fallback
    }

    /**
     * Recommended for this phone: the biggest brain that fits well (DeviceCheck GOOD) and whose
     * `recommend_ram_gb` the phone reaches (T-240: the Großes Gehirn only from 12 GB). If none fits
     * well, the smallest one that fits at all (TIGHT). `null` when no brain fits.
     */
    fun recommended(
        catalog: List<ModelInfo>,
        device: DeviceProfile,
    ): ModelInfo? {
        // free space only matters for brains still to download, not for the recommendation itself
        val roomy = device.copy(freeBytes = Long.MAX_VALUE)
        val current = brains(catalog).filterNot { it.isOptional }
        return current
            .filter { DeviceCheck.fit(it, roomy) == Fit.GOOD && device.ramGb >= it.recommendRamGb }
            .maxByOrNull { it.totalSize }
            ?: current.filter { DeviceCheck.fit(it, roomy) == Fit.GOOD }.minByOrNull { it.totalSize }
            ?: current.filter { DeviceCheck.fit(it, roomy) == Fit.TIGHT }.minByOrNull { it.totalSize }
    }

    /**
     * T-240: whether a brain is shown at all. The Großes Gehirn ([ModelInfo.isStrong]) is only offered
     * where it can run (not [Fit.TOO_SMALL]) or when it is already on the phone; standard brains always.
     * T-270: an optional brain ([ModelInfo.isOptional]) is always offered, but never recommended.
     */
    fun offered(
        model: ModelInfo,
        device: DeviceProfile,
        installed: Boolean,
    ): Boolean =
        when {
            installed -> true
            !model.isStrong -> true
            else -> DeviceCheck.fit(model, device.copy(freeBytes = Long.MAX_VALUE)) != Fit.TOO_SMALL
        }

    /** Menu rows: brains on the phone first (small to big), then the ones still to download. */
    fun options(
        catalog: List<ModelInfo>,
        selectedId: String?,
        installed: (ModelInfo) -> Boolean,
        device: DeviceProfile,
    ): List<BrainOption> {
        val brains = brains(catalog)
        if (brains.isEmpty()) return emptyList()
        val active = active(catalog, selectedId, installed)
        val best = recommended(catalog, device)
        return brains
            .filter { offered(it, device, installed(it)) }
            .map { model ->
                val isInstalled = installed(model)
                BrainOption(
                    model = model,
                    installed = isInstalled,
                    recommended = model.id == best?.id,
                    active = isInstalled && model.id == active.id,
                    fit = DeviceCheck.fit(model, device),
                )
            }.sortedWith(compareBy<BrainOption> { !it.installed }.thenBy { it.model.totalSize })
    }
}
