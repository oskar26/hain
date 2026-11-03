package app.hain.feature.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import app.hain.common.api.HainSettings
import app.hain.common.api.UiStyle
import app.hain.feature.settings.api.SettingsActions
import app.hain.feature.settings.api.SettingsBody
import app.hain.feature.settings.api.SettingsPage
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.HainStyle
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

/** Settings in the style proposals A and B (docs/design/STYLE_OPTIONS.md); CI workflow screens.yml only. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class SettingsStyleScreens(
    private val style: HainStyle,
    private val dark: Boolean,
) {
    @get:Rule
    val compose = createComposeRule()

    private val actions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    private val settings = HainSettings(uiStyle = if (style == HainStyle.B) UiStyle.B else UiStyle.A)

    private fun render(page: SettingsPage?) {
        compose.setContent {
            HainTheme(darkTheme = dark, style = style) {
                CompositionLocalProvider(LocalReduceMotion provides true) {
                    Box(Modifier.fillMaxSize().background(LocalHainColors.current.bg)) {
                        SettingsBody(settings, actions, startPage = page)
                    }
                }
            }
        }
    }

    private fun name(screen: String) = "${style.name.lowercase()}_${screen}_${if (dark) "dark" else "light"}"

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
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}-dark={1}")
        fun params(): List<Array<Any>> {
            val modes = listOf(false, true)
            return HainStyle.entries.flatMap { s -> modes.map { d -> arrayOf<Any>(s, d) } }
        }
    }
}

private const val OUT = "build/outputs/roborazzi"
