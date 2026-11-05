@file:Suppress("TooManyFunctions", "MatchingDeclarationName") // reading view: bar, article, states, Pim

package app.hain.feature.news.api

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import app.hain.cards.api.NewsTime
import app.hain.feature.news.R
import app.hain.tools.internal.news.Article
import app.hain.tools.internal.news.ArticleBlock
import app.hain.ui.HainIcons
import app.hain.ui.components.HainButton
import app.hain.ui.components.HainButtonStyle
import app.hain.ui.components.HainChip
import app.hain.ui.components.HainIconButton
import app.hain.ui.effects.hainGlass
import app.hain.ui.effects.hainGlassSource
import app.hain.ui.effects.rememberHainGlassState
import app.hain.ui.motion.HainMotion
import app.hain.ui.motion.hainTween
import app.hain.ui.pim.Pim
import app.hain.ui.pim.PimState
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainShapes
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface
import java.time.ZoneId
import java.util.Locale

/** Colours of the reading view for a [ReaderTheme] (T-076). */
internal data class ReaderColors(
    val bg: Color,
    val text: Color,
    val muted: Color,
    val surface: Color,
)

@Suppress("MagicNumber") // the fixed reader palettes
@Composable
internal fun readerColors(theme: ReaderTheme): ReaderColors {
    val c = LocalHainColors.current
    return when (theme) {
        ReaderTheme.APP -> ReaderColors(c.bg, c.text, c.textMuted, c.surface)
        ReaderTheme.LIGHT -> ReaderColors(Color(0xFFFFFFFF), Color(0xFF1B1D1C), Color(0xFF5E6460), Color(0xFFF2F4F3))
        ReaderTheme.DARK -> ReaderColors(Color(0xFF111312), Color(0xFFE7E9E8), Color(0xFF9AA19D), Color(0xFF1D201F))
        ReaderTheme.SEPIA -> ReaderColors(Color(0xFFF5EEDC), Color(0xFF4A3B2A), Color(0xFF7A6A55), Color(0xFFEDE3CB))
    }
}

/** Body text of the reading view from the settings: size, serif or sans, line spacing. */
@Composable
internal fun readerBody(
    prefs: NewsPrefs,
    color: Color,
): TextStyle {
    val base = LocalHainTypography.current.body
    val size = base.fontSize * prefs.textScale * BODY_GROWTH
    return base.copy(
        color = color,
        fontSize = size,
        lineHeight = (prefs.lineSpacing).em,
        fontFamily = if (prefs.serif) FontFamily.Serif else base.fontFamily,
    )
}

/**
 * The reading view (T-076): the article right in the app, without going to the website first.
 * Picture, source, time, headline, author, then the text with its pictures and charts, at most
 * [MAX_READ_WIDTH] wide. "Auf Website öffnen" stays (through LocalExternalLaunch). A cut-off
 * article says so (T-077). Pim floats in the corner for questions about the article (T-078).
 */
