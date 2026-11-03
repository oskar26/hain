package app.hain.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
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

    // ---- shared language for the whole app (T-010, docs/design/DESIGN_LANGUAGE.md) ----

    /** Small things appear or leave: icons, hints, the actions under a message. */
    const val FAST_MS = 150

    /** Menus open (scale + fade from their anchor). */
    const val MENU_MS = 180

    /** Pages and larger blocks cross-fade. */
    const val BASE_MS = 240

    /** Screen changes (start page → chat, settings): fade + slide. */
    const val PAGE_MS = 300

    /** The menu starts this small and grows to full size. */
    const val MENU_START_SCALE = 0.9f

    /** Calm spring for everything that moves position (drawer, list items). */
    const val CALM_DAMPING = 0.92f
    const val CALM_STIFFNESS = 500f
}

/** The one easing curve for tweens: quick start, soft landing. */
val HainEase: Easing = CubicBezierEasing(EASE_X1, 0f, 0f, 1f)

private const val EASE_X1 = 0.2f

/** A tween of [ms] on [HainEase], or [snap] when motion is reduced. */
fun <T> hainTween(
    ms: Int,
    reduceMotion: Boolean,
): FiniteAnimationSpec<T> = if (reduceMotion) snap() else tween(durationMillis = ms, easing = HainEase)

/** The calm spring, or [snap] when motion is reduced. */
fun <T> hainSpring(reduceMotion: Boolean): FiniteAnimationSpec<T> =
    if (reduceMotion) snap() else spring(dampingRatio = HainMotion.CALM_DAMPING, stiffness = HainMotion.CALM_STIFFNESS)

/** Composition variants that read [LocalReduceMotion]. */
@Composable
fun <T> hainTween(ms: Int): FiniteAnimationSpec<T> = hainTween(ms, LocalReduceMotion.current)

@Composable
fun <T> hainSpring(): FiniteAnimationSpec<T> = hainSpring(LocalReduceMotion.current)

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
