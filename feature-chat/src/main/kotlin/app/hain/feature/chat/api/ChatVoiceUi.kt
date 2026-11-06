@file:Suppress("TooManyFunctions") // mic button, setup cards, recording bar and read aloud: one small part each

package app.hain.feature.chat.api

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hain.feature.chat.R
import app.hain.models.api.DownloadState
import app.hain.models.api.ModelInfo
import app.hain.ui.HainIcons
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.MessageAction
import app.hain.ui.components.MessageActionKind
import app.hain.ui.components.ProgressBar
import app.hain.ui.components.PulsingDot
import app.hain.ui.motion.HainHaptics
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.voice.api.DictationError
import app.hain.voice.api.DictationState
import app.hain.voice.api.ReadAloudState
import app.hain.voice.api.SpeechLabels
import kotlinx.coroutines.delay

/** What the voice card above the message box asks for before the first dictation. */
internal enum class VoiceSetup { PERMISSION, PERMISSION_DENIED, PACK }

/** UI state of the voice parts of one chat screen (T-140): the setup card and the mic gestures. */
@Stable
internal class VoiceUi(
    val voice: ChatVoice,
    private val context: Context,
    private val haptics: HainHaptics,
) {
    var setup by mutableStateOf<VoiceSetup?>(null)
    var requestPermission: () -> Unit = {}

    val recording: Boolean get() = voice.dictation.state.value is DictationState.Recording

    private fun hasMic(): Boolean = context.checkSelfPermission(MIC) == PackageManager.PERMISSION_GRANTED

    /**
     * Starts a dictation when all is set; otherwise opens the card that explains what is missing (the
     * mic permission, asked only now, or the speech pack). Returns `true` when recording started.
     */
    fun tryStart(): Boolean {
        val pack = voice.sttPack() ?: return false
        return when {
            !voice.isInstalled(pack) -> {
                setup = VoiceSetup.PACK
                false
            }
            !hasMic() -> {
                setup = VoiceSetup.PERMISSION
                false
            }
            else -> {
                setup = null
                haptics.tick()
                voice.dictation.start(voice.language())
                true
            }
        }
    }

    fun finish() {
        haptics.tick()
        voice.dictation.finish()
    }

    fun cancel() = voice.dictation.cancel()

    /** The answer to the system's permission dialog. */
    fun onPermission(granted: Boolean) {
        // denied for good ("don't ask again"): only the phone settings can turn the mic on now
        val forGood = context.findActivity()?.shouldShowRequestPermissionRationale(MIC) == false
        setup = if (!granted && forGood) VoiceSetup.PERMISSION_DENIED else null
        if (granted) haptics.done()
    }

    fun openAppSettings() {
        val intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
        setup = null
    }
}

private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/** Remembers the voice UI; stops reading and frees the dictation engine when the app goes away. */
@Composable
internal fun rememberVoiceUi(voice: ChatVoice): VoiceUi {
    val context = LocalContext.current
    val haptics = rememberHainHaptics()
    val ui = remember(voice) { VoiceUi(voice, context, haptics) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ui.onPermission(it) }
    ui.requestPermission = { launcher.launch(Manifest.permission.RECORD_AUDIO) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, voice) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) voice.pause() }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            voice.readAloud.stop()
        }
    }
    return ui
}

/** Puts dictated [text] behind what is already in the message box (with one space between). */
internal fun appendDictation(
    input: String,
    text: String,
): String =
    when {
        text.isBlank() -> input
        input.isBlank() -> text.trim()
        input.last().isWhitespace() -> input + text.trim()
        else -> input + " " + text.trim()
    }

/** "0:07", "1:00": the dictation clock. */
internal fun dictationClock(ms: Long): String {
    val s = (ms / MS_PER_S).coerceAtLeast(0)
    return "%d:%02d".format(s / SEC_PER_MIN, s % SEC_PER_MIN)
}

/**
 * The mic in the message box (T-140). Tap starts and a second tap stops; press and hold records
 * until you let go, sliding off the button cancels. While it records the button turns green and
 * breathes with your voice (still with "reduce motion").
 */
