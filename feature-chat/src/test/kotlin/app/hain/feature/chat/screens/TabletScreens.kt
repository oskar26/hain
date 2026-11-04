package app.hain.feature.chat.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import app.hain.feature.chat.api.ChatSidePanel
import app.hain.feature.chat.api.ChatSidePanelWidth
import app.hain.feature.chat.api.DrawerActions
import app.hain.feature.chat.internal.Chat
import app.hain.feature.chat.internal.ChatGroup
import app.hain.feature.chat.internal.ChatUiState
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** T-235: the chat on a tablet in landscape (1280 × 800 dp): chat list always open on the left. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w1280dp-h800dp-land-xhdpi")
class TabletScreens {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun tabletChatLight() {
        compose.setContent { Frame(dark = false) { WideChat(answerState()) } }
        captureScreenRoboImage("$OUT/tablet_chat_light.png")
    }

    @Test
    fun tabletChatDark() {
        compose.setContent { Frame(dark = true) { WideChat(answerState()) } }
        captureScreenRoboImage("$OUT/tablet_chat_dark.png")
    }

    @Test
    fun tabletEmptyChatLight() {
        compose.setContent { Frame(dark = false) { WideChat(ChatUiState()) } }
        captureScreenRoboImage("$OUT/tablet_chat_empty_light.png")
    }
}

/** Side panel plus chat, laid out like ChatScreen on a wide window. */
@Composable
private fun WideChat(state: ChatUiState) {
    val texts = ShotTexts.DE
    ChatSidePanel(
        hasChats = true,
        groups =
            listOf(
                ChatGroup.TODAY to texts.drawerToday.mapIndexed { i, title -> Chat(i + 1L, title, emptyList(), 0) },
                ChatGroup.YESTERDAY to listOf(Chat(9, texts.drawerYesterday, emptyList(), 0)),
            ),
        searchQuery = "",
        currentChatId = 2,
        actions = DrawerActions({}, {}, {}, {}, { _, _ -> }, { _, _ -> }, {}, {}, onNews = {}),
    )
    Box(Modifier.fillMaxSize().padding(start = ChatSidePanelWidth)) { ChatFrame(state, menu = false) }
}
