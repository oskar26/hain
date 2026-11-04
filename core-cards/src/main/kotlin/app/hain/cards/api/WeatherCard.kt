package app.hain.cards.api

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.cards.R
import app.hain.ui.components.Hain3D
import app.hain.ui.components.Hain3DIcon
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.glassTile

/**
 * WeatherCard (T-064, 3b-2): the weather Pim looked up, as a calm glass tile under the answer —
 * place and temperature now, then one quiet line per day. Values come from the weather tool
 * (Open-Meteo); the card only shows them.
 */
@Composable
fun WeatherCard(
    data: WeatherCardData,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    Column(
        modifier
            .padding(top = spacing.sm)
            .fillMaxWidth()
            .glassTile(GlowHue.SKY, elevation = 6.dp, lightAt = Offset(x = 0.08f, y = 0.3f))
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Hain3DIcon(Hain3D.WEATHER, size = 36.dp)
            Spacer(Modifier.width(spacing.sm))
            Column(Modifier.weight(1f)) {
                BasicText(
                    data.place,
                    style = type.body.copy(color = colors.text),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() },
                )
                val wind = data.windKmh?.let { stringResource(R.string.cards_weather_wind, it) }
                val now = listOfNotNull(data.sky, wind).joinToString(" · ")
                if (now.isNotEmpty()) BasicText(now, style = type.bodySmall.copy(color = colors.textMuted))
            }
            data.temperature?.let {
                Spacer(Modifier.width(spacing.sm))
                BasicText(
                    stringResource(R.string.cards_weather_degrees, it),
                    style = type.title.copy(color = colors.text),
                )
            }
        }
        data.days.forEach { day -> DayRow(day) }
    }
}

@Composable
private fun DayRow(day: WeatherDay) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            BasicText(day.label, style = type.bodySmall.copy(color = colors.text))
            day.sky?.let { BasicText(it, style = type.label.copy(color = colors.textMuted)) }
        }
        day.rainChance?.let {
            BasicText(
                stringResource(R.string.cards_weather_rain, it),
                style = type.label.copy(color = colors.textMuted),
            )
            Spacer(Modifier.width(spacing.sm))
        }
        val range =
            listOfNotNull(
                day.max?.let { stringResource(R.string.cards_weather_degrees, it) },
                day.min?.let { stringResource(R.string.cards_weather_degrees, it) },
            ).joinToString(" / ")
        if (range.isNotEmpty()) BasicText(range, style = type.bodySmall.copy(color = colors.text))
    }
}
