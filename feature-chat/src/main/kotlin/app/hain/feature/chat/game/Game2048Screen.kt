package app.hain.feature.chat.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hain.feature.chat.R
import app.hain.ui.HainIcons
import app.hain.ui.components.HainButton
import app.hain.ui.components.HainButtonStyle
import app.hain.ui.components.HainIconButton
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface
import kotlin.math.abs
import kotlin.random.Random

/**
 * The secret 2048 game behind Pim (T-098): swipe to slide, tiles glide and pop when they merge, calm
 * Lichtung greens from 2 (light) to 2048 (full Hain green). Score, best score ([best], saved by the
 * caller through [onScore]), new game, one undo, a won and a lost card with Pim, keep playing after 2048.
 */
@Suppress("LongMethod", "CyclomaticComplexMethod") // one screen: header, board, overlays
@Composable
fun Game2048Screen(
    best: Int,
    onScore: (Int) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    seed: Int? = null,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    val haptics = rememberHainHaptics()
    val random = remember(seed) { if (seed != null) Random(seed) else Random }
    var board by remember { mutableStateOf(Game2048.start(random)) }
    var undo by remember { mutableStateOf<Board?>(null) }
    var keepPlaying by remember { mutableStateOf(false) }
    var record by remember(best) { mutableStateOf(best) }
    val won = Game2048.hasWon(board) && !keepPlaying
    val lost = !won && !Game2048.canMove(board)

    BackHandler(onBack = onClose)

    fun swipe(dir: Direction) {
        if (won || lost) return
        val result = Game2048.play(board, dir, random)
        if (!result.moved) return
        undo = board
        board = result.board
        result.merges.maxOrNull()?.let { haptics.merge(it) }
        if (board.score > record) {
            record = board.score
            onScore(board.score)
        }
        when {
            Game2048.hasWon(board) && !keepPlaying -> haptics.done()
            !Game2048.canMove(board) -> haptics.error()
        }
    }

    fun restart() {
        haptics.tick()
        board = Game2048.start(random)
        undo = null
        keepPlaying = false
    }

    Box(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            // the page swallows taps so nothing behind it reacts
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = spacing.lg, vertical = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HainIconButton(HainIcons.close, stringResource(R.string.game_close), onClose, tint = colors.icon)
                Spacer(Modifier.weight(1f))
                BasicText("2048", style = type.title.copy(color = colors.text, fontWeight = FontWeight.SemiBold))
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(TOUCH.dp))
            }
            Spacer(Modifier.height(spacing.md))
            Row(
                Modifier.widthIn(max = BOARD_MAX.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                ScoreBox(stringResource(R.string.game_score), board.score, Modifier.weight(1f))
                ScoreBox(stringResource(R.string.game_best), record, Modifier.weight(1f))
            }
            Spacer(Modifier.height(spacing.md))
            BasicText(
                stringResource(R.string.game_hint),
                style = type.bodySmall.copy(color = colors.textMuted, textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(spacing.md))
            BoardView(board, ::swipe)
            Spacer(Modifier.height(spacing.lg))
            Row(horizontalArrangement = Arrangement.spacedBy(spacing.sm)) {
                HainButton(stringResource(R.string.game_new), ::restart, style = HainButtonStyle.SECONDARY)
                HainButton(
                    stringResource(R.string.game_undo),
                    {
                        undo?.let {
                            haptics.tick()
                            board = it
                            undo = null
                        }
                    },
                    style = HainButtonStyle.GHOST,
                    enabled = undo != null && !won,
                )
            }
        }
        EndCard(
            visible = won,
            pim = PimState.CELEBRATE,
            title = stringResource(R.string.game_won_title),
            body = stringResource(R.string.game_won_body),
            primary = stringResource(R.string.game_keep) to { keepPlaying = true },
            secondary = stringResource(R.string.game_new) to ::restart,
        )
        EndCard(
            visible = lost,
            pim = PimState.SLEEPING,
            title = stringResource(R.string.game_lost_title),
            body = stringResource(R.string.game_lost_body),
            primary = stringResource(R.string.game_try) to ::restart,
            secondary = stringResource(R.string.game_close) to onClose,
        )
    }
}

