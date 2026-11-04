@file:Suppress("TooManyFunctions") // one small composable per part of the chat

package app.hain.feature.chat.api

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hain.cards.api.NewsCard
import app.hain.cards.api.NewsCardItem
import app.hain.common.api.AnswerMode
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.ActionEvent
import app.hain.feature.chat.internal.ChatError
import app.hain.feature.chat.internal.ChatUiState
import app.hain.feature.chat.internal.ChatViewModel
import app.hain.feature.chat.internal.Rating
import app.hain.feature.chat.internal.StepState
import app.hain.feature.chat.internal.UiMessage
import app.hain.llm.api.EngineState
import app.hain.llm.api.GenerationStats
import app.hain.tools.api.NewsHeadline
import app.hain.ui.HainIcons
import app.hain.ui.components.Composer
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.MessageAction
import app.hain.ui.components.MessageActionKind
import app.hain.ui.components.ProgressBar
import app.hain.ui.components.PulsingDot
import app.hain.ui.components.ReasoningBlock
import app.hain.ui.components.TopBar
import app.hain.ui.components.UserBubble
import app.hain.ui.effects.ThinkingGlow
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.HainMotionMoment
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.motion.motionSpec
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Chat screen (T-052) with the chat drawer (T-053, basic). */
@Suppress("LongMethod", "LongParameterList") // screen scaffold: top bar, list, composer, drawer
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onGetBrain: () -> Unit = {},
    answerMode: AnswerMode = AnswerMode.AUTO,
    onAnswerMode: (AnswerMode) -> Unit = {},
    onOpenGame: () -> Unit = {},
    extras: ChatExtras = ChatExtras(),
    banner: @Composable () -> Unit = {},
    onOpenNews: (() -> Unit)? = null,
    brainPicker: BrainPickerState? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val colors = LocalHainColors.current
    val haptics = rememberHainHaptics()

    BackHandler(enabled = drawerOpen) { drawerOpen = false }
    // T-096: a soft double tick when an answer is done, a short "no" on an error; never while streaming
    var wasGenerating by remember { mutableStateOf(false) }
    LaunchedEffect(state.isGenerating, state.error) {
        if (wasGenerating && !state.isGenerating) {
            if (state.error != null && state.error != ChatError.NO_BRAIN) haptics.error() else haptics.done()
        }
        wasGenerating = state.isGenerating
    }

    Box(modifier.fillMaxSize().background(colors.bg)) {
        val chatScale by animateFloatAsState(
            if (drawerOpen) OPEN_SCALE else 1f,
            motionSpec(HainMotionMoment.DRAWER),
            label = "chat-scale",
        )
        ThinkingGlow(visible = state.isGenerating, modifier = Modifier.fillMaxSize())
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = chatScale
                    scaleY = chatScale
                }.statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
            // T-055: tapping "Hain · Auf diesem Handy" opens the brain picker
            var pickerOpen by rememberSaveable { mutableStateOf(false) }
            TopBar(
                title = stringResource(R.string.chat_title),
                subtitle = engineLine(state),
                onTitleClick = brainPicker?.let { { pickerOpen = !pickerOpen } },
                titleClickLabel = stringResource(R.string.chat_brain_picker_open),
                titleAccessory = {
                    if (brainPicker != null) ModelPicker(pickerOpen, { pickerOpen = false }, brainPicker)
                },
                leading = {
                    HainIconButton(HainIcons.menu, stringResource(R.string.chat_menu), { drawerOpen = true })
                },
                trailing = {
                    if (state.online) OnlineDot()
                    HainIconButton(HainIcons.plus, stringResource(R.string.chat_new), viewModel::newChat)
                },
            )
            BrainLoadBar(state)
            var bannerHidden by rememberSaveable { mutableStateOf(false) }
            if (!state.brainInstalled && !bannerHidden) {
                BrainBanner(onGet = onGetBrain, onLater = { bannerHidden = true })
            }
            // T-212: quiet update banner or the one-time "check for updates?" card (from the app)
            banner()
            Box(Modifier.weight(1f).fillMaxWidth()) {
                val pageIn = fadeIn(hainTween(HainMotion.PAGE_MS))
                val pageSlide = slideInVertically(hainTween(HainMotion.PAGE_MS)) { it / SLIDE_DIV }
                val pageOut = fadeOut(hainTween(HainMotion.FAST_MS))
                AnimatedContent(
                    targetState = state.messages.isEmpty(),
                    transitionSpec = { (pageIn + pageSlide).togetherWith(pageOut) },
                    label = "chat-start",
                ) { empty ->
                    if (empty) {
                        Greeting(onSuggestion = { viewModel.onInputChange(it) }, onEgg = onOpenGame)
                    } else {
                        val ratings by viewModel.ratings.collectAsStateWithLifecycle()
                        // T-131: checklist ticks are saved, card buttons can ask the next question
                        val cards = remember(viewModel) { CardHandler(viewModel::updateCards, viewModel::ask) }
                        CompositionLocalProvider(LocalCardHandler provides cards) {
                            MessageList(
                                state,
                                ratings = ratings,
                                onRetry = { viewModel.regenerate(it) },
                                onEdit = viewModel::startEdit,
                                onRate = viewModel::rate,
                                onAction = viewModel::resolveAction,
                                extras = extras,
                            )
                        }
                    }
                }
            }
            ErrorLine(state, onGetBrain)
            if (state.editingId != null) EditBar(onCancel = viewModel::cancelEdit)
            Composer(
                text = state.input,
                onTextChange = viewModel::onInputChange,
                placeholder = stringResource(R.string.chat_placeholder),
                isGenerating = state.isGenerating,
                onSend = {
                    haptics.send()
                    viewModel.send()
                },
                onStop = viewModel::stop,
                sendLabel = stringResource(R.string.chat_send),
                stopLabel = stringResource(R.string.chat_stop),
                enabled = state.engineAvailable,
                modifier = Modifier.padding(LocalHainSpacing.current.md),
                // T-084: the thinking level lives inside the message box (brain + popover)
                leading = { ThinkingPicker(answerMode, onAnswerMode) },
            )
        }
        ChatListDrawer(
            open = drawerOpen,
            hasChats = state.chats.isNotEmpty(),
            groups = state.groups,
            searchQuery = state.searchQuery,
            currentChatId = state.currentChatId,
            actions =
                DrawerActions(
                    onClose = { drawerOpen = false },
                    onNewChat = {
                        viewModel.newChat()
                        drawerOpen = false
                    },
                    onOpen = {
                        viewModel.openChat(it)
                        drawerOpen = false
                    },
                    onDelete = viewModel::deleteChat,
                    onRename = viewModel::renameChat,
                    onPin = viewModel::setPinned,
                    onSearch = viewModel::onSearchChange,
                    onSettings = {
                        drawerOpen = false
                        onOpenSettings()
                    },
                    onNews =
                        onOpenNews?.let { open ->
                            {
                                drawerOpen = false
                                open()
                            }
                        },
                ),
        )
    }
}

