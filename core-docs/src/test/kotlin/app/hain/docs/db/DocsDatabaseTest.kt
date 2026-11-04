package app.hain.docs.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import app.hain.docs.internal.db.DocChunkEntity
import app.hain.docs.internal.db.DocsDatabase
import app.hain.docs.internal.db.DocumentEntity
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

/** hain_docs.db on a plain in-memory database (SQLCipher itself only runs on the phone). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class DocsDatabaseTest {
    private lateinit var db: DocsDatabase

    @Before
    fun setUp() {
        db =
            Room
                .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DocsDatabase::class.java)
                .setDriver(BundledSQLiteDriver())
                .build()
    }

    @After
    fun tearDown() = db.close()

    private fun doc(
        name: String,
        at: Long,
    ) = DocumentEntity(name = name, kind = "pdf", mime = "application/pdf", pages = 2, chars = 40, addedAt = at)

    @Test
    fun addListsNewestFirstWithChunks() =
        runTest {
            val parts = listOf(DocChunkEntity(0, 0, 1, 0, "eins"), DocChunkEntity(0, 0, 2, 1, "zwei"))
            val a = db.docs().add(doc("a.pdf", 1), parts)
            val b = db.docs().add(doc("b.pdf", 2), listOf(DocChunkEntity(0, 0, 1, 0, "drei")))
            assertEquals(
                listOf("b.pdf", "a.pdf"),
                db
                    .docs()
                    .all()
                    .first()
                    .map { it.name },
            )
            assertEquals(listOf("eins", "zwei"), db.docs().chunks(listOf(a)).map { it.text })
            assertEquals(3, db.docs().chunks(listOf(a, b)).size)
        }

    @Test
    fun deleteRemovesChunksToo() =
        runTest {
            val a = db.docs().add(doc("a.pdf", 1), listOf(DocChunkEntity(0, 0, 1, 0, "eins")))
            db.docs().delete(a)
            assertNull(db.docs().byId(a))
            assertTrue(db.docs().chunks(listOf(a)).isEmpty())
        }
}
