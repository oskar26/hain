package app.hain.feature.news.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import app.hain.feature.news.api.NewsActions
import app.hain.feature.news.api.NewsBody
import app.hain.feature.news.api.NewsSamples
import app.hain.feature.news.api.SummaryResult
import app.hain.feature.news.api.SummaryState
import app.hain.tools.internal.news.NewsMode
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.HainTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** News screenshots for docs/design/screens (T-073); run only via CI workflow screens.yml. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class NewsScreens {
    @get:Rule
    val compose = createComposeRule()

    private val now = System.currentTimeMillis()

    @Test
    fun newsLight() {
        compose.setContent { Frame(dark = false) { NewsBody(NewsSamples.state(now), NewsActions(), now = now) } }
        captureScreenRoboImage("$OUT/news_light.png")
    }

    @Test
    fun newsDark() {
        compose.setContent { Frame(dark = true) { NewsBody(NewsSamples.state(now), NewsActions(), now = now) } }
        captureScreenRoboImage("$OUT/news_dark.png")
    }

    @Test
    fun newsSummaryLight() {
        val base = NewsSamples.state(now, NewsMode.STANDARD_PLUS_OWN)
        val first = base.items.first().guid
        val points =
            listOf(
                "Der Bundestag hat den Haushalt für 2027 mit knapper Mehrheit beschlossen.",
                "Die Opposition kritisiert Kürzungen bei Sozialem und Klima.",
                "Die Schuldenbremse wird eingehalten.",
                "Mehr Geld fließt in Verteidigung und Bahn.",
                "Der Bundesrat berät im Dezember.",
            )
        val state = base.copy(summaries = mapOf(first to SummaryState.Ready(SummaryResult.Bullets(points))))
        compose.setContent { Frame(dark = false) { NewsBody(state, NewsActions(), now = now) } }
        captureScreenRoboImage("$OUT/news_summary_light.png")
    }

    @Test
    fun ownFeedsLight() {
        val state = NewsSamples.state(now, NewsMode.STANDARD_PLUS_OWN)
        compose.setContent { Frame(dark = false) { NewsBody(state, NewsActions(), startOnFeeds = true, now = now) } }
        captureScreenRoboImage("$OUT/news_feeds_light.png")
    }
}

private const val OUT = "build/outputs/roborazzi"

@Composable
private fun Frame(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    HainTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalReduceMotion provides true) { content() }
    }
}
