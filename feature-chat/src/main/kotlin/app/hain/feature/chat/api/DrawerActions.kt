package app.hain.feature.chat.api

import app.hain.feature.chat.export.ExportFormat

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
    /** T-054: chat id, file kind, `true` = share sheet, `false` = save as a file. */
    val onExport: (Long, ExportFormat, Boolean) -> Unit = { _, _, _ -> },
)
