package app.hain

import android.Manifest
import android.app.LocaleManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.os.SystemClock
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.hain.common.api.AnswerLanguage
import app.hain.common.api.AppIcon
import app.hain.common.api.BrainChoiceStore
import app.hain.common.api.HainSettings
import app.hain.common.api.LockDelay
import app.hain.common.api.SearxAddress
import app.hain.common.api.SettingsRepository
import app.hain.common.api.ThemeChoice
import app.hain.common.api.UpdateCheck
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.update.UpdateChecker
import app.hain.common.api.update.UpdateResult
import app.hain.common.api.update.UpdateRules
import app.hain.data.KeystoreKeyProvider
import app.hain.docs.api.DocLibrary
import app.hain.entry.HainWidget
import app.hain.entry.LaunchIntents
import app.hain.entry.LaunchRequest
import app.hain.entry.WidgetData
import app.hain.entry.applyTo
import app.hain.feature.chat.api.ChatDocs
import app.hain.feature.chat.api.ChatExtras
import app.hain.feature.chat.api.ChatScreen
import app.hain.feature.chat.api.ChatVoice
import app.hain.feature.chat.export.ChatZip
import app.hain.feature.chat.export.exportLabels
import app.hain.feature.chat.export.fileDate
import app.hain.feature.chat.game.Game2048Screen
import app.hain.feature.chat.internal.ChatEngine
import app.hain.feature.chat.internal.ChatStore
import app.hain.feature.chat.internal.ChatViewModel
import app.hain.feature.chat.internal.PrefsRatings
import app.hain.feature.memory.api.MemoryAnswerChips
import app.hain.feature.memory.api.MemoryScreen
import app.hain.feature.memory.api.SmartSearch
import app.hain.feature.news.api.NewsScreen
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
import app.hain.lock.AppLock
import app.hain.lock.LockScreen
import app.hain.memory.api.MemoryExtraction
import app.hain.memory.api.MemoryStore
import app.hain.memory.api.MemoryTurn
import app.hain.memory.api.RememberTool
import app.hain.models.api.DevModels
import app.hain.models.api.DeviceCheck
import app.hain.models.api.DeviceProfile
import app.hain.models.api.DownloadState
import app.hain.models.api.Fit
import app.hain.models.api.ModelInfo
import app.hain.models.api.ModelRepository
import app.hain.tools.api.Favicons
import app.hain.tools.api.HainTools
import app.hain.tools.api.ToolContext
import app.hain.tools.internal.news.NewsRepository
import app.hain.ui.LocalExternalLaunch
import app.hain.ui.effects.HainMenuBackdrop
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.HapticLevel
import app.hain.ui.motion.LocalHapticLevel
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.theme.HainTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.ZonedDateTime
import javax.inject.Inject
import app.hain.llm.internal.AnswerMode as LlmAnswerMode

@Suppress("TooManyFunctions") // activity: lifecycle, settings wiring, app lock, export
@AndroidEntryPoint
class MainActivity : FragmentActivity() {
    @Inject lateinit var llm: LlmEngine

    @Inject lateinit var models: ModelRepository

    @Inject lateinit var chatStore: ChatStore

    @Inject lateinit var settingsRepository: SettingsRepository

    @Inject lateinit var networkGate: NetworkGate

    @Inject lateinit var newsRepository: NewsRepository

    @Inject lateinit var memory: MemoryStore

    /** T-112/T-113/7b: "merk dir …" and the fitting facts before each answer; what it did, for the chips. */
    @Inject lateinit var memoryTurn: MemoryTurn

    /** T-056 × T-112: a private chat is open; the memory then neither reads nor writes (MEMORY.md §3). */
    @Volatile private var privateChatOpen = false

    @Inject lateinit var brainChoice: BrainChoiceStore

    /** T-055: brain picker in the top bar (catalogue + DataStore choice). */
    private val brainPicking by lazy {
        BrainPicking(models, brainChoice, lifecycleScope, ::deviceRamGb, ::ownModelPath)
    }

