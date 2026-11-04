package app.hain.cards.api

/**
 * Ticks the user set on checklists of one answer (CARDS.md §4), saved in `message.cards_json`.
 * The key is `card.block.item`; only items the user touched are stored, the rest keep what the
 * brain wrote. JSON: `{"checked":{"0.1.2":true}}`.
 */
data class CardState(
    val checks: Map<String, Boolean> = emptyMap(),
) {
    fun isChecked(
        card: Int,
        block: Int,
        item: Int,
        default: Boolean,
    ): Boolean = checks[key(card, block, item)] ?: default

    fun toggled(
        card: Int,
        block: Int,
        item: Int,
        default: Boolean,
    ): CardState {
        val now = !isChecked(card, block, item, default)
        val key = key(card, block, item)
        return CardState(if (now == default) checks - key else checks + (key to now))
    }

    /** JSON for `cards_json`, or null when the user changed nothing. */
    fun encode(): String? =
        if (checks.isEmpty()) {
            null
        } else {
            checks.entries
                .sortedBy { it.key }
                .joinToString(",", prefix = "{\"checked\":{", postfix = "}}") { "\"${it.key}\":${it.value}" }
        }

    companion object {
        private val entry = Regex(""""(\d{1,3}\.\d{1,3}\.\d{1,3})"\s*:\s*(true|false)""")

        /** Reads [json] from `cards_json`; anything broken counts as "nothing changed". */
        fun decode(json: String?): CardState {
            if (json.isNullOrBlank()) return CardState()
            return CardState(entry.findAll(json).associate { it.groupValues[1] to (it.groupValues[2] == "true") })
        }

        private fun key(
            card: Int,
            block: Int,
            item: Int,
        ) = "$card.$block.$item"
    }
}
