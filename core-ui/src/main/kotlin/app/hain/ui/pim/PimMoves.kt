package app.hain.ui.pim

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pim's little animations (T-089, docs/design/PIM_MOTION.md). Each move is a pure function of its
 * progress t (0–1), so every frame can be tested. All moves only change the rig (position, squash,
 * tilt, eyes, mouth, leaf); the leaf always hangs on the head ([PimRig]).
 */
enum class PimMove(
    val durationMs: Int,
    /** Loops while its state lasts (thinking, sleeping, celebrating, downloading). */
    val loops: Boolean = false,
) {
    // idle, chosen at random
    BLINK(BLINK_MS),
    LOOK_AROUND(LOOK_MS),
    WAVE(WAVE_MS),
    HOP(HOP_MS),
    NOD(NOD_MS),
    YAWN(YAWN_MS),
    HEART(HEART_MS),
    DOZE(DOZE_MS),

    // states
    THINK(THINK_MS, loops = true),
    SLEEP(SLEEP_MS, loops = true),
    CELEBRATE(CELEBRATE_MS, loops = true),
    DOWNLOAD(DOWNLOAD_MS, loops = true),
    JOY(JOY_MS),
    STARTLE(STARTLE_MS),

    // taps: a different one each time
    SQUISH(SQUISH_MS),
    GIGGLE(GIGGLE_MS),
    BOING(BOING_MS),
    TWIRL(TWIRL_MS),
    WINK(WINK_MS),
}

object PimMoves {
    /** Idle moves Pim picks from at random (blinks and glances a little more often). */
    val IDLE_POOL =
        listOf(
            PimMove.BLINK,
            PimMove.BLINK,
            PimMove.LOOK_AROUND,
            PimMove.LOOK_AROUND,
            PimMove.WAVE,
            PimMove.HOP,
            PimMove.NOD,
            PimMove.YAWN,
            PimMove.HEART,
        )

    /** What a tap does; never the same twice in a row. */
    val TAP_REACTIONS =
        listOf(PimMove.SQUISH, PimMove.GIGGLE, PimMove.BOING, PimMove.TWIRL, PimMove.WINK, PimMove.HEART)

    /** Every move, for the frame tests. */
    val ALL = PimMove.entries

    /** The move a state plays on and on, if any. */
    fun loopFor(state: PimState): PimMove? =
        when (state) {
            PimState.THINKING -> PimMove.THINK
            PimState.SLEEPING -> PimMove.SLEEP
            PimState.CELEBRATE -> PimMove.CELEBRATE
            PimState.DOWNLOADING -> PimMove.DOWNLOAD
            else -> null
        }

    /** The move a state starts with: joy when an answer is done, a fright on an error. */
    fun entryFor(state: PimState): PimMove? =
        when (state) {
            PimState.HAPPY -> PimMove.JOY
            PimState.ERROR -> PimMove.STARTLE
            else -> loopFor(state)
        }

    /** States in which Pim plays his idle moves. */
    fun idles(state: PimState): Boolean = state in IDLE_STATES

    private val IDLE_STATES = setOf(PimState.IDLE, PimState.HAPPY, PimState.LISTENING)

    /** The calm pose of a state (Lichtung: no mouth, just the eyes). */
    fun basePose(state: PimState): PimPose =
        when (state) {
            PimState.HAPPY -> PimPose(eyes = PimEyes.HAPPY)
            PimState.ERROR -> PimPose(eyes = PimEyes.WIDE, mouth = PimMouth.WOBBLY, fx = PimFx.DROP)
            PimState.CELEBRATE -> PimPose(eyes = PimEyes.HAPPY, mouth = PimMouth.SMILE)
            else -> poseFor(state)
        }

