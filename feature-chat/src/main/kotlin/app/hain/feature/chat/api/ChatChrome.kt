package app.hain.feature.chat.api

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hain.ui.effects.GlassEdge
import app.hain.ui.effects.ThinkingGlowState
import app.hain.ui.effects.chromeGlass
import app.hain.ui.effects.drawThinkingGlow
import app.hain.ui.effects.hainGlassSource
import app.hain.ui.effects.rememberThinkingGlow
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.isHainDark
import dev.chrisbanes.haze.HazeState

/**
 * T-230/T-232 (Oskar 04.11. 22:18): the chat's frame. The chat itself fills the whole screen and is
 * the glass source; the top bar ([top]) and the message box ([bottom]) float over it as frosted
 * glass, so messages scroll cleanly *under* them instead of being cut at a hard edge. [content] gets
 * the room the two take as padding (top bar + status bar, message box + keyboard/navigation bar).
 *
 * Behind the chat lies a calm "clearing" light ([ChatBackdrop]).
 */
@Suppress("LongParameterList") // three slots + the glass and the thinking flag
@Composable
internal fun ChatChrome(
    glass: HazeState,
    modifier: Modifier = Modifier,
    thinking: Boolean = false,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    // T-295: one glow for the whole screen, painted by the backdrop and again (same pixels) by the band
    // under the message box, so it reaches the bottom edge without a white strip
    val backdrop = rememberChatBackdrop(thinking)
    CompositionLocalProvider(LocalChatBackdrop provides backdrop) {
        ChromeLayout(glass, modifier.onGloballyPositioned { backdrop.place(it) }, top, bottom, content)
    }
}

@Composable
private fun ChromeLayout(
    glass: HazeState,
    modifier: Modifier,
    top: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit,
) {
    SubcomposeLayout(modifier.fillMaxSize()) { constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val wide = loose.copy(minWidth = constraints.maxWidth)
        val tops = subcompose(ChromeSlot.TOP, top).map { it.measure(wide) }
        val bottoms = subcompose(ChromeSlot.BOTTOM, bottom).map { it.measure(wide) }
        val topHeight = tops.maxOfOrNull { it.height } ?: 0
        val bottomHeight = bottoms.maxOfOrNull { it.height } ?: 0
        val padding = PaddingValues(top = topHeight.toDp(), bottom = bottomHeight.toDp())
        val body =
            subcompose(ChromeSlot.CONTENT) {
                Box(Modifier.fillMaxSize().hainGlassSource(glass)) {
                    ChatBackdrop()
                    content(padding)
                }
            }.map { it.measure(constraints) }
        val height = constraints.maxHeight
        layout(constraints.maxWidth, height) {
            body.forEach { it.place(0, 0) }
            tops.forEach { it.place(0, 0) }
            bottoms.forEach { it.place(0, height - it.height) }
        }
    }
}

private enum class ChromeSlot { TOP, BOTTOM, CONTENT }

/** T-272: where the page colour under the message box starts to fade in, and where it is solid. */
private const val BOTTOM_FADE_FROM = 0.35f
private const val BOTTOM_FADE_SOLID = 0.7f

/** The frosted top band: reaches under the status bar, a hairline where it meets the chat. */
@Composable
internal fun ChatTopGlass(
    glass: HazeState,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier.fillMaxWidth().chromeGlass(glass, edge = GlassEdge.BOTTOM).statusBarsPadding(),
        content = content,
    )
}

/**
 * The bottom of the chat: no band, only what floats there (the message box and the lines above it),
 * at most [CHAT_MAX_WIDTH] wide and in the middle on tablets; it rises with the keyboard.
 */
@Composable
internal fun ChatBottomArea(content: @Composable ColumnScope.() -> Unit) {
    val backdrop = LocalChatBackdrop.current
    val page = LocalHainColors.current.bg
    val top = remember { mutableFloatStateOf(0f) }
    Box(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned { top.floatValue = it.positionInRoot().y }
            // T-272 (Oskar 05.11.): the chat no longer shows through below the message box (navigation bar):
            // the page fades in from the middle of the box down to the screen edge. T-295: it is the same
            // page as behind the chat (colour, green light, thinking glow in screen coordinates), so the
            // glow runs on behind the box and the navigation bar instead of ending in a white strip.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawBehind {
                backdrop?.draw(this, top.floatValue) ?: drawRect(page)
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        BOTTOM_FADE_FROM to Color.Transparent,
                        BOTTOM_FADE_SOLID to Color.Black,
                        1f to Color.Black,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }.windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(Modifier.widthIn(max = CHAT_MAX_WIDTH).fillMaxWidth(), content = content)
    }
}

