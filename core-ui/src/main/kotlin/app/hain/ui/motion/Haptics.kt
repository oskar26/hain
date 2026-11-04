package app.hain.ui.motion

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView

/**
 * How strong Hain's haptics are (T-096, Settings → Look → "Vibration", docs/design/HAPTICS.md).
 * [scale] multiplies the strength of every primitive. Default [SUBTLE].
 */
@Suppress("MagicNumber") // the four strengths are the data
enum class HapticLevel(
    val scale: Float,
) {
    OFF(0f),
    SUBTLE(0.55f),
    NORMAL(0.8f),
    STRONG(1f),
}

/** The level the user chose; provided by the app root. */
val LocalHapticLevel = staticCompositionLocalOf { HapticLevel.SUBTLE }

/** The haptic moments of Hain (docs/design/HAPTICS.md). Streaming text never ticks. */
enum class HainHaptic {
    /** Send a message: a crisp click. */
    SEND,

    /** A switch or a choice in a segmented control: a light tick. */
    TOGGLE,

    /** The thinking level in the brain picker: the softest tick. */
    SELECT,

    /** An answer is done: a soft double tick. */
    DONE,

    /** Long press (e.g. on Pim): a low thud. */
    LONG_PRESS,

    /** Something went wrong: a short "no" (reject). */
    ERROR,

    /** Light touch feedback (tapping Pim, a tool confirm). */
    TICK,

    /** Two tiles merge in 2048. */
    MERGE,

    /** A long-press or context menu opens (the ⋯ menus). */
    MENU,

    /** A destructive choice is confirmed (delete a chat): one firm click. */
    CONFIRM,

    /** A pull gesture passes the point where letting go does something: the softest tick. */
    PULL,

    /** Pim's easter egg bursts: a deep thud with a light tick after it. */
    BURST,
}

/** One primitive of a pattern: [primitive] = a VibrationEffect.Composition primitive id. */
data class HapticStep(
    val primitive: Int,
    val scale: Float,
    val delayMs: Int = 0,
)

// VibrationEffect.Composition primitive ids (constant across API levels; kept here so the pure
// pattern table is unit-testable on the JVM).
internal const val P_CLICK = 1
internal const val P_THUD = 2
internal const val P_QUICK_FALL = 6
internal const val P_TICK = 7
internal const val P_LOW_TICK = 8

/**
 * The pattern of a moment at full strength (pure, unit-tested). Short and crisp, never a buzz:
 * only Composition primitives, scaled by the level later.
 */
@Suppress("MagicNumber") // the pattern table is the data (docs/design/HAPTICS.md)
fun HainHaptic.pattern(): List<HapticStep> =
    when (this) {
        HainHaptic.SEND -> listOf(HapticStep(P_CLICK, 0.7f))
        HainHaptic.TOGGLE -> listOf(HapticStep(P_TICK, 0.6f))
        HainHaptic.SELECT -> listOf(HapticStep(P_LOW_TICK, 0.8f))
        HainHaptic.DONE -> listOf(HapticStep(P_TICK, 0.45f), HapticStep(P_TICK, 0.7f, delayMs = 70))
        HainHaptic.LONG_PRESS -> listOf(HapticStep(P_THUD, 0.8f))
        HainHaptic.ERROR -> listOf(HapticStep(P_CLICK, 0.6f), HapticStep(P_QUICK_FALL, 0.5f, delayMs = 50))
        HainHaptic.TICK -> listOf(HapticStep(P_TICK, 0.5f))
        HainHaptic.MERGE -> listOf(HapticStep(P_CLICK, 0.5f))
        HainHaptic.MENU -> listOf(HapticStep(P_TICK, 0.55f))
        HainHaptic.CONFIRM -> listOf(HapticStep(P_CLICK, 0.8f))
        HainHaptic.PULL -> listOf(HapticStep(P_LOW_TICK, 0.7f))
        HainHaptic.BURST -> listOf(HapticStep(P_THUD, 1f), HapticStep(P_TICK, 0.5f, delayMs = 60))
    }

/** The framework constant used when the phone cannot play the primitives (pure, unit-tested). */
fun HainHaptic.fallback(): Int =
    when (this) {
        HainHaptic.SEND -> HapticFeedbackConstants.KEYBOARD_TAP
        HainHaptic.TOGGLE, HainHaptic.SELECT, HainHaptic.TICK, HainHaptic.MERGE, HainHaptic.PULL ->
            HapticFeedbackConstants.CLOCK_TICK
        HainHaptic.MENU, HainHaptic.BURST -> HapticFeedbackConstants.LONG_PRESS
        HainHaptic.CONFIRM -> HapticFeedbackConstants.VIRTUAL_KEY
        HainHaptic.DONE -> HapticFeedbackConstants.CONTEXT_CLICK
        HainHaptic.LONG_PRESS -> HapticFeedbackConstants.LONG_PRESS
        HainHaptic.ERROR ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.REJECT
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }
    }

/** [steps] scaled for [level]; empty when off. Strength stays within 0..1. */
fun scaled(
    steps: List<HapticStep>,
    level: HapticLevel,
    strength: Float = 1f,
): List<HapticStep> =
    if (level == HapticLevel.OFF) {
        emptyList()
    } else {
        steps.map { it.copy(scale = (it.scale * level.scale * strength).coerceIn(MIN_SCALE, 1f)) }
    }

