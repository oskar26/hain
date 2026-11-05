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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Settings screenshots for docs/design/screens (T-011); run only via CI workflow screens.yml. */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class SettingsScreens {
    @get:Rule
    val compose = createComposeRule()

    private val actions = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {})

    @Test
    fun hubLight() {
        compose.setContent { Frame(dark = false) { SettingsBody(HainSettings(), actions) } }
        captureScreenRoboImage("$OUT/settings_light.png")
    }

    @Test
    fun hubDark() {
        compose.setContent { Frame(dark = true) { SettingsBody(HainSettings(), actions) } }
        captureScreenRoboImage("$OUT/settings_dark.png")
    }

    @Test
    fun answersPage() {
        compose.setContent {
            Frame(dark = false) { SettingsBody(HainSettings(), actions, startPage = SettingsPage.ANSWERS) }
        }
        captureScreenRoboImage("$OUT/settings_answers_light.png")
    }

    // ---- T-234: Modelle & Pakete ----

    @Test
    fun hubWithNewsLight() {
        val withNews = SettingsActions({}, {}, {}, {}, {}, {}, {}, {}, {}, {}, onOpenNews = {})
        compose.setContent { Frame(dark = false) { SettingsBody(HainSettings(), withNews) } }
        captureScreenRoboImage("$OUT/settings_hub_light.png")
    }

    @Test
    fun packsHubLight() {
        compose.setContent { Frame(dark = false) { PacksScreen(FakeModels, ramGb = 8, onBack = {}) } }
        captureScreenRoboImage("$OUT/packs_hub_light.png")
    }

    @Test
    fun packsKnowledgeLight() {
        compose.setContent {
            Frame(dark = false) {
                PacksScreen(FakeModels, ramGb = 8, onBack = {}, startCategory = PackCategory.KNOWLEDGE)
            }
        }
        captureScreenRoboImage("$OUT/packs_knowledge_light.png")
    }

    @Test
    fun packsVoiceDark() {
        compose.setContent {
            Frame(dark = true) { PacksScreen(FakeModels, ramGb = 8, onBack = {}, startCategory = PackCategory.VOICE) }
        }
        captureScreenRoboImage("$OUT/packs_voice_dark.png")
    }

    @Test
    fun lookPageDark() {
        compose.setContent {
            Frame(dark = true) { SettingsBody(HainSettings(), actions, startPage = SettingsPage.LOOK) }
        }
        captureScreenRoboImage("$OUT/settings_look_dark.png")
    }
}

private const val OUT = "build/outputs/roborazzi"

/** A catalogue like the app's: one brain, dictation, search by meaning, three Wikipedia packs. */
internal object FakeModels : ModelRepository {
    @Suppress("LongParameterList")
    private fun pack(
        id: String,
        name: String,
        kind: String,
        bytes: Long,
        lang: List<String> = listOf("de"),
        default: Boolean = false,
    ) = ModelInfo(
        id,
        name,
        kind,
        listOf(ModelFile("https://example.org/$id.bin", "0".repeat(64), bytes)),
        2,
        "l",
        "u",
        default,
        lang,
    )

    override val catalog =
        listOf(
            pack("lfm3-1.7b-qad-q4_0", "LFM3 1.7B", ModelInfo.KIND_BRAIN, 1_049_615_680, default = true),
            pack("moonshine-small-de", "Moonshine Small (Deutsch)", "stt", 166_000_000),
            pack("moonshine-small-en", "Moonshine Small (English)", "stt", 209_000_000, listOf("en")),
            pack("lfm2.5-embedding-350m-q4_0", "LFM2.5 Embedding 350M", ModelInfo.KIND_EMBED, 219_308_800),
            pack("wikipedia-de-top-mini", "Wikipedia Deutsch · Top", ModelInfo.KIND_KNOWLEDGE, 136_444_709),
            pack("wikipedia-en-top-mini", "Wikipedia EN", ModelInfo.KIND_KNOWLEDGE, 296_858_160, listOf("en")),
            pack("wikipedia-de-all-mini", "Wikipedia Deutsch · alle", ModelInfo.KIND_KNOWLEDGE, 4_686_161_304),
        )
    private val installed = setOf("lfm3-1.7b-qad-q4_0", "moonshine-small-de", "wikipedia-de-top-mini")
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

@Composable
private fun Frame(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    HainTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalReduceMotion provides true) {
            Box(Modifier.fillMaxSize().background(LocalHainColors.current.bg)) { content() }
        }
    }
}