@Suppress("LongParameterList", "LongMethod")
@Composable
internal fun ReaderPane(
    open: OpenArticle,
    prefs: NewsPrefs,
    ask: AskState?,
    actions: NewsActions,
    now: Long,
    modifier: Modifier = Modifier,
    canAsk: Boolean = true,
    startWithAsk: Boolean = false,
) {
    val rc = readerColors(prefs.theme)
    val spacing = LocalHainSpacing.current
    val glass = rememberHainGlassState()
    var askOpen by rememberSaveable(open.item.guid) { mutableStateOf(startWithAsk) }
    var controlsOpen by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = askOpen) {
        askOpen = false
        actions.onCloseAsk()
    }
    BackHandler(enabled = controlsOpen && !askOpen) { controlsOpen = false }
    BackHandler(enabled = !askOpen && !controlsOpen) { actions.onCloseArticle() }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + BAR.dp
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + BUBBLE_SPACE.dp
    val article = (open.result as? ReaderResult.Ready)?.article
    Box(modifier.fillMaxSize().background(rc.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().hainGlassSource(glass),
            contentPadding = PaddingValues(top = top + spacing.sm, bottom = bottom),
        ) {
            val column = Modifier.readColumn()
            if (article != null) {
                articleItems(article, open, prefs, rc, now, actions, column)
            } else {
                item(key = "head") { ArticleHead(open, null, prefs, rc, now, column) }
                item(key = "state") { ReaderStateNote(open, rc, actions, column) }
            }
        }
        ReaderTopBar(
            source = open.item.source,
            onBack = actions.onCloseArticle,
            onControls = { controlsOpen = !controlsOpen },
            onWebsite = { actions.onOpenWebsite(open.item) },
            onShare = { actions.onShare(open.item) },
            modifier = Modifier.align(Alignment.TopCenter).hainGlass(glass).statusBarsPadding(),
        )
        AnimatedVisibility(
            visible = controlsOpen,
            enter = fadeIn(hainTween(HainMotion.MENU_MS)),
            exit = fadeOut(hainTween(HainMotion.FAST_MS)),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = top, end = spacing.sm),
        ) {
            Column(
                Modifier
                    .widthIn(max = CONTROLS_WIDTH.dp)
                    .hainSurface(HainLevel.FLOATING, LocalHainShapes.current.menu, fill = LocalHainColors.current.bg)
                    .padding(spacing.md),
            ) {
                ReaderControls(prefs, actions.onPrefs)
            }
        }
        if (article != null && canAsk) {
            PimBubble(
                onClick = { askOpen = true },
                modifier =
                    Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(end = spacing.lg, bottom = spacing.lg),
            )
        }
        AskSheet(
            visible = askOpen && article != null,
            ask = ask,
            actions = actions,
            onDismiss = {
                askOpen = false
                actions.onCloseAsk()
            },
        )
    }
}

/** The article's own width: centred, at most [MAX_READ_WIDTH] (tablets, landscape). */
private fun Modifier.readColumn(): Modifier =
    fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = MAX_READ_WIDTH.dp)
        // T-256: fill the column again, so short blocks (headings, list items) start at the left
        // edge like the paragraphs instead of being centred by wrapContentWidth
        .fillMaxWidth()
        .padding(horizontal = 20.dp)

