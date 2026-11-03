package app.hain.feature.chat.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import app.hain.common.api.AnswerMode
import app.hain.feature.chat.api.ChatListDrawer
import app.hain.feature.chat.api.DrawerActions
import app.hain.feature.chat.api.Greeting
import app.hain.feature.chat.api.MessageList
import app.hain.feature.chat.api.ThinkingPicker
import app.hain.feature.chat.internal.Chat
import app.hain.feature.chat.internal.ChatGroup
import app.hain.feature.chat.internal.ChatUiState
import app.hain.feature.chat.internal.SourceRef
import app.hain.feature.chat.internal.StepState
import app.hain.feature.chat.internal.ToolStep
import app.hain.feature.chat.internal.UiMessage
import app.hain.ui.HainIcons
import app.hain.ui.components.Composer
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.TopBar
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.LocalHainColors
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of the chat for docs/design/screens (T-011 before/after, T-085 long texts). Only run
 * with `-Phain.screens -Proborazzi.test.record=true` (CI workflow screens.yml): Robolectric's
 * native graphics need an x86 host.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class ChatScreens {
    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String) = captureScreenRoboImage("$OUT/$name.png")

    @Test
    fun emptyChatLight() {
        compose.setContent { Frame(dark = false) { ChatFrame(ChatUiState()) } }
        shot("chat_empty_light")
    }

    @Test
    fun emptyChatDark() {
        compose.setContent { Frame(dark = true) { ChatFrame(ChatUiState()) } }
        shot("chat_empty_dark")
    }

    @Test
    fun answerWithToolCardLight() {
        compose.setContent { Frame(dark = false) { ChatFrame(answerState(), AnswerMode.THOROUGH) } }
        shot("chat_answer_light")
    }

    @Test
    fun answerWithToolCardDark() {
        compose.setContent { Frame(dark = true) { ChatFrame(answerState(), AnswerMode.THOROUGH) } }
        shot("chat_answer_dark")
    }

    @Test
    fun thinkingPickerOpen() {
        compose.setContent { Frame(dark = false) { ChatFrame(ChatUiState(), AnswerMode.FAST) } }
        compose.onNodeWithContentDescription("Denk-Stufe", substring = true).performClick()
        compose.waitForIdle()
        shot("chat_thinking_picker_light")
    }

    @Test
    fun drawerLight() {
        compose.setContent {
            Frame(dark = false) {
                ChatFrame(answerState())
                Drawer()
            }
        }
        shot("drawer_light")
    }

    @Test
    fun drawerDark() {
        compose.setContent {
            Frame(dark = true) {
                ChatFrame(answerState())
                Drawer()
            }
        }
        shot("drawer_dark")
    }

    // ---- T-085: long texts ----

    @Test
    fun longOwnMessageFolds() {
        val long = List(40) { "Das ist Zeile ${it + 1} einer sehr langen eigenen Nachricht." }.joinToString(" ")
        val state = ChatUiState(messages = listOf(UiMessage(1, true, long), UiMessage(2, false, "Gerne, ich lese es.")))
        compose.setContent { Frame(dark = false) { ChatFrame(state) } }
        shot("long_own_message")
    }

    @Test
    fun longAnswerWithTableAndCode() {
        val state = ChatUiState(messages = listOf(UiMessage(1, true, "Vergleich bitte"), UiMessage(2, false, LONG_MD)))
        compose.setContent { Frame(dark = false) { ChatFrame(state) } }
        shot("long_answer_table_code")
    }

    @Test
    fun composerManyLines() {
        val input = List(12) { "Zeile ${it + 1} im Eingabefeld" }.joinToString("\n")
        compose.setContent { Frame(dark = false) { ChatFrame(answerState().copy(input = input)) } }
        shot("long_composer_lines")
    }

    @Test
    fun drawerLongTitles() {
        compose.setContent {
            Frame(dark = false) {
                ChatFrame(ChatUiState())
                Drawer(longTitles = true)
            }
        }
        shot("long_drawer_titles")
    }

    @Test
    fun fontScale200() {
        compose.setContent { Frame(dark = false, fontScale = 2f) { ChatFrame(answerState(), AnswerMode.THOROUGH) } }
        shot("long_font_200")
    }

    @Test
    @Config(qualifiers = "de-w360dp-h640dp-xhdpi")
    fun smallPhone360() {
        compose.setContent { Frame(dark = false) { ChatFrame(answerState(), AnswerMode.FAST) } }
        shot("long_small_360dp")
    }

    @Test
    @Config(qualifiers = "de-w850dp-h392dp-land-xxhdpi")
    fun landscape() {
        compose.setContent { Frame(dark = false) { ChatFrame(answerState()) } }
        shot("long_landscape")
    }
}

