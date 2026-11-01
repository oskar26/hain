package app.hain.feature.chat.internal

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Drawer sections (DESIGN_SYSTEM.md §5), in display order. */
enum class ChatGroup { PINNED, TODAY, YESTERDAY, LAST_7_DAYS, OLDER }

/** Sorts [chats] into drawer sections by their last activity; empty sections are left out. */
fun groupChats(
    chats: List<Chat>,
    now: Long,
    zone: ZoneId = ZoneId.systemDefault(),
): List<Pair<ChatGroup, List<Chat>>> {
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    return chats
        .sortedByDescending { it.updatedAt }
        .groupBy { chat ->
            val day = Instant.ofEpochMilli(chat.updatedAt).atZone(zone).toLocalDate()
            if (chat.pinned) ChatGroup.PINNED else groupOf(day, today)
        }.toSortedMap()
        .map { (group, list) -> group to list }
}

private fun groupOf(
    day: LocalDate,
    today: LocalDate,
): ChatGroup =
    when {
        !day.isBefore(today) -> ChatGroup.TODAY
        day == today.minusDays(1) -> ChatGroup.YESTERDAY
        !day.isBefore(today.minusDays(WEEK_DAYS)) -> ChatGroup.LAST_7_DAYS
        else -> ChatGroup.OLDER
    }

private const val WEEK_DAYS = 7L
