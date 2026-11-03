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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Welcome tour screenshots for docs/design/screens (T-011); run only via CI workflow screens.yml. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class OnboardingScreens {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun welcomeLight() {
        compose.setContent { Frame(dark = false) { WelcomeTour(onFinish = {}) } }
        captureScreenRoboImage("$OUT/onboarding_light.png")
    }

    @Test
    fun welcomeDark() {
        compose.setContent { Frame(dark = true) { WelcomeTour(onFinish = {}) } }
        captureScreenRoboImage("$OUT/onboarding_dark.png")
    }

    @Test
    fun success() {
        compose.setContent { Frame(dark = false) { SuccessScreen(onDone = {}) } }
        captureScreenRoboImage("$OUT/onboarding_success_light.png")
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
