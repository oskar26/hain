package app.hain.ui.pim

/**
 * The 2048 easter egg as numbers (T-097, Oskar 03.11. 23:26): nothing happens before Pim has been
 * held for [ARM_MS]; then he grows slowly and steadily to at most [MAX_SCALE]× his size, trembles more
 * and more and glows greener, and only after [HOLD_MS] of pressing without a break does he burst.
 * Letting go earlier springs him back. Pure, so the threshold and the size are unit-tested.
 */
object PimEgg {
    /** Pressing at least this long (ms, without a break) opens the game. */
    const val HOLD_MS = 5_000L

    /** Shorter than this is a tap or the start of a drag: no growing yet. */
    const val ARM_MS = 400L

    /** Pim never grows beyond this. */
    const val MAX_SCALE = 5f

    /** Strongest trembling at full charge, in dp. */
    const val MAX_SHAKE_DP = 4.5f

    /** How long the charge runs once it is armed (ms). */
    const val CHARGE_MS: Int = (HOLD_MS - ARM_MS).toInt()

    /** True once the press has lasted long enough. */
    fun shouldBurst(heldMs: Long): Boolean = heldMs >= HOLD_MS

    /** 0 until armed, then rising steadily to 1 exactly at [HOLD_MS]. */
    fun progress(heldMs: Long): Float = ((heldMs - ARM_MS).toFloat() / (HOLD_MS - ARM_MS)).coerceIn(0f, 1f)

    /**
     * Scale for a charge of [progress]: starts gently and keeps growing (ease-in), monotonic,
     * 1× at 0 and [MAX_SCALE]× at 1, never more.
     */
    fun scale(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        val eased = p * (EASE_LINEAR + (1f - EASE_LINEAR) * p)
        return (1f + (MAX_SCALE - 1f) * eased).coerceIn(1f, MAX_SCALE)
    }

    /** Trembling amplitude in dp: barely at first, clearly towards the end. */
    fun shakeDp(progress: Float): Float {
        val p = progress.coerceIn(0f, 1f)
        return MAX_SHAKE_DP * p * p
    }

    private const val EASE_LINEAR = 0.35f
}
