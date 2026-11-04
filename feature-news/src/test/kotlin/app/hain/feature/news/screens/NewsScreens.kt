package app.hain.feature.news.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.GateResponse
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.NetworkLogEntry
import app.hain.feature.news.api.AskResult
import app.hain.feature.news.api.AskState
import app.hain.feature.news.api.LocalNewsImages
import app.hain.feature.news.api.NewsActions
import app.hain.feature.news.api.NewsBody
import app.hain.feature.news.api.NewsImages
import app.hain.feature.news.api.NewsPage
import app.hain.feature.news.api.NewsSamples
import app.hain.feature.news.api.NewsUiState
import app.hain.feature.news.api.OpenArticle
import app.hain.feature.news.api.ReaderResult
import app.hain.tools.internal.news.NewsMode
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.theme.HainTheme
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.captureScreenRoboImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream

/**
 * News screenshots for docs/design/screens and the website (T-073, T-075–T-079); run only via the CI
 * workflow screens.yml. The pictures are drawn here (soft gradients), nothing is downloaded.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "de-w392dp-h850dp-xxhdpi")
class NewsScreens {
    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val folder = TemporaryFolder()

    private val now = System.currentTimeMillis()

    @Test
    fun newsListLight() {
        shoot("news_light.png", dark = false) { listState() }
    }

    @Test
    fun newsListDark() {
        shoot("news_dark.png", dark = true) { listState() }
    }

    @Test
    fun newsBriefingLight() {
        shoot("news_briefing_light.png", dark = false) {
            NewsSamples.state(now, pictures = true).copy(briefing = NewsSamples.briefing(now))
        }
    }

    @Test
    fun readerLight() {
        shoot("news_reader_light.png", dark = false) { withArticle(NewsSamples.state(now, pictures = true)) }
    }

    @Test
    fun readerDark() {
        shoot("news_reader_dark.png", dark = true) { withArticle(NewsSamples.state(now, pictures = true)) }
    }

    @Test
    fun readerPaywallLight() {
        shoot("news_reader_paywall_light.png", dark = false) {
            withArticle(NewsSamples.state(now, pictures = true), index = 1, locked = true)
        }
    }

    @Test
    fun askPimLight() {
        shoot("news_ask_light.png", dark = false, startWithAsk = true) {
            val open = withArticle(NewsSamples.state(now, pictures = true))
            open.copy(
                ask =
                    AskState(
                        "Fass den Artikel kurz zusammen.",
                        AskResult.Answer(
                            "• Der Bundestag hat den Haushalt knapp beschlossen.\n" +
                                "• Mehr Geld geht an die Bahn und an Schulen.\n" +
                                "• Bei einigen Förderprogrammen wird gespart.\n" +
                                "• Der Bundesrat berät im Dezember.",
                        ),
                    ),
            )
        }
    }

    // English app for the English website (texts of the headlines stay as the sources wrote them)
    @Test
    @Config(qualifiers = "en-w392dp-h850dp-xxhdpi")
    fun newsListLightEn() {
        shoot("news_light_en.png", dark = false) { listState() }
    }

    @Test
    @Config(qualifiers = "en-w392dp-h850dp-xxhdpi")
    fun newsListDarkEn() {
        shoot("news_dark_en.png", dark = true) { listState() }
    }

    @Test
    @Config(qualifiers = "en-w392dp-h850dp-xxhdpi")
    fun readerLightEn() {
        shoot("news_reader_light_en.png", dark = false) { withArticle(NewsSamples.state(now, pictures = true)) }
    }

    @Test
    @Config(qualifiers = "en-w392dp-h850dp-xxhdpi")
    fun readerDarkEn() {
        shoot("news_reader_dark_en.png", dark = true) { withArticle(NewsSamples.state(now, pictures = true)) }
    }

    @Test
    fun settingsLight() {
        shoot("news_settings_light.png", dark = false, page = NewsPage.SETTINGS) { NewsSamples.state(now) }
    }

    @Test
    fun ownFeedsLight() {
        shoot("news_feeds_light.png", dark = false, page = NewsPage.FEEDS) {
            NewsSamples.state(now, NewsMode.STANDARD_PLUS_OWN)
        }
    }

    @Test
    @Config(qualifiers = "de-w1280dp-h800dp-land-xhdpi")
    fun tabletListDetailLight() {
        shoot("news_tablet_light.png", dark = false) {
            withArticle(NewsSamples.state(now, pictures = true).copy(briefing = briefingShort()))
        }
    }

    @Test
    @Config(qualifiers = "de-w1280dp-h800dp-land-xhdpi")
    fun tabletListDetailDark() {
        shoot("news_tablet_dark.png", dark = true) {
            withArticle(NewsSamples.state(now, pictures = true).copy(briefing = briefingShort()))
        }
    }

    private fun listState() = NewsSamples.state(now, pictures = true).copy(briefing = briefingShort())

    private fun briefingShort() = NewsSamples.briefing(now).copy(expanded = false)

    private fun withArticle(
        state: NewsUiState,
        index: Int = 0,
        locked: Boolean = false,
    ): NewsUiState {
        val item = state.items[index].copy(paywall = locked)
        return state.copy(open = OpenArticle(item, ReaderResult.Ready(NewsSamples.article(item, locked))))
    }

    private fun shoot(
        file: String,
        dark: Boolean,
        page: NewsPage = NewsPage.LIST,
        startWithAsk: Boolean = false,
        state: () -> NewsUiState,
    ) {
        val s = state()
        val images = warmImages(s)
        compose.setContent {
            Frame(dark) {
                CompositionLocalProvider(LocalNewsImages provides images) {
                    NewsBody(s, NewsActions(), startPage = page, now = now, startWithAsk = startWithAsk)
                }
            }
        }
        compose.waitForIdle()
        captureScreenRoboImage("$OUT/$file")
    }

    /** The drawn pictures, already decoded in memory so the screenshot shows them right away. */
    private fun warmImages(state: NewsUiState): NewsImages {
        val images = NewsImages(NoGate, folder.newFolder()) { url -> picture(url) }
        val urls =
            state.items.mapNotNull { it.imageUrl } +
                listOf(NewsSamples.picture("chart")) +
                listOfNotNull(state.open?.item?.imageUrl)
        runBlocking {
            urls.distinct().forEach { url ->
                images.load(url, NewsImages.DEFAULT_WIDTH)
                images.load(url, NewsImages.THUMB_WIDTH)
            }
        }
        return images
    }
}

