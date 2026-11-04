package app.hain.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import app.hain.data.dao.FtsQuery
import app.hain.data.entities.MemoryFactEntity
import app.hain.data.entities.MemoryTopicEntity
import kotlinx.coroutines.flow.first
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

/** T-111: memory facts, their full-text index and topics on a plain in-memory database. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class MemoryDaoTest {
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
    fun insertObserveAndSearchFacts() =
        runTest {
            val dao = db.memory()
            val pilze = dao.insert(fact("mag keine Pilze", createdAt = 10))
            dao.insert(fact("spielt Gitarre", createdAt = 20))
            dao.insert(fact("Schwester heißt Rosa", createdAt = 30, status = MemoryFactEntity.SUGGESTED))

            assertEquals(
                listOf("Schwester heißt Rosa", "spielt Gitarre", "mag keine Pilze"),
                dao.observe().first().map { it.text },
            )
            assertEquals(listOf(pilze), dao.search(FtsQuery.of("pilz")!!).map { it.id })
            assertTrue(dao.search(FtsQuery.of("Fußball")!!).isEmpty())
        }

    @Test
    fun editDeleteAndStatusKeepTheIndexInSync() =
        runTest {
            val dao = db.memory()
            val id = dao.insert(fact("wohnt in Köln", createdAt = 1))
            dao.update(dao.byId(id)!!.copy(text = "wohnt in Bonn"))
            assertTrue(dao.search(FtsQuery.of("köln")!!).isEmpty())
            assertEquals(listOf(id), dao.search(FtsQuery.of("bonn")!!).map { it.id })

            dao.setStatus(id, MemoryFactEntity.DELETED)
            assertTrue(dao.all().isEmpty())
            assertTrue(dao.search(FtsQuery.of("bonn")!!).isEmpty())

            dao.delete(id)
            assertNull(dao.byId(id))
        }

    @Test
    fun deleteAllAndTopics() =
        runTest {
            val dao = db.memory()
            dao.insert(fact("a", createdAt = 1))
            dao.insert(fact("b", createdAt = 2))
            dao.upsertTopic(MemoryTopicEntity(title = "Schule", note = "10. Klasse", updatedAt = 1))
            dao.upsertTopic(MemoryTopicEntity(title = "Musik", note = "Gitarre", updatedAt = 2))
            assertEquals(listOf("Musik", "Schule"), dao.topics().map { it.title })

            dao.deleteAllFacts()
            dao.deleteAllTopics()
            assertTrue(dao.all().isEmpty())
            assertTrue(dao.topics().isEmpty())
        }

    private fun fact(
        text: String,
        createdAt: Long,
        status: String = MemoryFactEntity.ACTIVE,
    ) = MemoryFactEntity(
        text = text,
        category = "other",
        status = status,
        sensitive = false,
        importance = 0.5,
        originChatId = null,
        createdAt = createdAt,
        supersededBy = null,
    )
}
