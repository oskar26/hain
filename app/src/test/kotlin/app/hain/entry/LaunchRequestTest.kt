package app.hain.entry

import app.hain.feature.chat.internal.Chat
import app.hain.feature.chat.internal.UiMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-144: share intents, widget and tile starts. */
class LaunchRequestTest {
    private val send = "android.intent.action.SEND"

    @Test
    fun sharedTextOpensANewChatWithTheTextInTheBox() {
        val request = LaunchIntents.parse(send, "text/plain", "Was heißt das?\r\nZeile 2", null)
        assertEquals(LaunchRequest.NewChat("Was heißt das?\nZeile 2", focus = true), request)
    }

    @Test
    fun aSharedLinkKeepsThePageTitle() {
        val request = LaunchIntents.parse(send, "text/plain", "https://example.org/a?b=1", "Ein Artikel")
        assertEquals(LaunchRequest.NewChat("Ein Artikel\nhttps://example.org/a?b=1", focus = true), request)
        // a subject next to normal text (often the app's name) is left out
        val plain = LaunchIntents.parse(send, "text/plain", "Schau mal hier", "Meine App")
        assertEquals(LaunchRequest.NewChat("Schau mal hier", focus = true), plain)
        // the title is already in the text: not twice
        assertEquals("www.x.de", LaunchIntents.sharedText("www.x.de", "x.de"))
    }

    @Test
    fun onlyASubjectStillWorks() {
        assertEquals("Nur Titel", LaunchIntents.sharedText(null, "  Nur Titel "))
    }

    @Test
    fun emptyOrWrongSharesAreANormalStart() {
        assertNull(LaunchIntents.parse(send, "text/plain", "   ", null))
        assertNull(LaunchIntents.parse(send, "image/png", "x", null)) // a picture without the file
        assertNull(LaunchIntents.parse(send, null, "x", null))
        assertNull(LaunchIntents.parse("android.intent.action.MAIN", null, null, null))
        assertNull(LaunchIntents.parse(null, null, null, null))
    }

    @Test
    fun aSharedFileOpensANewChatWithTheFileAttached() {
        val pdf = "content://com.example.files/doc/42"
        assertEquals(
            LaunchRequest.NewChat("", focus = true, file = pdf),
            LaunchIntents.parse(send, "application/pdf", null, "Rechnung.pdf", stream = pdf),
        )
        // a caption that comes with a photo goes into the message box
        val photo = "content://media/external/images/media/7"
        assertEquals(
            LaunchRequest.NewChat("Was steht hier?", focus = true, file = photo),
            LaunchIntents.parse(send, "image/jpeg", "Was steht hier?", null, stream = photo),
        )
        // a text file comes as a file, not as text
        val txt = "content://x/notes.txt"
        val fromText = LaunchIntents.parse(send, "text/plain", null, null, stream = txt) as LaunchRequest.NewChat
        assertEquals(txt, fromText.file)
    }

    @Test
    fun foreignFilePathsAndOtherTypesAreNeverRead() {
        assertNull(LaunchIntents.parse(send, "application/pdf", null, null, stream = "file:///sdcard/secret.pdf"))
        assertNull(LaunchIntents.parse(send, "application/zip", null, null, stream = "content://x/a.zip"))
        assertNull(LaunchIntents.parse(send, null, null, null, stream = "content://x/a"))
        // shared text with a stream of a type Hain cannot read stays a text share
        assertEquals(
            LaunchRequest.NewChat("Hallo", focus = true),
            LaunchIntents.parse(send, "text/plain", "Hallo", null, stream = "file:///x"),
        )
    }

    @Test
    fun invisibleCharactersGoAndLongTextIsCut() {
        assertEquals("ab", LaunchIntents.sharedText("\u200Ba\u0007b\uFEFF", null))
        val long = "x".repeat(LaunchIntents.MAX_SHARED_CHARS + 50)
        val cut = LaunchIntents.sharedText(long, null)!!
        assertEquals(LaunchIntents.MAX_SHARED_CHARS + 1, cut.length)
        assertTrue(cut.endsWith("…"))
    }

    @Test
    fun widgetAndTileStarts() {
        assertEquals(
            LaunchRequest.NewChat(focus = true),
            LaunchIntents.parse(LaunchIntents.ACTION_NEW_CHAT, null, null, null, focus = true),
        )
        val open = LaunchIntents.parse(LaunchIntents.ACTION_OPEN_CHAT, null, null, null, chatId = 42)
        assertEquals(LaunchRequest.OpenChat(42), open)
        // no chat id: a new chat instead
        assertEquals(LaunchRequest.NewChat(), LaunchIntents.parse(LaunchIntents.ACTION_OPEN_CHAT, null, null, null))
    }

    @Test
    fun widgetShowsTheNewestChatWithMessages() {
        val msg = listOf(UiMessage(1, true, "hi"))
        val chats =
            listOf(
                Chat(1, "Alt", msg, updatedAt = 10),
                Chat(2, "Leer", emptyList(), updatedAt = 30),
                Chat(3, "Neu", msg, updatedAt = 20),
            )
        assertEquals(WidgetChat(3, "Neu"), WidgetData.latest(chats))
        assertNull(WidgetData.latest(emptyList()))
    }

    @Test
    fun widgetSkipsPrivateChatsAndHidesTheTitleWhileLocked() {
        val msg = listOf(UiMessage(1, true, "hi"))
        val chats =
            listOf(
                Chat(1, "Normal", msg, updatedAt = 10),
                Chat(2, "Geheim", msg, updatedAt = 30, isPrivate = true),
            )
        // T-056: a private chat never shows up on the home screen, even when it is the newest
        assertEquals(WidgetChat(1, "Normal"), WidgetData.latest(chats))
        // app lock on: the last chat stays reachable, but without its title
        assertEquals(WidgetChat(1, null), WidgetData.latest(chats, appLock = true))
        assertNull(WidgetData.latest(listOf(chats[1])))
    }
}