/** Orange point while Hain is online (T-069); the network log in Privacy shows every request. */
@Composable
private fun OnlineDot() {
    val colors = LocalHainColors.current
    val hint = stringResource(R.string.chat_online_hint)
    Row(
        Modifier.padding(end = 4.dp).semantics { contentDescription = hint },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PulsingDot(colors.online)
        Spacer(Modifier.width(4.dp))
        BasicText(
            stringResource(R.string.chat_online),
            style = LocalHainTypography.current.label.copy(color = colors.online),
        )
    }
}

/** T-055: thin progress line under the top bar while a brain loads in the background. */
@Composable
private fun BrainLoadBar(state: ChatUiState) {
    val loading = state.engine as? EngineState.Loading
    AnimatedVisibility(
        visible = loading != null && loading.progress in 0f..1f,
        enter = fadeIn(hainTween(HainMotion.FAST_MS)),
        exit = fadeOut(hainTween(HainMotion.FAST_MS)),
    ) {
        ProgressBar(
            fraction = loading?.progress ?: 1f,
            modifier = Modifier.padding(horizontal = LocalHainSpacing.current.lg).height(2.dp),
        )
    }
}

@Composable
private fun engineLine(state: ChatUiState): String? =
    when {
        !state.engineAvailable -> stringResource(R.string.chat_error_no_native)
        !state.brainInstalled -> stringResource(R.string.chat_brain_missing)
        state.engine is EngineState.Loading -> {
            val p = (state.engine as EngineState.Loading).progress
            if (p in 0f..1f) {
                stringResource(R.string.chat_loading_brain_progress, (p * PERCENT).toInt())
            } else {
                stringResource(R.string.chat_loading_brain)
            }
        }
        state.engine is EngineState.Ready -> stringResource(R.string.chat_brain_ready)
        else -> null
    }

