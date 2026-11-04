package app.hain.feature.chat.api

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import app.hain.cards.api.AnswerPart
import app.hain.cards.api.AnswerParts
import app.hain.cards.api.CardRenderer
import app.hain.cards.api.CardState
import app.hain.feature.chat.internal.UiMessage
import app.hain.ui.components.AnswerActionRow
import app.hain.ui.components.AssistantBlock

/**
 * What card taps do in the chat (T-131): [onCardsChange] saves the checklist ticks of an answer
 * (`cards_json`), [onAsk] sends a button's question as a new message. Provided by [ChatScreen];
 * the default does nothing (screenshots, previews).
 */
internal data class CardHandler(
    val onCardsChange: (messageId: Long, cardsJson: String?) -> Unit = { _, _ -> },
    val onAsk: ((String) -> Unit)? = null,
)

internal val LocalCardHandler = staticCompositionLocalOf { CardHandler() }

/**
 * The text of an answer with its cards (CARDS.md, T-131): markdown parts stay plain text, every
 * "```card" block becomes a [CardRenderer]. An answer without cards is the same single
 * [AssistantBlock] as before. While the answer streams, a card grows with it and its buttons wait.
 */
@Composable
internal fun AnswerBody(
    message: UiMessage,
    actions: (@Composable () -> Unit)?,
) {
    val parts = remember(message.text, message.streaming) { AnswerParts.split(message.text, message.streaming) }
    if (parts.none { it is AnswerPart.Card }) {
        AssistantBlock(markdown = message.text, actions = actions)
        return
    }
    val handler = LocalCardHandler.current
    val state = remember(message.cardState) { CardState.decode(message.cardState) }
    Column(Modifier.fillMaxWidth()) {
        parts.forEach { part ->
            when (part) {
                is AnswerPart.Text -> AssistantBlock(markdown = part.markdown)
                is AnswerPart.Card ->
                    CardRenderer(
                        part.card,
                        cardIndex = part.index,
                        state = state,
                        onStateChange = { handler.onCardsChange(message.id, it.encode()) },
                        onAsk = handler.onAsk,
                        complete = part.complete && !message.streaming,
                    )
            }
        }
        if (actions != null) AnswerActionRow(actions)
    }
}

/** The answer as plain text for copy and share: cards become readable lines with the user's ticks. */
internal fun UiMessage.plainAnswer(): String {
    if (!text.contains("```")) return text
    return AnswerParts.plainText(text, CardState.decode(cardState))
}