@Suppress("LongParameterList")
private fun androidx.compose.foundation.lazy.LazyListScope.articleItems(
    article: Article,
    open: OpenArticle,
    prefs: NewsPrefs,
    rc: ReaderColors,
    now: Long,
    actions: NewsActions,
    column: Modifier,
) {
    item(key = "head") { ArticleHead(open, article, prefs, rc, now, column) }
    if (article.locked) {
        item(key = "paywall") { PaywallNote(rc, column) { actions.onOpenWebsite(open.item) } }
    }
    items(article.blocks.size, key = { "b$it" }) { i ->
        Block(article.blocks[i], prefs, rc, column)
    }
    item(key = "website") {
        Box(column.padding(top = 24.dp)) {
            HainButton(
                stringResource(R.string.news_open_website),
                { actions.onOpenWebsite(open.item) },
                style = HainButtonStyle.SECONDARY,
            )
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ArticleHead(
    open: OpenArticle,
    article: Article?,
    prefs: NewsPrefs,
    rc: ReaderColors,
    now: Long,
    modifier: Modifier,
) {
    val type = LocalHainTypography.current
    val locale = Locale.getDefault()
    val time = NewsTime.label(open.item.published, now, ZoneId.systemDefault(), locale)
    val picture = article?.leadImage ?: open.item.imageUrl
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (picture != null && prefs.articleImages) {
            NewsPicture(
                picture,
                Modifier.fillMaxWidth().aspectRatio(LEAD_RATIO).clip(LocalHainShapes.current.card),
            )
        }
        BasicText(
            listOfNotNull(open.item.source, time).joinToString(" · "),
            style = type.label.copy(color = rc.muted),
        )
        val titleStyle = type.display.copy(color = rc.text, fontSize = type.display.fontSize * prefs.textScale)
        BasicText(
            article?.title?.takeIf { it.isNotBlank() } ?: open.item.title,
            style = if (prefs.serif) titleStyle.copy(fontFamily = FontFamily.Serif) else titleStyle,
            modifier = Modifier.semantics { heading() },
        )
        article?.byline?.let {
            BasicText(stringResource(R.string.news_reader_by, it), style = type.bodySmall.copy(color = rc.muted))
        }
    }
}

@Composable
private fun ReaderStateNote(
    open: OpenArticle,
    rc: ReaderColors,
    actions: NewsActions,
    modifier: Modifier,
) {
    val type = LocalHainTypography.current
    val text =
        when (open.result) {
            null -> R.string.news_reader_loading
            ReaderResult.Offline -> R.string.news_reader_offline
            ReaderResult.Failed -> R.string.news_reader_failed
            else -> R.string.news_reader_not_readable
        }
    Column(modifier.padding(top = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (open.item.summary.isNotBlank()) {
            BasicText(open.item.summary, style = type.body.copy(color = rc.text))
        }
        BasicText(stringResource(text), style = type.bodySmall.copy(color = rc.muted))
        if (open.result != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HainButton(stringResource(R.string.news_open_website), { actions.onOpenWebsite(open.item) })
                if (open.result != ReaderResult.NotReadable) {
                    HainButton(
                        stringResource(R.string.news_reader_retry),
                        { actions.onOpen(open.item) },
                        style = HainButtonStyle.GHOST,
                    )
                }
            }
        }
    }
}

@Composable
private fun PaywallNote(
    rc: ReaderColors,
    modifier: Modifier,
    onOpen: () -> Unit,
) {
    val type = LocalHainTypography.current
    val colors = LocalHainColors.current
    Column(
        modifier
            .padding(top = 16.dp)
            .fillMaxWidth()
            .background(rc.surface, LocalHainShapes.current.chip)
            .border(1.dp, colors.border, LocalHainShapes.current.chip)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        BasicText(stringResource(R.string.news_paywall_hint), style = type.bodySmall.copy(color = rc.text))
        TextAction(stringResource(R.string.news_open_website), onOpen)
    }
}

@Composable
private fun Block(
    block: ArticleBlock,
    prefs: NewsPrefs,
    rc: ReaderColors,
    modifier: Modifier,
) {
    val body = readerBody(prefs, rc.text)
    val type = LocalHainTypography.current
    when (block) {
        is ArticleBlock.Paragraph -> BasicText(block.text, style = body, modifier = modifier.padding(top = 14.dp))
        is ArticleBlock.Heading ->
            BasicText(
                block.text,
                style = body.copy(fontWeight = FontWeight.SemiBold, fontSize = body.fontSize * HEADING_GROWTH),
                modifier = modifier.padding(top = 24.dp).semantics { heading() },
            )
        is ArticleBlock.Quote ->
            Row(modifier.padding(top = 16.dp)) {
                Box(Modifier.width(3.dp).heightIn(min = 24.dp).background(LocalHainColors.current.accent, CircleShape))
                BasicText(
                    block.text,
                    style = body.copy(fontStyle = FontStyle.Italic),
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
        is ArticleBlock.ListItem ->
            Row(modifier.padding(top = 8.dp)) {
                BasicText(block.number?.let { "$it." } ?: "•", style = body, modifier = Modifier.width(26.dp))
                BasicText(block.text, style = body)
            }
        is ArticleBlock.Image ->
            if (prefs.articleImages) {
                Column(modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ArticleImage(block.url, block.caption)
                    block.caption?.let { BasicText(it, style = type.label.copy(color = rc.muted)) }
                }
            }
    }
}

/** A picture or chart inside the article at its own aspect ratio; gone again when it cannot load. */
@Composable
private fun ArticleImage(
    url: String,
    caption: String?,
) {
    val images = LocalNewsImages.current ?: return
    val state by produceState<ImageLoad>(ImageLoad.Loading, url) {
        value = images.load(url)?.let { ImageLoad.Done(it) } ?: ImageLoad.Missing
    }
    val shape = LocalHainShapes.current.chip
    when (val s = state) {
        ImageLoad.Loading ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(LEAD_RATIO)
                    .clip(shape)
                    .background(LocalHainColors.current.surface),
            )
        ImageLoad.Missing -> Unit
        is ImageLoad.Done ->
            Image(
                s.bitmap,
                contentDescription = caption,
                contentScale = ContentScale.FillWidth,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(s.bitmap.width.toFloat() / s.bitmap.height.coerceAtLeast(1))
                        .clip(shape),
            )
    }
}

private sealed interface ImageLoad {
    data object Loading : ImageLoad

    data object Missing : ImageLoad

    data class Done(
        val bitmap: ImageBitmap,
    ) : ImageLoad
}

@Suppress("LongParameterList")
@Composable
private fun ReaderTopBar(
    source: String,
    onBack: () -> Unit,
    onControls: () -> Unit,
    onWebsite: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalHainColors.current
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = BAR.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HainIconButton(HainIcons.back, stringResource(R.string.news_back), onBack)
            BasicText(
                source,
                style = LocalHainTypography.current.button.copy(color = colors.text),
                maxLines = 1,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            HainIconButton(
                painterResource(R.drawable.ic_news_text),
                stringResource(R.string.news_reader_text),
                onControls,
            )
            HainIconButton(HainIcons.share, stringResource(R.string.news_share), onShare)
            HainIconButton(
                painterResource(R.drawable.ic_news_external),
                stringResource(R.string.news_open_website),
                onWebsite,
            )
        }
        Hairline()
    }
}

/** Text size, serif or sans, line spacing, colours, pictures (T-076); in the "Aa" panel and on the settings page. */
@Suppress("LongMethod")
@Composable
internal fun ReaderControls(
    prefs: NewsPrefs,
    onPrefs: ((NewsPrefs) -> NewsPrefs) -> Unit,
) {
    val type = LocalHainTypography.current
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
        BasicText(stringResource(R.string.news_prefs_size), style = type.label.copy(color = colors.textMuted))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val scales = NewsPrefs.TEXT_SCALES
            val index = scales.indexOfFirst { it >= prefs.textScale - SCALE_EPSILON }.coerceAtLeast(0)
            HainIconButton(
                painterResource(R.drawable.ic_news_text),
                stringResource(R.string.news_smaller),
                { onPrefs { it.copy(textScale = scales[(index - 1).coerceAtLeast(0)]) } },
                enabled = index > 0,
                iconSize = 14.dp,
            )
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                scales.forEachIndexed { i, _ ->
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (i == index) 10.dp else 6.dp)
                            .background(if (i <= index) colors.accent else colors.border, CircleShape),
                    )
                }
            }
            HainIconButton(
                painterResource(R.drawable.ic_news_text),
                stringResource(R.string.news_bigger),
                { onPrefs { it.copy(textScale = scales[(index + 1).coerceAtMost(scales.lastIndex)]) } },
                enabled = index < scales.lastIndex,
                iconSize = 22.dp,
            )
        }
        BasicText(stringResource(R.string.news_prefs_font), style = type.label.copy(color = colors.textMuted))
        ChipLine(
            listOf(stringResource(R.string.news_prefs_serif), stringResource(R.string.news_prefs_sans)),
            if (prefs.serif) 0 else 1,
        ) { i -> onPrefs { it.copy(serif = i == 0) } }
        BasicText(stringResource(R.string.news_prefs_lines), style = type.label.copy(color = colors.textMuted))
        ChipLine(
            listOf(
                stringResource(R.string.news_prefs_lines_tight),
                stringResource(R.string.news_prefs_lines_normal),
                stringResource(R.string.news_prefs_lines_wide),
            ),
            NewsPrefs.LINE_SPACINGS.indexOfFirst { it >= prefs.lineSpacing - SCALE_EPSILON }.coerceAtLeast(0),
        ) { i -> onPrefs { it.copy(lineSpacing = NewsPrefs.LINE_SPACINGS[i]) } }
        BasicText(stringResource(R.string.news_prefs_theme), style = type.label.copy(color = colors.textMuted))
        ChipLine(
            listOf(
                stringResource(R.string.news_prefs_theme_app),
                stringResource(R.string.news_prefs_theme_light),
                stringResource(R.string.news_prefs_theme_dark),
                stringResource(R.string.news_prefs_theme_sepia),
            ),
            prefs.theme.ordinal,
        ) { i -> onPrefs { it.copy(theme = ReaderTheme.entries[i]) } }
    }
}

