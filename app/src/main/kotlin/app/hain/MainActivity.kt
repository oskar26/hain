package app.hain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
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
import app.hain.feature.settings.api.PacksScreen
import app.hain.feature.settings.api.SettingsActions
import app.hain.feature.settings.api.SettingsScreen
import app.hain.llm.api.LlmEngine
import app.hain.models.api.DownloadState
import app.hain.models.api.ModelRepository
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.HainTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import app.hain.llm.internal.AnswerMode as LlmAnswerMode

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var llm: LlmEngine

    @Inject lateinit var models: ModelRepository

    @Inject lateinit var chatStore: ChatStore

    @Inject lateinit var settingsRepository: SettingsRepository

    private fun settingsActions(
        onBack: () -> Unit,
        onOpenPacks: () -> Unit,
    ): SettingsActions {
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
            onOpenPacks = onOpenPacks,
        )
    }

    private fun chatViewModel(
        prompts: SystemPromptSource,
        settings: () -> HainSettings,
    ): ChatViewModel {
        val brain = models.defaultBrain
        return ChatViewModel(
            llm = llm,
            engine =
                ChatEngine(
                    llm,
                    prompts::current,
                    answerMode = { LlmAnswerMode.fromKey(settings().answerMode.name.lowercase()) },
                ),
            store = chatStore,
            modelPath = { models.mainFile(brain).absolutePath },
            brainInstalled = models.downloadState(brain).map { it is DownloadState.Installed },
        )
    }

    /** Small start flags (read on the main thread, so no flash of the wrong screen). */
    private val startPrefs by lazy { getSharedPreferences(START_PREFS, MODE_PRIVATE) }

    private fun deviceRamGb(): Int {
        val info = android.app.ActivityManager.MemoryInfo()
        getSystemService(android.app.ActivityManager::class.java).getMemoryInfo(info)
        return Math.round(info.totalMem / BYTES_PER_GB).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val brain = models.defaultBrain
        var current = HainSettings()
        val prompts = SystemPromptSource(applicationContext) { lang -> current.customInstructions(lang) }
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(HainSettings())
            current = settings
            var showSettings by rememberSaveable { mutableStateOf(false) }
            var showPacks by rememberSaveable { mutableStateOf(false) }
            HainTheme(
                darkTheme =
                    when (settings.theme) {
                        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
                        ThemeChoice.LIGHT -> false
                        ThemeChoice.DARK -> true
                    },
                highLegibility = settings.readableFont,
            ) {
                CompositionLocalProvider(LocalReduceMotion provides systemReducesMotion()) {
                    var installed by remember { mutableStateOf(models.isInstalled(brain)) }
                    var skipped by remember { mutableStateOf(startPrefs.getBoolean(KEY_SKIPPED, false)) }
                    if (!installed && !skipped) {
                        OnboardingScreen(
                            models = models,
                            onDone = { installed = models.isInstalled(brain) },
                            onSkip = {
                                startPrefs.edit().putBoolean(KEY_SKIPPED, true).apply()
                                skipped = true
                            },
                        )
                    } else {
                        val chatViewModel = viewModel { chatViewModel(prompts) { current } }
                        ChatScreen(
                            viewModel = chatViewModel,
                            onOpenSettings = { showSettings = true },
                            onGetBrain = { showPacks = true },
                        )
                        if (showSettings) {
                            SettingsScreen(settings, settingsActions({ showSettings = false }, { showPacks = true }))
                        }
                        if (showPacks) {
                            PacksScreen(models, deviceRamGb(), { showPacks = false })
                        }
                    }
                }
            }
        }
    }

    /** System "remove animations" (animator scale 0) → Pim and the glow stay still. */
    private fun systemReducesMotion(): Boolean =
        android.provider.Settings.Global.getFloat(
            contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
}

private const val BYTES_PER_GB = 1_000_000_000.0
private const val START_PREFS = "hain_start"
private const val KEY_SKIPPED = "onboarding_skipped"