    /** Pose of [move] at progress [t] (0–1) on top of [base]. */
    @Suppress("CyclomaticComplexMethod", "LongMethod") // one small formula per move
    fun pose(
        move: PimMove,
        base: PimPose,
        t: Float,
    ): PimPose {
        val k = bump(t)
        return when (move) {
            PimMove.BLINK -> {
                val closed = base.eyes == PimEyes.PILL && k > BLINK_SHUT
                base.copy(eyes = if (closed) PimEyes.CLOSED else base.eyes, eyeH = max(MIN_EYE, 1f - k))
            }
            PimMove.LOOK_AROUND -> {
                val dx = lookX(t)
                base.copy(eyeDX = dx, tilt = dx * LOOK_TILT, leaf = base.leaf - dx * LOOK_LEAF)
            }
            PimMove.WAVE -> {
                val wave = sin(t * WAVE_TURNS * TWO_PI)
                base.copy(
                    leaf = base.leaf + WAVE_LEAF * wave * k,
                    bend = WAVE_BEND * sin(t * WAVE_TURNS * TWO_PI + WAVE_LAG) * k,
                    tilt = WAVE_TILT * k,
                    eyes = if (k > FACE_ON) PimEyes.HAPPY else base.eyes,
                    mouth = if (k > FACE_ON) PimMouth.SMILE else base.mouth,
                )
            }
            PimMove.HOP -> hop(base, t, HOP_HEIGHT)
            PimMove.NOD -> {
                val n = max(0f, sin(t * 2f * TWO_PI))
                base.copy(
                    eyeDY = NOD_EYES * n,
                    sy = 1f - NOD_SQUASH * n,
                    bend = NOD_BEND * n,
                    leaf = base.leaf + NOD_LEAF * n,
                )
            }
            PimMove.YAWN -> {
                val y = plateau(t)
                base.copy(
                    mouth = if (y > MOUTH_ON) PimMouth.YAWN else base.mouth,
                    mouthOpen = y,
                    eyes = if (y > SQUEEZE_ON) PimEyes.SQUEEZE else base.eyes,
                    sx = 1f - YAWN_SQUASH * y,
                    sy = 1f + YAWN_STRETCH * y,
                    leaf = base.leaf - YAWN_DROOP * y,
                    tilt = -YAWN_TILT * y,
                )
            }
            PimMove.HEART ->
                base.copy(
                    eyes = PimEyes.HAPPY,
                    blush = k,
                    fx = PimFx.HEART,
                    fxY = -HEART_RISE * t,
                    fxA = k,
                    sx = 1f + HEART_PULSE * sin(t * 2f * TWO_PI) * k,
                    sy = 1f - HEART_PULSE * sin(t * 2f * TWO_PI) * k,
                )
            PimMove.DOZE -> sleep(base, (t * 2f) % 1f, fade = k)
            PimMove.SLEEP -> sleep(base, t, fade = 1f)
            PimMove.THINK ->
                base.copy(
                    eyeDX = THINK_EYE_X,
                    eyeDY = THINK_EYE_Y,
                    leaf = base.leaf + THINK_LEAF * sin(t * 2f * TWO_PI),
                    bend = THINK_BEND * sin(t * TWO_PI),
                    tilt = THINK_TILT * sin(t * TWO_PI),
                )
            PimMove.CELEBRATE -> {
                val h = abs(sin(t * PI.toFloat()))
                val landing = (1f - h) * (1f - h) * LANDING_SQUASH
                base.copy(
                    y = -CELEBRATE_HEIGHT * h,
                    sx = 1f + landing,
                    sy = 1f - landing + h * HOP_STRETCH,
                    tilt = CELEBRATE_TILT * sin(t * TWO_PI),
                    leaf = base.leaf + LEAF_FLICK * sin(t * TWO_PI + WAVE_LAG),
                    eyes = PimEyes.HAPPY,
                    fx = PimFx.SPARKLE,
                )
            }
            PimMove.DOWNLOAD ->
                base.copy(
                    y = -DOWNLOAD_BOB * bump(t),
                    eyeDY = DOWNLOAD_EYES,
                    leaf = base.leaf + DOWNLOAD_LEAF * sin(t * TWO_PI),
                )
            PimMove.JOY ->
                hop(base, t, JOY_HEIGHT).copy(
                    eyes = PimEyes.HAPPY,
                    mouth = PimMouth.SMILE,
                    fx = PimFx.SPARKLE,
                    fxA = k,
                )
            PimMove.STARTLE -> {
                val jump = if (t < STARTLE_UP) bump(t / STARTLE_UP / 2f) else damped(t - STARTLE_UP, STARTLE_FREQ)
                base.copy(
                    y = -STARTLE_HEIGHT * max(0f, jump),
                    sx = 1f - STARTLE_STRETCH * jump,
                    sy = 1f + STARTLE_STRETCH * jump,
                    eyes = PimEyes.WIDE,
                    mouth = PimMouth.OPEN,
                    mouthOpen = 1f - t * HALF,
                    fx = PimFx.BANG,
                    fxA = 1f - t * HALF,
                    leaf = base.leaf - STARTLE_LEAF * (1f - t),
                )
            }
            PimMove.SQUISH -> {
                // squash & stretch that settles, then a giggle shiver
                val d = exp(-SQUISH_DECAY * t) * kotlin.math.cos(t * SQUISH_FREQ * TWO_PI)
                base.copy(
                    sx = 1f + SQUISH_AMOUNT * d,
                    sy = 1f - SQUISH_AMOUNT * d,
                    x = if (t > GIGGLE_FROM) GIGGLE_SHAKE * sin(t * GIGGLE_SHAKE_FREQ * TWO_PI) * (1f - t) else 0f,
                    eyes = if (t < GIGGLE_FROM) PimEyes.SQUEEZE else PimEyes.HAPPY,
                    mouth = PimMouth.SMILE,
                    leaf = base.leaf + LEAF_FLICK * d,
                )
            }
            PimMove.GIGGLE ->
                base.copy(
                    tilt = GIGGLE_TILT * sin(t * GIGGLE_FREQ * TWO_PI) * exp(-GIGGLE_DECAY * t),
                    y = -GIGGLE_BOUNCE * abs(sin(t * GIGGLE_FREQ * TWO_PI)) * (1f - t),
                    eyes = PimEyes.HAPPY,
                    mouth = PimMouth.OPEN,
                    mouthOpen = HALF + HALF * abs(sin(t * GIGGLE_MOUTH * TWO_PI)),
                    blush = k,
                )
            PimMove.BOING -> {
                val stretch = if (t < BOING_UP) bump(t / BOING_UP / 2f) else 0f
                val air = if (t >= BOING_UP) sin((t - BOING_UP) / (1f - BOING_UP) * PI.toFloat()) else 0f
                base.copy(
                    sx = 1f - BOING_STRETCH * stretch,
                    sy = 1f + BOING_STRETCH * stretch,
                    y = -BOING_HEIGHT * air,
                    eyes = if (t < BOING_UP) PimEyes.WIDE else PimEyes.HAPPY,
                    leaf = base.leaf + LEAF_FLICK * air,
                )
            }
            PimMove.TWIRL ->
                base.copy(
                    leaf = base.leaf + FULL_TURN * ease(t),
                    eyeDY = -TWIRL_EYES * k,
                    eyeDX = TWIRL_EYES * sin(t * TWO_PI),
                    mouth = PimMouth.OPEN,
                    mouthOpen = TWIRL_MOUTH,
                )
            PimMove.WINK ->
                base.copy(
                    wink = k,
                    mouth = PimMouth.SMILE,
                    tilt = WINK_TILT * k,
                    blush = WINK_BLUSH * k,
                )
        }
    }

