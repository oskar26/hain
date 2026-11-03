package app.hain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
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
import app.hain.common.api.AnswerLanguage
import app.hain.common.api.HainSettings
import app.hain.common.api.SearxAddress
import app.hain.common.api.SettingsRepository
import app.hain.common.api.ThemeChoice
import app.hain.common.api.UiStyle
import app.hain.common.api.net.NetworkGate
import app.hain.feature.chat.api.ChatScreen
import app.hain.feature.chat.internal.ChatEngine
import app.hain.feature.chat.internal.ChatStore
import app.hain.feature.chat.internal.ChatViewModel
import app.hain.feature.chat.internal.PrefsRatings
import app.hain.feature.onboarding.api.OnboardingScreen
import app.hain.feature.settings.api.PacksScreen
import app.hain.feature.settings.api.SettingsActions
import app.hain.feature.settings.api.SettingsScreen
import app.hain.llm.api.LlmEngine
import app.hain.models.api.DownloadState
import app.hain.models.api.ModelRepository
import app.hain.tools.api.HainTools
import app.hain.tools.api.ToolContext
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.HainStyle
import app.hain.ui.theme.HainTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.ZonedDateTime
import javax.inject.Inject
import app.hain.llm.internal.AnswerMode as LlmAnswerMode

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var llm: LlmEngine

    @Inject lateinit var models: ModelRepository

    @Inject lateinit var chatStore: ChatStore

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var networkGate: NetworkGate

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
            onReduceMotion = { write { repo.setReduceMotion(it) } },
            onAnswerLanguage = { write { repo.setAnswerLanguage(it) } },
            onAnswerMode = { write { repo.setAnswerMode(it) } },
            onAboutMe = { write { repo.setAboutMe(it) } },
            onStyle = { write { repo.setStyle(it) } },
            onReset = { write { repo.reset() } },
            onOpenPacks = onOpenPacks,
            onOfflineMode = { write { repo.setOfflineMode(it) } },
            onNetworkLogDetail = { write { repo.setNetworkLogDetail(it) } },
            onClearNetworkLog = networkGate::clearLog,
            onSearxUrl = { write { repo.setSearxUrl(it) } },
            onUiStyle = { write { repo.setUiStyle(it) } },
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
                    turnHint = prompts::turnHint,
                    tools = HainTools.router(networkGate),
                    toolContext = {
                        val cfg = settings()
                        ToolContext(
                            toolLanguage(cfg),
                            ZonedDateTime.now(),
                            cfg.offlineMode,
                            searxUrl = SearxAddress.normalize(cfg.searxUrl),
                        )
                    },
                ),
            store = chatStore,
            modelPath = { models.mainFile(brain).absolutePath },
            brainInstalled = models.downloadState(brain).map { it is DownloadState.Installed },
            networkActive = networkGate.active.map { it > 0 },
            messageRatings = PrefsRatings(getSharedPreferences(RATINGS_PREFS, MODE_PRIVATE)),
        )
    }

    /** Language for Wikipedia and weather words: the answer language, or the phone's on "Automatisch". */
    private fun toolLanguage(settings: HainSettings): String =
        when (settings.answerLanguage) {
            AnswerLanguage.DE -> "de"
            AnswerLanguage.EN -> "en"
            AnswerLanguage.AUTO -> if (resources.configuration.locales[0]?.language == "de") "de" else "en"
        }

    /** Small start flags (read on the main thread, so no flash of the wrong screen). */
    private val startPrefs by lazy { getSharedPreferences(START_PREFS, MODE_PRIVATE) }

    private fun deviceRamGb(): Int {
        val info = android.app.ActivityManager.MemoryInfo()
        getSystemService(android.app.ActivityManager::class.java).getMemoryInfo(info)
        return Math.round(info.totalMem / BYTES_PER_GB).toInt()
    }

    @Suppress("LongMethod") // compose root: theme, onboarding, chat, settings overlays
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val brain = models.defaultBrain
        var current = HainSettings()
        val prompts = SystemPromptSource(applicationContext) { current }
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
                style = if (settings.uiStyle == UiStyle.B) HainStyle.B else HainStyle.A,
            ) {
                CompositionLocalProvider(LocalReduceMotion provides (settings.reduceMotion || systemReducesMotion())) {
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
                            answerMode = settings.answerMode,
                            onAnswerMode = { mode -> lifecycleScope.launch { settingsRepository.setAnswerMode(mode) } },
                        )
                        AnimatedVisibility(
                            visible = showSettings,
                            enter =
                                fadeIn(hainTween(HainMotion.PAGE_MS)) +
                                    slideInHorizontally(hainTween(HainMotion.PAGE_MS)) { it / SLIDE_DIV },
                            exit = fadeOut(hainTween(HainMotion.FAST_MS)),
                        ) {
                            val log by networkGate.log.collectAsStateWithLifecycle()
                            SettingsScreen(
                                settings,
                                settingsActions({ showSettings = false }, { showPacks = true }),
                                networkLog = log,
                            )
                        }
                        AnimatedVisibility(
                            visible = showPacks,
                            enter =
                                fadeIn(hainTween(HainMotion.PAGE_MS)) +
                                    slideInHorizontally(hainTween(HainMotion.PAGE_MS)) { it / SLIDE_DIV },
                            exit = fadeOut(hainTween(HainMotion.FAST_MS)),
                        ) {
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

private const val SLIDE_DIV = 6
private const val BYTES_PER_GB = 1_000_000_000.0
private const val START_PREFS = "hain_start"
private const val KEY_SKIPPED = "onboarding_skipped"
private const val RATINGS_PREFS = "hain_ratings"
