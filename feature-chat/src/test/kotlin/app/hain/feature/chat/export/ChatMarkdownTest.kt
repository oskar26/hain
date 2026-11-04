package app.hain.feature.chat.export

import app.hain.feature.chat.internal.Chat
import app.hain.feature.chat.internal.SourceRef
import app.hain.feature.chat.internal.UiMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class ChatMarkdownTest {
    private val labels =
        ExportLabels(you = "Du", hain = "Hain", exportedLine = "Chat vom 4. November 2026", sources = "Quellen")

    private fun chat(
        id: Long = 1,
        title: String = "Himmel",
        updatedAt: Long = 10,
        secret: Boolean = false,
        messages: List<UiMessage> =
            listOf(
                UiMessage(2, isUser = true, text = "Warum ist der Himmel blau?"),
                UiMessage(
                    3,
                    isUser = false,
                    text = "Wegen **Rayleigh**.\n\n```kotlin\nval x = 1\n```",
                    reasoning = "geheimes Denken",
                    sources = listOf(SourceRef("Wikipedia (Himmel)", "https://de.wikipedia.org/wiki/Himmel")),
                ),
            ),
    ) = Chat(id, title, messages, updatedAt = updatedAt, isPrivate = secret)

    @Test
    fun markdownHasTitleDateRolesTextAndSources() {
        val md = ChatMarkdown.export(chat(), labels)
        val expected =
            """
            # Himmel

            *Chat vom 4. November 2026*

            ---

            **Du**

            Warum ist der Himmel blau?

            **Hain**

            Wegen **Rayleigh**.

            ```kotlin
            val x = 1
            ```

            Quellen:

            - [Wikipedia (Himmel)](https://de.wikipedia.org/wiki/Himmel)

            """.trimIndent()
        assertEquals(expected, md)
    }

    @Test
    fun thinkingFailedAndEmptyAnswersAreLeftOut() {
        val messages =
            listOf(
                UiMessage(1, isUser = true, text = "Hallo"),
                UiMessage(2, isUser = false, text = "", failed = true),
                UiMessage(3, isUser = false, text = "  "),
                UiMessage(4, isUser = false, text = "Hi", reasoning = "nachdenken"),
            )
        val md = ChatMarkdown.export(chat(messages = messages), labels)
        assertFalse(md.contains("nachdenken"))
        assertEquals(2, Regex("\\*\\*(Du|Hain)\\*\\*").findAll(md).count())
    }

    @Test
    fun brokenTitlesAndLinkCharactersAreMadeSafe() {
        val messages =
            listOf(
                UiMessage(
                    1,
                    isUser = false,
                    text = "Text",
                    sources = listOf(SourceRef("A [b] c", "https://x.org/a b)")),
                ),
            )
        val md = ChatMarkdown.export(chat(title = "  zwei\n Zeilen ", messages = messages), labels)
        assertTrue(md.startsWith("# zwei Zeilen\n"))
        assertTrue(md.contains("- [A \\[b\\] c](https://x.org/a%20b%29)"))
    }

    @Test
    fun fileNamesAreSafe() {
        val name = ExportFileName.of(chat(title = "Warum ist der Himmel blau?"), "2026-11-04", "md")
        assertEquals("hain-warum-ist-der-himmel-blau-2026-11-04.md", name)
        assertEquals("gruesse-aus-koeln", ExportFileName.slug("Grüße aus Köln!"))
        assertEquals("chat", ExportFileName.slug("???"))
        assertTrue(ExportFileName.slug("a".repeat(200)).length <= 60)
    }

    @Test
    fun zipHasOneFilePerSavedChatAndNeverAPrivateOne() {
        val chats =
            listOf(
                chat(id = 1, title = "Himmel", updatedAt = 30),
                chat(id = 2, title = "Himmel", updatedAt = 20),
                chat(id = 3, title = "Geheim", updatedAt = 40, secret = true),
                chat(id = 4, title = "Leer", updatedAt = 50, messages = emptyList()),
            )
        val out = ByteArrayOutputStream()
        val count = ChatZip.write(chats, out, { labels }, { "2026-11-04" })
        assertEquals(2, count)
        val entries = mutableMapOf<String, String>()
        ZipInputStream(out.toByteArray().inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                entry = zip.nextEntry
            }
        }
        assertEquals(listOf("2026-11-04-himmel.md", "2026-11-04-himmel-2.md"), entries.keys.toList())
        assertTrue(entries.values.all { it.startsWith("# Himmel") })
        assertFalse(entries.values.any { it.contains("Geheim") })
    }
}