internal const val OUT = "build/outputs/roborazzi"

/** Theme, reduced motion (still pictures), an optional font scale and the page colour. */
@Composable
internal fun Frame(
    dark: Boolean,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    HainTheme(darkTheme = dark) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalReduceMotion provides true,
            LocalDensity provides Density(density.density, fontScale),
        ) {
            Box(Modifier.fillMaxSize().background(LocalHainColors.current.bg)) { content() }
        }
    }
}

/** The chat screen without its view model: top bar, list or greeting, the message box. */
@Composable
internal fun ChatFrame(
    state: ChatUiState,
    mode: AnswerMode = AnswerMode.AUTO,
) {
    Column(Modifier.fillMaxSize()) {
        TopBar(
            title = "Hain",
            subtitle = "Auf diesem Handy",
            leading = { HainIconButton(HainIcons.menu, "Menü", {}) },
            trailing = { HainIconButton(HainIcons.plus, "Neuer Chat", {}) },
        )
        Box(Modifier.weight(1f)) {
            if (state.messages.isEmpty()) {
                Greeting(onSuggestion = {}, hour = 15, seed = 3)
            } else {
                MessageList(state, emptyMap(), {}, {}, { _, _ -> }, { _, _, _ -> })
            }
        }
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
    }
}

@Composable
internal fun Drawer(longTitles: Boolean = false) {
    val today =
        if (longTitles) {
            listOf(
                Chat(1, LONG_TITLE, emptyList(), 0),
                Chat(2, NO_SPACE_TITLE, emptyList(), 0),
                Chat(3, "Kurz", emptyList(), 0, pinned = true),
            )
        } else {
            listOf(
                Chat(1, "Wetter in Köln am Wochenende", emptyList(), 0),
                Chat(2, "Erklär mir Photosynthese", emptyList(), 0),
            )
        }
    ChatListDrawer(
        open = true,
        hasChats = true,
        groups =
            listOf(
                ChatGroup.TODAY to today,
                ChatGroup.YESTERDAY to listOf(Chat(9, "Geburtstagsgedicht für Oma", emptyList(), 0)),
            ),
        searchQuery = "",
        currentChatId = 2,
        actions = DrawerActions({}, {}, {}, {}, { _, _ -> }, { _, _ -> }, {}, {}),
    )
}

internal fun answerState(): ChatUiState =
    ChatUiState(
        messages =
            listOf(
                UiMessage(1, true, "Wie wird das Wetter morgen in Köln, und was ist der Kölner Dom?"),
                UiMessage(
                    2,
                    false,
                    "Morgen wird es in **Köln** mild:\n\n- 14 °C am Nachmittag\n- leichter Wind aus Südwest\n- " +
                        "kaum Regen\n\nDer Kölner Dom ist eine gotische Kathedrale und seit 1996 Welterbe.",
                    steps =
                        listOf(
                            ToolStep("weather", "Köln, morgen", StepState.DONE, online = true),
                            ToolStep("wikipedia", "Kölner Dom", StepState.DONE, online = true),
                        ),
                    sources =
                        listOf(
                            SourceRef("Kölner Dom – Wikipedia", "https://de.wikipedia.org/wiki/K%C3%B6lner_Dom"),
                            SourceRef("Open-Meteo", "https://open-meteo.com"),
                        ),
                ),
            ),
    )

private const val LONG_TITLE = "Wie plane ich eine plastikfreie Woche mit Kindern, Hund und wenig Geld im Winter?"
private const val NO_SPACE_TITLE = "Ein_sehr_langer_Titel_ohne_Leerzeichen_der_nicht_umbrechen_kann"

internal val LONG_MD =
    listOf(
        "## Vergleich",
        "",
        "| Verkehrsmittel | CO₂ pro km | Kosten pro Monat | Zeit zur Arbeit | Bemerkung |",
        "|---|---|---|---|---|",
        "| Fahrrad | 0 g | 10 € | 25 min | gesund und leise |",
        "| Bus | 80 g | 49 € | 35 min | Deutschlandticket |",
        "| Auto | 150 g | 300 € | 20 min | Parkplatz suchen |",
        "",
        "```kotlin",
        "val eineSehrLangeZeileMitCode = listOf(\"Fahrrad\", \"Bus\", \"Auto\").joinToString(separator = \" · \")",
        "```",
        "",
        "1. Erster Punkt mit einem langen Satz, der über mehrere Zeilen umbrechen muss, ohne abgeschnitten zu werden.",
        "2. Zweiter Punkt",
        "3. Dritter Punkt",
    ).joinToString("\n")
