package app.hain.feature.chat.api

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.hain.feature.chat.internal.ActionCard
import app.hain.feature.chat.internal.ActionEvent

/**
 * The chat's ConfirmCard (T-068): the card itself lives in :core-cards since T-068e; here the chat
 * only hands it the phone's apps (PhoneIntents), so the button opens the clock, calendar or map app.
 */
@Composable
internal fun ConfirmCard(
    card: ActionCard,
    onEvent: (ActionEvent) -> Unit,
    modifier: Modifier = Modifier,
    autoStart: Boolean = false,
) {
    val context = LocalContext.current
    app.hain.cards.api.ConfirmCard(
        card = card,
        onEvent = onEvent,
        launch = { PhoneIntents.launch(context, it) },
        modifier = modifier,
        autoStart = autoStart,
    )
}
