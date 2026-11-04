package app.hain.feature.chat.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.hain.feature.chat.api.BrainPickerState
import app.hain.feature.chat.api.Greeting
import app.hain.feature.chat.api.ModelPicker
import app.hain.models.api.BrainChoice
import app.hain.models.api.DeviceProfile
import app.hain.models.api.ModelFile
import app.hain.models.api.ModelInfo
import app.hain.ui.HainIcons
import app.hain.ui.components.HainIconButton
import app.hain.ui.components.TopBar
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** T-055: the open brain picker under "Hain · Auf diesem Handy" (screens.yml). */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class ModelPickerScreens {
    @get:Rule
    val compose = createComposeRule()

    private fun brain(
        id: String,
        name: String,
        bytes: Long,
        minRam: Int,
    ) = ModelInfo(
        id = id,
        name = name,
        kind = "brain",
        files = listOf(ModelFile("https://a/$id.gguf", "0".repeat(64), bytes)),
        minRamGb = minRam,
        licence = "l",
        licenceUrl = "u",
        isDefault = id == "small",
    )

    private val catalog =
        listOf(
            brain("small", "LFM3 1.7B", 1_049_615_680L, 4),
            brain("medium", "Beispiel 4B", 2_500_000_000L, 6),
            brain("large", "Beispiel 8B", 5_000_000_000L, 12),
        )

    private fun render(dark: Boolean) {
        compose.setContent {
            Frame(dark = dark) {
                val state =
                    remember {
                        BrainPickerState(
                            options = {
                                BrainChoice.options(
                                    catalog,
                                    "small",
                                    { it.id != "large" },
                                    DeviceProfile(8, 10_000_000_000L),
                                )
                            },
                        )
                    }
                var open by remember { mutableStateOf(false) }
                Column(Modifier.fillMaxSize()) {
                    TopBar(
                        title = "Hain",
                        subtitle = "Auf diesem Handy",
                        leading = { HainIconButton(HainIcons.menu, "Chats", {}) },
                        trailing = { HainIconButton(HainIcons.plus, "Neu", {}) },
                        onTitleClick = { open = !open },
                        titleAccessory = { ModelPicker(open, { open = false }, state) },
                    )
                    Greeting(onSuggestion = {}, hour = 15, seed = 3)
                }
            }
        }
        compose.onNodeWithText("Hain").performClick()
        compose.waitForIdle()
    }

    @Test
    fun brainPickerLight() {
        render(dark = false)
        captureScreenRoboImage("$OUT/chat_brain_picker_light.png")
    }

    @Test
    fun brainPickerDark() {
        render(dark = true)
        captureScreenRoboImage("$OUT/chat_brain_picker_dark.png")
    }
}
