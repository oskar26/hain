package app.hain.feature.onboarding.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import app.hain.feature.onboarding.api.SuccessScreen
import app.hain.feature.onboarding.api.WelcomeTour
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
 * Welcome tour in the Lichtung style (docs/design/STYLE_OPTIONS.md), light and dark, German; CI workflow
 * screens.yml only.
 */
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class OnboardingStyleScreens(
    dark: Boolean,
) : OnboardingStyleShots(dark, suffix = "") {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params(): List<Array<Any>> = listOf(false, true).map { arrayOf<Any>(it) }
    }
}

/** The same screens in English for the English website (T-107), file names end in "_en". */
@Config(sdk = [35], qualifiers = "en-w392dp-h850dp-xxhdpi")
class OnboardingStyleScreensEn(
    dark: Boolean,
) : OnboardingStyleShots(dark, suffix = "_en") {
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
abstract class OnboardingStyleShots(
    private val dark: Boolean,
    private val suffix: String,
) {
    @get:Rule
    val compose = createComposeRule()

    private fun render(content: @Composable () -> Unit) {
        compose.setContent {
            HainTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalReduceMotion provides true) {
                    Box(Modifier.fillMaxSize().background(LocalHainColors.current.bg)) { content() }
                }
            }
        }
    }

    private fun name(screen: String) = "lichtung_${screen}_${if (dark) "dark" else "light"}"

    @Test
    fun welcome() {
        render { WelcomeTour(onFinish = {}) }
        captureScreenRoboImage("$OUT/styles/${name("onboarding")}$suffix.png")
    }

    @Test
    fun success() {
        render { SuccessScreen(onDone = {}) }
        captureScreenRoboImage("$OUT/styles/${name("onboarding_success")}$suffix.png")
    }
}

private const val OUT = "build/outputs/roborazzi"
