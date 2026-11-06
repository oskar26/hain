package app.hain.feature.settings.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import app.hain.common.api.HainSettings
import app.hain.feature.settings.api.PackCategory
import app.hain.feature.settings.api.PacksScreen
import app.hain.feature.settings.api.SettingsActions
import app.hain.feature.settings.api.SettingsBody
import app.hain.feature.settings.api.SettingsPage
import app.hain.models.api.DownloadState
import app.hain.models.api.ModelFile
import app.hain.models.api.ModelInfo
import app.hain.models.api.ModelRepository
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.HainTheme
import app.hain.ui.theme.LocalHainColors
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * T-269: settings screenshots for the website's showcase (0.2), German and English, light and dark:
 * build/outputs/roborazzi/web/web_<id>_<light|dark>[_en].png (CI workflow screens.yml).
 */
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class WebSettingsScreens(
    dark: Boolean,
) : WebSettingsShots(dark, suffix = "") {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params(): List<Array<Any>> = listOf(false, true).map { arrayOf<Any>(it) }
    }
}

/** The same screens with the English app. */
@Config(sdk = [35], qualifiers = "en-w392dp-h850dp-xxhdpi")
class WebSettingsScreensEn(
    dark: Boolean,
) : WebSettingsShots(dark, suffix = "_en") {
    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "dark={0}")
        fun params(): List<Array<Any>> = listOf(false, true).map { arrayOf<Any>(it) }
    }
}

/** The screenshots themselves; the subclasses pick the language. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
abstract class WebSettingsShots(
    private val dark: Boolean,
    private val suffix: String,
) {
    @get:Rule
    val compose = createComposeRule()

    private val actions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {})

    private fun shot(name: String) {
        compose.waitForIdle()
        captureScreenRoboImage("$OUT/web/web_${name}_${if (dark) "dark" else "light"}$suffix.png")
    }

    @Composable
    private fun Frame(content: @Composable () -> Unit) {
        HainTheme(darkTheme = dark) {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                Box(Modifier.fillMaxSize().background(LocalHainColors.current.bg)) { content() }
            }
        }
    }

    /** T-283: the Internet page – one main switch, what may go online, the network log. */
    @Test
    fun internet() {
        compose.setContent { Frame { SettingsBody(HainSettings(), actions, startPage = SettingsPage.INTERNET) } }
        shot("settings_internet")
    }

    @Test
    fun packsHub() {
        compose.setContent { Frame { PacksScreen(WebModels, ramGb = 8, onBack = {}) } }
        shot("packs_hub")
    }

    /** T-261: every brain says honestly what it brings and when it is worth it. */
    @Test
    fun packsBrains() {
        compose.setContent {
            Frame { PacksScreen(WebModels, ramGb = 8, onBack = {}, startCategory = PackCategory.BRAINS) }
        }
        shot("packs_brains")
    }

    @Test
    fun packsKnowledge() {
        compose.setContent {
            Frame { PacksScreen(WebModels, ramGb = 8, onBack = {}, startCategory = PackCategory.KNOWLEDGE) }
        }
        shot("packs_knowledge")
    }

    @Test
    fun packsVoice() {
        compose.setContent {
            Frame { PacksScreen(WebModels, ramGb = 8, onBack = {}, startCategory = PackCategory.VOICE) }
        }
        shot("packs_voice")
    }
}

private const val OUT = "build/outputs/roborazzi"

/** The 0.2 catalogue (core-models/src/main/assets/catalog.json) with made-up download states. */
private object WebModels : ModelRepository {
    @Suppress("LongParameterList")
    private fun pack(
        id: String,
        name: String,
        kind: String,
        bytes: Long,
        lang: List<String> = listOf("de"),
        tier: String = ModelInfo.TIER_STANDARD,
        minRam: Int = 2,
        recommendRam: Int = minRam,
    ) = ModelInfo(
        id = id,
        name = name,
        kind = kind,
        files = listOf(ModelFile("https://example.org/$id.bin", "0".repeat(64), bytes)),
        minRamGb = minRam,
        licence = "l",
        licenceUrl = "u",
        isDefault = id == "lfm3-omni-2b-q4_0",
        lang = lang,
        tier = tier,
        recommendRamGb = recommendRam,
    )

    override val catalog =
        listOf(
            pack("lfm3-omni-2b-q4_0", "LFM3-Omni 2B", ModelInfo.KIND_BRAIN, 1_050_000_000, minRam = 4),
            pack(
                "jan-v3.5-4b-q4_k_m",
                "Jan-v3.5 4B",
                ModelInfo.KIND_BRAIN,
                2_720_000_000,
                tier = ModelInfo.TIER_STRONG,
                minRam = 8,
                recommendRam = 12,
            ),
            pack("moonshine-small-de", "Moonshine Small (Deutsch)", "stt", 166_000_000),
            pack("moonshine-small-en", "Moonshine Small (English)", "stt", 209_000_000, listOf("en")),
            pack("lfm2.5-embedding-350m-q4_0", "LFM2.5 Embedding 350M", ModelInfo.KIND_EMBED, 219_308_800),
            pack("wikipedia-de-top-mini", "Wikipedia Deutsch · Top", ModelInfo.KIND_KNOWLEDGE, 136_444_709),
            pack(
                "wikipedia-en-top-mini",
                "Wikipedia English · Top",
                ModelInfo.KIND_KNOWLEDGE,
                296_858_160,
                listOf("en"),
            ),
            pack("wikipedia-de-all-mini", "Wikipedia Deutsch · alle", ModelInfo.KIND_KNOWLEDGE, 4_686_161_304),
        )
    private val installed = setOf("lfm3-omni-2b-q4_0", "moonshine-small-de", "wikipedia-de-top-mini")
    override val defaultBrain = catalog.first()

    override fun mainFile(model: ModelInfo) = File("/nowhere/${model.id}")

    override fun isInstalled(model: ModelInfo) = model.id in installed

    override fun downloadState(model: ModelInfo): Flow<DownloadState> =
        flowOf(
            when (model.id) {
                in installed -> DownloadState.Installed
                "wikipedia-en-top-mini" -> DownloadState.Running(118_000_000, 296_858_160)
                else -> DownloadState.Idle
            },
        )

    override fun startDownload(model: ModelInfo) = Unit

    override fun cancelDownload(model: ModelInfo) = Unit

    override fun delete(model: ModelInfo) = Unit

    override fun freeSpace() = 32_000_000_000L
}
