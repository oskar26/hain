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
 * surprised, the leaf swings, and on release he springs home relieved), or keep pressing: he grows,
 * shakes, glows greener and the haptics build up until, after about 2.5 s, he bursts into leaves and
 * confetti and [onEgg] opens the secret game. Letting go earlier calms him down again.
 * With "reduce motion" only the tap and [onEgg] via a long press are left, without movement.
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
        haptics.longPress()
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
                charging =
                    scope.launch {
                        delay(CHARGE_DELAY_MS)
                        if (reduce) {
                            // no growing and shaking: the long press alone opens the game
                            startBurst()
                            return@launch
                        }
                        mood = HeroMood.CHARGING
                        launch {
                            while (charge.value < 1f && mood == HeroMood.CHARGING) {
                                haptics.charge(charge.value)
                                val gap = CHARGE_TICK_SLOW - (CHARGE_TICK_SLOW - CHARGE_TICK_FAST) * charge.value
                                delay(gap.toLong())
                            }
                        }
                        charge.animateTo(1f, tween(CHARGE_MS, easing = LinearEasing))
                        if (mood == HeroMood.CHARGING) startBurst()
                    }
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    if (!change.pressed) break
                    tracker.addPosition(change.uptimeMillis, change.position)
                    val far = (change.position - down.position).getDistance() > viewConfiguration.touchSlop
                    if (!moved && far && mood != HeroMood.CHARGING && !reduce) {
                        moved = true
                        charging.cancel()
                        mood = HeroMood.DRAGGED
                        moodSince = driver.now
                    }
                    if (moved) {
                        change.consume()
                        val v = tracker.calculateVelocity()
                        dragVelocity = Offset(v.x, v.y)
                        scope.launch { drag.snapTo(drag.value + change.positionChange()) }
                    }
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
                        // let go too early: he calms down
                        mood = HeroMood.CALM
                        scope.launch { charge.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy)) }
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
                            val c = charge.value
                            val s = 1f + GROW * c
                            scaleX = s
                            scaleY = s
                            translationX = sin(driver.now * SHAKE_SPEED) * SHAKE * c * c * density
                            transformOrigin = TransformOrigin(0.5f, GROW_FROM)
                        }.then(gestures),
                overlay = { base ->
                    val velocity = if (mood == HeroMood.DRAGGED) dragVelocity else drag.velocity
                    heroPose(base, mood, charge.value, velocity, driver.now - moodSince)
                },
                colors = {
                    val c = charge.value
                    LightPimColors.copy(
                        body = lerp(LightPimColors.body, GLOW_BODY, c),
                        leaf = lerp(LightPimColors.leaf, GLOW_LEAF, c),
                    )
                },
            )
        }
        if (bursting) {
            Canvas(Modifier.size(size)) { drawBurst(pieces, burst.value) }
        }
    }
}

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
private const val CHARGE_DELAY_MS = 350L
private const val CHARGE_MS = 2150
private const val CHARGE_TICK_SLOW = 220f
private const val CHARGE_TICK_FAST = 55f
private const val BURST_MS = 850
private const val RETURN_DELAY_MS = 600L
private const val GROW = 0.55f
private const val GROW_FROM = 0.8f
private const val SHAKE = 3.5f
private const val SHAKE_SPEED = 0.09f
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
