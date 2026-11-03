package app.hain.feature.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import app.hain.common.api.HainSettings
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

/** Settings screenshots for docs/design/screens (T-011); run only via CI workflow screens.yml. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class SettingsScreens {
    @get:Rule
    val compose = createComposeRule()

    private val actions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {})

    @Test
    fun hubLight() {
        compose.setContent { Frame(dark = false) { SettingsBody(HainSettings(), actions) } }
        captureScreenRoboImage("$OUT/settings_light.png")
    }

    @Test
    fun hubDark() {
        compose.setContent { Frame(dark = true) { SettingsBody(HainSettings(), actions) } }
        captureScreenRoboImage("$OUT/settings_dark.png")
    }

    @Test
    fun answersPage() {
        compose.setContent {
            Frame(dark = false) { SettingsBody(HainSettings(), actions, startPage = SettingsPage.ANSWERS) }
        }
        captureScreenRoboImage("$OUT/settings_answers_light.png")
    }

    @Test
    fun lookPageDark() {
        compose.setContent {
            Frame(dark = true) { SettingsBody(HainSettings(), actions, startPage = SettingsPage.LOOK) }
        }
        captureScreenRoboImage("$OUT/settings_look_dark.png")
    }
}

private const val OUT = "build/outputs/roborazzi"

@Composable
private fun Frame(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    HainTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalReduceMotion provides true) {
            Box(Modifier.fillMaxSize().background(LocalHainColors.current.bg)) { content() }
        }
    }
}
