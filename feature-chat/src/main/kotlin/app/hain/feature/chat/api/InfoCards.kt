package app.hain.feature.chat.api

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import app.hain.cards.api.WeatherCard
import app.hain.cards.api.WikiCard
import app.hain.feature.chat.internal.InfoCardParser
import app.hain.tools.api.InfoCard

/** 3b-2: the weather or Wikipedia card under an answer; nothing when the data cannot be read. */
@Composable
internal fun InfoCardView(card: InfoCard) {
    val locale = LocalConfiguration.current.locales[0]
    val weather = remember(card, locale) { InfoCardParser.weather(card, locale) }
    val wiki = remember(card) { InfoCardParser.wiki(card) }
    val uri = LocalUriHandler.current
    when {
        weather != null -> WeatherCard(weather)
        // the page opens only after a tap (AGENTS.md: never open URLs without user tap)
        wiki != null -> WikiCard(wiki, onOpen = { uri.openUri(it) })
    }
}
