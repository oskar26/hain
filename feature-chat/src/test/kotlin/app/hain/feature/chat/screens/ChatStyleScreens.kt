package app.hain.feature.chat.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import app.hain.common.api.AnswerMode
import app.hain.feature.chat.internal.ChatUiState
import app.hain.feature.chat.internal.UiMessage
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Lichtung style (docs/design/STYLE_OPTIONS.md) on the chat screens, light and dark. Only run
 * via CI workflow screens.yml; files land in build/outputs/roborazzi/styles/.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class ChatStyleScreens(
    private val dark: Boolean,
) {
    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String) {
        compose.waitForIdle()
        captureScreenRoboImage("$OUT/styles/${styleShotName(dark, name)}.png")
    }

    @Test
    fun emptyChat() {
        compose.setContent { Frame(dark) { ChatFrame(ChatUiState()) } }
        shot("chat_empty")
    }

    @Test
    fun longAnswer() {
        val question = UiMessage(1, true, "Vergleich bitte Fahrrad, Bus und Auto")
        val state = ChatUiState(messages = listOf(question, UiMessage(2, false, LONG_MD)))
        compose.setContent { Frame(dark) { ChatFrame(state, AnswerMode.THOROUGH) } }
        shot("chat_long_answer")
    }

    @Test
    fun composerWithBrainPicker() {
        val input = List(5) { "Zeile ${it + 1}: noch ein Gedanke für die Frage" }.joinToString("\n")
        compose.setContent {
            Frame(dark) { ChatFrame(answerState().copy(input = input), AnswerMode.FAST) }
        }
        compose.onNodeWithContentDescription("Denk-Stufe", substring = true).performClick()
        compose.waitForIdle()
        shot("chat_composer_picker")
    }

    @Test
    fun drawer() {
        compose.setContent {
            Frame(dark) {
                ChatFrame(answerState())
                Drawer()
            }
        }
        shot("drawer")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params(): List<Array<Any>> = styleParams()
    }
}

/** Light and dark. */
internal fun styleParams(): List<Array<Any>> = listOf(false, true).map { arrayOf<Any>(it) }

/** e.g. "lichtung_chat_empty_light". */
internal fun styleShotName(
    dark: Boolean,
    screen: String,
): String = "lichtung_${screen}_${if (dark) "dark" else "light"}"
