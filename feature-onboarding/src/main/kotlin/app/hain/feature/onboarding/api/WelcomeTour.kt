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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.hain.feature.onboarding.R
import app.hain.ui.components.HainButton
import app.hain.ui.components.HainButtonStyle
import app.hain.ui.pim.JuicyPim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import kotlinx.coroutines.launch

private data class TourPage(
    val title: Int,
    val body: Int,
    val hero: Hero?,
)

private val PAGES =
    listOf(
        TourPage(R.string.tour_1_title, R.string.tour_1_body, null),
        TourPage(R.string.tour_2_title, R.string.tour_2_body, Hero.LOCK),
        TourPage(R.string.tour_3_title, R.string.tour_3_body, Hero.TREE),
        TourPage(R.string.tour_4_title, R.string.tour_4_body, Hero.SEED),
    )

/** Soft mint → light lime wash (light) or a deep forest tint (dark). Chat stays plain. */
@Composable
internal fun tourBackground(): Brush {
    val colors = LocalHainColors.current
    return Brush.verticalGradient(
        listOf(lerp(colors.bg, MINT_WASH, WASH_TOP), colors.bg, lerp(colors.bg, LIME_WASH, WASH_BOTTOM)),
    )
}

/** Playful welcome tour before the download (2e): one thought and one 3D object per page. */
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
            .padding(spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            if (!last) HainButton(stringResource(R.string.tour_skip), onFinish, style = HainButtonStyle.GHOST)
        }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { index ->
            val page = PAGES[index]
            Column(
                Modifier.fillMaxSize().padding(horizontal = spacing.md),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (page.hero == null) {
                    JuicyPim(PimState.HAPPY, HERO_SIZE)
                } else {
                    HeroObject(page.hero, HERO_SIZE)
                }
                Spacer(Modifier.height(spacing.xl))
                BasicText(
                    stringResource(page.title),
                    style = type.display.copy(color = colors.text, textAlign = TextAlign.Center),
                )
                Spacer(Modifier.height(spacing.md))
                BasicText(
                    stringResource(page.body),
                    style = type.body.copy(color = colors.textMuted, textAlign = TextAlign.Center),
                )
            }
        }
        Dots(count = PAGES.size, current = pager.currentPage)
        Spacer(Modifier.height(spacing.lg))
        HainButton(
            text = stringResource(if (last) R.string.tour_go else R.string.tour_next),
            onClick = {
                if (last) onFinish() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
            },
            modifier = Modifier.fillMaxWidth(),
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

private val HERO_SIZE = 180.dp
private val DOT = 8.dp
private val DOT_ACTIVE = 22.dp
private val MINT_WASH = Color(color = 0xFF7FD6A4)
private val LIME_WASH = Color(color = 0xFFD8F0A0)
private const val WASH_TOP = 0.28f
private const val WASH_BOTTOM = 0.22f
