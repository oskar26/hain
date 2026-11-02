package app.hain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hain.common.api.HainSettings
import app.hain.common.api.SettingsRepository
import app.hain.common.api.ThemeChoice
import app.hain.feature.chat.api.ChatScreen
import app.hain.feature.chat.internal.ChatEngine
import app.hain.feature.chat.internal.ChatStore
import app.hain.feature.chat.internal.ChatViewModel
import app.hain.feature.onboarding.api.OnboardingScreen
import app.hain.feature.settings.api.SettingsActions
import app.hain.feature.settings.api.SettingsScreen
import app.hain.llm.api.LlmEngine
import app.hain.models.api.ModelRepository
import app.hain.ui.theme.HainTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject
import app.hain.llm.internal.AnswerMode as LlmAnswerMode

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var llm: LlmEngine

    @Inject lateinit var models: ModelRepository

    @Inject lateinit var chatStore: ChatStore

    @Inject lateinit var settingsRepository: SettingsRepository

    private fun settingsActions(onBack: () -> Unit): SettingsActions {
        val repo = settingsRepository

        fun write(block: suspend () -> Unit) {
            lifecycleScope.launch { block() }
        }
        return SettingsActions(
            onBack = onBack,
            onTheme = { write { repo.setTheme(it) } },
            onReadableFont = { write { repo.setReadableFont(it) } },
            onAnswerLanguage = { write { repo.setAnswerLanguage(it) } },
            onAnswerMode = { write { repo.setAnswerMode(it) } },
            onAboutMe = { write { repo.setAboutMe(it) } },
            onStyle = { write { repo.setStyle(it) } },
            onReset = { write { repo.reset() } },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val brain = models.defaultBrain
        var current = HainSettings()
        val prompts = SystemPromptSource(applicationContext) { current.customInstructions() }
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(HainSettings())
            current = settings
            var showSettings by rememberSaveable { mutableStateOf(false) }
            HainTheme(
                darkTheme =
                    when (settings.theme) {
                        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
                        ThemeChoice.LIGHT -> false
                        ThemeChoice.DARK -> true
                    },
                highLegibility = settings.readableFont,
            ) {
                var installed by remember { mutableStateOf(models.isInstalled(brain)) }
                if (!installed) {
                    OnboardingScreen(models = models, onDone = { installed = models.isInstalled(brain) })
                } else {
                    val chatViewModel =
                        viewModel {
                            ChatViewModel(
                                llm = llm,
                                engine =
                                    ChatEngine(
                                        llm,
                                        prompts::current,
                                        answerMode = { LlmAnswerMode.fromKey(current.answerMode.name.lowercase()) },
                                    ),
                                store = chatStore,
                                modelPath = { models.mainFile(brain).absolutePath },
                            )
                        }
                    ChatScreen(
                        viewModel = chatViewModel,
                        modelName = brain.name,
                        onOpenSettings = { showSettings = true },
                    )
                    if (showSettings) {
                        SettingsScreen(settings, settingsActions { showSettings = false })
                    }
                }
            }
        }
    }
}