@Suppress("LongMethod", "CyclomaticComplexMethod") // one gesture handler for tap, hold, slide-off and TalkBack
@Composable
internal fun MicButton(
    ui: VoiceUi,
    enabled: Boolean,
) {
    val colors = LocalHainColors.current
    val reduce = LocalReduceMotion.current
    val state by ui.voice.dictation.state
        .collectAsStateWithLifecycle()
    val recording = state is DictationState.Recording
    val level = (state as? DictationState.Recording)?.level ?: 0f
    val scale by animateFloatAsState(
        if (recording && !reduce) 1f + LEVEL_GROW * level else 1f,
        hainTween(HainMotion.FAST_MS, reduce),
        label = "mic-level",
    )
    val label = stringResource(if (recording) R.string.chat_mic_stop else R.string.chat_mic)
    val hint = stringResource(R.string.chat_mic_hint)
    val current by rememberUpdatedState(enabled)
    Box(
        Modifier
            .size(TOUCH.dp)
            .semantics {
                role = Role.Button
                contentDescription = if (recording) label else hint
                onClick(label) {
                    if (ui.recording) {
                        ui.finish()
                    } else if (current) {
                        ui.tryStart()
                    }
                    true
                }
            }.pointerInput(ui) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    when {
                        ui.recording -> {
                            waitForUpOrCancellation()
                            ui.finish() // second tap of tap mode
                        }
                        !current || ui.voice.dictation.busy || !ui.tryStart() -> waitForUpOrCancellation()
                        else -> {
                            val up = waitForUpOrCancellation()
                            val held = (up?.uptimeMillis ?: Long.MAX_VALUE) - down.uptimeMillis >= HOLD_MS
                            when {
                                up == null -> ui.cancel() // slid off the button
                                held -> ui.finish() // hold mode: let go = done
                                else -> Unit // tap mode: keeps recording until the next tap
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(VISUAL.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }.clip(CircleShape)
                .background(if (recording) colors.accent else colors.bg.copy(alpha = 0f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                HainIcons.mic,
                contentDescription = null,
                colorFilter = ColorFilter.tint(if (recording) colors.onAccent else colors.icon),
                modifier = Modifier.size(ICON.dp).graphicsLayer { alpha = if (enabled) 1f else DISABLED },
            )
        }
    }
}

/**
 * Everything voice shows above the message box: the setup card (mic permission, speech pack), the
 * recording bar (cancel, level, clock, done), "Pim schreibt mit…" and short error lines. Puts the
 * finished text into the box via [onInput], where you can still edit it before sending.
 */
@Composable
internal fun VoicePanel(
    ui: VoiceUi,
    input: String,
    onInput: (String) -> Unit,
) {
    val state by ui.voice.dictation.state
        .collectAsStateWithLifecycle()
    val haptics = rememberHainHaptics()
    val latestInput by rememberUpdatedState(input)
    LaunchedEffect(state) {
        when (val s = state) {
            is DictationState.Done -> {
                onInput(appendDictation(latestInput, s.text))
                haptics.done()
                ui.voice.dictation.consume()
            }
            is DictationState.Failed -> {
                haptics.error()
                delay(ERROR_MS)
                ui.voice.dictation.consume()
            }
            else -> Unit
        }
    }
    val reduce = LocalReduceMotion.current
    AnimatedVisibility(
        visible = ui.setup != null,
        enter = fadeIn(hainTween(HainMotion.FAST_MS, reduce)) + expandVertically(hainTween(HainMotion.FAST_MS, reduce)),
        exit = fadeOut(hainTween(HainMotion.FAST_MS, reduce)) + shrinkVertically(hainTween(HainMotion.FAST_MS, reduce)),
    ) {
        ui.setup?.let { SetupCard(ui, it) }
    }
    when (val s = state) {
        is DictationState.Recording -> RecordingBar(s, onCancel = ui::cancel, onDone = ui::finish)
        DictationState.Transcribing -> WritingLine()
        is DictationState.Failed -> VoiceLine(stringResource(errorText(s.reason)))
        else -> Unit
    }
}

private fun errorText(reason: DictationError): Int =
    when (reason) {
        DictationError.NO_SPEECH -> R.string.chat_dictation_no_speech
        DictationError.NO_MIC -> R.string.chat_dictation_no_mic
        DictationError.ENGINE -> R.string.chat_dictation_engine
    }

@Composable
private fun SetupCard(
    ui: VoiceUi,
    setup: VoiceSetup,
) {
    when (setup) {
        VoiceSetup.PERMISSION ->
            VoiceCard(stringResource(R.string.chat_mic_permission), onClose = { ui.setup = null }) {
                CardAction(stringResource(R.string.chat_mic_allow)) { ui.requestPermission() }
            }
        VoiceSetup.PERMISSION_DENIED ->
            VoiceCard(stringResource(R.string.chat_mic_denied), onClose = { ui.setup = null }) {
                CardAction(stringResource(R.string.chat_mic_open_settings), ui::openAppSettings)
            }
        VoiceSetup.PACK -> ui.voice.sttPack()?.let { PackCard(ui, it) }
    }
}

/** The speech pack: size, download with progress, retry; a short "ready" when it is there. */
@Composable
private fun PackCard(
    ui: VoiceUi,
    pack: ModelInfo,
) {
    val flow = remember(pack.id) { ui.voice.packState(pack) }
    val state by flow.collectAsStateWithLifecycle(DownloadState.Idle)
    val language =
        stringResource(
            if (pack.lang.firstOrNull() == "de") R.string.chat_voice_lang_de else R.string.chat_voice_lang_en,
        )
    val close = {
        ui.setup = null
    }
    when (val s = state) {
        DownloadState.Installed -> {
            LaunchedEffect(Unit) {
                delay(READY_MS)
                ui.setup = null
            }
            VoiceCard(stringResource(R.string.chat_voice_pack_ready), onClose = close)
        }
        is DownloadState.Running, DownloadState.Queued, DownloadState.Verifying -> {
            val fraction = (s as? DownloadState.Running)?.fraction ?: if (s == DownloadState.Verifying) 1f else 0f
            VoiceCard(
                stringResource(R.string.chat_voice_pack_loading, (fraction * PERCENT).toInt()),
                onClose = close,
                below = { ProgressBar(fraction, Modifier.padding(top = LocalHainSpacing.current.sm)) },
            )
        }
        // T-287: stopped (here or in the notification): resume where it stopped
        is DownloadState.Paused ->
            VoiceCard(
                stringResource(R.string.chat_voice_pack_paused, (s.fraction * PERCENT).toInt()),
                onClose = close,
                below = { ProgressBar(s.fraction, Modifier.padding(top = LocalHainSpacing.current.sm)) },
            ) {
                CardAction(stringResource(R.string.chat_voice_pack_resume)) { ui.voice.downloadPack(pack) }
            }
        is DownloadState.Failed ->
            VoiceCard(stringResource(R.string.chat_voice_pack_failed), onClose = close) {
                CardAction(stringResource(R.string.chat_voice_pack_retry)) { ui.voice.downloadPack(pack) }
            }
        DownloadState.Idle ->
            VoiceCard(stringResource(R.string.chat_voice_pack, language, megabytes(pack.totalSize)), onClose = close) {
                CardAction(stringResource(R.string.chat_voice_pack_get)) { ui.voice.downloadPack(pack) }
            }
    }
}

private fun megabytes(bytes: Long): String = "${(bytes + MB / 2) / MB} MB"

/** A soft green card like the brain banner: text, an optional action, close. */
@Composable
private fun VoiceCard(
    text: String,
    onClose: () -> Unit,
    below: @Composable () -> Unit = {},
    action: @Composable () -> Unit = {},
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    Column(
        Modifier
            .padding(horizontal = spacing.md, vertical = spacing.xs)
            .fillMaxWidth()
            .background(colors.accentSoft, LocalHainShapes.current.card)
            .padding(start = spacing.md, top = spacing.sm, bottom = spacing.sm, end = spacing.xs),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                HainIcons.mic,
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.accent),
                modifier = Modifier.size(SMALL_ICON.dp),
            )
            Spacer(Modifier.width(spacing.sm))
            BasicText(
                text,
                style = LocalHainTypography.current.bodySmall.copy(color = colors.text),
                modifier = Modifier.weight(1f),
            )
            action()
            HainIconButton(HainIcons.close, stringResource(R.string.chat_voice_close), onClose, tint = colors.textMuted)
        }
        Box(Modifier.padding(end = spacing.sm)) { below() }
    }
}

