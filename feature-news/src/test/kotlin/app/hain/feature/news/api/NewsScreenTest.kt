package app.hain.feature.news.api

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import app.hain.tools.internal.news.NewsMode
import app.hain.ui.theme.HainTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * T-073 (ADR-14): exactly three modes and no switch for single standard feeds, neither on the news
 * list nor on the own-feeds page.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "de")
class NewsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val now = System.currentTimeMillis()
    private val toggles = SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)

    @Test fun threeModesAndNoToggles() {
        compose.setContent { HainTheme { NewsBody(NewsSamples.state(now), NewsActions(), now = now) } }
        compose.onNodeWithText("Standard").assertExists()
        compose.onNodeWithText("Standard + eigene").assertExists()
        compose.onNodeWithText("Nur eigene").assertExists()
        compose.onAllNodes(toggles).assertCountEquals(0)
    }

    @Test fun ownFeedsPageListsOnlyOwnFeedsWithoutToggles() {
        compose.setContent {
            HainTheme {
                val state = NewsSamples.state(now, NewsMode.STANDARD_PLUS_OWN)
                NewsBody(state, NewsActions(), startOnFeeds = true, now = now)
            }
        }
        compose.onNodeWithText("Kölner Stadtblog").assertExists()
        compose.onAllNodesWithText("tagesschau").assertCountEquals(0)
        compose.onAllNodesWithText("junge Welt").assertCountEquals(0)
        compose.onAllNodes(toggles).assertCountEquals(0)
    }
}