/**
 * The lines above the message box (error, waiting messages, editing, voice, files) on one frosted
 * card, so they stay readable over the chat; nothing at all when none of them shows.
 */
@Composable
internal fun ChatBottomCard(
    glass: HazeState,
    content: @Composable ColumnScope.() -> Unit,
) {
    var filled by remember { mutableStateOf(false) }
    val spacing = LocalHainSpacing.current
    val card =
        if (filled) {
            Modifier
                .padding(start = spacing.md, end = spacing.md, top = spacing.sm)
                .chromeGlass(glass, LocalHainShapes.current.card, GlassEdge.ALL)
        } else {
            Modifier
        }
    Box(Modifier.fillMaxWidth().then(card)) {
        Column(Modifier.fillMaxWidth().onSizeChanged { filled = it.height > 0 }, content = content)
    }
}

/**
 * T-232: the chat is no longer plain white or black: page colour with a very faint "clearing" light
 * of Hain green rising from the bottom (light and dark), under it Pim's thinking glow while he works.
 * Static, so it costs nothing and stays with "reduce motion".
 */
@Composable
internal fun ChatBackdrop() {
    val backdrop = LocalChatBackdrop.current
    val page = LocalHainColors.current.bg
    Box(Modifier.fillMaxSize().drawBehind { backdrop?.draw(this, rootTop = null) ?: drawRect(page) })
}

/**
 * T-295: the chat's page (colour, faint green light, thinking glow) in screen coordinates, so every layer
 * that paints a part of it (behind the chat, under the message box) matches the others exactly.
 */
@Stable
internal class ChatBackdropPaint(
    private val glow: ThinkingGlowState,
    private val bg: Color,
    private val wash: Color,
) {
    private val height = mutableFloatStateOf(0f)
    private val origin = mutableFloatStateOf(0f)

    fun place(coordinates: LayoutCoordinates) {
        height.floatValue = coordinates.size.height.toFloat()
        origin.floatValue = coordinates.positionInRoot().y
    }

    /**
     * Paints the part of the page that falls into [scope], whose top is at [rootTop] on the screen
     * (`null` = the layer is the whole chat).
     */
    fun draw(
        scope: DrawScope,
        rootTop: Float?,
    ) = with(scope) {
        val full = height.floatValue.takeIf { it > 0f } ?: size.height
        val top = rootTop?.let { (it - origin.floatValue).coerceAtLeast(0f) } ?: 0f
        drawRect(bg)
        clipRect(0f, 0f, size.width, size.height) {
            translate(top = -top) {
                drawRect(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        WASH_FROM to Color.Transparent,
                        1f to wash,
                        endY = full,
                    ),
                    size = Size(size.width, full),
                )
            }
        }
        drawThinkingGlow(glow, full, top)
    }
}

/** The page of the [ChatChrome] around; `null` outside one (previews, tests): then only the page colour. */
internal val LocalChatBackdrop = staticCompositionLocalOf<ChatBackdropPaint?> { null }

@Composable
private fun rememberChatBackdrop(thinking: Boolean): ChatBackdropPaint {
    val colors = LocalHainColors.current
    val wash = colors.accent.copy(alpha = if (isHainDark()) WASH_DARK else WASH_LIGHT)
    val glow = rememberThinkingGlow(thinking)
    return remember(glow, colors.bg, wash) { ChatBackdropPaint(glow, colors.bg, wash) }
}

/** T-234: the chat text and the message box are at most this wide on tablets, in the middle. */
internal val CHAT_MAX_WIDTH: Dp = 720.dp

/** The calm green wash behind the chat (lower part of the page). */
private const val WASH_FROM = 0.4f
private const val WASH_LIGHT = 0.08f
private const val WASH_DARK = 0.1f