@Composable
private fun CardAction(
    text: String,
    onClick: () -> Unit,
) {
    val spacing = LocalHainSpacing.current
    BasicText(
        text,
        style = LocalHainTypography.current.label.copy(color = LocalHainColors.current.accent),
        modifier =
            Modifier
                .clip(LocalHainShapes.current.button)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = spacing.sm, vertical = spacing.md),
    )
}

/** While recording: cancel, the level bars, the clock and "Fertig" (T-140). */
@Composable
private fun RecordingBar(
    state: DictationState.Recording,
    onCancel: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    val listening = stringResource(R.string.chat_dictation_listening)
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.md)
            .semantics { contentDescription = listening },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        val cancel = stringResource(R.string.chat_dictation_cancel)
        HainIconButton(HainIcons.close, cancel, onCancel, tint = colors.textMuted)
        LevelBars(state.levels, Modifier.weight(1f).height(BARS_HEIGHT.dp))
        BasicText(dictationClock(state.elapsedMs), style = type.label.copy(color = colors.textMuted))
        BasicText(
            stringResource(R.string.chat_dictation_done),
            style = type.label.copy(color = colors.accent),
            modifier =
                Modifier
                    .clip(LocalHainShapes.current.button)
                    .clickable(role = Role.Button, onClick = onDone)
                    .padding(horizontal = spacing.md, vertical = spacing.md),
        )
    }
}