@Suppress("LongMethod", "LongParameterList") // list, follow logic, jump-down button; one callback per action
@Composable
internal fun MessageList(
    state: ChatUiState,
    ratings: Map<Long, Rating>,
    onRetry: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onRate: (Long, Rating) -> Unit,
    onAction: (Long, Int, ActionEvent) -> Unit,
    extras: ChatExtras = ChatExtras(),
) {
    val spacing = LocalHainSpacing.current
    val listState = rememberLazyListState()
    // the one own message whose actions are showing (tap again or tap another to switch)
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    val last = state.messages.lastOrNull()
    // T-085: follow the streaming answer only while you are at the bottom. Scrolling up to read stops
    // the follow (it used to yank you back down with every new word); back at the bottom it resumes.
    var follow by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to listState.canScrollForward }
            .collect { (moving, more) -> if (moving) follow = !more }
    }
    LaunchedEffect(state.messages.size) {
        follow = true
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.lastIndex, Int.MAX_VALUE / 2)
    }
    LaunchedEffect(last?.text?.length, last?.reasoning?.length, last?.steps?.size) {
        if (follow && state.messages.isNotEmpty()) {
            listState.scrollToItem(state.messages.lastIndex, Int.MAX_VALUE / 2)
        }
    }
    // T-089: the answer that just finished gets a happy Pim for two seconds (not after an error or stop)
    var cheerId by remember { mutableStateOf<Long?>(null) }
    var wasGenerating by remember { mutableStateOf(state.isGenerating) }
    LaunchedEffect(state.isGenerating) {
        val done = wasGenerating && !state.isGenerating
        wasGenerating = state.isGenerating
        val answer = state.messages.lastOrNull()
        val finished = answer != null && !answer.isUser && !answer.stopped
        if (done && finished && state.error == null) {
            cheerId = answer.id
            delay(CHEER_MS)
            cheerId = null
        }
    }
    val scope = rememberCoroutineScope()
    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding =
                androidx.compose.foundation.layout
                    .PaddingValues(spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            items(state.messages, key = { it.id }) { message ->
                if (message.isUser) {
                    UserMessage(
                        message,
                        open = openId == message.id,
                        onTap = { openId = if (openId == message.id) null else message.id },
                        canEdit = !state.isGenerating,
                        onEdit = {
                            openId = null
                            onEdit(message.id)
                        },
                    )
                } else {
                    Answer(
                        message,
                        cheer = cheerId == message.id,
                        latest = message.id == last?.id,
                        extras = extras,
                        rating = ratings[message.id],
                        canRetry = !state.isGenerating,
                        onRetry = { onRetry(message.id) },
                        onRate = { onRate(message.id, it) },
                        onAction = { index, event -> onAction(message.id, index, event) },
                    )
                }
            }
        }
        ScrollDownButton(
            visible = !follow && listState.canScrollForward,
            onClick = {
                follow = true
                scope.launch { listState.animateScrollToItem(state.messages.lastIndex, Int.MAX_VALUE / 2) }
            },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = spacing.sm),
        )
    }
}

/** Small round glass button that brings you back to the newest message (T-085). */
@Composable
private fun ScrollDownButton(
    visible: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduce = LocalReduceMotion.current
    AnimatedVisibility(
        visible,
        modifier = modifier,
        enter =
            fadeIn(hainTween(HainMotion.PILL_FADE_MS, reduce)) +
                slideInVertically(hainTween(HainMotion.PILL_FADE_MS, reduce)) { it / 2 },
        exit = fadeOut(hainTween(HainMotion.PILL_FADE_MS, reduce)),
    ) {
        Box(
            Modifier
                .size(40.dp)
                .hainSurface(HainLevel.FLOATING, CircleShape)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                HainIcons.chevronRight,
                contentDescription = stringResource(R.string.chat_scroll_down),
                colorFilter = ColorFilter.tint(LocalHainColors.current.text),
                modifier = Modifier.size(20.dp).rotate(QUARTER_TURN),
            )
        }
    }
}

