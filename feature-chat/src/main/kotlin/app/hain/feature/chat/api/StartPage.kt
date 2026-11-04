@file:Suppress("MatchingDeclarationName") // the start page: Greeting + its Suggestion model

package app.hain.feature.chat.api

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.hain.feature.chat.R
import app.hain.ui.components.Hain3D
import app.hain.ui.components.Hain3DIcon
import app.hain.ui.components.pressScale
import app.hain.ui.motion.LocalReduceMotion
import app.hain.ui.pim.HeroPim
import app.hain.ui.theme.HainLevel
import app.hain.ui.theme.LocalHainColors
import app.hain.ui.theme.LocalHainSpacing
import app.hain.ui.theme.LocalHainTypography
import app.hain.ui.theme.hainSurface
import kotlinx.coroutines.delay
import kotlin.random.Random

/** One start page suggestion: a calm line icon and one short line. */
data class Suggestion(
    val icon: Hain3D,
    val text: String,
)

/** "leaf|Fun fact about moss" → icon + text; unknown icons get the sparkles (pure, tested). */
fun parseSuggestions(items: Array<String>): List<Suggestion> =
    items.mapNotNull { raw ->
        val text = raw.substringAfter('|', raw).trim()
        if (text.isEmpty()) return@mapNotNull null
        Suggestion(ICONS[raw.substringBefore('|', "").trim()] ?: Hain3D.SPARKLES, text)
    }

/** The suggestion of [slot] in rotation [round]: every round shows the next three (pure, tested). */
fun suggestionAt(
    pool: List<Suggestion>,
    round: Int,
    slot: Int,
    slots: Int = SLOTS,
): Suggestion = pool[(round * slots + slot).mod(pool.size)]

private val ICONS =
    mapOf(
        "sparkles" to Hain3D.SPARKLES,
        "leaf" to Hain3D.LEAF,
        "bubble" to Hain3D.BUBBLE,
        "recycle" to Hain3D.RECYCLE,
        "heart" to Hain3D.HEART,
        "bulb" to Hain3D.BULB,
        "weather" to Hain3D.WEATHER,
        "book" to Hain3D.BOOK,
        "phone" to Hain3D.PHONE,
        "calendar" to Hain3D.CALENDAR,
        "gift" to Hain3D.GIFT,
        "calc" to Hain3D.CALCULATOR,
        "globe" to Hain3D.GLOBE,
    )

/**
 * Empty chat (T-091): a big Pim you can tap, drag around and hold (the 2048 easter egg, [onEgg]),
 * a calm centred greeting and three one-line suggestions that fade to the next three every 6 s,
 * from a big pool shuffled anew each time the page opens. With the keyboard open there is little
 * room: Pim gets smaller and the suggestions step aside, nothing slides under the top bar.
 * [seed] fixes the order (screenshots); with "reduce motion" the suggestions stay put.
 */
@Suppress("LongMethod") // Pim, greeting and suggestions form one page
@Composable
internal fun Greeting(
    onSuggestion: (String) -> Unit,
    onEgg: () -> Unit = {},
    hour: Int =
        remember {
            java.time.LocalTime
                .now()
                .hour
        },
    seed: Int? = null,
) {
    val spacing = LocalHainSpacing.current
    val colors = LocalHainColors.current
    val type = LocalHainTypography.current
    val reduce = LocalReduceMotion.current
    val items = stringArrayResource(R.array.chat_suggestions)
    val pool = remember(items, seed) { parseSuggestions(items).shuffled(if (seed != null) Random(seed) else Random) }
    var round by remember { mutableIntStateOf(0) }
    LaunchedEffect(reduce, pool) {
        if (reduce || pool.size <= SLOTS) return@LaunchedEffect
        while (true) {
            delay(ROTATE_MS)
            round++
        }
    }
    val hello =
        stringResource(
            when (hour) {
                in MORNING -> R.string.chat_hello_morning
                in EVENING_FROM..LAST_HOUR, in 0 until MORNING.first -> R.string.chat_hello_evening
                else -> R.string.chat_hello_day
            },
        )
    BoxWithConstraints(Modifier.fillMaxSize().clipToBounds()) {
        val tall = maxHeight >= TALL.dp
        val pimSize =
            when {
                tall -> BIG_PIM.dp
                maxHeight >= MID.dp -> MID_PIM.dp
                else -> SMALL_PIM.dp
            }
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = spacing.xl, vertical = spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.md, Alignment.CenterVertically),
        ) {
            HeroPim(
                size = pimSize,
                label = stringResource(R.string.chat_pim_label),
                onEgg = onEgg,
                // dragged around, Pim floats above the greeting and the suggestions
                modifier = Modifier.zIndex(1f),
            )
            BasicText(
                "$hello " + stringResource(R.string.chat_hello_question),
                style = type.display.copy(color = colors.text, textAlign = TextAlign.Center),
                modifier = Modifier.widthIn(max = TEXT_MAX.dp),
            )
            if (tall && pool.isNotEmpty()) {
                BasicText(
                    stringResource(R.string.chat_greeting_body),
                    style = type.bodySmall.copy(color = colors.textMuted, textAlign = TextAlign.Center),
                )
                Spacer(Modifier.heightIn(min = spacing.xs))
                repeat(SLOTS) { slot ->
                    AnimatedContent(
                        targetState = suggestionAt(pool, round, slot),
                        transitionSpec = {
                            val wait = slot * STAGGER_MS
                            fadeIn(tween(FADE_MS, delayMillis = wait + FADE_MS / 2)) togetherWith
                                fadeOut(tween(FADE_MS, delayMillis = wait))
                        },
                        label = "suggestion-$slot",
                    ) { s -> SuggestionCard(s) { onSuggestion(s.text) } }
                }
            }
        }
    }
}

/** One suggestion: a calm Lichtung card, one line with its icon, presses in a little. */
@Composable
private fun SuggestionCard(
    suggestion: Suggestion,
    onClick: () -> Unit,
) {
    val colors = LocalHainColors.current
    val spacing = LocalHainSpacing.current
    val interaction = remember { MutableInteractionSource() }
    val scale = pressScale(interaction)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = CARD_MIN.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }.hainSurface(HainLevel.RAISED)
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Hain3DIcon(suggestion.icon, size = ICON.dp)
        Spacer(Modifier.width(spacing.sm))
        BasicText(
            suggestion.text,
            style = LocalHainTypography.current.body.copy(color = colors.text),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

/** For the screenshot tests: a Box with the page and a fixed suggestion order. */
@Composable
internal fun GreetingForShots() {
    Box(Modifier.fillMaxSize()) { Greeting(onSuggestion = {}, hour = HOUR_FOR_SHOTS, seed = 3) }
}

const val SLOTS = 3
private const val ROTATE_MS = 6_000L
private const val FADE_MS = 420
private const val STAGGER_MS = 140
private const val TALL = 560
private const val MID = 340
private const val BIG_PIM = 96
private const val MID_PIM = 72
private const val SMALL_PIM = 56
private const val TEXT_MAX = 340
private const val CARD_MIN = 48
private const val ICON = 24
private const val HOUR_FOR_SHOTS = 15
private const val MORNING_FROM = 5
private const val MORNING_TO = 10
private val MORNING = MORNING_FROM..MORNING_TO
private const val EVENING_FROM = 18
private const val LAST_HOUR = 23
