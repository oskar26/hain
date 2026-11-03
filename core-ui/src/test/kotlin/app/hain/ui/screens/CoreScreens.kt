package app.hain.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.hain.ui.components.Hain3DIconsPreview
import app.hain.ui.components.HainButton
import app.hain.ui.components.HainButtonStyle
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.pim.Pim
import app.hain.ui.pim.PimState
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

/** Pim, 3D icons and buttons for docs/design/screens (T-011); run only via CI workflow screens.yml. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class CoreScreens {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun kitLight() {
        compose.setContent { Frame(dark = false) { Kit() } }
        captureScreenRoboImage("$OUT/kit_light.png")
    }

    @Test
    fun kitDark() {
        compose.setContent { Frame(dark = true) { Kit() } }
        captureScreenRoboImage("$OUT/kit_dark.png")
    }
}

@Composable
private fun Kit() {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Pim(PimState.IDLE, 120.dp)
            Pim(PimState.HAPPY, 120.dp)
            Pim(PimState.SLEEPING, 72.dp)
        }
        Hain3DIconsPreview()
        HainButton("Los geht's", {}, Modifier.padding(horizontal = 8.dp))
        HainButton("Weiter", {}, style = HainButtonStyle.DARK)
        HainButton("Überspringen", {}, style = HainButtonStyle.SECONDARY)
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