/** Own message: a short tap shows edit and copy below it, on the right (T-057, like Gemini). */
@Composable
private fun UserMessage(
    message: UiMessage,
    open: Boolean,
    onTap: () -> Unit,
    canEdit: Boolean,
    onEdit: () -> Unit,
) {
    UserBubble(
        message.text,
        actionsVisible = open,
        onTap = onTap,
        moreLabel = stringResource(R.string.chat_show_more),
        lessLabel = stringResource(R.string.chat_show_less),
        actions = {
            if (canEdit) MessageAction(MessageActionKind.EDIT, stringResource(R.string.chat_edit), onEdit)
            CopyAction(message.text)
        },
    )
}

/** Copy, then a short green check so you know it worked. */
@Composable
private fun CopyAction(text: String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_MS)
            copied = false
        }
    }
    MessageAction(
        MessageActionKind.COPY,
        stringResource(if (copied) R.string.chat_copied else R.string.chat_copy),
        onClick = {
            clipboard.setText(AnnotatedString(text))
            copied = true
        },
        done = copied,
    )
}

@Suppress("LongParameterList", "LongMethod", "CyclomaticComplexMethod") // one callback per answer action
@Composable
private fun Answer(
    message: UiMessage,
    rating: Rating?,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onRate: (Rating) -> Unit,
    onAction: (Int, ActionEvent) -> Unit = { _, _ -> },
    cheer: Boolean = false,
    latest: Boolean = false,
    extras: ChatExtras = ChatExtras(),
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Column(Modifier.fillMaxWidth()) {
        val waiting = message.text.isEmpty() && message.reasoning == null && message.steps.isEmpty()
        // true once this answer was seen while it was still being written (4b-2)
        var fresh by remember(message.id) { mutableStateOf(message.streaming) }
        LaunchedEffect(message.id, message.streaming) { if (message.streaming) fresh = true }
        // T-213: a small Pim next to the newest answer; he peeks in while tools run, cheers when done
        if (latest && !(message.streaming && waiting)) AnswerPim(message, cheer)
        ToolTimeline(message.steps, research = message.research.takeIf { message.streaming })
        val reasoning = message.reasoning
        if (reasoning != null) {
            val title =
                message.thinkingSeconds?.let { stringResource(R.string.chat_thought_for, it) }
                    ?: stringResource(R.string.chat_thinking)
            // T-211: developer mode can open the raw thinking right away
            ReasoningBlock(title = title, reasoning = reasoning, initiallyExpanded = extras.rawThinking)
        }
        if (message.streaming && waiting) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // T-089/T-213: a real Pim, 40 dp, the leaf wiggling while he thinks
                LivePim(PimState.THINKING, THINKING_PIM.dp, tappable = false)
                Spacer(Modifier.width(12.dp))
                BasicText(stringResource(R.string.chat_thinking), style = type.bodySmall.copy(color = colors.textMuted))
            }
        } else {
            // T-131: answer text with its cards
            AnswerBody(
                message,
                actions =
                    if (message.streaming) {
                        null
                    } else {
                        { AnswerActions(message.plainAnswer(), rating, canRetry, onRetry, onRate) }
                    },
            )
            if (!message.streaming) {
                // 4b-2: without confirmation only an answer that was written just now starts by itself,
                // never one of an older chat that is opened again
                val autoStart = !extras.confirmActions && fresh
                message.actions.forEachIndexed { index, card ->
                    ConfirmCard(card, { onAction(index, it) }, autoStart = autoStart)
                }
                // 3b-2: what the weather or Wikipedia tool found, as a card
                message.cards.take(MAX_INFO_CARDS).forEach { InfoCardView(it) }
                // T-072: headlines of the news tool
                if (message.news.isNotEmpty()) AnswerNews(message.news)
                SourceChips(message.sources)
            }
            if (message.stopped) {
                BasicText(stringResource(R.string.chat_stopped), style = type.label.copy(color = colors.textMuted))
            }
            // T-211: speed under the answer in developer mode
            val stats = message.stats
            if (extras.showStats && stats != null && !message.streaming) AnswerStats(stats, extras.debugInfo(stats))
        }
    }
}

