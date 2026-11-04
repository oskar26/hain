package app.hain.feature.chat.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import app.hain.common.api.AnswerMode
import app.hain.feature.chat.internal.ChatUiState
import app.hain.feature.chat.internal.StepState
import app.hain.feature.chat.internal.ToolStep
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
 * The Lichtung style (docs/design/STYLE_OPTIONS.md) on the chat screens, light and dark, German.
 * Only run via CI workflow screens.yml; files land in build/outputs/roborazzi/styles/.
 */
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class ChatStyleScreens(
    dark: Boolean,
) : ChatStyleShots(dark, ShotTexts.DE, suffix = "") {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params(): List<Array<Any>> = styleParams()
    }
}

/** The same chat screens in English for the English website (T-107), e.g. "lichtung_chat_empty_light_en". */
@Config(sdk = [35], qualifiers = "en-w392dp-h850dp-xxhdpi")
class ChatStyleScreensEn(
    dark: Boolean,
) : ChatStyleShots(dark, ShotTexts.EN, suffix = "_en") {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params(): List<Array<Any>> = styleParams()
    }
}

/** The chat screenshots themselves; the subclasses pick the language. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class ChatStyleShots(
    private val dark: Boolean,
    private val texts: ShotTexts,
    private val suffix: String,
) {
    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String) {
        compose.waitForIdle()
        captureScreenRoboImage("$OUT/styles/${styleShotName(dark, name)}$suffix.png")
    }

    @Test
    fun emptyChat() {
        compose.setContent { Frame(dark) { ChatFrame(ChatUiState(), texts = texts) } }
        shot("chat_empty")
    }

    @Test
    fun longAnswer() {
        val question = UiMessage(1, true, texts.compare)
        val state = ChatUiState(messages = listOf(question, UiMessage(2, false, texts.longMarkdown)))
        compose.setContent { Frame(dark) { ChatFrame(state, AnswerMode.THOROUGH, texts) } }
        shot("chat_long_answer")
    }

    /** T-213: Pim peeks in at the newest answer while a tool runs. */
    @Test
    fun pimWhileToolsRun() {
        val question = UiMessage(1, true, texts.compare)
        val answer =
            UiMessage(
                2,
                false,
                "",
                streaming = true,
                steps = listOf(ToolStep("web_search", "Köln Wetter", StepState.RUNNING, online = true)),
            )
        compose.setContent { Frame(dark) { ChatFrame(ChatUiState(messages = listOf(question, answer)), texts = texts) } }
        shot("chat_pim_tools")
    }

    @Test
    fun composerWithBrainPicker() {
        val input = List(5) { texts.composerLine(it + 1) }.joinToString("\n")
        compose.setContent {
            Frame(dark) { ChatFrame(answerState(texts).copy(input = input), AnswerMode.FAST, texts) }
        }
        compose.onNodeWithContentDescription(texts.thinkingButton, substring = true).performClick()
        compose.waitForIdle()
        shot("chat_composer_picker")
    }

    @Test
    fun drawer() {
        compose.setContent {
            Frame(dark) {
                ChatFrame(answerState(texts), texts = texts)
                Drawer(texts = texts)
            }
        }
        shot("drawer")
    }
}

/** Light and dark. */
internal fun styleParams(): List<Array<Any>> = listOf(false, true).map { arrayOf<Any>(it) }

/** e.g. "lichtung_chat_empty_light". */
internal fun styleShotName(
    dark: Boolean,
    screen: String,
): String = "lichtung_${screen}_${if (dark) "dark" else "light"}"
