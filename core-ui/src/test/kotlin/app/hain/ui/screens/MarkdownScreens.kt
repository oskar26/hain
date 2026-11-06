package app.hain.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import app.hain.ui.components.AnswerCitations
import app.hain.ui.components.AssistantBlock
import app.hain.ui.components.LocalMarkdownLabels
import app.hain.ui.components.MarkdownLabels
import app.hain.ui.components.MarkdownShowcase
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

/** T-298: the showcase answer with every Markdown element, light and dark, at 360 dp; screens workflow only. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w360dp-h2000dp-xhdpi")
class MarkdownScreens {
    @get:Rule
    val compose = createComposeRule()

    private fun render(dark: Boolean) {
        compose.setContent {
            HainTheme(darkTheme = dark) {
                CompositionLocalProvider(
                    LocalReduceMotion provides true,
                    LocalMarkdownLabels provides LABELS,
                ) {
                    Box(Modifier.fillMaxSize().background(LocalHainColors.current.bg).padding(16.dp)) {
                        AssistantBlock(
                            markdown = MarkdownShowcase.TEXT,
                            citations = AnswerCitations(2, {}, { "Quelle $it" }),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun markdownLight() {
        render(dark = false)
        captureScreenRoboImage("build/outputs/roborazzi/markdown_all_light.png")
    }

    @Test
    fun markdownDark() {
        render(dark = true)
        captureScreenRoboImage("build/outputs/roborazzi/markdown_all_dark.png")
    }

    private companion object {
        val LABELS = MarkdownLabels("Code", "Kopieren", "Kopiert", "Erledigt", "Offen")
    }
}
