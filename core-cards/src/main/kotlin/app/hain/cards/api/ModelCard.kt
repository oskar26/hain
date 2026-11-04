package app.hain.cards.api

/**
 * A model card (CARDS.md §2, T-130): a small structured block the brain writes inside a fenced
 * "```card" block. Built only by [CardParser]; model output is untrusted, so every field is plain
 * text and every list is capped.
 */
data class ModelCard(
    val title: String?,
    val blocks: List<CardBlock>,
)

/** One part of a [ModelCard], in the order the brain wrote it. */
sealed interface CardBlock {
    /** `List` + `- item` lines: a calm bullet list. */
    data class Bullets(
        val items: List<String>,
    ) : CardBlock

    /** `Table` + `| a | b |` rows; the first row is the header. Rows are padded to the header width. */
    data class Table(
        val header: List<String>,
        val rows: List<List<String>>,
    ) : CardBlock

    /** `Checklist` + `- item [x]?` lines; [CheckItem.checked] is what the brain wrote. */
    data class Checklist(
        val items: List<CheckItem>,
    ) : CardBlock

    /** `Progress value=0..100 label="…"`. */
    data class Progress(
        val value: Int,
        val label: String?,
    ) : CardBlock

    /** `Button label="…" action=copy|share|ask:"…"`. Nothing happens before the user taps. */
    data class Button(
        val label: String,
        val action: CardAction,
    ) : CardBlock
}

data class CheckItem(
    val text: String,
    val checked: Boolean,
)

/** What a card button does on tap (CARDS.md §4). Never opens URLs, never runs anything. */
sealed interface CardAction {
    /** Copies the card as plain text. */
    data object Copy : CardAction

    /** Opens the share sheet with the card as plain text. */
    data object Share : CardAction

    /** Sends [text] as a new user message. */
    data class Ask(
        val text: String,
    ) : CardAction
}
