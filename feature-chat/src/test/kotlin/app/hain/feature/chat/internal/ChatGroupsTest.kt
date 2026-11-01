package app.hain.feature.chat.internal

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ChatGroupsTest {
    private val zone = ZoneId.of("Europe/Berlin")

    private fun at(
        day: Int,
        hour: Int,
    ) = LocalDateTime
        .of(2026, 11, day, hour, 0)
        .atZone(zone)
        .toInstant()
        .toEpochMilli()

    private fun chat(
        id: Long,
        updatedAt: Long,
        pinned: Boolean = false,
    ) = Chat(id, "c$id", emptyList(), updatedAt, pinned)

    @Test
    fun sortsIntoSectionsByLocalDay() {
        val now = at(day = 20, hour = 9)
        val chats =
            listOf(
                chat(1, at(20, 1)),
                chat(2, at(19, 23)),
                chat(3, at(14, 12)),
                chat(4, at(12, 12)),
                chat(5, at(2, 12), pinned = true),
                chat(6, at(20, 8)),
            )
        val groups = groupChats(chats, now, zone)
        assertEquals(
            listOf(
                ChatGroup.PINNED to listOf(5L),
                ChatGroup.TODAY to listOf(6L, 1L),
                ChatGroup.YESTERDAY to listOf(2L),
                ChatGroup.LAST_7_DAYS to listOf(3L),
                ChatGroup.OLDER to listOf(4L),
            ),
            groups.map { (group, list) -> group to list.map { it.id } },
        )
    }

    @Test
    fun emptyInputHasNoSections() {
        assertEquals(emptyList<Pair<ChatGroup, List<Chat>>>(), groupChats(emptyList(), at(20, 9), zone))
    }
}
