package app.hain.feature.settings.api

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import app.hain.common.api.HainSettings
import app.hain.common.api.UpdateCheck
import app.hain.common.api.WebSearch
import app.hain.common.api.net.NetworkLogEntry
import app.hain.feature.settings.R
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/**
 * T-283 (Oskar 06.11.): everything that goes online on one page. One main switch ("Internet für
 * Pim", the inverse of offline mode), then a plain list of what may go online: web search, weather
 * location, site icons, the daily update check, and what only happens when asked (weather,
 * Wikipedia, news) or tapped (models). The network log follows. Technical details (own SearXNG
 * address, full addresses in the log) stay in the developer area.
 */
@Composable
internal fun InternetPage(
    settings: HainSettings,
    actions: SettingsActions,
    networkLog: List<NetworkLogEntry>,
    hue: GlowHue = GlowHue.SKY,
) {
    val online = !settings.offlineMode
    // T-292: main switch, what may go online and the log as three section cards
    SettingsSection(null, hue, 0) {
        Toggle(
            R.string.settings_internet_main,
            R.string.settings_internet_main_hint,
            online,
        ) { actions.onOfflineMode(!it) }
    }
    SettingsSection(R.string.settings_internet_allowed, hue, 1) { AllowedList(settings, actions, online) }
    SettingsSection(R.string.settings_log_title, hue, 2) { NetworkLogSection(networkLog, actions, inSection = true) }
}

/** What may go online; dimmed (still readable) while the main switch is off. */
@Composable
private fun AllowedList(
    settings: HainSettings,
    actions: SettingsActions,
    online: Boolean,
) {
    if (!online) Info(R.string.settings_internet_paused)
    Column(
        Modifier.fillMaxWidth().alpha(if (online) 1f else PAUSED_ALPHA),
        verticalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.lg),
    ) {
        Choice(
            R.string.settings_web_search,
            settings.webSearch,
            listOf(
                WebSearch.ON to R.string.settings_web_search_on,
                WebSearch.ASK to R.string.settings_web_search_ask,
                WebSearch.OFF to R.string.settings_web_search_off,
            ),
            actions.onWebSearch,
        )
        Fact(R.string.settings_internet_lookups, R.string.settings_internet_lookups_hint)
        Toggle(
            R.string.settings_weather_location,
            R.string.settings_weather_location_hint,
            settings.weatherLocation,
            actions.onWeatherLocation,
        )
        Toggle(
            R.string.settings_source_icons,
            R.string.settings_source_icons_hint,
            settings.sourceIcons,
            actions.onSourceIcons,
        )
        Toggle(
            R.string.settings_internet_updates,
            R.string.settings_internet_updates_hint,
            settings.updateCheck == UpdateCheck.DAILY,
        ) { actions.onUpdateCheck(if (it) UpdateCheck.DAILY else UpdateCheck.OFF) }
        Fact(R.string.settings_internet_models, R.string.settings_internet_models_hint)
    }
}

/** A line that only says how something behaves (no switch): title and a short second line. */
@Composable
private fun Fact(
    label: Int,
    hint: Int,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Column(Modifier.fillMaxWidth().padding(vertical = LocalHainSpacing.current.xs)) {
        BasicText(stringResource(label), style = type.body.copy(color = colors.text))
        BasicText(stringResource(hint), style = type.bodySmall.copy(color = colors.textMuted))
    }
}

/** The list under a switched-off main switch is dimmed, still readable. */
private const val PAUSED_ALPHA = 0.5f
