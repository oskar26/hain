package app.hain.feature.chat.api

/** What the drawer can do with a chat (T-053). */
@Suppress("LongParameterList") // one callback per drawer action
internal class DrawerActions(
    val onClose: () -> Unit,
    val onNewChat: () -> Unit,
    val onOpen: (Long) -> Unit,
    val onDelete: (Long) -> Unit,
    val onRename: (Long, String) -> Unit,
    val onPin: (Long, Boolean) -> Unit,
    val onSearch: (String) -> Unit,
    val onSettings: () -> Unit,
    /** T-073: opens the news screen; `null` = no news row. */
    val onNews: (() -> Unit)? = null,
)
