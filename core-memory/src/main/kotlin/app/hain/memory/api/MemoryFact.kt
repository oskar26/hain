package app.hain.memory.api

/** What a fact is about (MEMORY.md §2); [HEALTH] is always sensitive and waits for the user's yes. */
enum class MemoryCategory(
    val key: String,
) {
    PREFERENCE("preference"),
    PERSON("person"),
    PLACE("place"),
    WORK("work"),
    HEALTH("health"),
    OTHER("other"),
    ;

    companion object {
        fun fromKey(key: String?): MemoryCategory = entries.firstOrNull { it.key == key?.trim()?.lowercase() } ?: OTHER
    }
}

/**
 * One remembered fact (MEMORY.md L2), as the memory page and the prompt see it.
 *
 * @property suggested waits under "Vorschläge" for the user's yes and is not used until then.
 */
data class MemoryFact(
    val id: Long,
    val text: String,
    val category: MemoryCategory = MemoryCategory.OTHER,
    val createdAt: Long,
    val suggested: Boolean = false,
    val sensitive: Boolean = false,
    val importance: Double = DEFAULT_IMPORTANCE,
    val originChatId: Long? = null,
) {
    companion object {
        const val DEFAULT_IMPORTANCE = 0.5

        /** Longest fact text (MEMORY.md: 200 chars each). */
        const val MAX_CHARS = 200
    }
}
