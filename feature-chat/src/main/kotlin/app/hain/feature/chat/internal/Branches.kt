package app.hain.feature.chat.internal

/** Where one message stands among its versions (T-057): "‹ 2/3 ›" under it. */
data class BranchInfo(
    /** 1-based position among the versions, oldest first. */
    val index: Int,
    val count: Int,
    val previousId: Long?,
    val nextId: Long?,
)

/**
 * The versions of a chat (T-057, like ChatGPT). Editing a message or asking again does not throw the
 * old version away: the new one becomes a sibling (same parent), and every message has one parent.
 * The chat shows one path from the first message down: at each step the sibling picked last
 * ([UiMessage.selectedAt], else the newest by id). Pure functions, no state.
 */
internal object Branches {
    /** Parent of a first message (never a real id: ids start at the clock). */
    const val ROOT = 0L

    /**
     * Fills in missing parents (chats from before branches, test data): a message without one hangs
     * under the message before it by id, the first one under [ROOT]. Explicit parents stay.
     */
    fun normalize(all: List<UiMessage>): List<UiMessage> {
        if (all.none { it.parentId == null }) return all
        var previous: Long? = null
        return all.sortedBy { it.id }.map { m ->
            val fixed = if (m.parentId == null) m.copy(parentId = previous ?: ROOT) else m
            previous = m.id
            fixed
        }
    }

    /** The visible path (first message → newest) and everything else (other versions). */
    fun split(all: List<UiMessage>): Pair<List<UiMessage>, List<UiMessage>> {
        val path = path(all)
        val shown = path.map { it.id }.toSet()
        return path to all.filterNot { it.id in shown }
    }

    /** The messages the chat shows: at every step the picked child. */
    fun path(all: List<UiMessage>): List<UiMessage> {
        if (all.isEmpty()) return emptyList()
        val children = childrenOf(all)
        val out = mutableListOf<UiMessage>()
        val seen = mutableSetOf<Long>()
        var pick = children[ROOT]?.maxWithOrNull(PICK_ORDER)
        // seen: a broken loop of parents never hangs the app
        while (pick != null && seen.add(pick.id)) {
            out += pick
            pick = children[pick.id]?.maxWithOrNull(PICK_ORDER)
        }
        return out
    }

    /** "‹ 2/3 ›" for every message on [path] that has more than one version. */
    fun info(
        all: List<UiMessage>,
        path: List<UiMessage>,
    ): Map<Long, BranchInfo> {
        if (all.size == path.size) return emptyMap()
        val ids = all.map { it.id }.toSet()
        val children = childrenOf(all)
        val out = mutableMapOf<Long, BranchInfo>()
        path.forEach { m ->
            val siblings = children[m.parentId?.takeIf { it in ids } ?: ROOT].orEmpty().sortedBy { it.id }
            if (siblings.size > 1) {
                val i = siblings.indexOfFirst { it.id == m.id }
                out[m.id] =
                    BranchInfo(
                        index = i + 1,
                        count = siblings.size,
                        previousId = siblings.getOrNull(i - 1)?.id,
                        nextId = siblings.getOrNull(i + 1)?.id,
                    )
            }
        }
        return out
    }

    /** A pick time that makes a message win over all its siblings. */
    fun pickTime(
        all: List<UiMessage>,
        parentId: Long?,
        now: Long,
    ): Long {
        val siblings = all.filter { (it.parentId ?: ROOT) == (parentId ?: ROOT) }
        val newest = siblings.maxOfOrNull { it.selectedAt ?: 0L } ?: 0L
        return maxOf(now, newest + 1)
    }

    private fun childrenOf(all: List<UiMessage>): Map<Long, List<UiMessage>> {
        val ids = all.map { it.id }.toSet()
        return all.groupBy { m -> m.parentId?.takeIf { it in ids } ?: ROOT }
    }

    private val PICK_ORDER = compareBy<UiMessage>({ it.selectedAt ?: 0L }, { it.id })
}
