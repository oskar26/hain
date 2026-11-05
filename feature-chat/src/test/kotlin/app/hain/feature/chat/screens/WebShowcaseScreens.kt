package app.hain.feature.chat.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import app.hain.cards.api.ActionCard
import app.hain.common.api.AnswerMode
import app.hain.docs.api.DocAttachment
import app.hain.docs.api.DocKind
import app.hain.feature.chat.api.BrainPickerState
import app.hain.feature.chat.api.CardHandler
import app.hain.feature.chat.api.ChatExtras
import app.hain.feature.chat.api.ChatSidePanel
import app.hain.feature.chat.api.ChatSidePanelWidth
import app.hain.feature.chat.api.DrawerActions
import app.hain.feature.chat.api.Greeting
import app.hain.feature.chat.api.LocalCardHandler
import app.hain.feature.chat.api.MessageList
import app.hain.feature.chat.api.ModelPicker
import app.hain.feature.chat.api.QueuedMessages
import app.hain.feature.chat.internal.BranchInfo
import app.hain.feature.chat.internal.Chat
import app.hain.feature.chat.internal.ChatGroup
import app.hain.feature.chat.internal.ChatUiState
import app.hain.feature.chat.internal.QueuedMessage
import app.hain.feature.chat.internal.SourceRef
import app.hain.feature.chat.internal.StepState
import app.hain.feature.chat.internal.ToolStep
import app.hain.feature.chat.internal.UiMessage
import app.hain.feature.memory.api.AnswerChips
import app.hain.feature.memory.api.AnswerChipsView
import app.hain.memory.api.MemoryCategory
import app.hain.memory.api.MemoryFact
import app.hain.models.api.BrainChoice
import app.hain.models.api.DeviceProfile
import app.hain.models.api.ModelFile
import app.hain.models.api.ModelInfo
import app.hain.tools.api.InfoCard
import app.hain.tools.api.PhoneAction
import app.hain.ui.HainIcons
import app.hain.ui.components.Composer
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.TopBar
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * T-269: one screenshot per feature for the website's showcase (0.2), German and English, light and
 * dark. Files: build/outputs/roborazzi/web/web_<id>_<light|dark>[_en].png (CI workflow screens.yml).
 * All sample texts are invented: no real people, places or addresses.
 */
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class WebShowcaseScreens(
    dark: Boolean,
) : WebShowcaseShots(dark, WebTexts.DE, suffix = "") {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params(): List<Array<Any>> = styleParams()
    }
}

/** The same screens with the English app and English sample texts. */
@Config(sdk = [35], qualifiers = "en-w392dp-h850dp-xxhdpi")
class WebShowcaseScreensEn(
    dark: Boolean,
) : WebShowcaseShots(dark, WebTexts.EN, suffix = "_en") {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params(): List<Array<Any>> = styleParams()
    }
}