/** One line of chips, one of them picked; scrolls sideways instead of wrapping. */
@Composable
internal fun ChipLine(
    labels: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(LocalHainSpacing.current.sm),
    ) {
        labels.forEachIndexed { i, label ->
            HainChip(
                label,
                selected = i == selected,
                onClick = { onSelect(i) },
                modifier = Modifier.semantics { this.selected = i == selected },
            )
        }
    }
}

/** Pim floating in the corner of the reading view: tap to ask about the article (T-078). */
@Composable
internal fun PimBubble(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.news_ask_pim)
    Box(
        modifier
            .size(BUBBLE.dp)
            .hainSurface(HainLevel.FLOATING, CircleShape, fill = LocalHainColors.current.bg)
            .clip(CircleShape)
            .clickable(onClickLabel = label, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Pim(PimState.IDLE, PIM_IN_BUBBLE.dp)
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(16.dp)
                .background(LocalHainColors.current.accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painterResource(R.drawable.ic_news_message),
                contentDescription = null,
                colorFilter =
                    androidx.compose.ui.graphics.ColorFilter
                        .tint(LocalHainColors.current.onAccent),
                modifier = Modifier.size(10.dp),
            )
        }
    }
}

/**
 * The bottom sheet for questions to Pim (T-078): three quick questions, a text field, Pim's answer
 * and "Im Chat weiterführen". Pim answers only from the article, on the phone.
 */