    /** Pim breathes a little all the time (also during moves); [timeMs] runs freely. */
    fun breathe(
        pose: PimPose,
        timeMs: Long,
    ): PimPose {
        val phase = (timeMs % BREATHE_MS).toFloat() / BREATHE_MS * TWO_PI
        val b = sin(phase) * BREATHE
        return pose.copy(sx = pose.sx + b, sy = pose.sy - b, leaf = pose.leaf + sin(phase + WAVE_LAG) * BREATHE_LEAF)
    }

    private fun hop(
        base: PimPose,
        t: Float,
        height: Float,
    ): PimPose {
        val squashIn = if (t < HOP_PREP) bump(t / HOP_PREP) else 0f
        val air = if (t in HOP_PREP..HOP_LAND) sin((t - HOP_PREP) / (HOP_LAND - HOP_PREP) * PI.toFloat()) else 0f
        val land = if (t > HOP_LAND) bump((t - HOP_LAND) / (1f - HOP_LAND)) else 0f
        val squash = (squashIn + land) * HOP_SQUASH
        return base.copy(
            y = -height * air,
            sx = 1f + squash - air * HOP_STRETCH * HALF,
            sy = 1f - squash + air * HOP_STRETCH,
            leaf = base.leaf - LEAF_FLICK * air + LEAF_FLICK * HALF * land,
        )
    }

    private fun sleep(
        base: PimPose,
        t: Float,
        fade: Float,
    ): PimPose =
        base.copy(
            eyes = PimEyes.CLOSED,
            sy = 1f + SLEEP_BREATHE * sin(t * TWO_PI),
            sx = 1f - SLEEP_BREATHE * sin(t * TWO_PI),
            leaf = base.leaf - SLEEP_DROOP * fade + SLEEP_SWAY * sin(t * TWO_PI),
            fx = PimFx.ZZZ,
            fxX = ZZ_DRIFT * t,
            fxY = -ZZ_RISE * t,
            fxA = bump(t) * fade,
        )

