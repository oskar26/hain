package app.hain.feature.onboarding.api

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.hain.feature.onboarding.R
import app.hain.ui.components.HainButton
import app.hain.ui.components.HainButtonStyle
import app.hain.ui.pim.JuicyPim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.GlowHue
import app.hain.ui.theme.HainStyle
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainStyle
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.calmTile
import kotlinx.coroutines.launch

private data class TourPage(
    val title: Int,
    val body: Int,
    val hero: Hero?,
    val glow: GlowHue = GlowHue.MINT,
)

private val PAGES =
    listOf(
        TourPage(R.string.tour_1_title, R.string.tour_1_body, null),
        TourPage(R.string.tour_2_title, R.string.tour_2_body, Hero.LOCK, GlowHue.SKY),
        TourPage(R.string.tour_3_title, R.string.tour_3_body, Hero.TREE, GlowHue.TEAL),
        TourPage(R.string.tour_4_title, R.string.tour_4_body, Hero.SEED, GlowHue.SUN),
    )

/**
 * The welcome background per style (docs/design/STYLE_OPTIONS.md): Stil A plain page colour, Stil B
 * a soft mint morning mist that fades into the page from the top (Copilot welcome reference).
 * Chat stays plain.
 */
@Composable
internal fun tourBackground(): Brush {
    val colors = LocalHainColors.current
    if (LocalHainStyle.current == HainStyle.A) return SolidColor(colors.bg)
    return Brush.verticalGradient(
        0f to lerp(colors.bg, MINT_WASH, WASH_TOP),
        WASH_END to colors.bg,
    )
}

/**
 * Welcome tour before the download (2e, T-011): the first page is a cloud of shiny bubbles with a
 * round white Pim knob below it, the others show their hero object inside a big glass bubble. Big
 * calm headline, two wide pills at the bottom (white "skip", dark "next").
 */
@Suppress("LongMethod") // pager page, bubble cloud and the two pills form one screen
@Composable
fun WelcomeTour(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    val pager = rememberPagerState { PAGES.size }
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == PAGES.lastIndex
    Column(
        modifier
            .fillMaxSize()
            .background(tourBackground())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = spacing.lg, vertical = spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { index ->
            val page = PAGES[index]
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (page.hero == null) {
                    BubbleCloud()
                    // the round white knob with Pim, overlapping the bottom of the cloud like in Wabi
                    Box(
                        Modifier
                            .offset(y = -KNOB_OVERLAP)
                            .size(KNOB)
                            .calmTile(CircleShape, shadow = KNOB_ELEVATION),
                        contentAlignment = Alignment.Center,
                    ) {
                        JuicyPim(PimState.HAPPY, KNOB * PIM_SHARE)
                    }
                } else {
                    GlassBubble(HERO_BUBBLE, page.glow) { HeroObject(page.hero, HERO_SIZE) }
                    Spacer(Modifier.height(spacing.xl))
                }
                BasicText(
                    stringResource(page.title),
                    style = type.display.copy(color = colors.text, textAlign = TextAlign.Center, fontSize = TITLE_SIZE),
                    modifier = Modifier.padding(horizontal = spacing.md),
                )
                Spacer(Modifier.height(spacing.md))
                BasicText(
                    stringResource(page.body),
                    style = type.body.copy(color = colors.textMuted, textAlign = TextAlign.Center),
                    modifier = Modifier.padding(horizontal = spacing.md),
                )
            }
        }
        Dots(count = PAGES.size, current = pager.currentPage)
        Spacer(Modifier.height(spacing.lg))
        if (!last) {
            HainButton(
                stringResource(R.string.tour_skip),
                onFinish,
                Modifier.fillMaxWidth(),
                style = HainButtonStyle.SECONDARY,
            )
            Spacer(Modifier.height(spacing.md))
        }
        HainButton(
            text = stringResource(if (last) R.string.tour_go else R.string.tour_next),
            onClick = {
                if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth(),
            style = if (last) HainButtonStyle.PRIMARY else HainButtonStyle.DARK,
        )
    }
}

@Composable
private fun Dots(
    count: Int,
    current: Int,
) {
    val colors = LocalHainColors.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(count) { i ->
            val active = i == current
            val width by animateDpAsState(if (active) DOT_ACTIVE else DOT, label = "dot-width")
            val color by animateColorAsState(if (active) colors.accent else colors.border, label = "dot-color")
            Box(Modifier.size(width = width, height = DOT).background(color, CircleShape))
        }
    }
}

private val HERO_SIZE = 150.dp
private val HERO_BUBBLE = 230.dp
private val KNOB = 76.dp
private val KNOB_OVERLAP = 30.dp
private val KNOB_ELEVATION = 3.dp
private const val PIM_SHARE = 0.72f
private val TITLE_SIZE = 32.sp
private val DOT = 8.dp
private val DOT_ACTIVE = 22.dp
private val MINT_WASH = Color(color = 0xFF7FD6A4)
private const val WASH_TOP = 0.22f
private const val WASH_END = 0.6f
