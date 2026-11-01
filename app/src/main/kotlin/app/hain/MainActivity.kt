package app.hain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hain.feature.chat.api.ChatScreen
import app.hain.feature.chat.internal.ChatEngine
import app.hain.feature.chat.internal.ChatStore
import app.hain.feature.chat.internal.ChatViewModel
import app.hain.feature.onboarding.api.OnboardingScreen
import app.hain.llm.api.LlmEngine
import app.hain.models.api.ModelRepository
import app.hain.ui.theme.HainTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var llm: LlmEngine

    @Inject lateinit var models: ModelRepository

    @Inject lateinit var chatStore: ChatStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val brain = models.defaultBrain
        val prompts = SystemPromptSource(applicationContext)
        setContent {
            HainTheme {
                var installed by remember { mutableStateOf(models.isInstalled(brain)) }
                if (!installed) {
                    OnboardingScreen(models = models, onDone = { installed = models.isInstalled(brain) })
                } else {
                    val chatViewModel =
                        viewModel {
                            ChatViewModel(
                                llm = llm,
                                engine = ChatEngine(llm, prompts::current),
                                store = chatStore,
                                modelPath = { models.mainFile(brain).absolutePath },
                            )
                        }
                    ChatScreen(viewModel = chatViewModel, modelName = brain.name)
                }
            }
        }
    }
}