/**
 * T-213: Pim, 24 dp, at the newest answer. While tools run he peeks in from the side (searching);
 * when the answer is done he is happy for a moment ([cheer]); on a failed answer he gets a fright.
 * With "reduce motion" he simply sits there in his mood.
 */
@Composable
private fun AnswerPim(
    message: UiMessage,
    cheer: Boolean,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val reduce = LocalReduceMotion.current
    val tooling = message.streaming && message.text.isEmpty() && message.steps.any { it.state == StepState.RUNNING }
    val mood = answerPimMood(message, cheer, tooling)
    val peek by animateFloatAsState(
        if (tooling && !reduce) 1f else 0f,
        hainTween(HainMotion.PAGE_MS, reduce),
        label = "pim-peek",
    )
    Row(
        Modifier.padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LivePim(
            mood,
            AVATAR_PIM.dp,
            Modifier.graphicsLayer {
                // a calm peek: leans in from the left and tilts a little
                translationX = if (tooling && !reduce) -PEEK_SHIFT * size.width * (1f - peek) else 0f
                rotationZ = PEEK_TILT * peek
            },
            tappable = false,
        )
        if (tooling) {
            Spacer(Modifier.width(8.dp))
            BasicText(stringResource(R.string.chat_pim_looking), style = type.label.copy(color = colors.textMuted))
        }
    }
}

/** Pim's mood at the newest answer (T-213): fright on failure, joy when done, thinking while busy. */
internal fun answerPimMood(
    message: UiMessage,
    cheer: Boolean,
    tooling: Boolean,
): PimState =
    when {
        message.failed -> PimState.ERROR
        cheer -> PimState.HAPPY
        tooling || message.streaming -> PimState.THINKING
        else -> PimState.IDLE
    }

/** T-211: "21.4 Token/s · erstes Token nach 0.8 s" and a button that copies the debug line. */
@Composable
private fun AnswerStats(
    stats: GenerationStats,
    debug: String,
) {
    val colors = LocalHainColors.current
    val clipboard = LocalClipboardManager.current
    val line =
        stringResource(
            R.string.chat_dev_stats,
            "%.1f".format(stats.tokensPerSecond),
            "%.1f".format(stats.firstTokenMs / MS_PER_S),
        )
    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        BasicText(line, style = LocalHainTypography.current.label.copy(color = colors.textMuted))
        Spacer(Modifier.width(LocalHainSpacing.current.sm))
        BasicText(
            stringResource(R.string.chat_dev_copy_debug),
            style = LocalHainTypography.current.label.copy(color = colors.accent),
            modifier =
                Modifier
                    .clickable(role = Role.Button) { clipboard.setText(AnnotatedString(debug)) }
                    .padding(vertical = 4.dp),
        )
    }
}

/**
 * The quiet row under a finished answer (T-057): copy, share, thumbs, ask again — evenly spaced,
 * grey. "Read aloud" joins here once Hain can speak (Häppchen 10).
 */
@Composable
private fun AnswerActions(
    text: String,
    rating: Rating?,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onRate: (Rating) -> Unit,
) {
    val context = LocalContext.current
    val shareLabel = stringResource(R.string.chat_share)
    CopyAction(text)
    MessageAction(MessageActionKind.SHARE, shareLabel, onClick = {
        val send =
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text)
        context.startActivity(Intent.createChooser(send, shareLabel))
    })
    MessageAction(
        MessageActionKind.THUMB_UP,
        stringResource(R.string.chat_good_answer),
        onClick = { onRate(Rating.UP) },
        selected = rating == Rating.UP,
    )
    MessageAction(
        MessageActionKind.THUMB_DOWN,
        stringResource(R.string.chat_bad_answer),
        onClick = { onRate(Rating.DOWN) },
        selected = rating == Rating.DOWN,
    )
    MessageAction(MessageActionKind.RETRY, stringResource(R.string.chat_retry), onRetry, enabled = canRetry)
}