private const val OUT = "build/outputs/roborazzi"

/** Hues of the drawn pictures per source: greens, blues and warm tones. */
private val HUES =
    mapOf("tagesschau" to 205f, "jungewelt" to 12f, "bbc" to 350f, "dlf" to 150f, "chart" to 160f)

/** A soft landscape-like gradient as a PNG: sky, two hills and a sun. */
private fun picture(url: String): ByteArray {
    val key = url.substringAfterLast('/').substringBefore('.')
    val hue = HUES[key] ?: ((url.hashCode() and 0xFFFF) % 360).toFloat()
    val w = 1200
    val h = 675
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val top = Color.HSVToColor(floatArrayOf(hue, 0.30f, 0.92f))
    val bottom = Color.HSVToColor(floatArrayOf((hue + 30f) % 360f, 0.55f, 0.55f))
    val sky = Paint().apply { shader = LinearGradient(0f, 0f, 0f, h.toFloat(), top, bottom, Shader.TileMode.CLAMP) }
    canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), sky)
    val sun = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(150, 255, 248, 225) }
    canvas.drawCircle(w * 0.74f, h * 0.30f, h * 0.11f, sun)
    val far = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(90, 255, 255, 255) }
    canvas.drawCircle(w * 0.22f, h * 1.10f, h * 0.62f, far)
    val near = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(70, 0, 40, 30) }
    canvas.drawCircle(w * 0.80f, h * 1.25f, h * 0.70f, near)
    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    return out.toByteArray()
}

/** The gate for the screenshots: never online, the pictures come from [picture]. */
private object NoGate : NetworkGate {
    override val log: StateFlow<List<NetworkLogEntry>> = MutableStateFlow(emptyList())
    override val active: StateFlow<Int> = MutableStateFlow(0)

    override suspend fun fetch(request: GateRequest): GateResponse = GateResponse(404, "")

    override fun clearLog() = Unit
}

@Composable
private fun Frame(
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    HainTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalReduceMotion provides true) { content() }
    }
}
