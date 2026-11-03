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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hain.common.api.AnswerMode
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.ActionEvent
import app.hain.feature.chat.internal.ChatError
import app.hain.feature.chat.internal.ChatUiState
import app.hain.feature.chat.internal.ChatViewModel
import app.hain.feature.chat.internal.Rating
import app.hain.feature.chat.internal.UiMessage
import app.hain.llm.api.EngineState
import app.hain.ui.HainIcons
import app.hain.ui.components.AssistantBlock
import app.hain.ui.components.Composer
import app.hain.ui.components.Hain3D
import app.hain.ui.components.Hain3DIcon
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.MessageAction
import app.hain.ui.components.MessageActionKind
import app.hain.ui.components.PulsingDot
import app.hain.ui.components.ReasoningBlock
import app.hain.ui.components.TopBar
import app.hain.ui.components.UserBubble
import app.hain.ui.components.pressScale
import app.hain.ui.effects.ThinkingGlow
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.HainMotionMoment
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.motion.motionSpec
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.pim.JuicyPim
import app.hain.ui.pim.Pim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.glassTile
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
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val colors = LocalHainColors.current
    val haptics = rememberHainHaptics()

    BackHandler(enabled = drawerOpen) { drawerOpen = false }

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
            TopBar(
                title = stringResource(R.string.chat_title),
                subtitle = engineLine(state),
                leading = {
                    HainIconButton(HainIcons.menu, stringResource(R.string.chat_menu), { drawerOpen = true })
                },
                trailing = {
                    if (state.online) OnlineDot()
                    HainIconButton(HainIcons.plus, stringResource(R.string.chat_new), viewModel::newChat)
                },
            )
            var bannerHidden by rememberSaveable { mutableStateOf(false) }
            if (!state.brainInstalled && !bannerHidden) {
                BrainBanner(onGet = onGetBrain, onLater = { bannerHidden = true })
            }
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
                        Greeting(onSuggestion = { viewModel.onInputChange(it) })
                    } else {
                        val ratings by viewModel.ratings.collectAsStateWithLifecycle()
                        MessageList(
                            state,
                            ratings = ratings,
                            onRetry = { viewModel.regenerate(it) },
                            onEdit = viewModel::startEdit,
                            onRate = viewModel::rate,
                            onAction = viewModel::resolveAction,
                        )
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

/**
 * Empty chat (T-011, like Oskar's reference 6): a small glossy Pim, a big light greeting on the
 * left and three suggestion tiles of glass, each with its 3D icon and its own soft light. With the
 * keyboard open there is little room: then Pim gets smaller and the suggestions step aside, so
 * nothing slides under the status bar (2d-2).
 */
@Composable
internal fun Greeting(
    onSuggestion: (String) -> Unit,
    hour: Int =
        remember {
            java.time.LocalTime
                .now()
                .hour
        },
) {
    val spacing = LocalHainSpacing.current
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val suggestions =
        listOf(
            Triple(stringResource(R.string.chat_suggestion_1), Hain3D.LEAF, GlowHue.MINT),
            Triple(stringResource(R.string.chat_suggestion_2), Hain3D.RECYCLE, GlowHue.TEAL),
            Triple(stringResource(R.string.chat_suggestion_4), Hain3D.CALENDAR, GlowHue.PEACH),
        )
    val hello =
        stringResource(
            when (hour) {
                in MORNING -> R.string.chat_hello_morning
                in EVENING_FROM..LAST_HOUR, in 0 until MORNING.first -> R.string.chat_hello_evening
                else -> R.string.chat_hello_day
            },
        )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val roomy = maxHeight >= ROOMY_HEIGHT.dp
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.xl, vertical = spacing.lg),
            verticalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterVertically),
        ) {
            JuicyPim(PimState.HAPPY, if (roomy) 64.dp else 48.dp)
            BasicText(
                "$hello\n" + stringResource(R.string.chat_hello_question),
                style = type.display.copy(color = colors.text),
            )
            if (roomy) {
                Spacer(Modifier.height(spacing.xs))
                suggestions.forEach { (text, icon, hue) -> SuggestionTile(text, icon, hue) { onSuggestion(text) } }
            }
        }
    }
}

/** One suggestion: a glass tile whose colour shines from its 3D icon on the left (T-011). */
@Composable
private fun SuggestionTile(
    text: String,
    icon: Hain3D,
    hue: GlowHue,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }.glassTile(hue, elevation = 6.dp, lightAt = Offset(x = 0.06f, y = 0.5f))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Hain3DIcon(icon, size = 32.dp)
        Spacer(Modifier.width(spacing.md))
        BasicText(
            text,
            style = LocalHainTypography.current.body.copy(color = colors.text),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
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

@Suppress("LongParameterList") // one callback per answer action
@Composable
private fun Answer(
    message: UiMessage,
    rating: Rating?,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onRate: (Rating) -> Unit,
    onAction: (Int, ActionEvent) -> Unit = { _, _ -> },
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Column(Modifier.fillMaxWidth()) {
        ToolTimeline(message.steps, research = message.research.takeIf { message.streaming })
        val reasoning = message.reasoning
        if (reasoning != null) {
            val title =
                message.thinkingSeconds?.let { stringResource(R.string.chat_thought_for, it) }
                    ?: stringResource(R.string.chat_thinking)
            ReasoningBlock(title = title, reasoning = reasoning)
        }
        val waiting = message.text.isEmpty() && message.reasoning == null && message.steps.isEmpty()
        if (message.streaming && waiting) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pim(PimState.THINKING, 28.dp)
                Spacer(Modifier.width(8.dp))
                BasicText(stringResource(R.string.chat_thinking), style = type.bodySmall.copy(color = colors.textMuted))
            }
        } else {
            AssistantBlock(
                markdown = message.text,
                actions =
                    if (message.streaming) {
                        null
                    } else {
                        { AnswerActions(message.text, rating, canRetry, onRetry, onRate) }
                    },
            )
            if (!message.streaming) {
                message.actions.forEachIndexed { index, card -> ConfirmCard(card, { onAction(index, it) }) }
                SourceChips(message.sources)
            }
            if (message.stopped) {
                BasicText(stringResource(R.string.chat_stopped), style = type.label.copy(color = colors.textMuted))
            }
        }
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
    BasicText(
        text,
        style = LocalHainTypography.current.bodySmall.copy(color = colors.danger),
        modifier = Modifier.padding(horizontal = LocalHainSpacing.current.lg),
    )
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
        Pim(PimState.SLEEPING, 32.dp)
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
private const val MORNING_FROM = 5
private const val MORNING_TO = 10
private val MORNING = MORNING_FROM..MORNING_TO
private const val EVENING_FROM = 18
private const val LAST_HOUR = 23
private const val PERCENT = 100
private const val ROOMY_HEIGHT = 420
private const val COPIED_MS = 1500L

private const val SLIDE_DIV = 14
