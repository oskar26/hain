@file:Suppress("TooManyFunctions") // one small composable per part of the chat

package app.hain.feature.chat.api

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hain.cards.api.NewsCard
import app.hain.cards.api.NewsCardItem
import app.hain.common.api.AnswerMode
import app.hain.feature.chat.R
import app.hain.feature.chat.export.ExportButton
import app.hain.feature.chat.export.rememberChatExport
import app.hain.feature.chat.internal.ActionEvent
import app.hain.feature.chat.internal.ChatError
import app.hain.feature.chat.internal.ChatUiState
import app.hain.feature.chat.internal.ChatViewModel
import app.hain.feature.chat.internal.Rating
import app.hain.feature.chat.internal.UiMessage
import app.hain.llm.api.EngineState
import app.hain.llm.api.GenerationStats
import app.hain.tools.api.NewsHeadline
import app.hain.ui.HainIcons
import app.hain.ui.LocalExternalLaunch
import app.hain.ui.components.Composer
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.LocalMarkdownLabels
import app.hain.ui.components.MarkdownLabels
import app.hain.ui.components.MessageAction
import app.hain.ui.components.MessageActionKind
import app.hain.ui.components.ProgressBar
import app.hain.ui.components.PulsingDot
import app.hain.ui.components.TopBar
import app.hain.ui.components.UserBubble
import app.hain.ui.effects.hainGlassSource
import app.hain.ui.effects.rememberHainGlassState
import app.hain.ui.layout.currentHainWindowSize
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.HainMotionMoment
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.motion.motionSpec
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.openExternally
import app.hain.ui.pim.LivePim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.voice.api.DictationState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Chat screen (T-052) with the chat drawer (T-053, basic). */
@Suppress("LongMethod", "LongParameterList", "CyclomaticComplexMethod") // scaffold: bar, list, composer, drawer
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
    /** Counts up when the message box should get the keyboard (widget "Frag Pim", T-144). */
    focusTick: Int = 0,
    voice: ChatVoice? = null,
    /** Häppchen 9: attach files (paper clip, "Meine Dateien"); `null` = no files. */
    docs: ChatDocs? = null,
    /** T-253: `content://` address of a file from the share sheet, read once like the paper clip. */
    sharedFile: String? = null,
    onSharedFileTaken: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Häppchen 10: dictation in the message box, read aloud under the answers (null = no voice)
    val voiceUi = voice?.let { rememberVoiceUi(it) }
    val composerFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(focusTick) {
        if (focusTick > 0) {
            delay(FOCUS_DELAY_MS) // the window has to have focus first (fresh start from widget or tile)
            runCatching { composerFocus.requestFocus() }
            keyboard?.show()
        }
    }
    // T-058: your own message always scrolls the chat down
    var sendTick by remember { mutableIntStateOf(0) }
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var showFiles by rememberSaveable { mutableStateOf(false) }
    val picker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) docs?.open(uri, privateChat = state.privateChat) { viewModel.attach(it) }
        }
    // T-253: a file shared into Hain goes the paper clip's way (progress line, text recognition question)
    LaunchedEffect(sharedFile, docs) {
        val file = sharedFile ?: return@LaunchedEffect
        docs?.open(Uri.parse(file), privateChat = state.privateChat) { viewModel.attach(it) }
        onSharedFileTaken()
    }
    // T-056: files of a private chat live only as long as the chat
    LaunchedEffect(state.privateChat) { if (!state.privateChat) docs?.forgetPrivate() }
    val colors = LocalHainColors.current
    val haptics = rememberHainHaptics()
    // T-054: export of one chat; T-056: private chats are never exported
    val export = rememberChatExport { id -> state.chats.firstOrNull { it.id == id } }

    // T-235: from 840 dp on the chat list stays open on the left instead of sliding in
    val wide = currentHainWindowSize().isExpanded
    LaunchedEffect(wide) { if (wide) drawerOpen = false }
    BackHandler(enabled = drawerOpen) { drawerOpen = false }
    // T-096: a soft double tick when an answer is done, a short "no" on an error; never while streaming
    var wasGenerating by remember { mutableStateOf(false) }
    LaunchedEffect(state.isGenerating, state.error) {
        if (wasGenerating && !state.isGenerating) {
            if (state.error != null && state.error != ChatError.NO_BRAIN) haptics.error() else haptics.done()
        }
        wasGenerating = state.isGenerating
    }

    val drawerActions =
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
            onExport = { id, format, share ->
                state.chats.firstOrNull { it.id == id }?.let { chat ->
                    if (share) export.share(chat, format) else export.save(chat, format)
                }
            },
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
        )

    Box(modifier.fillMaxSize().background(colors.bg)) {
        if (wide) {
            ChatSidePanel(state.chats.isNotEmpty(), state.groups, state.searchQuery, state.currentChatId, drawerActions)
        }
        val chatScale by animateFloatAsState(
            if (drawerOpen) OPEN_SCALE else 1f,
            motionSpec(HainMotionMoment.DRAWER),
            label = "chat-scale",
        )
        // T-230/T-232: top bar and message box are frosted glass over the chat, which scrolls under them
        val chrome = rememberHainGlassState()
        // T-055: tapping "Hain · Auf diesem Handy" opens the brain picker
        var pickerOpen by rememberSaveable { mutableStateOf(false) }
        var bannerHidden by rememberSaveable { mutableStateOf(false) }
        ChatChrome(
            glass = chrome,
            thinking = state.isGenerating,
            modifier =
                Modifier.padding(start = if (wide) ChatSidePanelWidth else 0.dp).graphicsLayer {
                    scaleX = chatScale
                    scaleY = chatScale
                },
            top = {
                ChatTopGlass(chrome) {
                    TopBar(
                        title =
                            stringResource(if (state.privateChat) R.string.chat_private_title else R.string.chat_title),
                        subtitle = engineLine(state),
                        onTitleClick = brainPicker?.let { { pickerOpen = !pickerOpen } },
                        titleClickLabel = stringResource(R.string.chat_brain_picker_open),
                        titleAccessory = {
                            if (brainPicker != null) ModelPicker(pickerOpen, { pickerOpen = false }, brainPicker)
                        },
                        // T-230: the online dot sits quietly in front of "Auf diesem Handy"
                        subtitleLeading = { if (state.online) OnlineDot() },
                        leading = {
                            val menu = stringResource(R.string.chat_menu)
                            if (!wide) HainIconButton(HainIcons.menu, menu, { drawerOpen = true })
                        },
                        trailing = {
                            val openChat = state.chats.firstOrNull { it.id == state.currentChatId }
                            if (openChat != null) {
                                ExportButton(openChat, export)
                            } else {
                                PrivateChatButton(state.privateChat, viewModel::togglePrivateChat)
                            }
                            HainIconButton(HainIcons.plus, stringResource(R.string.chat_new), viewModel::newChat)
                        },
                    )
                    BrainLoadBar(state)
                    if (state.privateChat) PrivateChatBanner(onEnd = viewModel::newChat)
                    if (!state.brainInstalled && !bannerHidden) {
                        BrainBanner(onGet = onGetBrain, onLater = { bannerHidden = true })
                    }
                    // T-212: quiet update banner or the one-time "check for updates?" card (from the app)
                    banner()
                }
            },
            bottom = {
                ChatBottomArea {
                    // T-230: the lines above the message box share one frosted card (nothing when empty)
                    ChatBottomCard(chrome) {
                        ErrorLine(state, onGetBrain)
                        // T-057: messages sent while Pim answers wait here ("wartet", ✕ takes one back)
                        if (state.queue.isNotEmpty()) QueuedMessages(state.queue, viewModel::cancelQueued)
                        if (state.editingId != null) EditBar(onCancel = viewModel::cancelEdit)
                        voiceUi?.let { VoicePanel(it, state.input, viewModel::onInputChange) }
                        if (docs != null) {
                            val docState by docs.state.collectAsStateWithLifecycle()
                            DocsBar(
                                docState,
                                state.attachments,
                                docs.packMegabytes,
                                onAnswer = docs::answer,
                                onDismiss = docs::dismissError,
                                onRemove = { viewModel.detach(it.id) },
                            )
                        }
                    }
                    // T-296: while Pim listens (tap mode) the message box morphs into the dictation pill
                    val dictation =
                        voiceUi
                            ?.voice
                            ?.dictation
                            ?.state
                            ?.collectAsStateWithLifecycle()
                    val recording = dictation?.value as? DictationState.Recording
                    val pill = recording != null && voiceUi?.holding == false
                    val reduce = LocalReduceMotion.current
                    AnimatedContent(
                        targetState = pill,
                        transitionSpec = {
                            val enter = fadeIn(hainTween(HainMotion.BASE_MS, reduce))
                            val exit = fadeOut(hainTween(HainMotion.FAST_MS, reduce))
                            (enter togetherWith exit).using(
                                SizeTransform(clip = false) { _, _ -> hainTween(HainMotion.BASE_MS, reduce) },
                            )
                        },
                        contentAlignment = Alignment.BottomCenter,
                        label = "composer-pill",
                    ) { showPill ->
                        val rec = dictation?.value as? DictationState.Recording
                        if (showPill && rec != null && voiceUi != null) {
                            DictationPill(
                                rec,
                                onCancel = voiceUi::cancel,
                                onDone = voiceUi::finish,
                                modifier = Modifier.padding(LocalHainSpacing.current.md),
                            )
                        } else {
                            Composer(
                                text = state.input,
                                onTextChange = viewModel::onInputChange,
                                placeholder = stringResource(R.string.chat_placeholder),
                                isGenerating = state.isGenerating,
                                onSend = {
                                    haptics.send()
                                    if (!state.isGenerating) sendTick++
                                    viewModel.send()
                                },
                                canQueue = state.editingId == null,
                                queueLabel = stringResource(R.string.chat_send_queue),
                                focusRequester = composerFocus,
                                onStop = viewModel::stop,
                                sendLabel = stringResource(R.string.chat_send),
                                stopLabel = stringResource(R.string.chat_stop),
                                enabled = state.engineAvailable,
                                modifier = Modifier.padding(LocalHainSpacing.current.md),
                                // T-296: after a dictation the send button takes over with a short pulse
                                sendModifier = Modifier.dictationHandoff(voiceUi?.handoff ?: 0),
                                // T-084: the thinking level lives inside the message box (brain + popover)
                                leading = {
                                    if (docs != null) {
                                        AttachButton(
                                            onFile = { picker.launch(DOC_TYPES) },
                                            onLibrary = { showFiles = true },
                                        )
                                    }
                                    ThinkingPicker(answerMode, onAnswerMode, canThink = state.brainThinks)
                                },
                                trailing = { voiceUi?.let { MicButton(it, enabled = !state.isGenerating) } },
                                glass = chrome,
                            )
                        }
                    }
                }
            },
        ) { chromePadding ->
            val pageIn = fadeIn(hainTween(HainMotion.PAGE_MS))
            val pageSlide = slideInVertically(hainTween(HainMotion.PAGE_MS)) { it / SLIDE_DIV }
            val pageOut = fadeOut(hainTween(HainMotion.FAST_MS))
            AnimatedContent(
                targetState = state.messages.isEmpty(),
                transitionSpec = { (pageIn + pageSlide).togetherWith(pageOut) },
                label = "chat-start",
            ) { empty ->
                if (empty) {
                    Box(Modifier.fillMaxSize().padding(chromePadding)) {
                        Greeting(onSuggestion = { viewModel.onInputChange(it) }, onEgg = onOpenGame)
                    }
                } else {
                    val ratings by viewModel.ratings.collectAsStateWithLifecycle()
                    // T-131: checklist ticks are saved, card buttons can ask the next question
                    val cards = remember(viewModel) { CardHandler(viewModel::updateCards, viewModel::ask) }
                    // T-298: the words of code blocks and task lists in answers
                    val mdLabels =
                        MarkdownLabels(
                            code = stringResource(R.string.chat_md_code),
                            copy = stringResource(R.string.chat_copy),
                            copied = stringResource(R.string.chat_copied),
                            taskDone = stringResource(R.string.chat_md_task_done),
                            taskOpen = stringResource(R.string.chat_md_task_open),
                        )
                    CompositionLocalProvider(
                        LocalCardHandler provides cards,
                        LocalChatVoice provides voice,
                        LocalMarkdownLabels provides mdLabels,
                    ) {
                        MessageList(
                            state,
                            ratings = ratings,
                            onRetry = { viewModel.regenerate(it) },
                            onEdit = viewModel::startEdit,
                            onRate = viewModel::rate,
                            onAction = viewModel::resolveAction,
                            extras = extras,
                            onSelectBranch = viewModel::selectBranch,
                            sendTick = sendTick,
                            chrome = chromePadding,
                        )
                    }
                }
            }
        }
        if (!wide) {
            ChatListDrawer(
                glass = chrome,
                open = drawerOpen,
                hasChats = state.chats.isNotEmpty(),
                groups = state.groups,
                searchQuery = state.searchQuery,
                currentChatId = state.currentChatId,
                actions = drawerActions,
            )
        }
        if (showFiles && docs != null) {
            MyFilesScreen(
                docs,
                onAttach = {
                    viewModel.attach(it)
                    showFiles = false
                },
                onBack = { showFiles = false },
            )
        }
    }
}