    private fun settingsActions(
        onBack: () -> Unit,
        onOpenPacks: () -> Unit,
        onOpenMemory: () -> Unit = {},
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
            onSourceIcons = { write { repo.setSourceIcons(it) } },
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
            onOpenMemory = onOpenMemory,
            onAppLock = { on ->
                if (on) {
                    // T-056: the lock only turns on after the phone confirmed it is you
                    val title = getString(R.string.lock_enable_title)
                    appLock.authenticate(title, getString(R.string.lock_enable_subtitle)) {
                        if (it) write { repo.setAppLock(true) }
                    }
                } else {
                    write { repo.setAppLock(false) }
                    appLock.disable()
                }
            },
            onLockDelay = { write { repo.setLockDelay(it) } },
            onBlockScreenshots = { write { repo.setBlockScreenshots(it) } },
            onExportAll = ::exportAllChats,
        )
    }

    /** T-054: every saved chat as a Markdown file in one ZIP, written to the file you picked. */
    private fun exportAllChats(uri: Uri) {
        lifecycleScope.launch {
            chatStore.loaded.first { it }
            val chats = chatStore.chats.value
            val count =
                withContext(Dispatchers.IO) {
                    runCatching {
                        contentResolver.openOutputStream(uri)?.use { out ->
                            ChatZip.write(
                                chats,
                                out,
                                labelsOf = { exportLabels(this@MainActivity, it) },
                                dateOf = { fileDate(it.updatedAt) },
                            )
                        }
                    }.getOrNull()
                }
            val text =
                if (count == null) {
                    getString(R.string.export_all_failed)
                } else {
                    resources.getQuantityString(R.plurals.export_all_done, count, count)
                }
            Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT).show()
        }
    }

    /** T-056: fingerprint / PIN lock; created in onCreate, before the activity is resumed. */
    private lateinit var appLock: AppLock

    /**
     * T-056: no screenshots while "Bildschirmfotos verhindern" is on; with the app lock (or the
     * block) the preview in recent apps stays blank (Android 13+).
     */
    private fun applyWindowPrivacy(
        blockScreenshots: Boolean,
        lock: Boolean,
    ) {
        if (blockScreenshots) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(!lock && !blockScreenshots)
        }
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

    /** Häppchen 9: "Meine Dateien" (own encrypted hain_docs.db) and the text recognition pack. */
    private val docLibrary by lazy {
        DocLibrary(applicationContext, KeystoreKeyProvider(applicationContext), networkGate)
    }

    /** Häppchen 9: attaching files in the chat (paper clip, progress, "Texterkennung laden?"). */
    private val chatDocs by lazy { ChatDocs(docLibrary, lifecycleScope) }

    /**
     * T-074: the brain file for news summaries, `null` while no brain is on the phone; the brain
     * picked in the top bar (T-055) like in the chat.
     */
    private val brainPath: () -> String? = {
        ownModelPath() ?: brainPicking.path().takeIf { models.isInstalled(brainPicking.active()) }
    }

    /** T-064c: site icons for the source chips, through the gate, kept in the app's cache folder. */
    private val favicons by lazy { Favicons(networkGate, File(cacheDir, "favicons")) }

    /** T-211: own models from Hugging Face for developer mode. */
    private val devModels by lazy { DevModels(models, networkGate, deviceRamGb()) }

    /** Häppchen 10: dictation (Moonshine pack) and read aloud for the chat, in the answer language. */
    private val chatVoice by lazy {
        ChatVoice.create(applicationContext, models, networkGate) { toolLanguage(current) }
    }

    private fun chatViewModel(
        prompts: SystemPromptSource,
        settings: () -> HainSettings,
    ): ChatViewModel =
        ChatViewModel(
            llm = llm,
            engine =
                ChatEngine(
                    llm,
                    prompts::current,
                    answerMode = { LlmAnswerMode.fromKey(settings().answerMode.name.lowercase()) },
                    turnHint = prompts::turnHint,
                    // T-056 × T-112: in a private chat the memory neither reads nor writes
                    tools =
                        HainTools.router(
                            networkGate,
                            newsRepository,
                            listOf(
                                RememberTool(memory, memoryTurn::onToolSaved) { privateChatOpen },
                                // Häppchen 9: search_document in the attached files
                                docLibrary.searchTool,
                            ),
                        ),
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
                    // 7b: off the main thread, may embed the question with the pack "Suche nach Bedeutung"
                    memoryHint = { messages ->
                        // the user writes again: a running background extraction gives the brain back
                        MemoryExtraction.current?.yieldToChat()
                        memoryTurn.hint(messages, toolLanguage(settings()), privateChat = privateChatOpen)
                    },
                ),
            store = chatStore,
            modelPath = brainPicking::path,
            // T-055: any brain of the catalogue on the phone is enough to chat
            brainInstalled = brainPicking.installedFlow(),
            networkActive = networkGate.active.map { it > 0 },
            messageRatings = PrefsRatings(getSharedPreferences(RATINGS_PREFS, MODE_PRIVATE)),
            contextSize = { DevTuning.contextSize(settings().dev) },
            // T-114: two minutes after the last answer the memory looks for facts in the chat
            // (never for a private chat, T-056: it is not in the database and the memory must skip it)
            onAnswered = { chatId -> if (!chatStore.isPrivate(chatId)) MemoryExtraction.current?.schedule(chatId) },
            documentPrompt = docLibrary::forPrompt,
        )

    /** T-110: the pack "Suche nach Bedeutung" from the catalogue (kind `embed`). */
    private val embedPack: ModelInfo? by lazy { models.catalog.firstOrNull { it.kind == ModelInfo.KIND_EMBED } }

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

    /** T-144: what the share sheet, widget or tile asked for; handled once the chat is there. */
    private var launchRequest by mutableStateOf<LaunchRequest?>(null)

    /** T-144: counts up when the message box should open the keyboard. */
    private var composerFocus by mutableIntStateOf(0)

    @Suppress("LongMethod", "CyclomaticComplexMethod") // compose root: theme, onboarding, chat, settings overlays
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) launchRequest = LaunchIntents.of(intent)
        // singleTask: share sheet, widget and tile reach the open Hain here
        addOnNewIntentListener { next ->
            setIntent(next)
            LaunchIntents.of(next)?.let { launchRequest = it }
        }
        // T-144: the home screen widget shows the newest chat; redraw it when that changes (also when
        // the app lock is switched, T-056: then the widget hides the title)
        lifecycleScope.launch {
            combine(chatStore.chats, settingsRepository.settings.map { it.appLock }) { chats, locked ->
                WidgetData.latest(chats, appLock = locked)
            }.distinctUntilChanged()
                .drop(1)
                .collect { HainWidget.refresh(applicationContext) }
        }
        val prompts = SystemPromptSource(applicationContext) { current }
        // T-056: start locked when the lock is on (read synchronously, so the chat never flashes up)
        val lockOn = startPrefs.getBoolean(KEY_LOCK, false)
        appLock = AppLock(this, initiallyLocked = lockOn)
        applyWindowPrivacy(startPrefs.getBoolean(KEY_NO_SCREENSHOTS, false), lockOn)
        lifecycleScope.launch {
            settingsRepository.settings.collect {
                storedIcon = it.appIcon
                startPrefs
                    .edit()
                    .putBoolean(KEY_LOCK, it.appLock)
                    .putString(KEY_LOCK_DELAY, it.lockDelay.name)
                    .putBoolean(KEY_NO_SCREENSHOTS, it.blockScreenshots)
                    .apply()
                applyWindowPrivacy(it.blockScreenshots, it.appLock)
            }
        }
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
            var showMemory by rememberSaveable { mutableStateOf(false) }
            var showGame by rememberSaveable { mutableStateOf(false) }
            var showNews by rememberSaveable { mutableStateOf(false) }
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
                    LocalExternalLaunch provides appLock::expectReturn,
                ) {
                    // T-056: while locked only the lock screen is there (no chat, menu or popup underneath)
                    val locked by appLock.locked.collectAsStateWithLifecycle()
                    if (locked) {
                        LockScreen(
                            onUnlock = {
                                val title = getString(R.string.lock_prompt_title)
                                appLock.unlock(title, getString(R.string.lock_prompt_subtitle))
                            },
                            onLeave = { moveTaskToBack(true) },
                        )
                        return@CompositionLocalProvider
                    }
                    HainMenuBackdrop {
                        var installed by remember {
                            mutableStateOf(brainPicking.anyInstalled())
                        }
                        var skipped by remember { mutableStateOf(startPrefs.getBoolean(KEY_SKIPPED, false)) }
                        if (!installed && !skipped) {
                            OnboardingScreen(
                                models = models,
                                onDone = { installed = brainPicking.anyInstalled() },
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
                            SideEffect { privateChatOpen = chatState.privateChat }
                            val brainPicker =
                                remember(chatViewModel) {
                                    brainPicking.state(chatViewModel::switchBrain) { showPacks = true }
                                }
                            val request = launchRequest
                            LaunchedEffect(request) {
                                if (request != null) {
                                    showSettings = false
                                    showPacks = false
                                    showGame = false
                                    request.applyTo(chatStore, chatViewModel) { composerFocus++ }
                                    launchRequest = null
                                }
                            }
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
                                docs = chatDocs,
                                onOpenNews = { showNews = true },
                                brainPicker = brainPicker,
                                focusTick = composerFocus,
                                voice = chatVoice,
                                extras =
                                    ChatExtras(
                                        showStats = dev.enabled && dev.showStats,
                                        rawThinking = dev.enabled && dev.showRawThinking,
                                        confirmActions = settings.confirmActions,
                                        // 7b: "Gemerkt ✓ · Rückgängig" and "3 Erinnerungen genutzt" under answers
                                        answerFooter = { userMessages -> MemoryAnswerChips(memoryTurn, userMessages) },
                                        favicons = if (settings.sourceIcons) favicons else null,
                                        debugInfo = { stats ->
                                            val model = File(brainPicking.path()).name
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
                            // T-073: news, from the drawer
                            AnimatedVisibility(
                                visible = showNews,
                                enter =
                                    fadeIn(hainTween(HainMotion.PAGE_MS)) +
                                        slideInHorizontally(hainTween(HainMotion.PAGE_MS)) { it / SLIDE_DIV },
                                exit = fadeOut(hainTween(HainMotion.FAST_MS)),
                            ) {
                                val newsViewModel =
                                    viewModel {
                                        newsScreenModel(
                                            NewsParts(newsRepository, networkGate, llm),
                                            modelPath = brainPath,
                                            contextSize = { DevTuning.contextSize(current.dev) },
                                            lang = { toolLanguage(current) },
                                        )
                                    }
                                NewsScreen(newsViewModel, { showNews = false })
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
                                    settingsActions(
                                        onBack = { showSettings = false },
                                        onOpenPacks = { showPacks = true },
                                        onOpenMemory = { showMemory = true },
                                    ),
                                    networkLog = log,
                                    extras =
                                        SettingsExtras(
                                            appInfo,
                                            updateStatus,
                                            devModels,
                                            lockAvailable = appLock.available(),
                                        ),
                                )
                            }
                            // T-115: Settings → Gedächtnis
                            AnimatedVisibility(
                                visible = showMemory,
                                enter =
                                    fadeIn(hainTween(HainMotion.PAGE_MS)) +
                                        slideInHorizontally(hainTween(HainMotion.PAGE_MS)) { it / SLIDE_DIV },
                                exit = fadeOut(hainTween(HainMotion.FAST_MS)),
                            ) {
                                MemoryScreen(
                                    store = memory,
                                    aboutMe = settings.aboutMe,
                                    onAboutMe = { text ->
                                        lifecycleScope.launch { settingsRepository.setAboutMe(text) }
                                    },
                                    onBack = { showMemory = false },
                                    smartSearch = smartSearchState(models, embedPack, deviceRamGb()),
                                    onGetSmartSearch = { embedPack?.let(models::startDownload) },
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

    /** T-056: back on screen → lock if Hain was away longer than the chosen time. */
    override fun onStart() {
        super.onStart()
        val delay =
            LockDelay.entries.firstOrNull { it.name == startPrefs.getString(KEY_LOCK_DELAY, null) }
                ?: LockDelay.IMMEDIATE
        appLock.onForeground(SystemClock.elapsedRealtime(), startPrefs.getBoolean(KEY_LOCK, false), delay)
    }

    /**
     * T-103: the launcher alias follows the stored icon choice when you leave Hain. Switching it
     * while Hain is in front can close the app on some phones; not on a rotation or language change.
     * T-056: leaving also starts the app-lock clock.
     */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) appLock.onBackground(SystemClock.elapsedRealtime())
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

/** Where the pack "Suche nach Bedeutung" stands, for the hint tile on the memory page (T-110). */
@Composable
private fun smartSearchState(
    models: ModelRepository,
    pack: ModelInfo?,
    ramGb: Int,
): SmartSearch {
    if (pack == null) return SmartSearch.Hidden
    val flow = remember(pack.id) { models.downloadState(pack) }
    val state by flow.collectAsStateWithLifecycle(DownloadState.Idle)
    val phase =
        when {
            state is DownloadState.Installed || models.isInstalled(pack) -> SmartSearch.Phase.INSTALLED
            state is DownloadState.Running -> SmartSearch.Phase.RUNNING
            state is DownloadState.Queued || state is DownloadState.Verifying -> SmartSearch.Phase.WAITING
            state is DownloadState.Failed -> SmartSearch.Phase.FAILED
            else -> SmartSearch.Phase.IDLE
        }
    val fit = DeviceCheck.fit(pack, DeviceProfile(ramGb, models.freeSpace()))
    val fits = fit != Fit.TOO_SMALL && fit != Fit.NO_SPACE
    val percent = ((state as? DownloadState.Running)?.fraction?.times(PERCENT_F))?.toInt() ?: 0
    return SmartSearch.of(phase, fits, pack.totalSize, percent)
}

private const val SLIDE_DIV = 6
private const val GAME_FROM = 0.9f
private const val BYTES_PER_GB = 1_000_000_000.0
private const val START_PREFS = "hain_start"
private const val KEY_SKIPPED = "onboarding_skipped"
private const val RATINGS_PREFS = "hain_ratings"
private const val KEY_LOCK = "app_lock"
private const val KEY_LOCK_DELAY = "app_lock_delay"
private const val KEY_NO_SCREENSHOTS = "block_screenshots"
private const val PERCENT_F = 100f
