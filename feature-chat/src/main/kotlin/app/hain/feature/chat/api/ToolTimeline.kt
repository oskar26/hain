package app.hain.feature.chat.api

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.SourceRef
import app.hain.feature.chat.internal.StepState
import app.hain.feature.chat.internal.ToolStep
import app.hain.ui.components.HainChip
import app.hain.ui.components.PulsingDot
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/**
 * What Pim did for an answer (T-059): one chip per tool call — pulsing while it runs, a ✓ that pops
 * in when done, ✕ when it failed — and an orange dot when it went online.
 */
@Composable
internal fun ToolTimeline(
    steps: List<ToolStep>,
    modifier: Modifier = Modifier,
) {
    if (steps.isEmpty()) return
    FlowRow(
        modifier.fillMaxWidth().padding(bottom = LocalHainSpacing.current.sm),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        steps.forEach { ToolChip(it) }
    }
}

@Composable
private fun toolText(step: ToolStep): String {
    val name =
        when (step.name) {
            "calculate" -> stringResource(R.string.chat_tool_calculate)
            "weather" -> stringResource(R.string.chat_tool_weather)
            "wikipedia" -> stringResource(R.string.chat_tool_wikipedia)
            else -> stringResource(R.string.chat_tool_other)
        }
    val tail =
        when {
            step.state == StepState.RUNNING && step.detail == null -> stringResource(R.string.chat_tool_running)
            step.state == StepState.FAILED && step.error == "offline" -> stringResource(R.string.chat_tool_offline)
            step.state == StepState.FAILED -> stringResource(R.string.chat_tool_failed)
            else -> step.detail
        }
    return if (tail.isNullOrBlank()) name else "$name · $tail"
}

@Composable
private fun ToolChip(step: ToolStep) {
    val colors = LocalHainColors.current
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
    HainChip(
        text = text,
        dot = if (step.online && step.state != StepState.RUNNING) colors.online else null,
        leading = {
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
        },
        modifier = Modifier.semantics { contentDescription = text },
    )
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
