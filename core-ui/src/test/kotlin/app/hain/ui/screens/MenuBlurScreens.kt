package app.hain.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.hain.ui.HainIcons
import app.hain.ui.components.HainPopupItem
import app.hain.ui.components.HainPopupMenu
import app.hain.ui.effects.HainMenuBackdrop
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.pim.Pim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainTypography
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** T-104: open ⋯ menu with the soft backdrop blur; run only via CI workflow screens.yml. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class MenuBlurScreens {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun menuBlurLight() {
        compose.setContent { Frame(dark = false) }
        compose.waitForIdle()
        captureScreenRoboImage("$OUT/menu_blur_light.png")
    }

    @Test
    fun menuBlurDark() {
        compose.setContent { Frame(dark = true) }
        compose.waitForIdle()
        captureScreenRoboImage("$OUT/menu_blur_dark.png")
    }
}

private const val OUT = "build/outputs/roborazzi"

@Composable
private fun Frame(dark: Boolean) {
    HainTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalReduceMotion provides true) {
            Box(Modifier.fillMaxSize().background(LocalHainColors.current.bg)) {
                HainMenuBackdrop { Page() }
            }
        }
    }
}

@Composable
private fun Page() {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Pim(PimState.HAPPY, 96.dp)
        repeat(LINES) {
            BasicText(
                "Chat ${it + 1} · Wie wird das Wetter morgen in Köln? Pim schaut nach.",
                style = type.body.copy(color = if (it == 2) colors.text else colors.textMuted),
            )
        }
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopEnd) {
            BasicText("⋯", style = type.title.copy(color = colors.text))
            HainPopupMenu(expanded = true, onDismiss = {}) {
                HainPopupItem(HainIcons.pin, "Anheften", {})
                HainPopupItem(HainIcons.edit, "Umbenennen", {})
                HainPopupItem(HainIcons.delete, "Löschen", {}, danger = true)
            }
        }
        repeat(LINES) {
            BasicText("Noch eine Zeile Text hinter dem Menü, damit man den Blur sieht.", style = type.body)
        }
    }
}

private const val LINES = 5
