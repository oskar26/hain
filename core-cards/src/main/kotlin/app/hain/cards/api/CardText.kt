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
                is CardBlock.Button -> Unit
            }
        }
        return out.joinToString("\n")
    }
}
