package app.hain.feature.chat.api

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.hain.models.api.BrainChoice
import app.hain.models.api.BrainOption
import app.hain.models.api.DeviceProfile
import app.hain.models.api.ModelFile
import app.hain.models.api.ModelInfo
import app.hain.ui.components.TopBar
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.HainTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** T-055: the brain picker in the top bar with a fake catalogue. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "en")
class ModelPickerTest {
    @get:Rule
    val compose = createComposeRule()

    private fun brain(
        id: String,
        name: String,
        bytes: Long,
        minRam: Int,
        isDefault: Boolean = false,
    ) = ModelInfo(
        id = id,
        name = name,
        kind = "brain",
        files = listOf(ModelFile("https://a/$id.gguf", "0".repeat(64), bytes)),
        minRamGb = minRam,
        licence = "l",
        licenceUrl = "u",
        isDefault = isDefault,
    )

    private val small = brain("small", "LFM3 1.7B", 1_000_000_000L, 4, isDefault = true)
    private val medium = brain("medium", "Mid 4B", 2_500_000_000L, 6)
    private val large = brain("large", "Big 8B", 5_000_000_000L, 12)
    private val catalog = listOf(small, medium, large)

    private val picked = mutableListOf<String>()
    private var packsOpened = 0

    private fun show(installed: Set<ModelInfo>) {
        compose.setContent {
            HainTheme(darkTheme = false) {
                CompositionLocalProvider(LocalReduceMotion provides true) {
                    var selected by remember { mutableStateOf<String?>(null) }
                    val state =
                        remember {
                            BrainPickerState(
                                options = {
                                    BrainChoice.options(catalog, selected, { it in installed }, DeviceProfile(8, 0L))
                                },
                                onPick = { option: BrainOption ->
                                    selected = option.model.id
                                    picked += option.model.id
                                },
                                onGetMore = { packsOpened++ },
                            )
                        }
                    var open by remember { mutableStateOf(false) }
                    TopBar(
                        title = "Hain",
                        subtitle = "On this phone",
                        onTitleClick = { open = !open },
                        titleClickLabel = "choose brain",
                        titleAccessory = { ModelPicker(open, { open = false }, state) },
                    )
                }
            }
        }
    }

    private fun openMenu() {
        compose.onNodeWithText("Hain").performClick()
        compose.waitForIdle()
    }

    @Test
    fun listsInstalledBrainsWithPlainNamesAndTheRecommendation() {
        show(setOf(small, medium))
        openMenu()
        compose.onNodeWithText("Pim’s brain").assertExists()
        compose.onNodeWithContentDescription("Quick brain", substring = true).assertIsSelected()
        compose
            .onNodeWithContentDescription("Balanced brain", substring = true)
            .assertExists()
        compose.onNodeWithText("Recommended for your phone").assertExists()
        compose.onNodeWithContentDescription("Not on your phone yet", substring = true).assertExists()
    }

    @Test
    fun pickingAnotherBrainSwitchesAndClosesTheMenu() {
        show(setOf(small, medium))
        openMenu()
        compose.onNodeWithContentDescription("Balanced brain", substring = true).performClick()
        compose.waitForIdle()
        assertEquals(listOf("medium"), picked)
        compose.onNodeWithText("Pim’s brain").assertDoesNotExist()
        openMenu()
        compose.onNodeWithContentDescription("Balanced brain", substring = true).assertIsSelected()
    }

    @Test
    fun theBrainInUseDoesNotReload() {
        show(setOf(small))
        openMenu()
        compose.onNodeWithContentDescription("Quick brain", substring = true).performClick()
        compose.waitForIdle()
        assertTrue(picked.isEmpty())
    }

    @Test
    fun aBrainNotOnThePhoneOpensTheKnowledgePacks() {
        show(setOf(small))
        openMenu()
        compose.onNodeWithContentDescription("Smart brain", substring = true).performClick()
        compose.waitForIdle()
        assertTrue(picked.isEmpty())
        assertEquals(1, packsOpened)
    }

    @Test
    fun moreBrainsOpensTheKnowledgePacks() {
        show(setOf(small))
        openMenu()
        compose.onNodeWithText("More brains in Knowledge packs").performClick()
        compose.waitForIdle()
        assertEquals(1, packsOpened)
    }
}
