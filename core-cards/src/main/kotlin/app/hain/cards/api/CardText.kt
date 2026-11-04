package app.hain.cards.api

/** A [ModelCard] as plain text (copy, share, the "Copy" button): readable without the app. */
object CardText {
    fun plain(
        card: ModelCard,
        cardIndex: Int = 0,
        state: CardState = CardState(),
    ): String {
        val out = mutableListOf<String>()
        card.title?.let { out += it }
        card.blocks.forEachIndexed { blockIndex, block ->
            when (block) {
                is CardBlock.Bullets -> block.items.forEach { out += "- $it" }
                is CardBlock.Checklist ->
                    block.items.forEachIndexed { i, item ->
                        val ticked = state.isChecked(cardIndex, blockIndex, i, item.checked)
                        out += (if (ticked) "- [x] " else "- [ ] ") + item.text
                    }
                is CardBlock.Table -> {
                    out += block.header.joinToString(" | ")
                    block.rows.forEach { out += it.joinToString(" | ") }
                }
                is CardBlock.Progress -> out += listOfNotNull(block.label, "${block.value} %").joinToString(": ")
                is CardBlock.Chart -> {
                    block.title?.let { out += it }
                    val unit = block.unit?.let { " $it" }.orEmpty()
                    block.points.forEach { out += "- ${it.label}: ${number(it.value)}$unit" }
                }
                is CardBlock.Button -> Unit
            }
        }
        return out.joinToString("\n")
    }

    /** "7.5", "12" — locale-free, so a copied card reads the same everywhere. */
    private fun number(value: Double): String =
        if (value == Math.rint(value) && kotlin.math.abs(value) < WHOLE_LIMIT) {
            value.toLong().toString()
        } else {
            "%.2f".format(java.util.Locale.ROOT, value).trimEnd('0').trimEnd('.')
        }

    private const val WHOLE_LIMIT = 1e15
}
