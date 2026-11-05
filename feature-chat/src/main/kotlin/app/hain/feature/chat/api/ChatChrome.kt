package app.hain.feature.chat.api

import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hain.ui.effects.GlassEdge
import app.hain.ui.effects.ThinkingGlow
import app.hain.ui.effects.chromeGlass
import app.hain.ui.effects.hainGlassSource
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
                    ChatBackdrop(thinking)
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
    val page = LocalHainColors.current.bg
    Box(
        Modifier
            .fillMaxWidth()
            // T-272 (Oskar 05.11.): the chat no longer shows through below the message box (navigation bar):
            // the page colour fades in from the middle of the box down to the screen edge
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        BOTTOM_FADE_FROM to Color.Transparent,
                        BOTTOM_FADE_SOLID to page,
                        1f to page,
                    ),
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
internal fun ChatBackdrop(thinking: Boolean) {
    val colors = LocalHainColors.current
    val wash = colors.accent.copy(alpha = if (isHainDark()) WASH_DARK else WASH_LIGHT)
    Box(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .drawBehind {
                drawRect(Brush.verticalGradient(0f to Color.Transparent, WASH_FROM to Color.Transparent, 1f to wash))
            },
    )
    ThinkingGlow(visible = thinking, modifier = Modifier.fillMaxSize())
}

/**
 * T-234 (Android large-screen guidelines, iPad readable width): the chat text and the message box
 * are at most this wide and sit in the middle on tablets and in landscape.
 */
internal val CHAT_MAX_WIDTH: Dp = 720.dp

private const val WASH_FROM = 0.4f
private const val WASH_LIGHT = 0.08f
private const val WASH_DARK = 0.1f
