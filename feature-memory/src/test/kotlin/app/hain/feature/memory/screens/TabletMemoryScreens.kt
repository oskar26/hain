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
import app.hain.feature.memory.api.SmartSearch
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

/** T-251: the memory page on a tablet in landscape (1280 × 800 dp): list left, "Über dich" + settings right. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w1280dp-h800dp-land-xhdpi")
class TabletMemoryScreens {
    @get:Rule
    val compose = createComposeRule()

    private val now = 1_793_750_400_000L
    private val day = 86_400_000L

    private val facts =
        listOf(
            MemoryFact(1, "mag Brettspiele", MemoryCategory.PREFERENCE, now),
            MemoryFact(2, "mag keine Pilze", MemoryCategory.PREFERENCE, now - 3 * day),
            MemoryFact(3, "hat eine Katze namens Mo", MemoryCategory.PERSON, now - 9 * day),
            MemoryFact(4, "lernt gerade Spanisch", MemoryCategory.WORK, now - 20 * day),
            MemoryFact(5, "fährt gern Fahrrad", MemoryCategory.OTHER, now - 30 * day),
            MemoryFact(6, "trinkt keinen Kaffee", MemoryCategory.PREFERENCE, now, suggested = true),
        )

    @Test
    fun tabletMemoryLight() {
        val state = MemoryPageState(facts, aboutMe = "Ich mag kurze Antworten.", smartSearch = SmartSearch.Offer(219))
        compose.setContent { TabletFrame(dark = false) { MemoryPage(state, MemoryActions()) } }
        captureScreenRoboImage("$OUT/tablet_memory_light.png")
    }

    @Test
    fun tabletMemoryDark() {
        val state = MemoryPageState(facts, smartSearch = SmartSearch.On)
        compose.setContent { TabletFrame(dark = true) { MemoryPage(state, MemoryActions()) } }
        captureScreenRoboImage("$OUT/tablet_memory_dark.png")
    }
}

private const val OUT = "build/outputs/roborazzi"

@Composable
private fun TabletFrame(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    HainTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalReduceMotion provides true) {
            Box(Modifier.fillMaxSize().background(LocalHainColors.current.bg)) { content() }
        }
    }
}
