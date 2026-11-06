package app.hain.feature.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import app.hain.common.api.DevSettings
import app.hain.common.api.HainSettings
import app.hain.feature.settings.api.AppInfo
import app.hain.feature.settings.api.SettingsActions
import app.hain.feature.settings.api.SettingsBody
import app.hain.feature.settings.api.SettingsExtras
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

/**
 * Settings in the Lichtung style (docs/design/STYLE_OPTIONS.md), light and dark, German; CI workflow
 * screens.yml only.
 */
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class SettingsStyleScreens(
    dark: Boolean,
) : SettingsStyleShots(dark, suffix = "") {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params(): List<Array<Any>> = listOf(false, true).map { arrayOf<Any>(it) }
    }
}

/** The same screens in English for the English website (T-107), file names end in "_en". */
@Config(sdk = [35], qualifiers = "en-w392dp-h850dp-xxhdpi")
class SettingsStyleScreensEn(
    dark: Boolean,
) : SettingsStyleShots(dark, suffix = "_en") {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params(): List<Array<Any>> = listOf(false, true).map { arrayOf<Any>(it) }
    }
}

/** The screenshots themselves; the subclasses pick the language. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class SettingsStyleShots(
    private val dark: Boolean,
    private val suffix: String,
) {
    @get:Rule
    val compose = createComposeRule()

    private val actions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {})
    private val settings = HainSettings()

    private fun render(
        page: SettingsPage?,
        values: HainSettings = settings,
        extras: SettingsExtras = SettingsExtras(),
    ) {
        compose.setContent {
            HainTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalReduceMotion provides true) {
                    Box(Modifier.fillMaxSize().background(LocalHainColors.current.bg)) {
                        SettingsBody(values, actions, startPage = page, extras = extras)
                    }
                }
            }
        }
    }

    private fun name(screen: String) = "lichtung_${screen}_${if (dark) "dark" else "light"}"

    @Test
    fun hub() {
        render(null)
        captureScreenRoboImage("$OUT/styles/${name("settings")}$suffix.png")
    }

    /** T-214: "Über Hain" with Pim, links, version and the update check. */
    @Test
    fun aboutPage() {
        render(
            SettingsPage.ABOUT,
            extras = SettingsExtras(AppInfo("0.1.3.1-debug", 412, "a1b2c3d4e5f6", "2026-11-04")),
        )
        captureScreenRoboImage("$OUT/styles/${name("settings_about")}$suffix.png")
    }

    /** T-211: developer settings (unlocked). */
    @Test
    fun developerPage() {
        render(SettingsPage.DEVELOPER, settings.copy(dev = DevSettings(enabled = true, showStats = true)))
        captureScreenRoboImage("$OUT/styles/${name("settings_dev")}$suffix.png")
    }

    /** T-283: one page for everything that goes online, main switch on and off. */
    @Test
    fun internetPage() {
        render(SettingsPage.INTERNET)
        captureScreenRoboImage("$OUT/styles/${name("settings_internet")}$suffix.png")
    }

    @Test
    fun internetPageOff() {
        render(SettingsPage.INTERNET, settings.copy(offlineMode = true))
        captureScreenRoboImage("$OUT/styles/${name("settings_internet_off")}$suffix.png")
    }

    @Test
    fun answersPage() {
        render(SettingsPage.ANSWERS)
        captureScreenRoboImage("$OUT/styles/${name("settings_answers")}$suffix.png")
    }

    @Test
    fun lookPage() {
        render(SettingsPage.LOOK)
        captureScreenRoboImage("$OUT/styles/${name("settings_look")}$suffix.png")
    }
}

private const val OUT = "build/outputs/roborazzi"
