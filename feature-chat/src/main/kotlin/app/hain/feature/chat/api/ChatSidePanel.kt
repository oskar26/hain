package app.hain.feature.chat.api

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.Chat
import app.hain.feature.chat.internal.ChatGroup
import app.hain.ui.HainIcons
import app.hain.ui.components.HainButton
import app.hain.ui.components.HainButtonStyle
import app.hain.ui.components.HainMenuRow
import app.hain.ui.layout.HainPaneDivider
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/**
 * T-235: on wide windows (from 840 dp) the chat list stays open on the left as a calm side panel:
 * no scrim, no slide, a hairline towards the chat. Same content and actions as the drawer (T-053).
 */
@Suppress("LongParameterList") // the same flat drawer state as [ChatListDrawer]
@Composable
internal fun ChatSidePanel(
    hasChats: Boolean,
    groups: List<Pair<ChatGroup, List<Chat>>>,
    searchQuery: String,
    currentChatId: Long?,
    actions: DrawerActions,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxHeight()) {
        DrawerContent(
            hasChats,
            groups,
            searchQuery,
            currentChatId,
            actions,
            Modifier
                .fillMaxHeight()
                .width(SIDE_PANEL_WIDTH.dp)
                .background(LocalHainColors.current.surface),
        )
        HainPaneDivider()
    }
}

/** Pim and title, search, "Neuer Chat", the grouped list, then news and settings at the bottom. */
@Suppress("LongParameterList")
@Composable
internal fun DrawerContent(
    hasChats: Boolean,
    groups: List<Pair<ChatGroup, List<Chat>>>,
    searchQuery: String,
    currentChatId: Long?,
    actions: DrawerActions,
    modifier: Modifier,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    Column(
        modifier
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(spacing.md),
    ) {
        Row(
            Modifier.padding(horizontal = spacing.xs, vertical = spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LivePim(PimState.HAPPY, 32.dp)
            Spacer(Modifier.width(spacing.sm))
            BasicText(
                stringResource(R.string.chat_title),
                style = LocalHainTypography.current.title.copy(color = colors.text),
            )
        }
        SearchField(searchQuery, actions.onSearch)
        Spacer(Modifier.height(spacing.md))
        HainButton(
            stringResource(R.string.chat_new),
            actions.onNewChat,
            Modifier.fillMaxWidth(),
            style = HainButtonStyle.SECONDARY,
        )
        Spacer(Modifier.height(spacing.md))
        DrawerList(hasChats, groups, searchQuery, currentChatId, actions, Modifier.weight(1f))
        actions.onNews?.let { openNews ->
            HainMenuRow(HainIcons.news, stringResource(R.string.chat_news), openNews, Modifier.fillMaxWidth())
        }
        HainMenuRow(
            HainIcons.settings,
            stringResource(R.string.chat_settings),
            actions.onSettings,
            Modifier.fillMaxWidth(),
        )
    }
}

/** Room the side panel takes on the left (panel plus hairline); the chat starts after it. */
internal val ChatSidePanelWidth: Dp = (SIDE_PANEL_WIDTH + 1).dp

/** A little wider than the phone drawer (300 dp), so long chat titles keep more words. */
private const val SIDE_PANEL_WIDTH = 320