/** Häppchen 9: what the paper clip offers in the system file picker. */
private val DOC_TYPES = arrayOf("application/pdf", "image/*", "text/plain", "text/markdown", "text/*")

/**
 * Orange point while Hain is online (T-069); the network log in Privacy shows every request. Since
 * T-230 only the pulsing dot, in front of "Auf diesem Handy", so the top bar stays calm.
 */
@Composable
private fun OnlineDot() {
    val hint = stringResource(R.string.chat_online_hint)
    Box(Modifier.padding(end = 6.dp).semantics { contentDescription = hint }) {
        PulsingDot(LocalHainColors.current.online)
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
    onSelectBranch: (Long) -> Unit = {},
    sendTick: Int = 0,
    /** T-230: room the frosted top bar and message box take; the list scrolls under them. */
    chrome: PaddingValues = PaddingValues(0.dp),
) {
    val spacing = LocalHainSpacing.current
    val viewport = rememberChatViewportState()
    val listState = viewport.list
    val glass = rememberHainGlassState()
    // the one own message whose actions are showing (tap again or tap another to switch)
    var openId by rememberSaveable { mutableStateOf<Long?>(null) }
    val last = state.messages.lastOrNull()
    // T-085/T-058: follow the streaming answer only while you are at the bottom (ChatViewport)
    ChatViewportEffects(viewport, state.currentChatId, state.messages, sendTick)
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
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val reduce = LocalReduceMotion.current
        // T-234: on tablets the chat text is at most CHAT_MAX_WIDTH wide, in the middle
        val side = maxOf(spacing.lg, (maxWidth - CHAT_MAX_WIDTH) / 2)
        // T-289: in landscape the camera cut-out and a side navigation bar never cover the messages
        val dir = LocalLayoutDirection.current
        val safe =
            WindowInsets.displayCutout
                .union(WindowInsets.navigationBars)
                .only(WindowInsetsSides.Horizontal)
                .asPaddingValues()
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().hainGlassSource(glass),
            contentPadding =
                PaddingValues(
                    start = maxOf(side, spacing.lg + safe.calculateStartPadding(dir)),
                    end = maxOf(side, spacing.lg + safe.calculateEndPadding(dir)),
                    top = chrome.calculateTopPadding() + spacing.md,
                    bottom = chrome.calculateBottomPadding() + spacing.sm,
                ),
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            // T-290: stable keys and two row kinds, so a scrolled-away answer is reused for the next answer
            items(
                state.messages,
                key = { it.id },
                contentType = { if (it.isUser) USER_ROW else ANSWER_ROW },
            ) { message ->
                val branch = state.branches[message.id]
                // T-058: new messages and switched versions slide into place (only fade with reduce motion)
                val item =
                    Modifier.animateItem(
                        fadeInSpec = hainTween(HainMotion.PAGE_MS, reduce),
                        placementSpec = if (reduce) null else hainTween(HainMotion.PAGE_MS, false),
                        fadeOutSpec = hainTween(HainMotion.FAST_MS, reduce),
                    )
                if (message.isUser) {
                    Column(item.fillMaxWidth(), horizontalAlignment = Alignment.End) {
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
                        // T-057: "‹ 2/3 ›" under an edited message
                        if (branch != null) BranchPicker(branch, !state.isGenerating, onSelectBranch)
                    }
                } else {
                    Answer(
                        message,
                        cheer = cheerId == message.id,
                        latest = message.id == last?.id,
                        extras = extras,
                        // 7b: the user messages up to this answer, for the memory chips
                        userMessages = { userMessagesBefore(state.messages, message.id) },
                        rating = ratings[message.id],
                        canRetry = !state.isGenerating,
                        onRetry = { onRetry(message.id) },
                        onRate = { onRate(message.id, it) },
                        onAction = { index, event -> onAction(message.id, index, event) },
                        modifier = item,
                        branch = {
                            if (branch != null) BranchPicker(branch, !state.isGenerating, onSelectBranch)
                        },
                    )
                }
            }
        }
        ScrollToBottomPill(
            visible = viewport.showPill,
            newAnswer = viewport.unseen,
            glass = glass,
            onClick = { scope.launch { viewport.jumpToEnd(state.messages.lastIndex, animate = true) } },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = chrome.calculateBottomPadding() + spacing.sm),
        )
    }
}

