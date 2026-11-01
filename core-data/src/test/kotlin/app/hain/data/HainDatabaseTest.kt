package app.hain.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import app.hain.data.dao.FtsQuery
import app.hain.data.entities.ChatEntity
import app.hain.data.entities.MessageEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode

/**
 * DAO tests on a plain (unencrypted) in-memory database with the bundled SQLite driver;
 * SQLCipher itself only runs on the device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF) // no Conscrypt JNI for every host (e.g. linux-aarch64)
class HainDatabaseTest {
    private lateinit var db: HainDatabase

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), HainDatabase::class.java)
                .setDriver(BundledSQLiteDriver())
                .build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun insertAndQueryChatsWithMessages() =
        runTest {
            db.chats().upsert(ChatEntity(1, "Plastik sparen", createdAt = 10, updatedAt = 10))
            db.chats().upsert(ChatEntity(2, "Wetter", createdAt = 20, updatedAt = 20, pinned = true))
            db.messages().upsert(message(3, chatId = 1, text = "Wie spare ich Plastik?"))
            val answer = message(4, chatId = 1, text = "Mit Stoffbeuteln.", role = MessageEntity.ROLE_ASSISTANT)
            db.messages().upsert(answer)

            assertEquals(listOf(2L, 1L), db.chats().all().map { it.id })
            assertEquals(listOf(3L, 4L), db.messages().forChat(1).map { it.id })
            assertEquals(4L, db.chats().maxId())
        }

    @Test
    fun renamePinAndCascadeDelete() =
        runTest {
            db.chats().upsert(ChatEntity(1, "a", createdAt = 1, updatedAt = 1))
            db.messages().upsert(message(2, chatId = 1, text = "hallo"))
            db.chats().rename(1, "Neu")
            db.chats().setPinned(1, true)
            val chat = db.chats().byId(1)!!
            assertEquals("Neu", chat.title)
            assertTrue(chat.pinned)

            db.chats().delete(1)
            assertNull(db.chats().byId(1))
            assertTrue(db.messages().all().isEmpty())
        }

    @Test
    fun fullTextSearchFindsChatsByPrefixAndUmlauts() =
        runTest {
            db.chats().upsert(ChatEntity(1, "a", createdAt = 1, updatedAt = 1))
            db.chats().upsert(ChatEntity(2, "b", createdAt = 2, updatedAt = 2))
            db.messages().upsert(message(3, chatId = 1, text = "Rezept für Gemüsesuppe"))
            db.messages().upsert(message(4, chatId = 2, text = "Fahrrad reparieren"))

            assertEquals(listOf(1L), db.messages().searchChatIds(FtsQuery.of("gemüse")!!))
            assertEquals(listOf(2L), db.messages().searchChatIds(FtsQuery.of("Fahr")!!))
            assertTrue(db.messages().searchChatIds(FtsQuery.of("Auto")!!).isEmpty())

            db.messages().upsert(message(4, chatId = 2, text = "Auto waschen"))
            assertEquals(listOf(2L), db.messages().searchChatIds(FtsQuery.of("auto")!!))
            db.messages().delete(4)
            assertTrue(db.messages().searchChatIds(FtsQuery.of("auto")!!).isEmpty())
        }

    private fun message(
        id: Long,
        chatId: Long,
        text: String,
        role: String = MessageEntity.ROLE_USER,
    ) = MessageEntity(id = id, chatId = chatId, role = role, content = text, createdAt = id)
}
