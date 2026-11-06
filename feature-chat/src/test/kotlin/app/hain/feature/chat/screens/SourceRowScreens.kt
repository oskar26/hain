package app.hain.feature.chat.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.api.SourceChips
import app.hain.feature.chat.internal.BranchInfo
import app.hain.feature.chat.internal.ChatUiState
import app.hain.feature.chat.internal.SourceRef
import app.hain.feature.chat.internal.StepState
import app.hain.feature.chat.internal.ToolStep
import app.hain.feature.chat.internal.UiMessage
import app.hain.ui.components.AnswerCitations
import app.hain.ui.components.AssistantBlock
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * T-274: green source chips in the text and the compact sources line (closed and open).
 * T-272: at 360 dp the row under an answer with "‹ 3/3 ›" stays one line, the page colour reaches under
 * the message box, a long tool step ends in "…". Recorded by the screens workflow only.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w360dp-h740dp-xhdpi")
class SourceRowScreens {
    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String) = captureScreenRoboImage("$OUT/$name.png")

    @Test
    fun sourceChipsLight() {
        compose.setContent { Frame(dark = false) { ChatFrame(sourcesState()) } }
        shot("sources_chips_360dp")
    }

    @Test
    fun sourceChipsDark() {
        compose.setContent { Frame(dark = true) { ChatFrame(sourcesState()) } }
        shot("sources_chips_360dp_dark")
    }

    @Test
    fun sourceListOpen() {
        compose.setContent {
            Frame(dark = false) {
                Column(Modifier.padding(16.dp)) {
                    AssistantBlock(
                        markdown = ANSWER,
                        citations = AnswerCitations(SOURCES.size, {}, { "Quelle $it" }),
                    )
                    SourceChips(SOURCES, initiallyOpen = true)
                }
            }
        }
        shot("sources_list_open")
    }

    @Test
    fun actionRowOneLine360() {
        compose.setContent { Frame(dark = false) { ChatFrame(branchState()) } }
        shot("action_row_branch_360dp")
    }

    companion object {
        private const val ANSWER =
            "Das **Deutschlandticket** kostet seit Januar 2026 **63 €** im Monat [1]. Es gilt bundesweit " +
                "im Nahverkehr [2], nicht aber im ICE oder IC [2, 3]."

        private val SOURCES =
            listOf(
                SourceRef("Deutschlandticket – Wikipedia", "https://de.wikipedia.org/wiki/Deutschlandticket"),
                SourceRef("Fragen und Antworten zum Deutschlandticket", "https://www.bundesregierung.de/dt"),
                SourceRef("Deutschlandticket: Was gilt im Fernverkehr?", "https://www.tagesschau.de/dt"),
            )

        internal fun sourcesState(): ChatUiState =
            ChatUiState(
                messages =
                    listOf(
                        UiMessage(1, true, "Was kostet das Deutschlandticket?"),
                        UiMessage(
                            2,
                            false,
                            ANSWER,
                            steps =
                                listOf(
                                    ToolStep(
                                        "web_search",
                                        "Deutschlandticket Preis 2026 und Gültigkeit im Fernverkehr",
                                        StepState.DONE,
                                        online = true,
                                    ),
                                    ToolStep("read_pages", "3", StepState.DONE, online = true),
                                ),
                            sources = SOURCES,
                        ),
                    ),
            )

        internal fun branchState(): ChatUiState {
            val base = sourcesState()
            return base.copy(branches = mapOf(2L to BranchInfo(3, 3, 1L, null)))
        }
    }
}
