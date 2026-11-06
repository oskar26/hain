@file:Suppress("MatchingDeclarationName") // the section is the point of this file

package app.hain.feature.settings.api

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.common.api.net.NetworkLogEntry
import app.hain.common.api.net.Purpose
import app.hain.feature.settings.R
import app.hain.ui.components.HainCard
import app.hain.ui.components.SegmentedControl
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import java.text.DateFormat
import java.util.Date

/** Which requests the log shows (T-069 filter). */
internal enum class LogFilter(
    val label: Int,
    val purposes: Set<Purpose>?,
) {
    ALL(R.string.settings_log_all, null),
    WEATHER(R.string.settings_log_weather, setOf(Purpose.WEATHER, Purpose.GEO)),
    WIKI(R.string.settings_log_wiki, setOf(Purpose.WIKI)),

    // T-071: news feeds and the articles Pim summarises have their own line
    NEWS(R.string.settings_log_news, setOf(Purpose.NEWS)),
    OTHER(
        R.string.settings_log_other,
        Purpose.entries.toSet() - setOf(Purpose.WEATHER, Purpose.GEO, Purpose.WIKI, Purpose.NEWS),
    ),
    ;

    fun matches(entry: NetworkLogEntry) = purposes == null || entry.purpose in purposes
}

/**
 * Network log (T-069): every time Hain went online in the last 7 days — when, where to, why and how
 * much. Filter by kind or clear it; "full addresses" sits in the developer area (T-266).
 */
@Composable
internal fun NetworkLogSection(
    entries: List<NetworkLogEntry>,
    actions: SettingsActions,
    /** T-292: inside a section card the card already has the heading and the frame. */
    inSection: Boolean = false,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    var filter by rememberSaveable { mutableIntStateOf(0) }
    if (!inSection) BasicText(stringResource(R.string.settings_log_title), style = type.body.copy(color = colors.text))
    BasicText(stringResource(R.string.settings_log_hint), style = type.label.copy(color = colors.textMuted))
    val filters = LogFilter.entries
    SegmentedControl(filters.map { stringResource(it.label) }, filter, { filter = it })
    val shown = entries.filter { filters[filter].matches(it) }
    val rows: @Composable () -> Unit = {
        if (shown.isEmpty()) {
            BasicText(
                stringResource(R.string.settings_log_empty),
                style = type.bodySmall.copy(color = colors.textMuted),
            )
        }
        shown.take(MAX_ROWS).forEach { LogRow(it) }
    }
    if (inSection) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) { rows() }
    } else {
        HainCard { rows() }
    }
    if (entries.isNotEmpty()) {
        BasicText(
            stringResource(R.string.settings_log_clear),
            style = type.bodySmall.copy(color = colors.accent),
            modifier = Modifier.clickable(onClick = actions.onClearNetworkLog).padding(vertical = spacing.sm),
        )
    }
}

@Composable
private fun LogRow(entry: NetworkLogEntry) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val time = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.at))
    val failed = entry.code == null || entry.code !in OK_CODES
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(if (failed) colors.danger else colors.online, CircleShape))
        Column(Modifier.weight(1f).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(entry.host, style = type.bodySmall.copy(color = colors.text), maxLines = 1)
            BasicText(
                "$time · ${stringResource(purposeLabel(entry.purpose))} · ${kilobytes(entry.bytes)}",
                style = type.label.copy(color = colors.textMuted),
                maxLines = 1,
            )
            entry.url?.let {
                BasicText(
                    it,
                    style = type.label.copy(color = colors.textMuted),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun purposeLabel(purpose: Purpose): Int =
    when (purpose) {
        Purpose.WEATHER -> R.string.settings_log_weather
        Purpose.GEO -> R.string.settings_log_place
        Purpose.WIKI -> R.string.settings_log_wiki
        Purpose.NEWS -> R.string.settings_log_news
        Purpose.MODEL_DOWNLOAD -> R.string.settings_log_download
        Purpose.UPDATE -> R.string.settings_log_update
        Purpose.IMAGES -> R.string.settings_log_images
        else -> R.string.settings_log_other
    }

private fun kilobytes(bytes: Long): String = if (bytes < KB) "$bytes B" else "${(bytes + HALF_KB) / KB} KB"

private const val MAX_ROWS = 100
private const val KB = 1024L
private const val HALF_KB = 512L
private const val HTTP_OK = 200
private const val HTTP_REDIRECT = 300
private val OK_CODES = HTTP_OK until HTTP_REDIRECT
