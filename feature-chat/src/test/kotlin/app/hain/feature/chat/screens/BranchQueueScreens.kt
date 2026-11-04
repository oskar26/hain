package app.hain.feature.chat.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.api.MessageList
import app.hain.feature.chat.api.QueuedMessages
import app.hain.feature.chat.internal.BranchInfo
import app.hain.feature.chat.internal.ChatUiState
import app.hain.feature.chat.internal.QueuedMessage
import app.hain.feature.chat.internal.UiMessage
import app.hain.ui.components.Composer
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * T-057 screenshots: "‹ 2/2 ›" under an edited message and "‹ 3/3 ›" under an answer asked again,
 * and a message waiting ("wartet") while Pim answers. Recorded by the screens workflow only.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class BranchQueueScreens {
    @get:Rule
    val compose = createComposeRule()

    private fun state(): ChatUiState {
        val question = UiMessage(10, true, "Wie lange kocht ein Ei weich?")
        val answer = UiMessage(11, false, "Etwa **6 Minuten** in kochendem Wasser, dann kurz abschrecken.")
        val follow = UiMessage(12, true, "Und hart?")
        val streaming = UiMessage(13, false, "Hart gekocht braucht ein Ei", streaming = true)
        return ChatUiState(
            messages = listOf(question, answer, follow, streaming),
            isGenerating = true,
            branches = mapOf(10L to BranchInfo(2, 2, 1L, null), 11L to BranchInfo(3, 3, 2L, null)),
            queue = listOf(QueuedMessage(20, 1, "Und wie lange bei einem Wachtelei?")),
        )
    }

    @Test
    fun branchesAndWaitingMessage() {
        val s = state()
        compose.setContent {
            Frame(dark = false) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) { MessageList(s, emptyMap(), {}, {}, { _, _ -> }, { _, _, _ -> }) }
                    QueuedMessages(s.queue, {})
                    Composer(
                        text = "Noch eine Frage",
                        onTextChange = {},
                        placeholder = "Frag Pim",
                        isGenerating = true,
                        onSend = {},
                        onStop = {},
                        sendLabel = "Senden",
                        stopLabel = "Stopp",
                        modifier = Modifier.padding(12.dp),
                        canQueue = true,
                    )
                }
            }
        }
        captureScreenRoboImage("$OUT/branches_queue.png")
    }
}