/**
 * Premium haptics (T-096): crisp VibrationEffect.Composition primitives where the phone supports
 * all of them, else the matching HapticFeedbackConstants. Never `vibrate(long)`, never a buzz.
 * Respects the level setting and the system's "touch feedback" switch.
 */
@Suppress("TooManyFunctions") // one short, named call per haptic moment
class HainHaptics(
    private val view: View,
    private val level: HapticLevel = HapticLevel.SUBTLE,
) {
    private val vibrator: Vibrator? by lazy { vibratorOf(view.context) }

    fun perform(
        haptic: HainHaptic,
        strength: Float = 1f,
    ) {
        if (level == HapticLevel.OFF || !touchFeedbackOn()) return
        val steps = scaled(haptic.pattern(), level, strength)
        val v = vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && v != null && supportsAll(v, steps)) {
            compose(v, steps)
        } else {
            view.performHapticFeedback(haptic.fallback())
        }
    }

    fun send() = perform(HainHaptic.SEND)

    fun toggle() = perform(HainHaptic.TOGGLE)

    fun select() = perform(HainHaptic.SELECT)

    fun done() = perform(HainHaptic.DONE)

    fun longPress() = perform(HainHaptic.LONG_PRESS)

    fun error() = perform(HainHaptic.ERROR)

    fun tick() = perform(HainHaptic.TICK)

    fun toolConfirm() = perform(HainHaptic.TOGGLE)

    fun menu() = perform(HainHaptic.MENU)

    fun confirm() = perform(HainHaptic.CONFIRM)

    fun pull() = perform(HainHaptic.PULL)

    fun burst() = perform(HainHaptic.BURST)

    /** 2048: bigger tiles merge a little firmer. */
    fun merge(value: Int) = perform(HainHaptic.MERGE, mergeStrength(value))

    /**
     * Easter egg charge, [progress] 0–1: short primitives that get firmer (LOW_TICK → TICK → CLICK);
     * the caller makes them denser ([chargeGapMs]). Never one long vibration.
     */
    fun charge(progress: Float) {
        val p = progress.coerceIn(0f, 1f)
        perform(chargeMoment(p), chargeStrength(p))
    }

    private fun touchFeedbackOn(): Boolean =
        runCatching {
            Settings.System.getInt(view.context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
        }.getOrDefault(true)

    @Suppress("SpreadOperator") // 1–2 primitives, no real copy cost
    @RequiresApi(Build.VERSION_CODES.S)
    private fun supportsAll(
        v: Vibrator,
        steps: List<HapticStep>,
    ): Boolean = v.hasVibrator() && v.areAllPrimitivesSupported(*steps.map { it.primitive }.distinct().toIntArray())

    @RequiresApi(Build.VERSION_CODES.S)
    private fun compose(
        v: Vibrator,
        steps: List<HapticStep>,
    ) {
        val composition = VibrationEffect.startComposition()
        steps.forEach { composition.addPrimitive(it.primitive, it.scale, it.delayMs) }
        val effect = composition.compose()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH))
        } else {
            val audio =
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            @Suppress("DEPRECATION")
            v.vibrate(effect, audio)
        }
    }

    private companion object {
        fun vibratorOf(context: Context): Vibrator? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Vibrator::class.java)
            }
    }
}

/** 2 → 0.5 … 2048 → 1.0 (pure). */
fun mergeStrength(value: Int): Float {
    var bits = 0
    var v = value
    while (v > 1) {
        v = v shr 1
        bits++
    }
    return (MERGE_BASE + bits * MERGE_STEP).coerceIn(MERGE_BASE, 1f)
}

/** Which short primitive the easter egg charge plays at [progress] (pure). */
fun chargeMoment(progress: Float): HainHaptic =
    when {
        progress < CHARGE_LOW -> HainHaptic.SELECT
        progress < CHARGE_MID -> HainHaptic.TICK
        else -> HainHaptic.SEND
    }

/** Charge strength, rising with [progress] from 0.3 to 1 (pure). */
fun chargeStrength(progress: Float): Float {
    val p = progress.coerceIn(0f, 1f)
    return CHARGE_BASE + (1f - CHARGE_BASE) * p
}

/** Gap between two charge pulses: from 260 ms down to 45 ms, so they get denser (pure). */
fun chargeGapMs(progress: Float): Long {
    val p = progress.coerceIn(0f, 1f)
    return (CHARGE_GAP_SLOW - (CHARGE_GAP_SLOW - CHARGE_GAP_FAST) * p * p).toLong()
}

private const val CHARGE_LOW = 0.35f
private const val CHARGE_MID = 0.7f
private const val CHARGE_BASE = 0.3f
private const val CHARGE_GAP_SLOW = 260f
private const val CHARGE_GAP_FAST = 45f
private const val MIN_SCALE = 0.05f
private const val MERGE_BASE = 0.45f
private const val MERGE_STEP = 0.05f

@Composable
fun rememberHainHaptics(): HainHaptics {
    val view = LocalView.current
    val level = LocalHapticLevel.current
    return remember(view, level) { HainHaptics(view, level) }
}
