package app.hain.feature.chat.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.hain.feature.chat.api.PimStatus
import app.hain.feature.chat.internal.StepState
import app.hain.feature.chat.internal.ToolStep
import app.hain.feature.chat.internal.UiMessage
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * T-280: thinking and tool steps fold into one line ("Nachgedacht · 4 s ›", "3 Werkzeuge genutzt ›"),
 * closed and open, and the live line while a tool runs. Recorded by the screens workflow only.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w360dp-h740dp-xhdpi")
class StepsFoldScreens {
    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String) = captureScreenRoboImage("$OUT/$name.png")

    private val steps =
        listOf(
            ToolStep("wikipedia", "Moos", StepState.DONE, online = true),
            ToolStep("web_search", "Moose Pflanzen", StepState.DONE, online = true),
            ToolStep("read_pages", "3", StepState.DONE, online = true),
            ToolStep("weather", "Köln", StepState.DONE, online = true),
        )

    private val done =
        UiMessage(
            id = 7L,
            isUser = false,
            text = "Moose sind grüne Landpflanzen ohne echte Wurzeln.",
            reasoning = "Die Frage ist, was Moos ist.\nIch schaue bei Wikipedia nach und suche im Web.",
            thinkingSeconds = 4,
            steps = steps,
        )

    @Test
    fun folded() {
        compose.setContent {
            Frame(dark = false) {
                Column(Modifier.padding(16.dp)) { PimStatus(done, latest = false, cheer = false) }
            }
        }
        shot("steps_folded_360dp")
    }

    @Test
    fun unfolded() {
        compose.setContent {
            Frame(dark = false) {
                Column(Modifier.padding(16.dp)) {
                    PimStatus(done, latest = false, cheer = false, initiallyExpanded = true)
                }
            }
        }
        shot("steps_unfolded_360dp")
    }

    @Test
    fun liveWhileWorking() {
        val running =
            done.copy(
                text = "",
                streaming = true,
                steps = steps.take(2) + ToolStep("read_pages", null, StepState.RUNNING, online = true),
            )
        compose.setContent {
            Frame(dark = true) {
                Column(Modifier.padding(16.dp)) { PimStatus(running, latest = false, cheer = false) }
            }
        }
        shot("steps_live_360dp_dark")
    }
}
