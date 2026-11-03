package app.hain.feature.settings.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import app.hain.common.api.HainSettings
import app.hain.feature.settings.api.SettingsActions
import app.hain.feature.settings.api.SettingsBody
import app.hain.feature.settings.api.SettingsPage
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.HainStyle
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.hainBackdrop
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Style decision (03.11.2026, STYLE_OPTIONS.md): A and B, light and dark. Only in CI (screens.yml). */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class StyleScreens {
    @get:Rule
    val compose = createComposeRule()

    private fun record(
        name: String,
        content: @Composable () -> Unit,
    ) {
        val combo = mutableStateOf(COMBOS.first())
        compose.setContent {
            HainTheme(darkTheme = combo.value.second, style = combo.value.first) {
                CompositionLocalProvider(LocalReduceMotion provides true) {
                    Box(Modifier.fillMaxSize().hainBackdrop()) { content() }
                }
            }
        }
        COMBOS.forEach { c ->
            combo.value = c
            compose.waitForIdle()
            val mode = if (c.second) "dark" else "light"
            captureScreenRoboImage(
                "build/outputs/roborazzi/style_${c.first.name}_${name}_$mode.png",
            )
        }
    }

    private val actions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {})

    @Test
    fun hub() = record("settings") { SettingsBody(HainSettings(), actions) }

    @Test
    fun lookPage() =
        record("settings_look") {
            SettingsBody(HainSettings(), actions, startPage = SettingsPage.LOOK)
        }
}

private val COMBOS =
    listOf(HainStyle.A to false, HainStyle.A to true, HainStyle.B to false, HainStyle.B to true)
