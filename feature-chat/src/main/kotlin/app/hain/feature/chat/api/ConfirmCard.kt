package app.hain.feature.chat.api

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.ActionCard
import app.hain.feature.chat.internal.ActionEvent
import app.hain.feature.chat.internal.ActionState
import app.hain.tools.api.PhoneAction
import app.hain.ui.components.Hain3D
import app.hain.ui.components.Hain3DIcon
import app.hain.ui.components.HainButton
import app.hain.ui.components.HainButtonStyle
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.glassTile
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

/**
 * ConfirmCard (T-068): what Pim prepared — alarm, timer, calendar event or route — with one green
 * button ("Stellen", "Starten", "Eintragen") and "Abbrechen". Nothing happens before the tap; then
 * the phone's own app takes over. After that the card just says how it went.
 */
@Composable
internal fun ConfirmCard(
    card: ActionCard,
    onEvent: (ActionEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    val (title, detail) = describe(card.action)
    Column(
        modifier
            .padding(top = spacing.sm)
            .fillMaxWidth()
            // T-011: a glass tile that glows in the action's colour, with its 3D icon
            .glassTile(hue(card.action), elevation = 6.dp, lightAt = Offset(x = 0.08f, y = 0.3f))
            .padding(spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Hain3DIcon(icon3d(card.action), size = 36.dp)
            Spacer(Modifier.width(spacing.sm))
            Column(Modifier.weight(1f)) {
                BasicText(
                    title,
                    style = type.body.copy(color = colors.text),
                    modifier = Modifier.semantics { heading() },
                )
                if (!detail.isNullOrBlank()) {
                    BasicText(detail, style = type.bodySmall.copy(color = colors.textMuted))
                }
            }
        }
        CardFooter(card, onEvent)
    }
}

/** The buttons while the card waits, afterwards one quiet line on how it went. */
@Composable
private fun CardFooter(
    card: ActionCard,
    onEvent: (ActionEvent) -> Unit,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    val context = LocalContext.current
    when (card.state) {
        ActionState.PENDING ->
            // T-085: with big fonts the two buttons wrap instead of running off the card
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                HainButton(
                    stringResource(confirmLabel(card.action)),
                    onClick = { onEvent(PhoneIntents.launch(context, card.action)) },
                )
                HainButton(
                    stringResource(R.string.chat_action_cancel),
                    onClick = { onEvent(ActionEvent.CANCELLED) },
                    style = HainButtonStyle.SECONDARY,
                )
            }
        ActionState.DONE ->
            BasicText(
                "✓ " + stringResource(doneLabel(card.action)),
                style = type.label.copy(color = colors.accent),
            )
        ActionState.CANCELLED ->
            BasicText(
                stringResource(R.string.chat_action_cancelled),
                style = type.label.copy(color = colors.textMuted),
            )
        ActionState.NO_APP ->
            BasicText(
                stringResource(noAppLabel(card.action)),
                style = type.bodySmall.copy(color = colors.textMuted),
            )
    }
}

private fun icon3d(action: PhoneAction): Hain3D =
    when (action) {
        is PhoneAction.CalendarEvent -> Hain3D.CALENDAR
        is PhoneAction.Navigate -> Hain3D.GLOBE
        else -> Hain3D.PHONE
    }

private fun hue(action: PhoneAction): GlowHue =
    when (action) {
        is PhoneAction.CalendarEvent -> GlowHue.PEACH
        is PhoneAction.Navigate -> GlowHue.SKY
        else -> GlowHue.MINT
    }

private fun confirmLabel(action: PhoneAction): Int =
    when (action) {
        is PhoneAction.Alarm -> R.string.chat_action_set
        is PhoneAction.Timer -> R.string.chat_action_start
        is PhoneAction.CalendarEvent -> R.string.chat_action_add
        is PhoneAction.Navigate -> R.string.chat_action_start
    }

private fun doneLabel(action: PhoneAction): Int =
    when (action) {
        is PhoneAction.Alarm -> R.string.chat_action_done_alarm
        is PhoneAction.Timer -> R.string.chat_action_done_timer
        is PhoneAction.CalendarEvent -> R.string.chat_action_done_event
        is PhoneAction.Navigate -> R.string.chat_action_done_route
    }

private fun noAppLabel(action: PhoneAction): Int =
    when (action) {
        is PhoneAction.Alarm, is PhoneAction.Timer -> R.string.chat_action_no_clock
        is PhoneAction.CalendarEvent -> R.string.chat_action_no_calendar
        is PhoneAction.Navigate -> R.string.chat_action_no_maps
    }

/** Title and detail line of the card, in the phone's language. */
@Composable
private fun describe(action: PhoneAction): Pair<String, String?> {
    val locale = LocalConfiguration.current.locales[0]
    return when (action) {
        is PhoneAction.Alarm ->
            stringResource(R.string.chat_action_alarm, action.clock) to
                listOfNotNull(action.label, repeatDays(action.days)).joinToString(" · ").ifEmpty { null }
        is PhoneAction.Timer -> stringResource(R.string.chat_action_timer, duration(action.seconds)) to action.label
        is PhoneAction.CalendarEvent -> {
            val zone = if (action.allDay) ZoneOffset.UTC else ZoneId.systemDefault()
            val start = Instant.ofEpochMilli(action.startMillis).atZone(zone)
            val end = Instant.ofEpochMilli(action.endMillis).atZone(zone)
            val day = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
            val clock = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
            val whenText =
                if (action.allDay) {
                    start.format(day) + " · " + stringResource(R.string.chat_action_all_day)
                } else {
                    start.format(day) + " · " + start.format(clock) + "–" + end.format(clock)
                }
            action.title to listOfNotNull(whenText, action.location).joinToString(" · ")
        }
        is PhoneAction.Navigate -> action.destination to stringResource(R.string.chat_action_route_hint)
    }
}

/** 4b-2: "Jeden Tag", "Mo–Fr", "Am Wochenende" or "Mo., Mi."; `null` for a one-time alarm. */
@Composable
private fun repeatDays(days: List<Int>): String? {
    if (days.isEmpty()) return null
    val locale = LocalConfiguration.current.locales[0]
    return when (days) {
        (1..DAYS_PER_WEEK).toList() -> stringResource(R.string.chat_action_every_day)
        (1..WORK_DAYS).toList() -> stringResource(R.string.chat_action_weekdays)
        listOf(WORK_DAYS + 1, DAYS_PER_WEEK) -> stringResource(R.string.chat_action_weekend)
        else -> days.joinToString(", ") { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, locale) }
    }
}

@Composable
private fun duration(seconds: Int): String {
    val h = seconds / SECONDS_PER_HOUR
    val m = seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val s = seconds % SECONDS_PER_MINUTE
    return listOfNotNull(
        if (h > 0) stringResource(R.string.chat_action_hours, h) else null,
        if (m > 0) stringResource(R.string.chat_action_minutes, m) else null,
        if (s > 0) stringResource(R.string.chat_action_seconds, s) else null,
    ).joinToString(" ")
}

private const val SECONDS_PER_HOUR = 3600
private const val SECONDS_PER_MINUTE = 60
private const val DAYS_PER_WEEK = 7
private const val WORK_DAYS = 5