@Composable
internal fun BoxScope.AskSheet(
    visible: Boolean,
    ask: AskState?,
    actions: NewsActions,
    onDismiss: () -> Unit,
) {
    val colors = LocalHainColors.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(hainTween(HainMotion.BASE_MS)),
        exit = fadeOut(hainTween(HainMotion.FAST_MS)),
        modifier = Modifier.matchParentSize(),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(colors.scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(hainTween(HainMotion.PAGE_MS)) { it } + fadeIn(hainTween(HainMotion.BASE_MS)),
        exit = slideOutVertically(hainTween(HainMotion.BASE_MS)) { it } + fadeOut(hainTween(HainMotion.FAST_MS)),
        modifier = Modifier.align(Alignment.BottomCenter),
    ) {
        AskSheetBody(ask, actions, onDismiss)
    }
}

@Suppress("LongMethod")
@Composable
private fun AskSheetBody(
    ask: AskState?,
    actions: NewsActions,
    onDismiss: () -> Unit,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val spacing = LocalHainSpacing.current
    var text by rememberSaveable { mutableStateOf("") }
    val quick =
        listOf(
            stringResource(R.string.news_ask_chip_summary) to stringResource(R.string.news_ask_summary),
            stringResource(R.string.news_ask_chip_simple) to stringResource(R.string.news_ask_simple),
            stringResource(R.string.news_ask_chip_background) to stringResource(R.string.news_ask_background),
        )
    val running = ask != null && ask.result == null
    Column(
        Modifier
            .fillMaxWidth()
            .wrapContentWidth(Alignment.CenterHorizontally)
            .widthIn(max = MAX_READ_WIDTH.dp)
            .fillMaxWidth()
            .hainSurface(HainLevel.FLOATING, LocalHainShapes.current.sheet, fill = colors.bg)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            .navigationBarsPadding()
            .imePadding()
            .padding(spacing.lg)
            .heightIn(max = SHEET_MAX.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pim(if (running) PimState.THINKING else PimState.IDLE, 28.dp)
            BasicText(
                stringResource(R.string.news_ask_pim),
                style = type.button.copy(color = colors.text),
                modifier = Modifier.weight(1f).padding(start = spacing.sm).semantics { heading() },
            )
            HainIconButton(HainIcons.close, stringResource(R.string.news_reader_close), onDismiss)
        }
        when {
            ask == null -> Unit
            ask.result == null -> {
                BasicText(ask.question, style = type.bodySmall.copy(color = colors.textMuted))
                BasicText(stringResource(R.string.news_ask_thinking), style = type.body.copy(color = colors.text))
            }
            else -> AskAnswer(ask, actions)
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            quick.forEach { (label, question) ->
                HainChip(label, onClick = { if (!running) actions.onAsk(question) })
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(colors.surface, LocalHainShapes.current.composer)
                .padding(start = spacing.md, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val hint = stringResource(R.string.news_ask_hint)
            val send = {
                if (text.isNotBlank() && !running) {
                    actions.onAsk(text)
                    text = ""
                }
            }
            Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                if (text.isEmpty()) BasicText(hint, style = type.body.copy(color = colors.textMuted))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = type.body.copy(color = colors.text),
                    cursorBrush = SolidColor(colors.accent),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send() }),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = hint },
                )
            }
            HainIconButton(
                painterResource(R.drawable.ic_news_send),
                stringResource(R.string.news_ask_send),
                send,
                enabled = text.isNotBlank() && !running,
                tint = colors.onAccent,
                background = colors.accent,
            )
        }
        BasicText(stringResource(R.string.news_ask_note), style = type.label.copy(color = colors.textMuted))
    }
}