@Composable
private fun ScoreBox(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Column(
        modifier
            .hainSurface(HainLevel.FLAT, RoundedCornerShape(RADIUS.dp))
            .padding(vertical = LocalHainSpacing.current.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BasicText(label, style = type.label.copy(color = colors.textMuted))
        BasicText("$value", style = type.title.copy(color = colors.text, fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun BoardView(
    board: Board,
    onSwipe: (Direction) -> Unit,
) {
    val colors = LocalHainColors.current
    val threshold = with(LocalDensity.current) { SWIPE.dp.toPx() }
    val label = stringResource(R.string.game_board, board.score)
    BoxWithConstraints(Modifier.widthIn(max = BOARD_MAX.dp).fillMaxWidth()) {
        val side = maxWidth
        val gap = GAP.dp
        val cell = (side - gap * (Game2048.SIZE + 1)) / Game2048.SIZE
        Box(
            Modifier
                .size(side)
                .clip(RoundedCornerShape(RADIUS.dp))
                .background(colors.surface)
                .semantics { contentDescription = label }
                .pointerInput(Unit) {
                    var drag = Offset.Zero
                    detectDragGestures(
                        onDragStart = { drag = Offset.Zero },
                        onDragEnd = {
                            val (x, y) = drag
                            if (maxOf(abs(x), abs(y)) >= threshold) {
                                onSwipe(
                                    when {
                                        abs(x) > abs(y) && x > 0 -> Direction.RIGHT
                                        abs(x) > abs(y) -> Direction.LEFT
                                        y > 0 -> Direction.DOWN
                                        else -> Direction.UP
                                    },
                                )
                            }
                        },
                    ) { change, amount ->
                        change.consume()
                        drag += amount
                    }
                },
        ) {
            repeat(Game2048.SIZE * Game2048.SIZE) { i ->
                Box(
                    Modifier
                        .offset(
                            x = gap + (cell + gap) * (i % Game2048.SIZE),
                            y = gap + (cell + gap) * (i / Game2048.SIZE),
                        ).size(cell)
                        .clip(RoundedCornerShape(TILE_RADIUS.dp))
                        .background(colors.surfaceHigh),
                )
            }
            board.tiles.forEach { tile -> key(tile.id) { TileView(tile, cell, gap) } }
        }
    }
}

@Composable
private fun TileView(
    tile: Tile,
    cell: Dp,
    gap: Dp,
) {
    val reduce = LocalReduceMotion.current
    val density = LocalDensity.current
    val target =
        with(density) {
            IntOffset(
                (gap + (cell + gap) * tile.col).roundToPx(),
                (gap + (cell + gap) * tile.row).roundToPx(),
            )
        }
    val pos by animateIntOffsetAsState(target, if (reduce) tween(0) else tween(SLIDE_MS), label = "tile")
    val scale = remember { Animatable(if (tile.fresh && !reduce) 0f else 1f) }
    LaunchedEffect(tile.value) {
        if (reduce) return@LaunchedEffect
        if (tile.merged) {
            scale.snapTo(1f)
            scale.animateTo(POP, tween(POP_MS, delayMillis = SLIDE_MS / 2))
            scale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
            )
        } else if (tile.fresh) {
            scale.animateTo(1f, tween(GROW_MS, delayMillis = SLIDE_MS))
        }
    }
    val (fill, ink) = tileColors(tile.value)
    Box(
        Modifier
            .offset { pos }
            .size(cell)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }.clip(RoundedCornerShape(TILE_RADIUS.dp))
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        val digits = tile.value.toString().length
        BasicText(
            "${tile.value}",
            style =
                LocalHainTypography.current.title.copy(
                    color = ink,
                    fontWeight = FontWeight.Bold,
                    fontSize =
                        (
                            if (digits <= 2) {
                                BIG_SP
                            } else if (digits == THREE_DIGITS) {
                                MID_SP
                            } else {
                                SMALL_SP
                            }
                        ).sp,
                ),
        )
    }
}

@Suppress("LongParameterList")
@Composable
private fun EndCard(
    visible: Boolean,
    pim: PimState,
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    AnimatedVisibility(visible, enter = fadeIn() + scaleIn(initialScale = CARD_FROM), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(colors.scrim).clickable(remember { MutableInteractionSource() }, null) {},
            contentAlignment = Alignment.Center,
        ) {
            Column(
                Modifier
                    .padding(spacing.xl)
                    .widthIn(max = CARD_MAX.dp)
                    .hainSurface(HainLevel.FLOATING, RoundedCornerShape(RADIUS.dp))
                    .padding(spacing.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                LivePim(pim, CARD_PIM.dp, tappable = false)
                BasicText(title, style = type.title.copy(color = colors.text, textAlign = TextAlign.Center))
                BasicText(body, style = type.bodySmall.copy(color = colors.textMuted, textAlign = TextAlign.Center))
                Spacer(Modifier.height(spacing.xs))
                HainButton(primary.first, primary.second)
                HainButton(secondary.first, secondary.second, style = HainButtonStyle.GHOST)
            }
        }
    }
}

/** Tile fill and number colour: light mint for 2 up to full Hain green for 2048 (pure). */
internal fun tileColors(value: Int): Pair<Color, Color> {
    val index = TILE_FILLS.indices.firstOrNull { (2 shl it) >= value } ?: TILE_FILLS.lastIndex
    val fill = TILE_FILLS[index.coerceIn(0, TILE_FILLS.lastIndex)]
    return fill to if (index >= DARK_INK_UNTIL) Color.White else INK
}

private val INK = Color(color = 0xFF0F3D2A)
private val TILE_FILLS =
    listOf(
        Color(color = 0xFFEAF7F0), // 2
        Color(color = 0xFFD6F0E2), // 4
        Color(color = 0xFFBFE7D2), // 8
        Color(color = 0xFFA3DCBF), // 16
        Color(color = 0xFF84CFA9), // 32
        Color(color = 0xFF63C193), // 64
        Color(color = 0xFF45B47F), // 128
        Color(color = 0xFF2FA972), // 256
        Color(color = 0xFF1F9E68), // 512
        Color(color = 0xFF16935F), // 1024
        Color(color = 0xFF16A36B), // 2048
        Color(color = 0xFF0E7A4F), // beyond
    )
private const val DARK_INK_UNTIL = 5
private const val BOARD_MAX = 420
private const val GAP = 8
private const val RADIUS = 16
private const val TILE_RADIUS = 10
private const val SWIPE = 24
private const val SLIDE_MS = 110
private const val POP_MS = 90
private const val GROW_MS = 140
private const val POP = 1.14f
private const val THREE_DIGITS = 3
private const val BIG_SP = 30
private const val MID_SP = 26
private const val SMALL_SP = 20
private const val TOUCH = 48
private const val CARD_MAX = 320
private const val CARD_PIM = 72
private const val CARD_FROM = 0.92f