/** The showcase screenshots themselves; the subclasses pick the language. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class WebShowcaseShots(
    private val dark: Boolean,
    private val t: WebTexts,
    private val suffix: String,
) {
    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String) {
        compose.waitForIdle()
        captureScreenRoboImage("$OUT/web/web_${name}_${if (dark) "dark" else "light"}$suffix.png")
    }

    /** Weather card, Wikipedia and sources under one answer. */
    @Test
    fun tools() {
        val answer =
            UiMessage(
                2,
                false,
                t.weatherAnswer,
                steps =
                    listOf(
                        ToolStep("weather", t.weatherStep, StepState.DONE, online = true),
                        ToolStep("wikipedia", t.wikiStep, StepState.DONE, online = true),
                    ),
                sources = t.sources.map { (title, url) -> SourceRef(title, url) },
                cards = listOf(InfoCard(InfoCard.WEATHER, t.weatherJson)),
            )
        val state = ChatUiState(messages = listOf(UiMessage(1, true, t.weatherQuestion), answer))
        compose.setContent { Frame(dark) { ChatFrame(state, AnswerMode.AUTO, t.shot) } }
        shot("tools")
    }

    /** "Gründlich": Pim searches the web and reads several pages, each step visible. */
    @Test
    fun research() {
        val answer =
            UiMessage(
                2,
                false,
                t.researchPartial,
                streaming = true,
                steps =
                    listOf(
                        ToolStep("web_search", t.researchQuery, StepState.DONE, online = true),
                        ToolStep("read_pages", "3", StepState.DONE, online = true),
                    ),
                sources = t.researchSources.map { (title, url) -> SourceRef(title, url) },
            )
        val state =
            ChatUiState(messages = listOf(UiMessage(1, true, t.researchQuestion), answer), isGenerating = true)
        compose.setContent { Frame(dark) { ChatFrame(state, AnswerMode.THOROUGH, t.shot) } }
        shot("research")
    }

    /** Alarm and timer prepared as cards; nothing happens before the tap. */
    @Test
    fun actions() {
        val answer =
            UiMessage(
                2,
                false,
                t.actionAnswer,
                actions =
                    listOf(
                        ActionCard(PhoneAction.Alarm(6, 30, t.alarmLabel, listOf(1, 2, 3, 4, 5))),
                        ActionCard(PhoneAction.Timer(8 * 60, t.timerLabel)),
                    ),
            )
        val state = ChatUiState(messages = listOf(UiMessage(1, true, t.actionQuestion), answer))
        compose.setContent { Frame(dark) { ChatFrame(state, AnswerMode.FAST, t.shot) } }
        shot("actions")
    }

    @Test
    fun cards() {
        val state =
            ChatUiState(
                messages =
                    listOf(
                        UiMessage(1, true, t.cardQuestion),
                        UiMessage(2, false, t.cardAnswer, cardState = "{\"checked\":{\"0.1.0\":true}}"),
                    ),
            )
        compose.setContent {
            Frame(dark) {
                CompositionLocalProvider(LocalCardHandler provides CardHandler(onAsk = {})) {
                    ChatFrame(state, texts = t.shot)
                }
            }
        }
        shot("cards")
    }

    /** T-262: "Merken: …" under the answer and the memories it used. */
    @Test
    fun memory() {
        val now = 1_793_750_400_000L
        val used =
            listOf(
                MemoryFact(1, t.memoryUsed[0], MemoryCategory.PREFERENCE, now),
                MemoryFact(2, t.memoryUsed[1], MemoryCategory.PREFERENCE, now),
            )
        val chips = AnswerChips(used = used, suggestion = MemoryFact(3, t.memorySuggestion, MemoryCategory.WORK, now))
        val extras = ChatExtras(answerFooter = { AnswerChipsView(chips, onUndo = {}) })
        val state =
            ChatUiState(
                messages = listOf(UiMessage(1, true, t.memoryQuestion), UiMessage(2, false, t.memoryAnswer)),
            )
        compose.setContent { Frame(dark) { ChatFrame(state, texts = t.shot, extras = extras) } }
        shot("memory")
    }

    /** A PDF attached with the paper clip; Pim answers with the page. */
    @Test
    fun documents() {
        val doc = DocAttachment(1, t.docName, DocKind.PDF, 12, 5_400)
        val state =
            ChatUiState(
                messages =
                    listOf(
                        UiMessage(1, true, t.docQuestion, attachments = listOf(doc)),
                        UiMessage(2, false, t.docAnswer),
                    ),
            )
        compose.setContent { Frame(dark) { ChatFrame(state, texts = t.shot) } }
        shot("documents")
    }

    /** "‹ 2/2 ›" versions and a question waiting while Pim still answers (T-057). */
    @Test
    fun branches() {
        val s =
            ChatUiState(
                messages =
                    listOf(
                        UiMessage(10, true, t.eggQuestion),
                        UiMessage(11, false, t.eggAnswer),
                        UiMessage(12, true, t.eggFollow),
                        UiMessage(13, false, t.eggStreaming, streaming = true),
                    ),
                isGenerating = true,
                branches = mapOf(10L to BranchInfo(2, 2, 1L, null), 11L to BranchInfo(3, 3, 2L, null)),
                queue = listOf(QueuedMessage(20, 1, t.eggQueued)),
            )
        compose.setContent {
            Frame(dark) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) { MessageList(s, emptyMap(), {}, {}, { _, _ -> }, { _, _, _ -> }) }
                    QueuedMessages(s.queue, {})
                    Composer(
                        text = t.eggTyping,
                        onTextChange = {},
                        placeholder = t.shot.placeholder,
                        isGenerating = true,
                        onSend = {},
                        onStop = {},
                        sendLabel = t.shot.send,
                        stopLabel = t.shot.stop,
                        modifier = Modifier.padding(12.dp),
                        canQueue = true,
                    )
                }
            }
        }
        shot("branches")
    }

    /** The brain picker in the top bar: LFM3 1.7B (standard), the optional Omni brain and the big brain. */
    @Test
    fun brainPicker() {
        compose.setContent {
            Frame(dark) {
                val state =
                    remember {
                        BrainPickerState(
                            options = {
                                BrainChoice.options(
                                    BRAINS,
                                    "lfm3-1.7b-qad-q4_0",
                                    { it.id == "lfm3-1.7b-qad-q4_0" },
                                    DeviceProfile(8, 20_000_000_000L),
                                )
                            },
                        )
                    }
                var open by remember { mutableStateOf(false) }
                Column(Modifier.fillMaxSize()) {
                    TopBar(
                        title = "Hain",
                        subtitle = t.shot.subtitle,
                        leading = { HainIconButton(HainIcons.menu, t.shot.menu, {}) },
                        trailing = { HainIconButton(HainIcons.plus, t.shot.newChat, {}) },
                        onTitleClick = { open = !open },
                        titleAccessory = { ModelPicker(open, { open = false }, state) },
                    )
                    Greeting(onSuggestion = {}, hour = 15, seed = 3)
                }
            }
        }
        compose.onNodeWithText("Hain").performClick()
        shot("brain_picker")
    }

    /** T-235: on a tablet in landscape the chat list stays open on the left. */
    @Test
    @Config(qualifiers = "+w1280dp-h800dp-land-xhdpi")
    fun tablet() {
        val texts = t.shot
        compose.setContent {
            Frame(dark) {
                ChatSidePanel(
                    hasChats = true,
                    groups =
                        listOf(
                            ChatGroup.TODAY to
                                texts.drawerToday.mapIndexed { i, title -> Chat(i + 1L, title, emptyList(), 0) },
                            ChatGroup.YESTERDAY to listOf(Chat(9, texts.drawerYesterday, emptyList(), 0)),
                        ),
                    searchQuery = "",
                    currentChatId = 1,
                    actions = DrawerActions({}, {}, {}, {}, { _, _ -> }, { _, _ -> }, {}, {}, onNews = {}),
                )
                Box(Modifier.fillMaxSize().padding(start = ChatSidePanelWidth)) {
                    ChatFrame(answerState(texts), texts = texts, menu = false)
                }
            }
        }
        shot("tablet")
    }

    private companion object {
        @Suppress("LongParameterList")
        fun brain(
            id: String,
            name: String,
            bytes: Long,
            tier: String,
            minRam: Int,
            recommendRam: Int,
        ) = ModelInfo(
            id = id,
            name = name,
            kind = "brain",
            files = listOf(ModelFile("https://example.org/$id.gguf", "0".repeat(64), bytes)),
            minRamGb = minRam,
            licence = "l",
            licenceUrl = "u",
            isDefault = id == "lfm3-1.7b-qad-q4_0",
            tier = tier,
            recommendRamGb = recommendRam,
        )

        /** Like the app's catalogue (core-models/src/main/assets/catalog.json). */
        val BRAINS =
            listOf(
                brain("lfm3-1.7b-qad-q4_0", "LFM3 1.7B", 1_049_615_680L, ModelInfo.TIER_STANDARD, 4, 4),
                brain("lfm3-omni-2b-q4_0", "LFM3-Omni 2B", 1_050_000_000L, ModelInfo.TIER_OPTIONAL, 4, 4),
                brain("jan-v3.5-4b-q4_k_m", "Jan-v3.5 4B", 2_720_000_000L, ModelInfo.TIER_STRONG, 8, 12),
            )
    }
}

