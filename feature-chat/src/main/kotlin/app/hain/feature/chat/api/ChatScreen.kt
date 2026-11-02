package app.hain.feature.chat.api

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.hain.feature.chat.R
import app.hain.feature.chat.internal.ChatError
import app.hain.feature.chat.internal.ChatUiState
import app.hain.feature.chat.internal.ChatViewModel
import app.hain.feature.chat.internal.UiMessage
import app.hain.llm.api.EngineState
import app.hain.ui.HainIcons
import app.hain.ui.components.AssistantBlock
import app.hain.ui.components.Composer
import app.hain.ui.components.EmptyState
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.MessageAction
import app.hain.ui.components.MessageActionKind
import app.hain.ui.components.ReasoningBlock
import app.hain.ui.components.TopBar
import app.hain.ui.components.UserBubble
import app.hain.ui.effects.ThinkingGlow
import app.hain.ui.motion.rememberHainHaptics
import app.hain.ui.pim.JuicyPim
import app.hain.ui.pim.Pim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography

/** Chat screen (T-052) with the chat drawer (T-053, basic). */
@Suppress("LongMethod") // screen scaffold: top bar, list, composer, drawer
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    val colors = LocalHainColors.current
    val haptics = rememberHainHaptics()

    BackHandler(enabled = drawerOpen) { drawerOpen = false }

    Box(modifier.fillMaxSize().background(colors.bg)) {
        val chatScale by animateFloatAsState(if (drawerOpen) OPEN_SCALE else 1f, label = "chat-scale")
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
                    HainIconButton(HainIcons.plus, stringResource(R.string.chat_new), viewModel::newChat)
                },
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (state.messages.isEmpty()) {
                    Greeting(onSuggestion = { viewModel.onInputChange(it) })
                } else {
                    MessageList(state, onRetry = viewModel::regenerate)
                }
            }
            ErrorLine(state)
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

@Composable
private fun engineLine(state: ChatUiState): String? =
    when {
        !state.engineAvailable -> stringResource(R.string.chat_error_no_native)
        state.engine is EngineState.Loading -> stringResource(R.string.chat_loading_brain)
        state.engine is EngineState.Ready -> stringResource(R.string.chat_brain_ready)
        else -> null
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Greeting(onSuggestion: (String) -> Unit) {
    val spacing = LocalHainSpacing.current
    val suggestions =
        listOf(
            stringResource(R.string.chat_suggestion_1),
            stringResource(R.string.chat_suggestion_2),
            stringResource(R.string.chat_suggestion_3),
            stringResource(R.string.chat_suggestion_4),
        )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        EmptyState(
            title = stringResource(R.string.chat_greeting),
            body = stringResource(R.string.chat_greeting_body),
            art = { JuicyPim(PimState.HAPPY, 104.dp) },
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing.sm, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            suggestions.forEach { SuggestionChip(it) { onSuggestion(it) } }
        }
    }
}

@Composable
private fun SuggestionChip(
    text: String,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val shape = LocalHainShapes.current.chip
    BasicText(
        text,
        style = LocalHainTypography.current.bodySmall.copy(color = colors.text),
        modifier =
            Modifier
                .fillMaxWidth(CHIP_WIDTH)
                .border(1.dp, colors.border, shape)
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

@Composable
private fun MessageList(
    state: ChatUiState,
    onRetry: () -> Unit,
) {
    val spacing = LocalHainSpacing.current
    val listState = rememberLazyListState()
    val last = state.messages.lastOrNull()
    LaunchedEffect(state.messages.size, last?.text?.length, last?.reasoning?.length) {
        if (state.messages.isNotEmpty()) listState.scrollToItem(state.messages.lastIndex, Int.MAX_VALUE / 2)
    }
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
                UserBubble(message.text)
            } else {
                Answer(message, isLast = message.id == last?.id, onRetry = onRetry)
            }
        }
    }
}

@Composable
private fun Answer(
    message: UiMessage,
    isLast: Boolean,
    onRetry: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Column(Modifier.fillMaxWidth()) {
        val reasoning = message.reasoning
        if (reasoning != null) {
            val title =
                message.thinkingSeconds?.let { stringResource(R.string.chat_thought_for, it) }
                    ?: stringResource(R.string.chat_thinking)
            ReasoningBlock(title = title, reasoning = reasoning)
        }
        if (message.streaming && message.text.isEmpty() && message.reasoning == null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pim(PimState.THINKING, 28.dp)
                Spacer(Modifier.width(8.dp))
                BasicText(stringResource(R.string.chat_thinking), style = type.bodySmall.copy(color = colors.textMuted))
            }
        } else {
            val shareLabel = stringResource(R.string.chat_share)
            AssistantBlock(
                markdown = message.text,
                actions =
                    if (message.streaming) {
                        null
                    } else {
                        {
                            MessageAction(MessageActionKind.COPY, stringResource(R.string.chat_copy)) {
                                clipboard.setText(AnnotatedString(message.text))
                            }
                            if (isLast) {
                                MessageAction(MessageActionKind.RETRY, stringResource(R.string.chat_retry), onRetry)
                            }
                            MessageAction(MessageActionKind.SHARE, shareLabel) {
                                val send =
                                    Intent(Intent.ACTION_SEND)
                                        .setType("text/plain")
                                        .putExtra(Intent.EXTRA_TEXT, message.text)
                                context.startActivity(Intent.createChooser(send, shareLabel))
                            }
                        }
                    },
            )
            if (message.stopped) {
                BasicText(stringResource(R.string.chat_stopped), style = type.label.copy(color = colors.textMuted))
            }
        }
    }
}

@Composable
private fun ErrorLine(state: ChatUiState) {
    val error = state.error ?: return
    val text =
        when (error) {
            ChatError.MODEL_LOAD -> stringResource(R.string.chat_error_load)
            ChatError.CONTEXT_FULL -> stringResource(R.string.chat_error_context)
            ChatError.GENERATION -> stringResource(R.string.chat_error_generation)
        }
    BasicText(
        text,
        style = LocalHainTypography.current.bodySmall.copy(color = LocalHainColors.current.danger),
        modifier = Modifier.padding(horizontal = LocalHainSpacing.current.lg),
    )
}

private const val OPEN_SCALE = 0.94f
private const val CHIP_WIDTH = 0.9f
