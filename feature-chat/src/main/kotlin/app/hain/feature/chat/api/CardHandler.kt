package app.hain.feature.chat.api

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.hain.cards.api.AnswerPart
import app.hain.cards.api.AnswerParts
import app.hain.cards.api.CardRenderer
import app.hain.cards.api.CardState
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.UiMessage
import app.hain.ui.components.AnswerActionRow
import app.hain.ui.components.AnswerCitations
import app.hain.ui.components.AssistantBlock
import app.hain.ui.components.CitationMarks

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
    // T-274: "[1]" in the text → small raised chip, tap opens that source
    val context = LocalContext.current
    val sources = message.sources
    val label = stringResource(R.string.chat_source_chip)
    val citations =
        AnswerCitations(
            count = sources.size,
            onTap = { n -> sources.getOrNull(n - 1)?.let { openSource(context, it.url) } },
            label = { n -> label.format(n, sources.getOrNull(n - 1)?.title.orEmpty()) },
        )
    if (parts.none { it is AnswerPart.Card }) {
        AssistantBlock(
            markdown = message.text,
            actions = actions,
            citations = citations,
        )
        return
    }
    val handler = LocalCardHandler.current
    val state = remember(message.cardState) { CardState.decode(message.cardState) }
    Column(Modifier.fillMaxWidth()) {
        parts.forEach { part ->
            when (part) {
                is AnswerPart.Text ->
                    AssistantBlock(
                        markdown = part.markdown,
                        citations = citations,
                    )
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

/**
 * The answer as plain text for copy, share and read aloud: cards become readable lines with the user's
 * ticks; T-274: without the "[1]" markers (the sources are listed under the answer).
 */
internal fun UiMessage.plainAnswer(): String {
    val plain = if (!text.contains("```")) text else AnswerParts.plainText(text, CardState.decode(cardState))
    return CitationMarks.strip(plain)
}

/** Opens a source page in the browser; nothing happens when no app can open it. */
internal fun openSource(
    context: Context,
    url: String,
) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}