/** Sample texts of the showcase, German and English; [shot] holds the chat frame's own labels. */
data class WebTexts(
    val shot: ShotTexts,
    val weatherQuestion: String,
    val weatherAnswer: String,
    val weatherStep: String,
    val wikiStep: String,
    val weatherJson: String,
    val sources: List<Pair<String, String>>,
    val researchQuestion: String,
    val researchQuery: String,
    val researchPartial: String,
    val researchSources: List<Pair<String, String>>,
    val actionQuestion: String,
    val actionAnswer: String,
    val alarmLabel: String,
    val timerLabel: String,
    val cardQuestion: String,
    val cardAnswer: String,
    val memoryQuestion: String,
    val memoryAnswer: String,
    val memoryUsed: List<String>,
    val memorySuggestion: String,
    val docName: String,
    val docQuestion: String,
    val docAnswer: String,
    val eggQuestion: String,
    val eggAnswer: String,
    val eggFollow: String,
    val eggStreaming: String,
    val eggQueued: String,
    val eggTyping: String,
) {
    companion object {
        val DE =
            WebTexts(
                shot = ShotTexts.DE,
                weatherQuestion = "Wie wird das Wetter morgen in Paris, und was ist der Eiffelturm?",
                weatherAnswer =
                    "Morgen wird es in **Paris** mild, um 15 °C und kaum Regen.\n\n" +
                        "Der Eiffelturm ist ein Turm aus Schmiedeeisen, gebaut für die Weltausstellung 1889.",
                weatherStep = "Paris, morgen",
                wikiStep = "Eiffelturm",
                weatherJson =
                    """{"place":"Paris","now":{"t":12.4,"wind_kmh":9.0,"sky":"bewölkt"},"days":[""" +
                        """{"date":"2026-11-06","day":"heute","max":13,"min":7,"rain_chance":20,"sky":"bewölkt"},""" +
                        """{"date":"2026-11-07","max":15,"min":8,"rain_chance":10,"sky":"sonnig"},""" +
                        """{"date":"2026-11-08","max":14,"min":9,"rain_chance":40,"sky":"Schauer"}]}""",
                sources =
                    listOf(
                        "Eiffelturm – Wikipedia" to "https://de.wikipedia.org/wiki/Eiffelturm",
                        "Open-Meteo" to "https://open-meteo.com",
                    ),
                researchQuestion = "Wie pflege ich eine Monstera im Winter?",
                researchQuery = "Monstera Pflege Winter",
                researchPartial =
                    "Im Winter braucht die **Monstera** weniger Wasser:\n\n" +
                        "- erst gießen, wenn die oberen 3 cm Erde trocken sind\n- hell, aber nicht an die Heizung",
                researchSources =
                    listOf(
                        "Monstera im Winter – Pflanzenblog" to "https://pflanzen.example.org/monstera",
                        "Zimmerpflanzen-Ratgeber" to "https://ratgeber.example.org/winter",
                    ),
                actionQuestion = "Weck mich jeden Werktag um 6:30 und stell einen Timer auf 8 Minuten",
                actionAnswer = "Vorbereitet – tipp auf **Stellen**, dann übernimmt deine Uhr-App.",
                alarmLabel = "Aufstehen",
                timerLabel = "Eier",
                cardQuestion = "Mach mir einen Lernplan für die Mathearbeit am Freitag",
                cardAnswer =
                    "Klar, hier ist dein Plan bis Freitag:\n\n" +
                        "```card\n" +
                        "Card title=\"Lernplan Mathe\"\n" +
                        "Table\n" +
                        "| Tag | Thema | Zeit |\n" +
                        "| Mo | Brüche | 30 min |\n" +
                        "| Di | Gleichungen | 30 min |\n" +
                        "| Mi | Probearbeit | 45 min |\n" +
                        "Checklist\n" +
                        "- Formelblatt schreiben\n" +
                        "- Aufgaben S. 42 rechnen\n" +
                        "- Fragen sammeln\n" +
                        "Progress value=40 label=\"Schon geschafft\"\n" +
                        "Button label=\"Kopieren\" action=copy\n" +
                        "Button label=\"Mehr Übungen\" action=ask:\"Gib mir Übungsaufgaben zu Brüchen\"\n" +
                        "```",
                memoryQuestion = "Ich lerne gerade Spanisch. Hast du ein Rezept für heute Abend?",
                memoryAnswer =
                    "Wie wäre es mit einer **Tortilla de patatas** – ohne Pilze, ganz vegetarisch:\n\n" +
                        "- 4 Kartoffeln, 1 Zwiebel, 5 Eier\n- langsam in Olivenöl garen\n\n¡Buen provecho!",
                memoryUsed = listOf("isst vegetarisch", "mag keine Pilze"),
                memorySuggestion = "lernt Spanisch",
                docName = "Bedienungsanleitung.pdf",
                docQuestion = "Wie lange hält der Akku, und wie lade ich ihn?",
                docAnswer =
                    "Laut Anleitung hält der Akku **bis zu 8 Stunden** [S. 4].\n\n" +
                        "Zum Laden das Kabel an die Buchse unten stecken; die Lampe leuchtet grün, " +
                        "wenn er voll ist [S. 5].",
                eggQuestion = "Wie lange kocht ein Ei weich?",
                eggAnswer = "Etwa **6 Minuten** in kochendem Wasser, dann kurz abschrecken.",
                eggFollow = "Und hart?",
                eggStreaming = "Hart gekocht braucht ein Ei",
                eggQueued = "Und wie lange bei einem Wachtelei?",
                eggTyping = "Noch eine Frage",
            )

        val EN =
            WebTexts(
                shot = ShotTexts.EN,
                weatherQuestion = "What's the weather in Paris tomorrow, and what is the Eiffel Tower?",
                weatherAnswer =
                    "Tomorrow will be mild in **Paris**, around 15 °C with hardly any rain.\n\n" +
                        "The Eiffel Tower is a wrought-iron tower built for the 1889 World's Fair.",
                weatherStep = "Paris, tomorrow",
                wikiStep = "Eiffel Tower",
                weatherJson =
                    """{"place":"Paris","now":{"t":12.4,"wind_kmh":9.0,"sky":"cloudy"},"days":[""" +
                        """{"date":"2026-11-06","day":"today","max":13,"min":7,"rain_chance":20,"sky":"cloudy"},""" +
                        """{"date":"2026-11-07","max":15,"min":8,"rain_chance":10,"sky":"sunny"},""" +
                        """{"date":"2026-11-08","max":14,"min":9,"rain_chance":40,"sky":"showers"}]}""",
                sources =
                    listOf(
                        "Eiffel Tower – Wikipedia" to "https://en.wikipedia.org/wiki/Eiffel_Tower",
                        "Open-Meteo" to "https://open-meteo.com",
                    ),
                researchQuestion = "How do I care for a monstera in winter?",
                researchQuery = "monstera care winter",
                researchPartial =
                    "In winter a **monstera** needs less water:\n\n" +
                        "- water only when the top 3 cm of soil are dry\n- bright, but away from the radiator",
                researchSources =
                    listOf(
                        "Monstera in winter – plant blog" to "https://plants.example.org/monstera",
                        "Houseplant guide" to "https://guide.example.org/winter",
                    ),
                actionQuestion = "Wake me every weekday at 6:30 and set a timer for 8 minutes",
                actionAnswer = "Prepared – tap **Set**, then your clock app takes over.",
                alarmLabel = "Get up",
                timerLabel = "Eggs",
                cardQuestion = "Make me a study plan for Friday's maths test",
                cardAnswer =
                    "Sure, here is your plan until Friday:\n\n" +
                        "```card\n" +
                        "Card title=\"Maths study plan\"\n" +
                        "Table\n" +
                        "| Day | Topic | Time |\n" +
                        "| Mon | Fractions | 30 min |\n" +
                        "| Tue | Equations | 30 min |\n" +
                        "| Wed | Practice test | 45 min |\n" +
                        "Checklist\n" +
                        "- Write a formula sheet\n" +
                        "- Do the exercises on p. 42\n" +
                        "- Collect questions\n" +
                        "Progress value=40 label=\"Done so far\"\n" +
                        "Button label=\"Copy\" action=copy\n" +
                        "Button label=\"More exercises\" action=ask:\"Give me exercises on fractions\"\n" +
                        "```",
                memoryQuestion = "I'm learning Spanish at the moment. Got a recipe for tonight?",
                memoryAnswer =
                    "How about a **tortilla de patatas** – no mushrooms, fully vegetarian:\n\n" +
                        "- 4 potatoes, 1 onion, 5 eggs\n- cook slowly in olive oil\n\n¡Buen provecho!",
                memoryUsed = listOf("is vegetarian", "doesn't like mushrooms"),
                memorySuggestion = "is learning Spanish",
                docName = "Manual.pdf",
                docQuestion = "How long does the battery last, and how do I charge it?",
                docAnswer =
                    "According to the manual the battery lasts **up to 8 hours** [p. 4].\n\n" +
                        "To charge it, plug the cable into the socket at the bottom; the light turns green " +
                        "when it is full [p. 5].",
                eggQuestion = "How long does a soft-boiled egg take?",
                eggAnswer = "About **6 minutes** in boiling water, then rinse it in cold water.",
                eggFollow = "And hard-boiled?",
                eggStreaming = "A hard-boiled egg takes",
                eggQueued = "And a quail egg?",
                eggTyping = "One more question",
            )
    }
}
