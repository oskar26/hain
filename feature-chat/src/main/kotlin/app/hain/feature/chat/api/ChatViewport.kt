package app.hain.feature.chat.api

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.internal.UiMessage

/**
 * When the chat follows new text (T-058). Pure rules, tested on the JVM.
 *
 * - You are "at the bottom" while the end of the list is at most [BOTTOM_SLACK_DP] away.
 * - Only your own scrolling changes whether the chat follows: scroll up to read and new words no
 *   longer pull you down; scroll back to the bottom and it follows again.
 * - Anything new while you read further up (an answer starts, more text comes) shows the pill
 *   with "Neue Antwort".
 */
internal object ViewportRules {
    const val BOTTOM_SLACK_DP = 48

    /** Pixels between the end of the list and the bottom edge; `null` = the last item is not on screen. */
    fun atBottom(
        distancePx: Int?,
        slackPx: Float,
    ): Boolean = distancePx != null && distancePx <= slackPx

    /** New state of "follow" after a frame: only a scroll by the user ([userScrolling]) changes it. */
    fun follow(
        current: Boolean,
        userScrolling: Boolean,
        atBottom: Boolean,
    ): Boolean = if (userScrolling) atBottom else current

    /** Whether the pill should say "Neue Antwort": something grew while you were not following. */
    fun unseen(
        current: Boolean,
        following: Boolean,
        grew: Boolean,
    ): Boolean =
        when {
            following -> false
            grew -> true
            else -> current
        }
}

/** Scroll state of the chat (T-058): the list, whether it follows, whether there is unseen text. */
@Stable
internal class ChatViewportState(
    val list: LazyListState,
) {
    var follow by mutableStateOf(true)
        internal set
    var unseen by mutableStateOf(false)
        internal set

    /** `true` while the chat scrolls by itself; such scrolls never change [follow]. */
    internal var auto by mutableStateOf(false)
        private set

    /** Scrolls by the app (follow, jump) run through here, so they are not taken for the user's. */
    internal suspend fun byApp(block: suspend LazyListState.() -> Unit) {
        auto = true
        try {
            list.block()
        } finally {
            auto = false
        }
    }

    /** The pill shows while you read further up and there is more below. */
    val showPill: Boolean get() = !follow && list.canScrollForward

    /** Back to the newest message and follow again ([animate] = soft scroll, for the pill). */
    suspend fun jumpToEnd(
        lastIndex: Int,
        animate: Boolean,
    ) {
        follow = true
        unseen = false
        if (lastIndex < 0) return
        byApp { if (animate) animateScrollToItem(lastIndex, END_OFFSET) else scrollToItem(lastIndex, END_OFFSET) }
    }
}

@Composable
internal fun rememberChatViewportState(): ChatViewportState {
    val list = rememberLazyListState()
    return remember(list) { ChatViewportState(list) }
}

/**
 * Wires [ChatViewportState] to the chat: follows streaming text only while you are at the bottom,
 * jumps to the end when you open a chat or send ([sendTick]), and marks new text you have not seen.
 */
@Composable
internal fun ChatViewportEffects(
    state: ChatViewportState,
    chatId: Long?,
    messages: List<UiMessage>,
    sendTick: Int,
) {
    val slack = with(LocalDensity.current) { ViewportRules.BOTTOM_SLACK_DP.dp.toPx() }
    val list = state.list
    LaunchedEffect(list, slack) {
        snapshotFlow { (list.isScrollInProgress && !state.auto) to ViewportRules.atBottom(distanceToEnd(list), slack) }
            .collect { (moving, bottom) ->
                state.follow = ViewportRules.follow(state.follow, moving, bottom)
                if (state.follow) state.unseen = false
            }
    }
    // a new chat opens at its end
    LaunchedEffect(chatId) { state.jumpToEnd(messages.lastIndex, animate = false) }
    // your own message always brings you down
    LaunchedEffect(sendTick) { if (sendTick > 0) state.jumpToEnd(messages.lastIndex, animate = false) }
    val last = messages.lastOrNull()
    val growth = listOf(messages.size.toLong(), last?.id ?: 0L, (last?.text?.length ?: 0).toLong())
    var seen by remember { mutableStateOf(growth) }
    LaunchedEffect(growth) {
        val grew = growth != seen
        seen = growth
        if (state.follow && messages.isNotEmpty()) {
            state.byApp { scrollToItem(messages.lastIndex, END_OFFSET) }
        } else {
            state.unseen = ViewportRules.unseen(state.unseen, state.follow, grew && last?.isUser == false)
        }
    }
    // reasoning and tool steps also make the last item taller
    LaunchedEffect(last?.reasoning?.length, last?.steps?.size) {
        if (state.follow && messages.isNotEmpty()) state.byApp { scrollToItem(messages.lastIndex, END_OFFSET) }
    }
}

/** Distance from the end of the last item to the bottom of the list; `null` when it is off screen. */
@Suppress("ReturnCount")
private fun distanceToEnd(list: LazyListState): Int? {
    val info = list.layoutInfo
    if (info.totalItemsCount == 0) return 0
    val lastItem = info.visibleItemsInfo.lastOrNull() ?: return null
    if (lastItem.index != info.totalItemsCount - 1) return null
    val end = lastItem.offset + lastItem.size
    return (end - (info.viewportEndOffset - info.afterContentPadding)).coerceAtLeast(0)
}

/** Scroll offset that puts the end of a (tall) item at the bottom of the list. */
private const val END_OFFSET = Int.MAX_VALUE / 2