@Composable
private fun ErrorLine(
    state: ChatUiState,
    onGetBrain: () -> Unit,
) {
    val error = state.error ?: return
    val colors = LocalHainColors.current
    if (error == ChatError.NO_BRAIN) {
        // a friendly hint, not an error: Pim simply has no brain yet
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onGetBrain)
                .padding(horizontal = LocalHainSpacing.current.lg, vertical = LocalHainSpacing.current.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BasicText(
                stringResource(R.string.chat_no_brain_hint),
                style = LocalHainTypography.current.bodySmall.copy(color = colors.textMuted),
                modifier = Modifier.weight(1f),
            )
            BasicText(
                stringResource(R.string.chat_brain_get),
                style = LocalHainTypography.current.label.copy(color = colors.accent),
            )
        }
        return
    }
    val text =
        when (error) {
            ChatError.MODEL_LOAD -> stringResource(R.string.chat_error_load)
            ChatError.CONTEXT_FULL -> stringResource(R.string.chat_error_context)
            else -> stringResource(R.string.chat_error_generation)
        }
    // T-089: Pim gets a fright next to the error
    Row(
        Modifier.padding(horizontal = LocalHainSpacing.current.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LivePim(PimState.ERROR, ERROR_PIM.dp, tappable = false)
        Spacer(Modifier.width(LocalHainSpacing.current.sm))
        BasicText(text, style = LocalHainTypography.current.bodySmall.copy(color = colors.danger))
    }
}

/** Quiet reminder while the brain is missing: one line, a button, "later" hides it until next start. */
@Composable
private fun BrainBanner(
    onGet: () -> Unit,
    onLater: () -> Unit,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val type = LocalHainTypography.current
    Row(
        Modifier
            .padding(horizontal = spacing.md)
            .fillMaxWidth()
            .background(colors.accentSoft, LocalHainShapes.current.card)
            .padding(start = spacing.md, top = spacing.sm, bottom = spacing.sm, end = spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LivePim(PimState.SLEEPING, 32.dp, tappable = false)
        Spacer(Modifier.width(spacing.sm))
        BasicText(
            stringResource(R.string.chat_brain_banner),
            style = type.bodySmall.copy(color = colors.text),
            modifier = Modifier.weight(1f),
        )
        BasicText(
            stringResource(R.string.chat_brain_get),
            style = type.label.copy(color = colors.accent),
            modifier =
                Modifier
                    .clickable(onClick = onGet)
                    .padding(horizontal = spacing.sm, vertical = spacing.sm),
        )
        HainIconButton(HainIcons.close, stringResource(R.string.chat_brain_later), onLater, tint = colors.textMuted)
    }
}

/** Shown above the composer while a sent message is being edited. */
@Composable
private fun EditBar(onCancel: () -> Unit) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    Row(
        Modifier.fillMaxWidth().padding(start = spacing.lg, end = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            HainIcons.edit,
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.textMuted),
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(spacing.sm))
        BasicText(
            stringResource(R.string.chat_editing),
            style = LocalHainTypography.current.label.copy(color = colors.textMuted),
            modifier = Modifier.weight(1f),
        )
        HainIconButton(HainIcons.close, stringResource(R.string.chat_edit_cancel), onCancel, tint = colors.textMuted)
    }
}

private const val OPEN_SCALE = 0.94f
private const val QUARTER_TURN = 90f
private const val PERCENT = 100
private const val COPIED_MS = 1500L

private const val SLIDE_DIV = 14
private const val CHEER_MS = 2_200L
private const val ERROR_PIM = 32
private const val THINKING_PIM = 40
private const val AVATAR_PIM = 24
private const val PEEK_SHIFT = 0.6f
private const val PEEK_TILT = -8f
private const val MS_PER_S = 1000f

private const val MAX_INFO_CARDS = 3

/** T-072: the headlines of the `news` tool as a card; a tap opens the article in the browser. */
@Composable
private fun AnswerNews(news: List<NewsHeadline>) {
    val uri = LocalUriHandler.current
    NewsCard(
        items = news.map { NewsCardItem(it.source, it.title, it.url, it.publishedAt, it.paywall) },
        onOpen = { url -> runCatching { uri.openUri(url) } },
    )
}
