package app.hain.ui.pim

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.chargeGapMs
import app.hain.ui.motion.rememberHainHaptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * The big Pim of the start page (T-091, T-092): tap him for a reaction, drag him anywhere (he looks
 * surprised, the leaf swings, and on release he springs home relieved), or keep pressing without a
 * break (T-097): he slowly grows to up to 5×, trembles more and more, glows greener and the haptics
 * get denser and firmer until, after 5 s ([PimEgg.HOLD_MS]), he bursts into leaves with a thud and
 * [onEgg] opens the secret game. Letting go earlier springs him gently back. The whole Pim is scaled
 * as one layer, so the leaf always stays on his head.
 * With "reduce motion" only the tap and [onEgg] via a 5 s press are left, without movement.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod") // one gesture with three outcomes
@Composable
fun HeroPim(
    size: Dp,
    label: String,
    onEgg: () -> Unit,
    modifier: Modifier = Modifier,
    state: PimState = PimState.IDLE,
) {
    val reduce = LocalReduceMotion.current
    val scope = rememberCoroutineScope()
    val haptics = rememberHainHaptics()
    val driver = rememberPimDriver()
    val drag = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
    val charge = remember { Animatable(0f) }
    var mood by remember { mutableStateOf(HeroMood.CALM) }
    var moodSince by remember { mutableLongStateOf(0L) }
    var dragVelocity by remember { mutableStateOf(Offset.Zero) }
    var bursting by remember { mutableStateOf(false) }
    val burst = remember { Animatable(0f) }
    val pieces = remember { burstPieces(Random(seed = 26)) }

    fun startBurst() {
        haptics.burst()
        if (reduce) {
            onEgg()
            return
        }
        bursting = true
        scope.launch {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(BURST_MS, easing = LinearEasing))
            onEgg()
            delay(RETURN_DELAY_MS)
            charge.snapTo(0f)
            bursting = false
            mood = HeroMood.CALM
        }
    }

    val gestures =
        Modifier.pointerInput(reduce) {
            awaitEachGesture {
                val down = awaitFirstDown()
                val tracker = VelocityTracker()
                var moved = false
                var charging: Job? = null
                val pressedAt = System.currentTimeMillis()
                charging =
                    scope.launch {
                        if (reduce) {
                            // no growing and shaking: holding 5 s alone opens the game
                            delay(PimEgg.HOLD_MS)
                            startBurst()
                            return@launch
                        }
                        delay(PimEgg.ARM_MS)
                        mood = HeroMood.CHARGING
                        launch {
                            // short primitives, denser and firmer as he grows (never one long buzz)
                            while (charge.value < 1f && mood == HeroMood.CHARGING) {
                                haptics.charge(charge.value)
                                delay(chargeGapMs(charge.value))
                            }
                        }
                        charge.animateTo(1f, tween(PimEgg.CHARGE_MS, easing = LinearEasing))
                        // never earlier than 5 s after the finger went down
                        val held = System.currentTimeMillis() - pressedAt
                        if (!PimEgg.shouldBurst(held)) delay(PimEgg.HOLD_MS - held)
                        if (mood == HeroMood.CHARGING) startBurst()
                    }
                var change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                while (change != null && change.pressed) {
                    tracker.addPosition(change.uptimeMillis, change.position)
                    val far = (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                    val canDrag = !reduce && mood != HeroMood.CHARGING
                    if (!moved && far && canDrag) {
                        moved = true
                        charging.cancel()
                        mood = HeroMood.DRAGGED
                        moodSince = driver.now
                    }
                    if (moved) {
                        change.consume()
                        val v = tracker.calculateVelocity()
                        dragVelocity = Offset(v.x, v.y)
                        val delta = change.positionChange()
                        scope.launch { drag.snapTo(drag.value + delta) }
                    }
                    change = awaitPointerEvent().changes.firstOrNull { it.id == down.id }
                }
                charging.cancel()
                when {
                    bursting -> Unit
                    moved -> {
                        mood = HeroMood.RELIEVED
                        moodSince = driver.now
                        scope.launch {
                            drag.animateTo(
                                Offset.Zero,
                                spring(dampingRatio = HOME_DAMPING, stiffness = Spring.StiffnessLow),
                                initialVelocity = dragVelocity,
                            )
                            dragVelocity = Offset.Zero
                        }
                    }
                    mood == HeroMood.CHARGING -> {
                        // let go too early: he springs gently back to his normal size
                        mood = HeroMood.CALM
                        scope.launch {
                            charge.animateTo(0f, releaseSpring())
                        }
                    }
                    else -> {
                        haptics.tick()
                        driver.tap()
                    }
                }
            }
        }

    LaunchedEffect(mood) {
        if (mood == HeroMood.RELIEVED) {
            delay(RELIEF_MS)
            mood = HeroMood.CALM
        }
    }
    Box(
        modifier
            .size(size)
            .semantics {
                contentDescription = label
                onClick {
                    driver.tap()
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (!bursting) {
            LivePim(
                state = state,
                size = size,
                driver = driver,
                tappable = false,
                modifier =
                    Modifier
                        .offset { IntOffset(drag.value.x.roundToInt(), drag.value.y.roundToInt()) }
                        .graphicsLayer {
                            // one layer for body, face and leaf: the leaf can never come loose
                            val c = charge.value.coerceAtLeast(0f)
                            val tense = 1f + sin(driver.now * TENSE_SPEED) * TENSE * c
                            val s = PimEgg.scale(c)
                            scaleX = s / tense
                            scaleY = s * tense
                            val shake = PimEgg.shakeDp(c) * density
                            val t = driver.now.toFloat()
                            translationX = (sin(t * SHAKE_A) + SHAKE_MIX * sin(t * SHAKE_B)) * shake
                            translationY = sin(t * SHAKE_C) * shake * SHAKE_MIX
                            rotationZ = sin(t * SHAKE_B) * TREMBLE_DEG * c * c
                            transformOrigin = TransformOrigin(GROW_X, GROW_FROM)
                        }.then(gestures),
                overlay = { base ->
                    val velocity = if (mood == HeroMood.DRAGGED) dragVelocity else drag.velocity
                    heroPose(base, mood, charge.value, velocity, driver.now - moodSince)
                },
                colors = {
                    val c = charge.value.coerceIn(0f, 1f)
                    LightPimColors.copy(
                        body = lerp(LightPimColors.body, GLOW_BODY, c),
                        leaf = lerp(LightPimColors.leaf, GLOW_LEAF, c),
                    )
                },
            )
        }
        if (bursting) {
            // the burst starts as big as Pim was at the end, so leaves fly over the whole page
            Canvas(Modifier.requiredSize(size * BURST_SPREAD)) { drawBurst(pieces, burst.value) }
        }
    }
}

/** Soft, slightly bouncy way back to normal size when the finger lets go early. */
private fun releaseSpring() = spring<Float>(dampingRatio = RELEASE_DAMPING, stiffness = Spring.StiffnessMediumLow)

/** What the big Pim is going through right now. */
internal enum class HeroMood { CALM, DRAGGED, RELIEVED, CHARGING }

/** Pose on top of the director's: surprised while dragged, relieved after, squeezed while charging (pure). */
internal fun heroPose(
    base: PimPose,
    mood: HeroMood,
    charge: Float,
    velocity: Offset,
    sinceMs: Long,
): PimPose {
    val swing = (-velocity.x / LEAF_PER_VELOCITY).coerceIn(-MAX_SWING, MAX_SWING)
    val lean = (velocity.x / TILT_PER_VELOCITY).coerceIn(-MAX_LEAN, MAX_LEAN)
    return when (mood) {
        HeroMood.DRAGGED ->
            base.copy(
                eyes = PimEyes.WIDE,
                mouth = PimMouth.OPEN,
                mouthOpen = 0.7f,
                tilt = lean,
                leaf = base.leaf + swing,
                sy = base.sy + DRAG_STRETCH,
                sx = base.sx - DRAG_STRETCH,
            )
        HeroMood.RELIEVED -> {
            val fade = 1f - (sinceMs.toFloat() / RELIEF_MS).coerceIn(0f, 1f)
            base.copy(
                eyes = PimEyes.HAPPY,
                mouth = if (fade > 0.2f) PimMouth.SMILE else base.mouth,
                tilt = base.tilt + lean,
                leaf = base.leaf + swing,
                blush = RELIEF_BLUSH * fade,
            )
        }
        HeroMood.CHARGING ->
            base.copy(
                eyes = if (charge > SQUEEZE_FROM) PimEyes.SQUEEZE else PimEyes.WIDE,
                mouth = PimMouth.WOBBLY,
                blush = charge,
                leaf = base.leaf + CHARGE_LEAF * charge,
            )
        HeroMood.CALM -> base
    }
}

/** One leaf or confetti piece of the burst. */
internal data class BurstPiece(
    val angle: Float,
    val speed: Float,
    val spin: Float,
    val leaf: Boolean,
    val color: Color,
    val size: Float,
)

internal fun burstPieces(random: Random): List<BurstPiece> =
    List(BURST_PIECES) { i ->
        BurstPiece(
            angle = (i.toFloat() / BURST_PIECES) * 2f * PI.toFloat() + random.nextFloat() * ANGLE_JITTER,
            speed = SPEED_MIN + random.nextFloat() * SPEED_SPREAD,
            spin = (random.nextFloat() - 0.5f) * SPIN,
            leaf = i % 3 != 0,
            color = BURST_COLORS[i % BURST_COLORS.size],
            size = PIECE_MIN + random.nextFloat() * PIECE_SPREAD,
        )
    }

private fun DrawScope.drawBurst(
    pieces: List<BurstPiece>,
    p: Float,
) {
    val unit = size.minDimension
    val c = center
    val fade = (1f - p).coerceIn(0f, 1f)
    pieces.forEach { piece ->
        val travel = piece.speed * unit * (1f - (1f - p) * (1f - p))
        val pos =
            Offset(
                c.x + cos(piece.angle) * travel,
                c.y + sin(piece.angle) * travel + GRAVITY * unit * p * p,
            )
        val r = piece.size * unit
        val color = piece.color.copy(alpha = fade)
        if (piece.leaf) {
            rotate(piece.spin * p, pos) {
                drawOval(color, topLeft = pos - Offset(r, r / 2), size = Size(r * 2, r))
            }
        } else {
            drawCircle(color, radius = r / 2, center = pos)
        }
    }
}

/** Shared frame clock + director, so a parent can drive the same Pim (tap, drag, charge). */
class PimDriver internal constructor() {
    internal val director = PimDirector()
    internal val clock = mutableLongStateOf(0L)
    val now: Long get() = clock.longValue

    fun tap(): PimMove = director.tap(now)
}

@Composable
fun rememberPimDriver(): PimDriver = remember { PimDriver() }

/** Runs [driver]'s clock while composed (used by [LivePim]). */
internal suspend fun runClock(
    driver: PimDriver,
    state: PimState,
) {
    driver.director.setState(state, withFrameMillis { it })
    while (true) {
        withFrameMillis { now ->
            driver.director.tick(now)
            driver.clock.longValue = now
        }
    }
}

private val GLOW_BODY = Color(color = 0xFF2FC383)
private val GLOW_LEAF = Color(color = 0xFF0E8A57)
private val BURST_COLORS =
    listOf(
        Color(color = 0xFF16A36B),
        Color(color = 0xFF7FD6A4),
        Color(color = 0xFF3FAF77),
        Color(color = 0xFFDCF3E6),
        Color.White,
    )
private const val BURST_MS = 900
private const val BURST_SPREAD = 4
private const val RETURN_DELAY_MS = 600L
private const val GROW_X = 0.5f
private const val GROW_FROM = 0.5f
private const val RELEASE_DAMPING = 0.55f
private const val TENSE = 0.035f
private const val TENSE_SPEED = 0.012f
private const val SHAKE_A = 0.11f
private const val SHAKE_B = 0.23f
private const val SHAKE_C = 0.17f
private const val SHAKE_MIX = 0.45f
private const val TREMBLE_DEG = 2.5f
private const val HOME_DAMPING = 0.42f
private const val RELIEF_MS = 1100L
private const val RELIEF_BLUSH = 0.5f
private const val LEAF_PER_VELOCITY = 30f
private const val TILT_PER_VELOCITY = 90f
private const val MAX_SWING = 45f
private const val MAX_LEAN = 18f
private const val DRAG_STRETCH = 0.06f
private const val SQUEEZE_FROM = 0.55f
private const val CHARGE_LEAF = 20f
private const val BURST_PIECES = 36
private const val ANGLE_JITTER = 0.25f
private const val SPEED_MIN = 0.45f
private const val SPEED_SPREAD = 0.6f
private const val SPIN = 720f
private const val PIECE_MIN = 0.035f
private const val PIECE_SPREAD = 0.04f
private const val GRAVITY = 0.35f
