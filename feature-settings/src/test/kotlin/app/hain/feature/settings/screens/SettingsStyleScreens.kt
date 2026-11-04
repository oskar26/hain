package app.hain.feature.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Settings in the Lichtung style (docs/design/STYLE_OPTIONS.md), light and dark; CI workflow screens.yml only. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class SettingsStyleScreens(
    private val dark: Boolean,
) {
    @get:Rule
    val compose = createComposeRule()

    private val actions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    private val settings = HainSettings()

    private fun render(page: SettingsPage?) {
        compose.setContent {
            HainTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalReduceMotion provides true) {
                    Box(Modifier.fillMaxSize().background(LocalHainColors.current.bg)) {
                        SettingsBody(settings, actions, startPage = page)
                    }
                }
            }
        }
    }

    private fun name(screen: String) = "lichtung_${screen}_${if (dark) "dark" else "light"}"

    @Test
    fun hub() {
        render(null)
        captureScreenRoboImage("$OUT/styles/${name("settings")}.png")
    }

    @Test
    fun lookPage() {
        render(SettingsPage.LOOK)
        captureScreenRoboImage("$OUT/styles/${name("settings_look")}.png")
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params(): List<Array<Any>> = listOf(false, true).map { arrayOf<Any>(it) }
    }
}

private const val OUT = "build/outputs/roborazzi"
