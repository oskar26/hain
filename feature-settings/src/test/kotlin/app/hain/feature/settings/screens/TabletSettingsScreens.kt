package app.hain.feature.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import app.hain.common.api.HainSettings
import app.hain.feature.settings.api.PackCategory
import app.hain.feature.settings.api.PacksScreen
import app.hain.feature.settings.api.SettingsActions
import app.hain.feature.settings.api.SettingsBody
import app.hain.feature.settings.api.SettingsPage
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

/** T-235: settings as list-detail on a tablet in landscape (1280 × 800 dp); T-251: packs in two columns. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w1280dp-h800dp-land-xhdpi")
class TabletSettingsScreens {
    @get:Rule
    val compose = createComposeRule()

    private val actions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, onOpenNews = {})

    @Test
    fun tabletSettingsLight() {
        compose.setContent { TabletFrame(dark = false) { SettingsBody(HainSettings(), actions) } }
        captureScreenRoboImage("$OUT/tablet_settings_light.png")
    }

    @Test
    fun tabletSettingsAnswersDark() {
        compose.setContent {
            TabletFrame(dark = true) { SettingsBody(HainSettings(), actions, startPage = SettingsPage.ANSWERS) }
        }
        captureScreenRoboImage("$OUT/tablet_settings_answers_dark.png")
    }

    /** T-251: Modelle & Pakete in two columns on a wide window. */
    @Test
    fun tabletPacksLight() {
        compose.setContent { TabletFrame(dark = false) { PacksScreen(FakeModels, ramGb = 8, onBack = {}) } }
        captureScreenRoboImage("$OUT/tablet_packs_light.png")
    }

    @Test
    fun tabletPacksKnowledgeDark() {
        compose.setContent {
            TabletFrame(dark = true) {
                PacksScreen(FakeModels, ramGb = 8, onBack = {}, startCategory = PackCategory.KNOWLEDGE)
            }
        }
        captureScreenRoboImage("$OUT/tablet_packs_knowledge_dark.png")
    }
}

private const val OUT = "build/outputs/roborazzi"

@Composable
private fun TabletFrame(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    HainTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalReduceMotion provides true) {
            Box(Modifier.fillMaxSize().background(LocalHainColors.current.bg)) { content() }
        }
    }
}