@Composable
private fun AskAnswer(
    ask: AskState,
    actions: NewsActions,
) {
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    BasicText(ask.question, style = type.bodySmall.copy(color = colors.textMuted))
    when (val r = ask.result) {
        is AskResult.Answer -> {
            BasicText(r.text, style = type.body.copy(color = colors.text))
            HainButton(stringResource(R.string.news_ask_continue), actions.onContinueInChat)
        }
        AskResult.NoBrain ->
            BasicText(stringResource(R.string.news_ask_no_brain), style = type.body.copy(color = colors.text))
        AskResult.Failed, null ->
            BasicText(stringResource(R.string.news_ask_failed), style = type.body.copy(color = colors.text))
    }
}

/** What the right side of the tablet layout shows before an article is picked (T-076). */
@Composable
internal fun ReaderPlaceholder(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Pim(PimState.IDLE, 56.dp)
            BasicText(
                stringResource(R.string.news_reader_pick),
                style = LocalHainTypography.current.body.copy(color = LocalHainColors.current.textMuted),
            )
        }
    }
}

/** Widest the article text gets (T-076): about 70 characters per line. */
const val MAX_READ_WIDTH = 680
private const val BAR = 56
private const val BUBBLE = 56
private const val PIM_IN_BUBBLE = 34
private const val BUBBLE_SPACE = 96
private const val CONTROLS_WIDTH = 320
private const val SHEET_MAX = 640
private const val LEAD_RATIO = 16f / 9f
private const val BODY_GROWTH = 1.06f
private const val HEADING_GROWTH = 1.15f
private const val SCALE_EPSILON = 0.01f
