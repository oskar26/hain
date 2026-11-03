package app.hain.feature.chat.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import app.hain.common.api.AnswerMode
import app.hain.feature.chat.api.ChatListDrawer
import app.hain.feature.chat.api.ChatSurface
import app.hain.feature.chat.api.DrawerActions
import app.hain.feature.chat.api.Greeting
import app.hain.feature.chat.api.MessageList
import app.hain.feature.chat.api.ThinkingPicker
import app.hain.feature.chat.internal.ActionCard
import app.hain.feature.chat.internal.Chat
import app.hain.feature.chat.internal.ChatGroup
import app.hain.feature.chat.internal.ChatUiState
import app.hain.feature.chat.internal.SourceRef
import app.hain.feature.chat.internal.StepState
import app.hain.feature.chat.internal.ToolStep
import app.hain.feature.chat.internal.UiMessage
import app.hain.tools.api.PhoneAction
import app.hain.ui.HainIcons
import app.hain.ui.components.Composer
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.TopBar
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.HainStyle
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.hainBackdrop
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The style decision (03.11.2026, docs/design/STYLE_OPTIONS.md): every chat screen in style A and B,
 * light and dark, as `style_<A|B>_<screen>_<light|dark>.png`. Only with `-Phain.screens` in CI.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class StyleScreens {
    @get:Rule
    val compose = createComposeRule()

    /** Sets the content once and records it in all four combinations; [prepare] runs before the first. */
    private fun record(
        name: String,
        prepare: () -> Unit = {},
        content: @Composable () -> Unit,
    ) {
        val combo = mutableStateOf(COMBOS.first())
        compose.setContent { StyleFrame(combo.value.first, combo.value.second, content) }
        compose.waitForIdle()
        prepare()
        COMBOS.forEach { c ->
            combo.value = c
            compose.waitForIdle()
            val mode = if (c.second) "dark" else "light"
            captureScreenRoboImage("$OUT/style_${c.first.name}_${name}_$mode.png")
        }
    }

    @Test
    fun emptyChat() = record("chat_empty") { StyledChat(ChatUiState()) }

    @Test
    fun longAnswer() = record("chat_answer") { StyledChat(ChatUiState(messages = longAnswer())) }

    @Test
    fun composerWithPicker() =
        record(
            "composer_picker",
            prepare = {
                compose.onNodeWithContentDescription("Denk-Stufe", substring = true).performClick()
                compose.waitForIdle()
            },
        ) {
            val input = "Plane mir eine Woche ohne Plastik.\nMit Kindern und Hund.\nUnd bitte günstig."
            StyledChat(ChatUiState(messages = shortAnswer(), input = input), AnswerMode.THOROUGH)
        }

    @Test
    fun drawer() =
        record("drawer") {
            Box(Modifier.fillMaxSize()) {
                StyledChat(ChatUiState(messages = shortAnswer()))
                StyledDrawer()
            }
        }

    @Test
    fun confirmCard() = record("confirm") { StyledChat(ChatUiState(messages = alarmAnswer())) }
}

private val COMBOS =
    listOf(HainStyle.A to false, HainStyle.A to true, HainStyle.B to false, HainStyle.B to true)

@Composable
private fun StyleFrame(
    style: HainStyle,
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    HainTheme(darkTheme = dark, style = style) {
        CompositionLocalProvider(LocalReduceMotion provides true) {
            Box(Modifier.fillMaxSize().hainBackdrop()) { content() }
        }
    }
}

/** The real chat layout (ChatSurface) without its view model. */
@Composable
private fun StyledChat(
    state: ChatUiState,
    mode: AnswerMode = AnswerMode.AUTO,
) {
    ChatSurface(
        thinking = false,
        empty = state.messages.isEmpty(),
        topBar = {
            TopBar(
                title = "Hain",
                subtitle = "Auf diesem Handy",
                leading = { HainIconButton(HainIcons.menu, "Menü", {}) },
                trailing = { HainIconButton(HainIcons.plus, "Neuer Chat", {}) },
            )
        },
        bottomBar = {
            Composer(
                text = state.input,
                onTextChange = {},
                placeholder = "Nachricht an Hain",
                isGenerating = false,
                onSend = {},
                onStop = {},
                sendLabel = "Senden",
                stopLabel = "Stopp",
                modifier = Modifier.padding(12.dp),
                leading = { ThinkingPicker(mode, {}) },
            )
        },
    ) { inset ->
        if (state.messages.isEmpty()) {
            Greeting(onSuggestion = {}, bottomInset = inset, hour = 15)
        } else {
            MessageList(state, emptyMap(), {}, {}, { _, _ -> }, { _, _, _ -> }, bottomInset = inset)
        }
    }
}

@Composable
private fun StyledDrawer() {
    ChatListDrawer(
        open = true,
        hasChats = true,
        groups =
            listOf(
                ChatGroup.TODAY to
                    listOf(
                        Chat(1, "Wetter in Köln am Wochenende", emptyList(), 0),
                        Chat(2, "Plastikfreie Woche planen", emptyList(), 0, pinned = true),
                    ),
                ChatGroup.YESTERDAY to
                    listOf(
                        Chat(9, "Geburtstagsgedicht für Oma", emptyList(), 0),
                        Chat(10, "Erklär mir Photosynthese", emptyList(), 0),
                    ),
            ),
        searchQuery = "",
        currentChatId = 2,
        actions = DrawerActions({}, {}, {}, {}, { _, _ -> }, { _, _ -> }, {}, {}),
    )
}

private fun shortAnswer(): List<UiMessage> =
    listOf(
        UiMessage(1, true, "Was ist der Kölner Dom?"),
        UiMessage(
            2,
            false,
            "Der Kölner Dom ist eine gotische Kathedrale am Rhein und seit 1996 Welterbe.",
        ),
    )

private fun longAnswer(): List<UiMessage> =
    listOf(
        UiMessage(1, true, "Wie spare ich Strom im Haushalt? Gern mit kleinem Rechenbeispiel."),
        UiMessage(
            2,
            false,
            listOf(
                "## Strom sparen",
                "",
                "Die größten Hebel im Haushalt:",
                "",
                "- **Kühlschrank** auf 7 °C statt 4 °C",
                "- Geräte ganz ausschalten statt Stand-by",
                "- Wäsche bei 30 °C und an der Luft trocknen",
                "",
                "```kotlin",
                "val ersparnis = 120 * 0.35 // kWh × €/kWh = 42 € im Jahr",
                "```",
                "",
                "1. Steckerleiste mit Schalter kaufen",
                "2. Verbrauch einen Monat lang notieren",
            ).joinToString("\n"),
            steps =
                listOf(
                    ToolStep("web_search", "Strom sparen Haushalt", StepState.DONE, online = true),
                    ToolStep("calculator", "120 × 0,35", StepState.DONE, online = false),
                ),
            sources =
                listOf(
                    SourceRef(
                        "Verbraucherzentrale – Strom sparen",
                        "https://www.verbraucherzentrale.de",
                    ),
                    SourceRef("Umweltbundesamt", "https://www.umweltbundesamt.de"),
                ),
        ),
    )

private fun alarmAnswer(): List<UiMessage> =
    listOf(
        UiMessage(1, true, "Weck mich morgen um 6:30."),
        UiMessage(
            2,
            false,
            "Ich habe den Wecker vorbereitet. Tipp auf **Stellen**, dann ist er an.",
            actions = listOf(ActionCard(PhoneAction.Alarm(6, 30))),
        ),
    )