    /** Glance left, hold, glance right, hold, back. */
    private fun lookX(t: Float): Float =
        when {
            t < LOOK_1 -> -LOOK_X * ease(t / LOOK_1)
            t < LOOK_2 -> -LOOK_X
            t < LOOK_3 -> -LOOK_X + 2f * LOOK_X * ease((t - LOOK_2) / (LOOK_3 - LOOK_2))
            t < LOOK_4 -> LOOK_X
            else -> LOOK_X * (1f - ease((t - LOOK_4) / (1f - LOOK_4)))
        }
}

/**
 * Who plays what and when (T-089): the state's own move, random idle moves every few seconds, a
 * short doze with "Zz" after a long wait, and a different tap reaction each time. Pure: the
 * composable feeds the frame time.
 */
class PimDirector(
    private val random: Random = Random.Default,
) {
    var state: PimState = PimState.IDLE
        private set
    var move: PimMove? = null
        private set
    private var startedAt = 0L
    private var nextIdleAt = 0L
    private var lastActive = 0L
    private var lastIdle: PimMove? = null
    private var lastTap: PimMove? = null

    fun setState(
        next: PimState,
        now: Long,
    ) {
        if (next == state && startedAt != 0L) return
        state = next
        lastActive = now
        play(PimMoves.entryFor(next), now)
    }

    /** A tap: returns the reaction, never the same as last time. */
    fun tap(now: Long): PimMove {
        val choices = PimMoves.TAP_REACTIONS.filter { it != lastTap }
        val pick = choices[random.nextInt(choices.size)]
        lastTap = pick
        lastActive = now
        play(pick, now)
        return pick
    }

    /** Called every frame. */
    fun tick(now: Long) {
        val m = move
        if (m != null && !m.loops && now - startedAt >= m.durationMs) play(PimMoves.loopFor(state), now)
        if (move == null && PimMoves.idles(state) && now >= nextIdleAt) {
            val pick = if (now - lastActive >= DOZE_AFTER_MS) PimMove.DOZE else nextIdle()
            if (pick == PimMove.DOZE) lastActive = now
            play(pick, now)
        }
    }

    fun pose(now: Long): PimPose {
        val base = PimMoves.basePose(state)
        val m = move ?: return PimMoves.breathe(base, now)
        val elapsed = (now - startedAt).coerceAtLeast(0L)
        val t =
            if (m.loops) {
                (elapsed % m.durationMs).toFloat() / m.durationMs
            } else {
                (elapsed.toFloat() / m.durationMs).coerceIn(0f, 1f)
            }
        return PimMoves.breathe(PimMoves.pose(m, base, t), now)
    }

    private fun play(
        next: PimMove?,
        now: Long,
    ) {
        move = next
        startedAt = now
        if (next == null) nextIdleAt = now + IDLE_PAUSE_MIN + random.nextLong(IDLE_PAUSE_SPREAD)
    }

    private fun nextIdle(): PimMove {
        val choices = PimMoves.IDLE_POOL.filter { it != lastIdle }
        val pick = choices[random.nextInt(choices.size)]
        lastIdle = pick
        return pick
    }

    companion object {
        const val IDLE_PAUSE_MIN = 2_400L
        const val IDLE_PAUSE_SPREAD = 3_600L
        const val DOZE_AFTER_MS = 40_000L
    }
}

/** 0 → 1 → 0 like a sine half wave. */
internal fun bump(t: Float): Float = sin(t.coerceIn(0f, 1f) * PI.toFloat())

/** Smooth ease in-out. */
internal fun ease(t: Float): Float {
    val x = t.coerceIn(0f, 1f)
    return x * x * (3f - 2f * x)
}

/** Rises fast, stays, falls (yawn). */
private fun plateau(t: Float): Float =
    when {
        t < PLATEAU_IN -> ease(t / PLATEAU_IN)
        t > PLATEAU_OUT -> 1f - ease((t - PLATEAU_OUT) / (1f - PLATEAU_OUT))
        else -> 1f
    }

/** A spring settling from 1 to 0. */
private fun damped(
    t: Float,
    freq: Float,
): Float = exp(-DAMP * t) * kotlin.math.cos(t * freq * TWO_PI)

