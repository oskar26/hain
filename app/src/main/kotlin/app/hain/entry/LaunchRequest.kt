package app.hain.entry

import android.content.Context
import android.content.Intent
import app.hain.MainActivity
import app.hain.feature.chat.internal.ChatStore
import app.hain.feature.chat.internal.ChatViewModel
import kotlinx.coroutines.flow.first

/**
 * What Hain should open when it is started from outside (T-144): the share sheet, the home screen
 * widget or the quick settings tile. Nothing is ever sent on its own: shared text only lands in the
 * message box of a new chat.
 */
sealed interface LaunchRequest {
    /** A new chat; [text] goes into the message box, [focus] opens the keyboard. */
    data class NewChat(
        val text: String = "",
        val focus: Boolean = false,
    ) : LaunchRequest

    /** An existing chat (widget "last chat"); a new chat when it is gone. */
    data class OpenChat(
        val chatId: Long,
    ) : LaunchRequest
}

/** Parses start intents into a [LaunchRequest] and builds the intents of widget and tile. */
object LaunchIntents {
    const val ACTION_NEW_CHAT = "app.hain.action.NEW_CHAT"
    const val ACTION_OPEN_CHAT = "app.hain.action.OPEN_CHAT"
    const val EXTRA_CHAT_ID = "app.hain.extra.CHAT_ID"
    const val EXTRA_FOCUS = "app.hain.extra.FOCUS"

    /** Longer shared text is cut here; the message box is for a question, not a whole book. */
    const val MAX_SHARED_CHARS = 8_000

    private const val SEND = "android.intent.action.SEND"
    private const val NO_CHAT = -1L
    private val LONE_LINK = Regex("^(https?://|www\\.)\\S+$", RegexOption.IGNORE_CASE)
    private val INVISIBLE = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F\\u200B\\uFEFF]")

    /**
     * The request for one start. [text] and [subject] are the share extras (`EXTRA_TEXT`,
     * `EXTRA_SUBJECT`), [chatId] and [focus] come from widget and tile. `null` = a normal start.
     */
    @Suppress("LongParameterList") // the plain values of one intent, so it stays testable without Android
    fun parse(
        action: String?,
        type: String?,
        text: CharSequence?,
        subject: String?,
        chatId: Long = NO_CHAT,
        focus: Boolean = false,
    ): LaunchRequest? =
        when (action) {
            SEND -> shared(type, text, subject)
            ACTION_NEW_CHAT -> LaunchRequest.NewChat(focus = focus)
            ACTION_OPEN_CHAT -> if (chatId > 0) LaunchRequest.OpenChat(chatId) else LaunchRequest.NewChat()
            else -> null
        }

    private fun shared(
        type: String?,
        text: CharSequence?,
        subject: String?,
    ): LaunchRequest? {
        if (type?.startsWith("text/") != true) return null
        return sharedText(text, subject)?.let { LaunchRequest.NewChat(it, focus = true) }
    }

    /**
     * The text for the message box. A browser shares the page title as subject and the link as
     * text: then both come in ("Titel" + link below). Other subjects (often just the app's name) are
     * left out. Invisible characters go, line ends become `\n`, at most [MAX_SHARED_CHARS].
     */
    fun sharedText(
        text: CharSequence?,
        subject: String?,
    ): String? {
        val body = clean(text?.toString())
        val title = clean(subject)
        val joined =
            when {
                body.isEmpty() -> title
                title.isNotEmpty() && LONE_LINK.matches(body) && !body.contains(title) -> "$title\n$body"
                else -> body
            }
        if (joined.isEmpty()) return null
        return if (joined.length <= MAX_SHARED_CHARS) joined else joined.take(MAX_SHARED_CHARS).trimEnd() + "…"
    }

    private fun clean(raw: String?): String =
        raw
            .orEmpty()
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replace(INVISIBLE, "")
            .trim()

    /** Reads a start [intent]. */
    fun of(intent: Intent?): LaunchRequest? {
        if (intent == null) return null
        return parse(
            action = intent.action,
            type = intent.type,
            text = intent.getCharSequenceExtra(Intent.EXTRA_TEXT),
            subject = intent.getStringExtra(Intent.EXTRA_SUBJECT),
            chatId = intent.getLongExtra(EXTRA_CHAT_ID, NO_CHAT),
            focus = intent.getBooleanExtra(EXTRA_FOCUS, false),
        )
    }

    /** "Neuer Chat" from widget and tile; [focus] opens the keyboard ("Frag Pim"). */
    fun newChat(
        context: Context,
        focus: Boolean,
    ): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(ACTION_NEW_CHAT)
            .putExtra(EXTRA_FOCUS, focus)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    /** The widget's "last chat". */
    fun openChat(
        context: Context,
        chatId: Long,
    ): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(ACTION_OPEN_CHAT)
            .putExtra(EXTRA_CHAT_ID, chatId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
}

/**
 * Carries out this request in the chat once the saved chats are loaded: a new chat (shared text in
 * the message box, [onFocus] opens the keyboard) or the chat [LaunchRequest.OpenChat] names, a new
 * one when it was deleted meanwhile.
 */
suspend fun LaunchRequest.applyTo(
    store: ChatStore,
    chat: ChatViewModel,
    onFocus: () -> Unit,
) {
    store.loaded.first { it }
    when (this) {
        is LaunchRequest.NewChat -> {
            if (text.isBlank()) chat.newChat() else chat.newChatWith(text)
            if (focus || text.isNotBlank()) onFocus()
        }
        is LaunchRequest.OpenChat ->
            if (store.chat(chatId)?.messages?.isNotEmpty() == true) chat.openChat(chatId) else chat.newChat()
    }
}
