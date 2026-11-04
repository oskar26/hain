package app.hain.feature.chat.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import app.hain.feature.chat.api.CardHandler
import app.hain.feature.chat.api.LocalCardHandler
import app.hain.feature.chat.internal.ChatUiState
import app.hain.feature.chat.internal.UiMessage
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * T-131: answer cards in the chat (table, checklist with a tick from the user, progress, buttons,
 * list) in light and dark. Only with `-Phain.screens` (CI workflow screens.yml).
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class CardScreens {
    @get:Rule
    val compose = createComposeRule()

    private fun shot(name: String) = captureScreenRoboImage("$OUT/cards/$name.png")

    /** Like in the app: the "ask" button is live (without a handler it would look disabled). */
    @Composable
    private fun Chat(state: ChatUiState) {
        CompositionLocalProvider(LocalCardHandler provides CardHandler(onAsk = {})) { ChatFrame(state) }
    }

    @Test
    fun planCardLight() {
        compose.setContent { Frame(dark = false) { Chat(planState()) } }
        shot("cards_light")
    }

    @Test
    fun planCardDark() {
        compose.setContent { Frame(dark = true) { Chat(planState()) } }
        shot("cards_dark")
    }

    @Test
    fun listCardLight() {
        compose.setContent { Frame(dark = false) { Chat(listState()) } }
        shot("cards_list_light")
    }

    private fun planState() =
        ChatUiState(
            messages =
                listOf(
                    UiMessage(1, true, "Mach mir einen Lernplan für die Mathearbeit am Freitag"),
                    UiMessage(2, false, PLAN, cardState = "{\"checked\":{\"0.1.0\":true}}"),
                ),
        )

    private fun listState() =
        ChatUiState(
            messages =
                listOf(
                    UiMessage(1, true, "Was brauche ich für Pfannkuchen?"),
                    UiMessage(2, false, PANCAKES),
                ),
        )

    private companion object {
        const val PLAN =
            "Klar, hier ist dein Plan bis Freitag:\n\n" +
                "```card\n" +
                "Card title=\"Lernplan Mathe\"\n" +
                "Table\n" +
                "| Tag | Thema | Zeit |\n" +
                "| Mo | Brüche | 30 min |\n" +
                "| Di | Gleichungen | 30 min |\n" +
                "| Mi | Probearbeit | 45 min |\n" +
                "Checklist\n" +
                "- Formelblatt schreiben\n" +
                "- Aufgaben S. 42 rechnen\n" +
                "- Fragen an Frau Berg sammeln\n" +
                "Progress value=40 label=\"Schon geschafft\"\n" +
                "Button label=\"Kopieren\" action=copy\n" +
                "Button label=\"Mehr Übungen\" action=ask:\"Gib mir Übungsaufgaben zu Brüchen\"\n" +
                "```\n\n" +
                "Viel Erfolg! Du schaffst das."

        const val PANCAKES =
            "Für etwa 8 Pfannkuchen:\n\n" +
                "```card\n" +
                "Card title=\"Einkaufsliste\"\n" +
                "List\n" +
                "- 250 g Mehl\n" +
                "- 500 ml Milch\n" +
                "- 3 Eier\n" +
                "- 1 Prise Salz\n" +
                "Button label=\"Teilen\" action=share\n" +
                "```"
    }
}