private const val TWO_PI = (2.0 * PI).toFloat()
private const val HALF = 0.5f
private const val FULL_TURN = 360f
private const val BLINK_MS = 220
private const val LOOK_MS = 2400
private const val WAVE_MS = 1600
private const val HOP_MS = 900
private const val NOD_MS = 1000
private const val YAWN_MS = 2200
private const val HEART_MS = 1800
private const val DOZE_MS = 6400
private const val THINK_MS = 1800
private const val SLEEP_MS = 3200
private const val CELEBRATE_MS = 900
private const val DOWNLOAD_MS = 1400
private const val JOY_MS = 1200
private const val STARTLE_MS = 900
private const val SQUISH_MS = 750
private const val GIGGLE_MS = 900
private const val BOING_MS = 850
private const val TWIRL_MS = 1000
private const val WINK_MS = 700
private const val BLINK_SHUT = 0.8f
private const val MIN_EYE = 0.12f
private const val LOOK_X = 4f
private const val LOOK_TILT = 0.6f
private const val LOOK_LEAF = 1.5f
private const val LOOK_1 = 0.15f
private const val LOOK_2 = 0.42f
private const val LOOK_3 = 0.58f
private const val LOOK_4 = 0.85f
private const val WAVE_TURNS = 3f
private const val WAVE_LEAF = 28f
private const val WAVE_BEND = 8f
private const val WAVE_TILT = 4f
private const val WAVE_LAG = 0.8f
private const val FACE_ON = 0.3f
private const val HOP_HEIGHT = 14f
private const val HOP_PREP = 0.2f
private const val HOP_LAND = 0.8f
private const val HOP_SQUASH = 0.1f
private const val HOP_STRETCH = 0.06f
private const val LEAF_FLICK = 16f
private const val NOD_EYES = 3f
private const val NOD_SQUASH = 0.03f
private const val NOD_BEND = 6f
private const val NOD_LEAF = 8f
private const val MOUTH_ON = 0.05f
private const val SQUEEZE_ON = 0.4f
private const val YAWN_SQUASH = 0.03f
private const val YAWN_STRETCH = 0.05f
private const val YAWN_DROOP = 10f
private const val YAWN_TILT = 3f
private const val HEART_RISE = 10f
private const val HEART_PULSE = 0.04f
private const val THINK_EYE_X = 2.5f
private const val THINK_EYE_Y = -3f
private const val THINK_LEAF = 14f
private const val THINK_BEND = 4f
private const val THINK_TILT = 3f
private const val CELEBRATE_HEIGHT = 16f
private const val CELEBRATE_TILT = 4f
private const val LANDING_SQUASH = 0.12f
private const val DOWNLOAD_BOB = 2f
private const val DOWNLOAD_EYES = 2f
private const val DOWNLOAD_LEAF = 6f
private const val JOY_HEIGHT = 16f
private const val STARTLE_UP = 0.25f
private const val STARTLE_FREQ = 1.5f
private const val STARTLE_HEIGHT = 6f
private const val STARTLE_STRETCH = 0.12f
private const val STARTLE_LEAF = 25f
private const val SQUISH_DECAY = 4f
private const val SQUISH_FREQ = 2.2f
private const val SQUISH_AMOUNT = 0.18f
private const val GIGGLE_FROM = 0.3f
private const val GIGGLE_SHAKE = 1.2f
private const val GIGGLE_SHAKE_FREQ = 9f
private const val GIGGLE_TILT = 6f
private const val GIGGLE_FREQ = 4f
private const val GIGGLE_DECAY = 2.5f
private const val GIGGLE_BOUNCE = 2f
private const val GIGGLE_MOUTH = 6f
private const val BOING_UP = 0.3f
private const val BOING_STRETCH = 0.16f
private const val BOING_HEIGHT = 10f
private const val TWIRL_EYES = 3f
private const val TWIRL_MOUTH = 0.4f
private const val WINK_TILT = 5f
private const val WINK_BLUSH = 0.4f
private const val BREATHE_MS = 3200L
private const val BREATHE = 0.018f
private const val BREATHE_LEAF = 3f
private const val SLEEP_BREATHE = 0.03f
private const val SLEEP_DROOP = 12f
private const val SLEEP_SWAY = 3f
private const val ZZ_DRIFT = 3f
private const val ZZ_RISE = 6f
private const val PLATEAU_IN = 0.25f
private const val PLATEAU_OUT = 0.7f
private const val DAMP = 5f
