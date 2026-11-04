package app.hain

import android.Manifest
import android.app.LocaleManager
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hain.common.api.AnswerLanguage
import app.hain.common.api.AppIcon
import app.hain.common.api.HainSettings
import app.hain.common.api.SearxAddress
import app.hain.common.api.SettingsRepository
import app.hain.common.api.ThemeChoice
import app.hain.common.api.UpdateCheck
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.update.UpdateChecker
import app.hain.common.api.update.UpdateResult
import app.hain.common.api.update.UpdateRules
import app.hain.feature.chat.api.ChatExtras
import app.hain.feature.chat.api.ChatScreen
import app.hain.feature.chat.api.ChatVoice
import app.hain.feature.chat.game.Game2048Screen
import app.hain.feature.chat.internal.ChatEngine
import app.hain.feature.chat.internal.ChatStore
import app.hain.feature.chat.internal.ChatViewModel
import app.hain.feature.chat.internal.PrefsRatings
import app.hain.feature.onboarding.api.OnboardingScreen
import app.hain.feature.settings.api.AppInfo
import app.hain.feature.settings.api.PacksScreen
import app.hain.feature.settings.api.SettingsActions
import app.hain.feature.settings.api.SettingsExtras
import app.hain.feature.settings.api.SettingsScreen
import app.hain.feature.settings.api.UpdateAskCard
import app.hain.feature.settings.api.UpdateBanner
import app.hain.feature.settings.api.UpdateStatus
import app.hain.llm.api.DevTuning
import app.hain.llm.api.LlmEngine
import app.hain.models.api.DevModels
import app.hain.models.api.DownloadState
import app.hain.models.api.ModelRepository
import app.hain.tools.api.HainTools
import app.hain.tools.api.ToolContext
import app.hain.ui.effects.HainMenuBackdrop
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.HapticLevel
import app.hain.ui.motion.LocalHapticLevel
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.HainTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
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
            onConfirmActions = { write { repo.setConfirmActions(it) } },
            onWebSearch = { write { repo.setWebSearch(it) } },
            onWeatherLocation = { on ->
                if (on && !WeatherLocation.allowed(this)) {
                    locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                } else {
                    write { repo.setWeatherLocation(on) }
                }
            },
            onHaptics = { write { repo.setHaptics(it) } },
            onAppIcon = { write { repo.setAppIcon(it) } },
            onDev = { write { repo.setDev(it) } },
            onUpdateCheck = { write { repo.setUpdateCheck(it) } },
            onUpdateChannel = { write { repo.setUpdateChannel(it) } },
            onCheckUpdate = { checkForUpdate() },
        )
    }

    /** 3b-2: Android asks for the rough location once; only a yes switches "Standort fürs Wetter" on. */
    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            lifecycleScope.launch { settingsRepository.setWeatherLocation(granted) }
        }

    /** T-215: build facts for "Über Hain". */
    private val appInfo =
        AppInfo(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, BuildConfig.GIT_SHA, BuildConfig.BUILD_DATE)

    /** T-212: where the update check stands (Compose state, read by the banner and "Über Hain"). */
    private var updateStatus by mutableStateOf<UpdateStatus>(UpdateStatus.Idle)

    /** Asks GitHub for a newer build through the NetworkGate; never installs anything. */
    private fun checkForUpdate() {
        if (updateStatus == UpdateStatus.Checking) return
        updateStatus = UpdateStatus.Checking
        lifecycleScope.launch {
            val result = UpdateChecker(networkGate).check(current.updateChannel, BuildConfig.VERSION_CODE)
            settingsRepository.setLastUpdateCheck(System.currentTimeMillis())
            updateStatus =
                when (result) {
                    is UpdateResult.Newer -> UpdateStatus.Newer(result.release.versionName, result.page)
                    UpdateResult.UpToDate -> UpdateStatus.UpToDate
                    UpdateResult.Failed -> UpdateStatus.Failed
                }
        }
    }

    /** T-211: the own GGUF of developer mode when it is in use and on the phone, else `null`. */
    private fun ownModelPath(): String? = DevTuning.ownModel(current.dev) { File(it).isFile }

    /** T-211: own models from Hugging Face for developer mode. */
    private val devModels by lazy { DevModels(models, networkGate, deviceRamGb()) }

    /** Häppchen 10: dictation (Moonshine pack) and read aloud for the chat, in the answer language. */
    private val chatVoice by lazy {
        ChatVoice.create(applicationContext, models, networkGate) { toolLanguage(current) }
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
                            webSearch = cfg.webSearch,
                            here = if (cfg.weatherLocation) WeatherLocation.last(this) else null,
                        )
                    },
                    tuning = { settings().dev },
                    ownModel = { ownModelPath() != null },
                ),
            store = chatStore,
            modelPath = { ownModelPath() ?: models.mainFile(brain).absolutePath },
            brainInstalled = models.downloadState(brain).map { it is DownloadState.Installed },
            networkActive = networkGate.active.map { it > 0 },
            messageRatings = PrefsRatings(getSharedPreferences(RATINGS_PREFS, MODE_PRIVATE)),
            contextSize = { DevTuning.contextSize(settings().dev) },
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

    /** 2f: onboarding language step sets the app language (Android 13+) and the answer language. */
    private fun pickLanguage(tag: String) {
        lifecycleScope.launch {
            settingsRepository.setAnswerLanguage(if (tag == "de") AnswerLanguage.DE else AnswerLanguage.EN)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getSystemService(LocaleManager::class.java)?.applicationLocales = LocaleList.forLanguageTags(tag)
            }
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod") // compose root: theme, onboarding, chat, settings overlays
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val brain = models.defaultBrain
        val prompts = SystemPromptSource(applicationContext) { current }
        lifecycleScope.launch { settingsRepository.settings.collect { storedIcon = it.appIcon } }
        // T-212: once a day at start, only when switched on (off by default)
        lifecycleScope.launch {
            val stored = settingsRepository.settings.first()
            current = stored
            if (UpdateRules.due(stored, System.currentTimeMillis())) checkForUpdate()
        }
        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(HainSettings())
            current = settings
            var showSettings by rememberSaveable { mutableStateOf(false) }
            var showPacks by rememberSaveable { mutableStateOf(false) }
            var showGame by rememberSaveable { mutableStateOf(false) }
            HainTheme(
                darkTheme =
                    when (settings.theme) {
                        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
                        ThemeChoice.LIGHT -> false
                        ThemeChoice.DARK -> true
                    },
                highLegibility = settings.readableFont,
            ) {
                CompositionLocalProvider(
                    LocalReduceMotion provides (settings.reduceMotion || systemReducesMotion()),
                    LocalHapticLevel provides HapticLevel.entries[settings.haptics.ordinal],
                ) {
                    HainMenuBackdrop {
                        var installed by remember { mutableStateOf(models.isInstalled(brain)) }
                        var skipped by remember { mutableStateOf(startPrefs.getBoolean(KEY_SKIPPED, false)) }
                        if (!installed && !skipped) {
                            OnboardingScreen(
                                models = models,
                                onDone = { installed = models.isInstalled(brain) },
                                onLanguage = { tag -> pickLanguage(tag) },
                                iconFlat = settings.appIcon == AppIcon.FLAT,
                                onIconFlat = { flat ->
                                    lifecycleScope.launch {
                                        settingsRepository.setAppIcon(if (flat) AppIcon.FLAT else AppIcon.THREE_D)
                                    }
                                },
                                onSkip = {
                                    startPrefs.edit().putBoolean(KEY_SKIPPED, true).apply()
                                    skipped = true
                                },
                            )
                        } else {
                            val chatViewModel = viewModel { chatViewModel(prompts) { current } }
                            val chatState by chatViewModel.state.collectAsStateWithLifecycle()
                            var updateHidden by rememberSaveable { mutableStateOf(false) }
                            val dev = settings.dev
                            ChatScreen(
                                viewModel = chatViewModel,
                                onOpenSettings = { showSettings = true },
                                onGetBrain = { showPacks = true },
                                answerMode = settings.answerMode,
                                onAnswerMode = { mode ->
                                    lifecycleScope.launch { settingsRepository.setAnswerMode(mode) }
                                },
                                onOpenGame = { showGame = true },
                                voice = chatVoice,
                                extras =
                                    ChatExtras(
                                        showStats = dev.enabled && dev.showStats,
                                        rawThinking = dev.enabled && dev.showRawThinking,
                                        confirmActions = settings.confirmActions,
                                        debugInfo = { stats ->
                                            val model = File(ownModelPath() ?: models.mainFile(brain).absolutePath).name
                                            DevTuning.debugLine(model, current.dev, stats)
                                        },
                                    ),
                                banner = {
                                    val status = updateStatus
                                    if (status is UpdateStatus.Newer && !updateHidden) {
                                        val uri = LocalUriHandler.current
                                        UpdateBanner(status.versionName, { uri.openUri(status.page) }, {
                                            updateHidden =
                                                true
                                        })
                                    } else if (UpdateRules.shouldAsk(settings, chatState.chats.size)) {
                                        UpdateAskCard(
                                            onYes = {
                                                lifecycleScope.launch {
                                                    settingsRepository.setUpdateCheck(
                                                        UpdateCheck.DAILY,
                                                    )
                                                }
                                                checkForUpdate()
                                            },
                                            onNo = {
                                                lifecycleScope.launch { settingsRepository.dismissUpdateQuestion() }
                                            },
                                        )
                                    }
                                },
                            )
                            // T-098: the 2048 easter egg opens right after Pim's burst
                            AnimatedVisibility(
                                visible = showGame,
                                enter =
                                    fadeIn(hainTween(HainMotion.PAGE_MS)) +
                                        scaleIn(hainTween(HainMotion.PAGE_MS), GAME_FROM),
                                exit = fadeOut(hainTween(HainMotion.FAST_MS)),
                            ) {
                                Game2048Screen(
                                    best = settings.game2048Best,
                                    onScore = { score ->
                                        lifecycleScope.launch { settingsRepository.recordGame2048(score) }
                                    },
                                    onClose = { showGame = false },
                                )
                            }
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
                                    extras = SettingsExtras(appInfo, updateStatus, devModels),
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
    }

    /** Latest settings, read by the prompt source and [onStop]. */
    private var current = HainSettings()

    /** Stored icon choice; null until DataStore was read, so [onStop] never flips it by mistake. */
    private var storedIcon: AppIcon? = null

    /**
     * T-103: the launcher alias follows the stored icon choice when you leave Hain. Switching it
     * while Hain is in front can close the app on some phones; not on a rotation or language change.
     */
    override fun onStop() {
        super.onStop()
        val icon = storedIcon
        if (icon != null && !isChangingConfigurations) AppIconSwitcher.apply(applicationContext, icon)
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
private const val GAME_FROM = 0.9f
private const val BYTES_PER_GB = 1_000_000_000.0
private const val START_PREFS = "hain_start"
private const val KEY_SKIPPED = "onboarding_skipped"
private const val RATINGS_PREFS = "hain_ratings"
