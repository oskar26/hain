package app.hain.feature.chat.api

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.feature.chat.export.ExportItems
import app.hain.feature.chat.internal.Chat
import app.hain.feature.chat.internal.ChatGroup
import app.hain.ui.HainIcons
import app.hain.ui.components.HainButton
import app.hain.ui.components.HainButtonStyle
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.HainMenuRow
import app.hain.ui.components.HainPopupItem
import app.hain.ui.components.HainPopupMenu
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.HainMotionMoment
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainSpring
import app.hain.ui.motion.hainTween
import app.hain.ui.motion.motionSpec
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.pim.PullPim
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.HainRadius
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.glassTile
import app.hain.ui.theme.hainSurface

/** Chat list drawer: search, sections Pinned/Today/Yesterday/7 days/Older, rename, pin, delete (T-053). */
@Suppress("LongParameterList", "LongMethod") // drawer state is passed flat so previews stay simple
@Composable
internal fun ChatListDrawer(
    open: Boolean,
    hasChats: Boolean,
    groups: List<Pair<ChatGroup, List<Chat>>>,
    searchQuery: String,
    currentChatId: Long?,
    actions: DrawerActions,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val reduce = LocalReduceMotion.current
    // scrim fades, the panel glides on the calm drawer spring; both snap with reduced motion (T-053)
    AnimatedVisibility(
        open,
        enter = fadeIn(hainTween(HainMotion.BASE_MS, reduce)),
        exit = fadeOut(hainTween(HainMotion.BASE_MS, reduce)),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.scrim)
                .clickable(remember { MutableInteractionSource() }, indication = null, onClick = actions.onClose),
        )
    }
    AnimatedVisibility(
        open,
        enter = slideInHorizontally(motionSpec(HainMotionMoment.DRAWER, reduce)) { -it },
        exit = slideOutHorizontally(motionSpec(HainMotionMoment.DRAWER, reduce)) { -it },
    ) {
        // T-011: the drawer is a pane of glass lit softly in mint from the top, rounded on the open side
        Column(
            Modifier
                .fillMaxHeight()
                .width(DRAWER_WIDTH.dp)
                .glassTile(GlowHue.MINT, DrawerShape, DRAWER_ELEVATION.dp, Offset(x = 0.15f, y = 0f), floorGlow = false)
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
}

@Suppress("LongParameterList")
@Composable
private fun DrawerList(
    hasChats: Boolean,
    groups: List<Pair<ChatGroup, List<Chat>>>,
    searchQuery: String,
    currentChatId: Long?,
    actions: DrawerActions,
    modifier: Modifier = Modifier,
) {
    val type = LocalHainTypography.current
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    var expandedId by rememberSaveable { mutableStateOf<Long?>(null) }
    val reduce = LocalReduceMotion.current
    // list changes (pin, rename, delete) fade and glide instead of jumping; nothing moves when reduced
    val itemMotion: Modifier.(LazyItemScope) -> Modifier = { scope ->
        with(scope) {
            if (reduce) {
                animateItem(fadeInSpec = null, placementSpec = null, fadeOutSpec = null)
            } else {
                animateItem(
                    fadeInSpec = hainTween(HainMotion.FAST_MS, false),
                    placementSpec = hainSpring(false),
                    fadeOutSpec = hainTween(HainMotion.FAST_MS, false),
                )
            }
        }
    }
    if (groups.isEmpty()) {
        val empty =
            if (hasChats && searchQuery.isNotBlank()) {
                stringResource(R.string.chat_search_empty)
            } else {
                stringResource(R.string.chat_history_empty)
            }
        // T-089: Pim looks around when the search finds nothing (and waits when there are no chats yet)
        Column(
            Modifier.fillMaxWidth().padding(spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            LivePim(if (hasChats) PimState.LISTENING else PimState.IDLE, EMPTY_PIM.dp)
            BasicText(
                empty,
                style = type.bodySmall.copy(color = colors.textMuted, textAlign = TextAlign.Center),
            )
        }
    }
    // T-089: pull the list down and Pim peeks in
    PullPim(modifier) {
        DrawerGroups(groups, currentChatId, expandedId, { expandedId = it }, actions, itemMotion)
    }
}

@Suppress("LongParameterList")
@Composable
private fun DrawerGroups(
    groups: List<Pair<ChatGroup, List<Chat>>>,
    currentChatId: Long?,
    expandedId: Long?,
    onExpand: (Long?) -> Unit,
    actions: DrawerActions,
    itemMotion: Modifier.(LazyItemScope) -> Modifier,
) {
    val type = LocalHainTypography.current
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    LazyColumn(Modifier.fillMaxSize()) {
        groups.forEach { (group, chats) ->
            item(key = "header-${group.name}") {
                BasicText(
                    stringResource(group.label()),
                    style = type.label.copy(color = colors.textMuted),
                    modifier =
                        Modifier.itemMotion(this).padding(
                            start = spacing.md,
                            end = spacing.md,
                            top = spacing.md,
                            bottom = spacing.xs,
                        ),
                )
            }
            items(chats, key = { it.id }) { chat ->
                ChatRow(
                    chat = chat,
                    selected = chat.id == currentChatId,
                    expanded = chat.id == expandedId,
                    onToggle = { onExpand(if (expandedId == chat.id) null else chat.id) },
                    actions = actions,
                    modifier = Modifier.itemMotion(this),
                )
            }
        }
    }
}

private fun ChatGroup.label(): Int =
    when (this) {
        ChatGroup.PINNED -> R.string.chat_group_pinned
        ChatGroup.TODAY -> R.string.chat_today
        ChatGroup.YESTERDAY -> R.string.chat_group_yesterday
        ChatGroup.LAST_7_DAYS -> R.string.chat_group_week
        ChatGroup.OLDER -> R.string.chat_group_older
    }

@Composable
private fun SearchField(
    query: String,
    onChange: (String) -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val shape = LocalHainShapes.current.card
    Row(
        Modifier
            .fillMaxWidth()
            .hainSurface(HainLevel.FLAT, shape)
            .padding(start = spacing.md, end = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            HainIcons.search,
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.textMuted),
            modifier = Modifier.size(ICON_SIZE.dp),
        )
        Spacer(Modifier.width(spacing.sm))
        Box(Modifier.weight(1f).padding(vertical = spacing.md), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                BasicText(
                    stringResource(R.string.chat_search),
                    style = type.body.copy(color = colors.textMuted),
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = type.body.copy(color = colors.text),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            val clear = stringResource(R.string.chat_search_clear)
            HainIconButton(HainIcons.close, clear, { onChange("") }, tint = colors.textMuted)
        }
    }
}

/**
 * One chat in the list. The ⋯ button opens a small menu right at the row (2d-2: no big empty box);
 * rename edits the title in place, delete asks once inside the menu.
 */
@Suppress("LongParameterList")
@Composable
private fun ChatRow(
    chat: Chat,
    selected: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    actions: DrawerActions,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    var mode by remember(chat.id, expanded) { mutableStateOf(RowMode.MENU) }
    if (expanded && mode == RowMode.RENAME) {
        RenameField(chat.title, onDone = { title ->
            if (title != null) actions.onRename(chat.id, title)
            onToggle()
        })
        return
    }
    // the highlight covers the whole row, ⋯ included; while its menu is open the row is marked (T-053)
    val highlight by animateColorAsState(
        when {
            expanded -> colors.surfaceHigh
            selected -> colors.surface
            else -> Color.Transparent
        },
        hainTween(HainMotion.FAST_MS),
        label = "row-highlight",
    )
    Row(
        modifier
            .fillMaxWidth()
            .clip(LocalHainShapes.current.row)
            .background(highlight),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HainMenuRow(
            icon = if (chat.pinned) HainIcons.pin else null,
            label = chat.title,
            onClick = { actions.onOpen(chat.id) },
            modifier = Modifier.weight(1f),
            // T-104: long press = the same ⋯ menu (with the soft blur behind it)
            onLongClick = { if (!expanded) onToggle() },
        )
        Box {
            HainIconButton(HainIcons.more, stringResource(R.string.chat_more), onToggle, tint = colors.icon)
            RowMenu(chat, expanded, mode, { mode = it }, onToggle, actions)
        }
    }
}

/** The ⋯ menu of one chat as a glass tile right under the button (T-053). */
@Suppress("LongParameterList")
@Composable
private fun RowMenu(
    chat: Chat,
    expanded: Boolean,
    mode: RowMode,
    onMode: (RowMode) -> Unit,
    onToggle: () -> Unit,
    actions: DrawerActions,
) {
    val density = LocalDensity.current
    // glass tile right under the ⋯, growing out of its top-right corner
    HainPopupMenu(
        expanded = expanded,
        onDismiss = onToggle,
        alignment = Alignment.TopEnd,
        offset =
            with(density) {
                IntOffset(MENU_SHADOW_ROOM.dp.roundToPx(), (MENU_OFFSET - MENU_SHADOW_ROOM).dp.roundToPx())
            },
    ) {
        if (mode == RowMode.EXPORT) {
            // T-054: PDF or Markdown, share or save
            ExportItems { format, share ->
                onToggle()
                actions.onExport(chat.id, format, share)
            }
        } else if (mode == RowMode.CONFIRM_DELETE) {
            ConfirmDelete(
                onDelete = {
                    actions.onDelete(chat.id)
                    onToggle()
                },
                onCancel = { onMode(RowMode.MENU) },
            )
        } else {
            HainPopupItem(
                HainIcons.pin,
                stringResource(if (chat.pinned) R.string.chat_unpin else R.string.chat_pin),
                {
                    actions.onPin(chat.id, !chat.pinned)
                    onToggle()
                },
            )
            HainPopupItem(HainIcons.edit, stringResource(R.string.chat_rename), { onMode(RowMode.RENAME) })
            HainPopupItem(HainIcons.share, stringResource(R.string.chat_export), { onMode(RowMode.EXPORT) })
            HainPopupItem(
                HainIcons.delete,
                stringResource(R.string.chat_delete_short),
                { onMode(RowMode.CONFIRM_DELETE) },
                danger = true,
            )
        }
    }
}

private enum class RowMode { MENU, RENAME, CONFIRM_DELETE, EXPORT }

/** Inline title editor; [onDone] gets the new title, or `null` when cancelled. */
@Composable
private fun RenameField(
    title: String,
    onDone: (String?) -> Unit,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    var text by rememberSaveable { mutableStateOf(title) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Row(Modifier.padding(start = spacing.md), verticalAlignment = Alignment.CenterVertically) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = LocalHainTypography.current.body.copy(color = colors.text),
            cursorBrush = SolidColor(colors.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onDone(text) }),
            modifier = Modifier.weight(1f).focusRequester(focus),
        )
        val save = stringResource(R.string.chat_rename_save)
        HainIconButton(HainIcons.check, save, { onDone(text) }, tint = colors.accent)
        HainIconButton(HainIcons.close, stringResource(R.string.chat_cancel), { onDone(null) }, tint = colors.textMuted)
    }
}