/** The last [levels] as rounded green bars, newest on the right. */
@Composable
private fun LevelBars(
    levels: List<Float>,
    modifier: Modifier = Modifier,
) {
    val color = LocalHainColors.current.accent
    val muted = LocalHainColors.current.border
    Canvas(modifier) {
        val slots = BAR_SLOTS
        val gap = size.width / slots
        val barWidth = (gap * BAR_FILL).coerceAtLeast(1f)
        val minHeight = size.height * MIN_BAR
        val padded = List((slots - levels.size).coerceAtLeast(0)) { 0f } + levels.takeLast(slots)
        padded.forEachIndexed { i, level ->
            val h = (minHeight + (size.height - minHeight) * level).coerceAtMost(size.height)
            drawRoundRect(
                color = if (level > 0f) color else muted,
                topLeft = Offset(i * gap + (gap - barWidth) / 2, (size.height - h) / 2),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2),
            )
        }
    }
}

@Composable
private fun WritingLine() {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    Row(
        Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulsingDot(colors.accent)
        Spacer(Modifier.width(LocalHainSpacing.current.sm))
        BasicText(
            stringResource(R.string.chat_dictation_writing),
            style = LocalHainTypography.current.bodySmall.copy(color = colors.textMuted),
        )
    }
}

@Composable
private fun VoiceLine(text: String) {
    val spacing = LocalHainSpacing.current
    BasicText(
        text,
        style = LocalHainTypography.current.bodySmall.copy(color = LocalHainColors.current.textMuted),
        modifier = Modifier.fillMaxWidth().padding(horizontal = spacing.lg, vertical = spacing.xs),
    )
}

/**
 * "Vorlesen" in the row under an answer (T-141): reads the answer sentence by sentence in Pim's
 * pitch and speed; the same button stops. Only there when the app gave the chat a voice.
 */
@Composable
internal fun ReadAloudAction(text: String) {
    val voice = LocalChatVoice.current ?: return
    val context = LocalContext.current
    val state by voice.readAloud.state.collectAsStateWithLifecycle()
    val key = remember(text) { "answer-" + text.hashCode() + "-" + text.length }
    val speaking = (state as? ReadAloudState.Speaking)?.key == key
    val labels =
        SpeechLabels(
            codeSkipped = stringResource(R.string.chat_read_code),
            formulaSkipped = stringResource(R.string.chat_read_formula),
        )
    val failed = stringResource(R.string.chat_read_failed)
    LaunchedEffect(state) {
        if ((state as? ReadAloudState.Failed)?.key == key) {
            Toast.makeText(context, failed, Toast.LENGTH_LONG).show()
            voice.readAloud.consume()
        }
    }
    MessageAction(
        MessageActionKind.SPEAK,
        stringResource(if (speaking) R.string.chat_read_stop else R.string.chat_read_aloud),
        onClick = { voice.readAloud.toggle(key, text, voice.speechLanguage(text), labels) },
        selected = speaking,
    )
}

private const val MIC = Manifest.permission.RECORD_AUDIO
private const val HOLD_MS = 350L
private const val ERROR_MS = 3_500L
private const val READY_MS = 2_500L
private const val MS_PER_S = 1000L
private const val SEC_PER_MIN = 60
private const val PERCENT = 100
private const val MB = 1_000_000L
private const val TOUCH = 48
private const val VISUAL = 40
private const val ICON = 20
private const val SMALL_ICON = 18
private const val LEVEL_GROW = 0.18f
private const val DISABLED = 0.4f
private const val BARS_HEIGHT = 28
private const val BAR_SLOTS = 24
private const val BAR_FILL = 0.55f
private const val MIN_BAR = 0.12f
