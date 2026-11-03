package app.hain.feature.chat.api

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.ChatEngine
import app.hain.feature.chat.internal.SourceRef
import app.hain.feature.chat.internal.StepState
import app.hain.feature.chat.internal.ToolStep
import app.hain.ui.components.Hain3D
import app.hain.ui.components.Hain3DIcon
import app.hain.tools.api.PhoneAction
import app.hain.ui.components.HainChip
import app.hain.ui.components.PulsingDot
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.glassTile

/**
 * What Pim did for an answer (T-059), since T-011 as one glass tool card that glows in the colour of
 * the first tool: a row per tool call with its small 3D icon — pulsing while it runs, a ✓ that pops
 * in when done, ✕ when it failed — and an orange dot when it went online. Long rows wrap onto a
 * second line instead of running off the card (T-085).
 */
@Composable
internal fun ToolTimeline(
    steps: List<ToolStep>,
    modifier: Modifier = Modifier,
) {
    if (steps.isEmpty()) return
    val spacing = LocalHainSpacing.current
    Column(
        modifier
            .padding(bottom = spacing.md)
            .fillMaxWidth()
            .glassTile(toolHue(steps.first().name), elevation = 6.dp, lightAt = Offset(x = 0.08f, y = 0.5f))
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalArrangement = Arrangement.spacedBy(spacing.xs),
    ) {
        steps.forEach { ToolRow(it) }
    }
}

/** The 3D icon of a tool. */
internal fun toolIcon(name: String): Hain3D =
    when (name) {
        "calculate" -> Hain3D.CALCULATOR
        "weather" -> Hain3D.WEATHER
        "wikipedia" -> Hain3D.BOOK
        ChatEngine.SEARCH_TOOL -> Hain3D.SEARCH
        "read_page", ChatEngine.READ_STEP -> Hain3D.DOCUMENT
        PhoneAction.ADD_EVENT -> Hain3D.CALENDAR
        PhoneAction.NAVIGATE -> Hain3D.GLOBE
        PhoneAction.SET_ALARM, PhoneAction.SET_TIMER -> Hain3D.PHONE
        else -> Hain3D.SPARKLES
    }

/** The colour that glows inside a tool's card. */
internal fun toolHue(name: String): GlowHue =
    when (name) {
        "calculate" -> GlowHue.PEACH
        "weather" -> GlowHue.SUN
        "wikipedia" -> GlowHue.SKY
        ChatEngine.SEARCH_TOOL -> GlowHue.LILAC
        "read_page", ChatEngine.READ_STEP -> GlowHue.TEAL
        else -> GlowHue.MINT
    }

@Composable
private fun toolText(step: ToolStep): String {
    if (step.name == ChatEngine.READ_STEP) return readPagesText(step)
    val name = stringResource(toolName(step.name))
    val tail =
        when {
            step.state == StepState.RUNNING && step.detail == null -> stringResource(R.string.chat_tool_running)
            step.state == StepState.FAILED && step.error == "offline" -> stringResource(R.string.chat_tool_offline)
            step.state == StepState.FAILED -> stringResource(R.string.chat_tool_failed)
            else -> step.detail
        }
    return if (tail.isNullOrBlank()) name else "$name · $tail"
}

private fun toolName(name: String): Int =
    when (name) {
        "calculate" -> R.string.chat_tool_calculate
        "weather" -> R.string.chat_tool_weather
        "wikipedia" -> R.string.chat_tool_wikipedia
        ChatEngine.SEARCH_TOOL -> R.string.chat_tool_search
        "read_page" -> R.string.chat_tool_read_page
        PhoneAction.SET_ALARM -> R.string.chat_tool_alarm
        PhoneAction.SET_TIMER -> R.string.chat_tool_timer
        PhoneAction.ADD_EVENT -> R.string.chat_tool_event
        PhoneAction.NAVIGATE -> R.string.chat_tool_route
        else -> R.string.chat_tool_other
    }

/** "Lese 3 Seiten…" while the pages of a search load, then "3 Seiten gelesen". */
@Composable
private fun readPagesText(step: ToolStep): String {
    val n = step.detail?.toIntOrNull() ?: 0
    return when (step.state) {
        StepState.RUNNING -> pluralStringResource(R.plurals.chat_tool_reading_pages, n, n)
        StepState.DONE -> pluralStringResource(R.plurals.chat_tool_read_pages, n, n)
        StepState.FAILED -> stringResource(R.string.chat_tool_pages) + " · " + stringResource(R.string.chat_tool_failed)
    }
}

@Composable
private fun ToolRow(step: ToolStep) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val text = toolText(step)
    // The ✓ pops in only when a step finishes while you watch, not for old answers.
    val reduce = LocalReduceMotion.current
    val pop = remember { Animatable(1f) }
    var seen by remember { mutableStateOf(step.state) }
    LaunchedEffect(step.state) {
        if (step.state != seen && step.state == StepState.DONE && !reduce) {
            pop.snapTo(POP_START)
            pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        seen = step.state
    }
    Row(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = text },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Hain3DIcon(toolIcon(step.name), size = 28.dp)
        Spacer(Modifier.width(spacing.sm))
        BasicText(
            text,
            style = LocalHainTypography.current.bodySmall.copy(color = colors.text),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(spacing.sm))
        if (step.online && step.state != StepState.RUNNING) {
            Box(Modifier.size(7.dp).background(colors.online, CircleShape))
            Spacer(Modifier.width(6.dp))
        }
        when (step.state) {
            StepState.RUNNING -> PulsingDot(if (step.online) colors.online else colors.accent)
            StepState.DONE ->
                BasicText(
                    "✓",
                    style = LocalHainTypography.current.label.copy(color = colors.accent),
                    modifier = Modifier.scale(pop.value),
                )
            StepState.FAILED ->
                BasicText("✕", style = LocalHainTypography.current.label.copy(color = colors.danger))
        }
    }
}

/** The pages an answer is based on; tap one to open it in the browser. */
@Composable
internal fun SourceChips(
    sources: List<SourceRef>,
    modifier: Modifier = Modifier,
) {
    if (sources.isEmpty()) return
    val context = LocalContext.current
    val colors = LocalHainColors.current
    Column(modifier.fillMaxWidth().padding(top = LocalHainSpacing.current.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                stringResource(R.string.chat_sources),
                style = LocalHainTypography.current.label.copy(color = colors.textMuted),
            )
        }
        FlowRow(
            Modifier.padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            sources.forEachIndexed { index, source ->
                HainChip(
                    text = "[${index + 1}] ${source.title}",
                    onClick = {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(source.url)))
                        }
                    },
                )
            }
        }
    }
}

private const val POP_START = 0.4f
