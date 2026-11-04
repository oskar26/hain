package app.hain.entry

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import app.hain.R
import app.hain.common.api.SettingsRepository
import app.hain.feature.chat.internal.Chat
import app.hain.feature.chat.internal.ChatStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/**
 * What the widget shows of your chats: the newest one (T-144). [title] is `null` while the app lock
 * is on (T-056): the home screen then only says "Letzter Chat", never what it was about.
 */
data class WidgetChat(
    val id: Long,
    val title: String?,
)

/** Pure picking for the widget, tested on the JVM. */
object WidgetData {
    /**
     * The newest chat with messages, or `null`. Private chats (T-056) are never shown; with the app
     * lock on ([appLock]) the chat keeps its place but loses its title.
     */
    fun latest(
        chats: List<Chat>,
        appLock: Boolean = false,
    ): WidgetChat? =
        chats
            .filter { it.messages.isNotEmpty() && !it.isPrivate }
            .maxByOrNull { it.updatedAt }
            ?.let { WidgetChat(it.id, if (appLock) null else it.title) }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun chatStore(): ChatStore

    fun settingsRepository(): SettingsRepository
}

/**
 * Home screen widget "Frag Pim" (T-144, Glance), style Lichtung: a calm white card (dark: deep
 * green-grey) with a hairline edge, Hain's sprout, a "Frag Pim …" field that opens a new chat with
 * the keyboard, a green "+" for a new chat and the last chat below. Everything opens the app;
 * the widget itself never sends anything.
 */
class HainWidget : GlanceAppWidget() {
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val last = lastChat(context)
        provideContent { WidgetContent(context, last) }
    }

    private suspend fun lastChat(context: Context): WidgetChat? {
        val app = context.applicationContext
        val entry =
            runCatching { EntryPointAccessors.fromApplication(app, WidgetEntryPoint::class.java) }
                .getOrNull() ?: return null
        val store = entry.chatStore()
        withTimeoutOrNull(LOAD_TIMEOUT_MS) { store.loaded.first { it } }
        // T-056: settings not readable in time → treat the lock as on (no title on the home screen)
        val locked =
            withTimeoutOrNull(LOAD_TIMEOUT_MS) {
                runCatching {
                    entry
                        .settingsRepository()
                        .settings
                        .first()
                        .appLock
                }.getOrNull()
            } ?: true
        return WidgetData.latest(store.chats.value, appLock = locked)
    }

    companion object {
        private const val LOAD_TIMEOUT_MS = 3_000L

        /** Redraws every Hain widget (after the chats changed). */
        suspend fun refresh(context: Context) {
            runCatching { HainWidget().updateAll(context.applicationContext) }
        }
    }
}

class HainWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HainWidget()
}

@Suppress("MagicNumber") // the Lichtung palette (core-ui Lichtung.kt), light and dark
private object WidgetColors {
    val edge = ColorProvider(day = Color(0xFFE5E8E6), night = Color(0xFF272C2A))
    val card = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF171B19))
    val field = ColorProvider(day = Color(0xFFF5F6F5), night = Color(0xFF202523))
    val text = ColorProvider(day = Color(0xFF0E1110), night = Color(0xFFECEFED))
    val muted = ColorProvider(day = Color(0xFF5E6561), night = Color(0xFF9BA39F))
    val accent = ColorProvider(day = Color(0xFF16A36B), night = Color(0xFF1FB877))
    val onAccent = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF0E1110))
}

@Suppress("LongMethod") // one small card: field, plus button, last chat
@Composable
private fun WidgetContent(
    context: Context,
    last: WidgetChat?,
) {
    val ask = context.getString(R.string.widget_ask)
    val newChat = context.getString(R.string.widget_new_chat)
    // hairline edge: the edge colour behind a card 1 dp smaller
    Box(
        GlanceModifier
            .fillMaxSize()
            .cornerRadius(RADIUS.dp)
            .background(WidgetColors.edge)
            .padding(1.dp),
    ) {
        Column(
            GlanceModifier
                .fillMaxSize()
                .cornerRadius((RADIUS - 1).dp)
                .background(WidgetColors.card)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // "Frag Pim …": looks like a message box, opens a new chat with the keyboard
                Row(
                    GlanceModifier
                        .defaultWeight()
                        .height(TOUCH.dp)
                        .cornerRadius((TOUCH / 2).dp)
                        .background(WidgetColors.field)
                        .padding(horizontal = 14.dp)
                        .clickable(actionStartActivity(LaunchIntents.newChat(context, focus = true)))
                        .semantics { contentDescription = ask },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        ImageProvider(R.drawable.ic_widget_sprout),
                        contentDescription = null,
                        colorFilter = ColorFilter.tint(WidgetColors.accent),
                        modifier = GlanceModifier.size(18.dp),
                    )
                    Spacer(GlanceModifier.width(8.dp))
                    Text(ask, style = TextStyle(color = WidgetColors.muted, fontSize = 15.sp), maxLines = 1)
                }
                Spacer(GlanceModifier.width(8.dp))
                Box(
                    GlanceModifier
                        .size(TOUCH.dp)
                        .cornerRadius((TOUCH / 2).dp)
                        .background(WidgetColors.accent)
                        .clickable(actionStartActivity(LaunchIntents.newChat(context, focus = false)))
                        .semantics { contentDescription = newChat },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        ImageProvider(R.drawable.ic_widget_plus),
                        contentDescription = newChat,
                        colorFilter = ColorFilter.tint(WidgetColors.onAccent),
                        modifier = GlanceModifier.size(20.dp),
                    )
                }
            }
            if (last != null) {
                Spacer(GlanceModifier.height(6.dp))
                LastChatRow(context, last)
            }
        }
    }
}

@Composable
private fun LastChatRow(
    context: Context,
    last: WidgetChat,
) {
    val title = last.title
    val label =
        if (title == null) {
            context.getString(R.string.widget_last_chat_locked)
        } else {
            context.getString(R.string.widget_last_chat, title)
        }
    val shown = title ?: context.getString(R.string.widget_last_chat_locked)
    Row(
        GlanceModifier
            .fillMaxWidth()
            .height(TOUCH.dp)
            .cornerRadius(12.dp)
            .padding(horizontal = 4.dp)
            .clickable(actionStartActivity(LaunchIntents.openChat(context, last.id)))
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            ImageProvider(R.drawable.ic_widget_chat),
            contentDescription = null,
            colorFilter = ColorFilter.tint(WidgetColors.muted),
            modifier = GlanceModifier.size(16.dp),
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            shown,
            style = TextStyle(color = WidgetColors.text, fontSize = 14.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )
    }
}

private const val RADIUS = 16
private const val TOUCH = 48