@Composable
private fun ConfirmDelete(
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    val haptics = rememberHainHaptics()
    Column(Modifier.widthIn(max = CONFIRM_WIDTH.dp).padding(spacing.md)) {
        BasicText(
            stringResource(R.string.chat_delete_confirm),
            style = type.bodySmall.copy(color = LocalHainColors.current.text),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(spacing.sm))
        // T-085: with big fonts the two buttons wrap instead of running out of the menu
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            HainButton(
                stringResource(R.string.chat_delete_yes),
                {
                    haptics.confirm()
                    onDelete()
                },
                style = HainButtonStyle.SECONDARY,
            )
            HainButton(stringResource(R.string.chat_cancel), onCancel, style = HainButtonStyle.GHOST)
        }
    }
}

private const val DRAWER_WIDTH = 300
private const val DRAWER_ELEVATION = 24
private val DrawerShape =
    androidx.compose.foundation.shape.RoundedCornerShape(
        topStart = 0.dp,
        topEnd = HainRadius.LARGE.dp,
        bottomEnd = HainRadius.LARGE.dp,
        bottomStart = 0.dp,
    )
private const val ICON_SIZE = 20
private const val MENU_OFFSET = 44
private const val MENU_SHADOW_ROOM = 12
private const val CONFIRM_WIDTH = 240
private const val EMPTY_PIM = 64
