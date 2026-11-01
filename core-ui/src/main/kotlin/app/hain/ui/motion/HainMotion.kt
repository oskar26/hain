package app.hain.ui.motion

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Set by the app when the user enables reduce motion (SETTINGS.md `reduce_motion`)
 * or the system asks for it. Every motion helper in this package must collapse
 * to [snap] when this is true — no exceptions.
 */
val LocalReduceMotion = staticCompositionLocalOf { false }

/**
 * Motion constants, ported verbatim from UI_LIBRARIES.md §3 (source of truth).
 * Springs instead of linear tweens; nothing here is hand-tuned.
 */
object HainMotion {
    /** Send: bubble leaves composer with a lively spring. */
    const val SEND_DAMPING = 0.82f
    const val SEND_STIFFNESS = 380f

    /** Card appears: soft spring with a small stagger between children. */
    const val CARD_DAMPING = 0.85f
    const val CARD_STIFFNESS = Spring.StiffnessMediumLow

    /** Drawer open: content scales away with a calm spring. */
    const val DRAWER_DAMPING = 0.9f
    const val DRAWER_STIFFNESS = Spring.StiffnessMediumLow

    /** Tokens stream: new words fade + rise, max 30 animated at once. */
    const val TOKEN_FADE_MS = 160
    const val MAX_ANIMATED_WORDS = 30

    /** Tool chip slides in and morphs to a check. */
    const val TOOL_CHIP_MS = 220

    /** Send ↔ Stop icon morph (rotate + crossfade). */
    const val SEND_STOP_MORPH_MS = 180

    /** Scroll-to-bottom pill fades in and slides up 8 dp. */
    const val PILL_FADE_MS = 150

    /** "Pim denkt nach…" shimmer while the model warms up. */
    const val SHIMMER_MS = 1400

    /** Suggestion chips / card children enter staggered by this much. */
    const val STAGGER_MS = 40
}

/** The animated moments named in UI_LIBRARIES.md §3. */
enum class HainMotionMoment {
    SEND,
    CARD,
    DRAWER,
    TOKEN_FADE,
    TOOL_CHIP,
    SEND_STOP_MORPH,
    PILL_FADE,
}

/**
 * The spec for [moment], or [snap] when reduce motion is on. Pure so it is
 * unit-testable without composition.
 */
fun <T> motionSpec(
    moment: HainMotionMoment,
    reduceMotion: Boolean,
): FiniteAnimationSpec<T> {
    if (reduceMotion) return snap()
    return when (moment) {
        HainMotionMoment.SEND ->
            spring(dampingRatio = HainMotion.SEND_DAMPING, stiffness = HainMotion.SEND_STIFFNESS)
        HainMotionMoment.CARD ->
            spring(dampingRatio = HainMotion.CARD_DAMPING, stiffness = HainMotion.CARD_STIFFNESS)
        HainMotionMoment.DRAWER ->
            spring(dampingRatio = HainMotion.DRAWER_DAMPING, stiffness = HainMotion.DRAWER_STIFFNESS)
        HainMotionMoment.TOKEN_FADE -> tween(durationMillis = HainMotion.TOKEN_FADE_MS)
        HainMotionMoment.TOOL_CHIP -> tween(durationMillis = HainMotion.TOOL_CHIP_MS)
        HainMotionMoment.SEND_STOP_MORPH -> tween(durationMillis = HainMotion.SEND_STOP_MORPH_MS)
        HainMotionMoment.PILL_FADE -> tween(durationMillis = HainMotion.PILL_FADE_MS)
    }
}

/** Composition variant: reads [LocalReduceMotion] so callers never forget it. */
@Composable
fun <T> motionSpec(moment: HainMotionMoment): FiniteAnimationSpec<T> = motionSpec(moment, LocalReduceMotion.current)
