package app.hain.feature.chat.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import app.hain.feature.chat.api.ChatBottomArea
import app.hain.feature.chat.api.ChatChrome
import app.hain.feature.chat.api.ChatListDrawer
import app.hain.feature.chat.api.ChatTopGlass
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
import app.hain.ui.effects.LocalForceFrost
import app.hain.ui.effects.rememberHainGlassState
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
            // T-230: Robolectric blurs the main window when asked; the chrome is glass in the pictures
            LocalForceFrost provides true,
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
    texts: ShotTexts = ShotTexts.DE,
) {
    // T-230/T-232: the same frosted frame as the app (top bar and message box over the chat)
    val glass = rememberHainGlassState()
    ChatChrome(
        glass = glass,
        top = {
            ChatTopGlass(glass) {
                TopBar(
                    title = "Hain",
                    subtitle = texts.subtitle,
                    onTitleClick = {},
                    leading = { HainIconButton(HainIcons.menu, texts.menu, {}) },
                    trailing = {
                        HainIconButton(HainIcons.share, texts.menu, {})
                        HainIconButton(HainIcons.plus, texts.newChat, {})
                    },
                )
            }
        },
        bottom = {
            ChatBottomArea {
                Composer(
                    text = state.input,
                    onTextChange = {},
                    placeholder = texts.placeholder,
                    isGenerating = state.isGenerating,
                    onSend = {},
                    onStop = {},
                    sendLabel = texts.send,
                    stopLabel = texts.stop,
                    modifier = Modifier.padding(12.dp),
                    leading = { ThinkingPicker(mode, {}) },
                )
            }
        },
    ) { padding ->
        if (state.messages.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding)) { Greeting(onSuggestion = {}, hour = 15, seed = 3) }
        } else {
            MessageList(state, emptyMap(), {}, {}, { _, _ -> }, { _, _, _ -> }, chrome = padding)
        }
    }
}

@Composable
internal fun Drawer(
    longTitles: Boolean = false,
    texts: ShotTexts = ShotTexts.DE,
) {
    val today =
        if (longTitles) {
            listOf(
                Chat(1, LONG_TITLE, emptyList(), 0),
                Chat(2, NO_SPACE_TITLE, emptyList(), 0),
                Chat(3, "Kurz", emptyList(), 0, pinned = true),
            )
        } else {
            texts.drawerToday.mapIndexed { i, title -> Chat(i + 1L, title, emptyList(), 0) }
        }
    ChatListDrawer(
        open = true,
        hasChats = true,
        groups =
            listOf(
                ChatGroup.TODAY to today,
                ChatGroup.YESTERDAY to listOf(Chat(9, texts.drawerYesterday, emptyList(), 0)),
            ),
        searchQuery = "",
        currentChatId = 2,
        actions = DrawerActions({}, {}, {}, {}, { _, _ -> }, { _, _ -> }, {}, {}),
    )
}

internal fun answerState(texts: ShotTexts = ShotTexts.DE): ChatUiState =
    ChatUiState(
        messages =
            listOf(
                UiMessage(1, true, texts.question),
                UiMessage(
                    2,
                    false,
                    texts.answer,
                    steps = texts.steps.map { (tool, label) -> ToolStep(tool, label, StepState.DONE, online = true) },
                    sources = texts.sources.map { (title, url) -> SourceRef(title, url) },
                ),
            ),
    )

/**
 * The sample texts in the screenshots: German for docs/design/screens and the German website,
 * English for the English website (T-107). Labels the app draws itself come from its resources.
 */
data class ShotTexts(
    val subtitle: String,
    val menu: String,
    val newChat: String,
    val placeholder: String,
    val send: String,
    val stop: String,
    val thinkingButton: String,
    val drawerToday: List<String>,
    val drawerYesterday: String,
    val question: String,
    val answer: String,
    val steps: List<Pair<String, String>>,
    val sources: List<Pair<String, String>>,
    val compare: String,
    val longMarkdown: String,
    val composerLine: (Int) -> String,
) {
    companion object {
        val DE =
            ShotTexts(
                subtitle = "Auf diesem Handy",
                menu = "Menü",
                newChat = "Neuer Chat",
                placeholder = "Nachricht an Hain",
                send = "Senden",
                stop = "Stopp",
                thinkingButton = "Denk-Stufe",
                drawerToday = listOf("Wetter in Köln am Wochenende", "Erklär mir Photosynthese"),
                drawerYesterday = "Geburtstagsgedicht für Oma",
                question = "Wie wird das Wetter morgen in Köln, und was ist der Kölner Dom?",
                answer =
                    "Morgen wird es in **Köln** mild:\n\n- 14 °C am Nachmittag\n- leichter Wind aus Südwest\n- " +
                        "kaum Regen\n\nDer Kölner Dom ist eine gotische Kathedrale und seit 1996 Welterbe.",
                steps = listOf("weather" to "Köln, morgen", "wikipedia" to "Kölner Dom"),
                sources =
                    listOf(
                        "Kölner Dom – Wikipedia" to "https://de.wikipedia.org/wiki/K%C3%B6lner_Dom",
                        "Open-Meteo" to "https://open-meteo.com",
                    ),
                compare = "Vergleich bitte Fahrrad, Bus und Auto",
                longMarkdown = LONG_MD,
                composerLine = { "Zeile $it: noch ein Gedanke für die Frage" },
            )

        val EN =
            ShotTexts(
                subtitle = "On this phone",
                menu = "Open chats",
                newChat = "New chat",
                placeholder = "Message Hain",
                send = "Send",
                stop = "Stop",
                thinkingButton = "Thinking",
                drawerToday = listOf("Weekend weather in Paris", "Explain photosynthesis"),
                drawerYesterday = "Birthday poem for Grandma",
                question = "What's the weather in Paris tomorrow, and what is the Eiffel Tower?",
                answer =
                    "Tomorrow will be mild in **Paris**:\n\n- 15 °C in the afternoon\n- a light breeze from the " +
                        "southwest\n- hardly any rain\n\nThe Eiffel Tower is a wrought-iron tower built for the " +
                        "1889 World's Fair.",
                steps = listOf("weather" to "Paris, tomorrow", "wikipedia" to "Eiffel Tower"),
                sources =
                    listOf(
                        "Eiffel Tower – Wikipedia" to "https://en.wikipedia.org/wiki/Eiffel_Tower",
                        "Open-Meteo" to "https://open-meteo.com",
                    ),
                compare = "Please compare bike, bus and car",
                longMarkdown = LONG_MD_EN,
                composerLine = { "Line $it: one more thought for the question" },
            )
    }
}

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

/** [LONG_MD] in English, for the English website (T-107). */
internal val LONG_MD_EN =
    listOf(
        "## Comparison",
        "",
        "| Transport | CO₂ per km | Cost per month | Time to work | Note |",
        "|---|---|---|---|---|",
        "| Bike | 0 g | €10 | 25 min | healthy and quiet |",
        "| Bus | 80 g | €49 | 35 min | monthly pass |",
        "| Car | 150 g | €300 | 20 min | finding a parking spot |",
        "",
        "```kotlin",
        "val aVeryLongLineOfCode = listOf(\"Bike\", \"Bus\", \"Car\").joinToString(separator = \" · \")",
        "```",
        "",
        "1. First point with a long sentence that has to wrap over several lines without being cut off.",
        "2. Second point",
        "3. Third point",
    ).joinToString("\n")
