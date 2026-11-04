package app.hain.feature.memory.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import app.hain.feature.memory.api.MemoryActions
import app.hain.feature.memory.api.MemoryPage
import app.hain.feature.memory.api.MemoryPageState
import app.hain.feature.memory.api.MemoryTab
import app.hain.memory.api.MemoryCategory
import app.hain.memory.api.MemoryFact
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

/** Memory page screenshots for docs/design/screens (T-115); run only via CI workflow screens.yml. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class MemoryScreens {
    @get:Rule
    val compose = createComposeRule()

    private val day = 86_400_000L
    private val now = 1_793_750_400_000L

    private val facts =
        listOf(
            MemoryFact(1, "spielt Gitarre im Schulorchester", MemoryCategory.PREFERENCE, now),
            MemoryFact(2, "mag keine Pilze", MemoryCategory.PREFERENCE, now - 3 * day),
            MemoryFact(3, "Schwester Rosa, Bruder Artur", MemoryCategory.PERSON, now - 9 * day),
            MemoryFact(4, "geht in die 10. Klasse", MemoryCategory.WORK, now - 20 * day),
            MemoryFact(5, "wohnt in Köln-Rodenkirchen", MemoryCategory.PLACE, now - 30 * day),
            MemoryFact(6, "hat Heuschnupfen", MemoryCategory.HEALTH, now, suggested = true, sensitive = true),
        )

    @Test
    fun factsLight() {
        compose.setContent { Frame(dark = false) { MemoryPage(MemoryPageState(facts), MemoryActions()) } }
        captureScreenRoboImage("$OUT/memory_light.png")
    }

    @Test
    fun factsDark() {
        compose.setContent { Frame(dark = true) { MemoryPage(MemoryPageState(facts), MemoryActions()) } }
        captureScreenRoboImage("$OUT/memory_dark.png")
    }

    @Test
    fun suggestionsLight() {
        compose.setContent {
            Frame(dark = false) {
                MemoryPage(MemoryPageState(facts), MemoryActions(), startTab = MemoryTab.SUGGESTIONS)
            }
        }
        captureScreenRoboImage("$OUT/memory_suggestions_light.png")
    }

    @Test
    fun emptyLight() {
        compose.setContent { Frame(dark = false) { MemoryPage(MemoryPageState(), MemoryActions()) } }
        captureScreenRoboImage("$OUT/memory_empty_light.png")
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