/** 7b: the user messages of the chat up to the answer [answerId]. */
internal fun userMessagesBefore(
    messages: List<UiMessage>,
    answerId: Long,
): List<String> = messages.takeWhile { it.id != answerId }.filter { it.isUser }.map { it.text }

/** Own message: a short tap shows edit and copy below it, on the right (T-057, like Gemini). */
@Composable
private fun UserMessage(
    message: UiMessage,
    open: Boolean,
    onTap: () -> Unit,
    canEdit: Boolean,
    onEdit: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.End) {
        // Häppchen 9: the files of this message as chips above the bubble
        if (message.attachments.isNotEmpty()) {
            AttachmentChips(message.attachments, Modifier.padding(bottom = 4.dp))
        }
        if (message.text.isNotBlank()) {
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
    }
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
    modifier: Modifier = Modifier,
    branch: @Composable () -> Unit = {},
    userMessages: () -> List<String> = { emptyList() },
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Column(modifier.fillMaxWidth()) {
        // true once this answer was seen while it was still being written (4b-2)
        var fresh by remember(message.id) { mutableStateOf(message.streaming) }
        LaunchedEffect(message.id, message.streaming) { if (message.streaming) fresh = true }
        // T-231: one element for every phase (waits, thinks, looks things up, writes): Pim + words,
        // the thinking opens inside it, the tool steps hang below it
        PimStatus(message, latest = latest, cheer = cheer, initiallyExpanded = extras.rawThinking)
        if (!message.streaming || message.text.isNotEmpty()) {
            // T-131: answer text with its cards
            AnswerBody(
                message,
                actions =
                    if (message.streaming) {
                        null
                    } else {
                        {
                            // T-057: "‹ 2/3 ›" first in the row, like ChatGPT
                            branch()
                            AnswerActions(message.plainAnswer(), rating, canRetry, onRetry, onRate)
                        }
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
                SourceChips(message.sources, favicons = extras.favicons)
                extras.answerFooter?.let { footer -> footer(remember(message.id) { userMessages() }) }
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
 * T-211: "21.4 Token/s · erstes Token nach 0.8 s" and a button that copies the debug line. T-230: one
 * quiet line in small type (ends in … when narrow), the copy button is an icon.
 */
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
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_MS)
            copied = false
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            line,
            style = LocalHainTypography.current.label.copy(color = colors.textMuted, fontSize = STATS_SP.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        HainIconButton(
            if (copied) HainIcons.check else HainIcons.copy,
            stringResource(R.string.chat_dev_copy_debug),
            {
                clipboard.setText(AnnotatedString(debug))
                copied = true
            },
            tint = if (copied) colors.accent else colors.icon,
            iconSize = STATS_ICON.dp,
        )
    }
}

/**
 * The quiet row under a finished answer (T-057): copy, share, read aloud (Häppchen 10, when the app
 * gave the chat a voice), thumbs, ask again — evenly spaced, grey.
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
    val external = LocalExternalLaunch.current
    val shareLabel = stringResource(R.string.chat_share)
    CopyAction(text)
    MessageAction(MessageActionKind.SHARE, shareLabel, onClick = {
        val send =
            Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_TEXT, text)
        external()
        context.startActivity(Intent.createChooser(send, shareLabel))
    })
    ReadAloudAction(text)
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
private const val USER_ROW = 0
private const val ANSWER_ROW = 1
private const val FOCUS_DELAY_MS = 200L
private const val PERCENT = 100
private const val COPIED_MS = 1500L

private const val SLIDE_DIV = 14
private const val CHEER_MS = 2_200L
private const val ERROR_PIM = 32
private const val MS_PER_S = 1000f
private const val STATS_SP = 12
private const val STATS_ICON = 15

private const val MAX_INFO_CARDS = 3

/** T-072: the headlines of the `news` tool as a card; a tap opens the article in the browser. */
@Composable
private fun AnswerNews(news: List<NewsHeadline>) {
    val uri = LocalUriHandler.current
    val external = LocalExternalLaunch.current
    NewsCard(
        items = news.map { NewsCardItem(it.source, it.title, it.url, it.publishedAt, it.paywall) },
        onOpen = { url -> openExternally(external) { uri.openUri(url) } },
    )
}
