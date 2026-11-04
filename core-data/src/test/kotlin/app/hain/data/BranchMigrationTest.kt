package app.hain.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import app.hain.data.entities.MessageEntity
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import java.io.File

/**
 * T-057: a chat written by version 2 (built from the exported schema `schemas/…/2.json`) opens with
 * the current version 5, through the memory tables of version 3 (#38), the branches of version 4 (#47)
 * and the memory vectors of version 5 (#46).
 * Room checks every table against the entities; old messages become one straight line
 * (each one's parent is the message before it), and the new `selected_at` column can be used.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class BranchMigrationTest {
    @Test
    fun version2ChatsBecomeOneLineOfBranches() {
        runTest {
            val file = File.createTempFile("hain-v2-branches", ".db").also { it.delete() }
            createVersion2(file)
            val db = open(file)
            try {
                val messages = db.messages().forChat(1).associateBy { it.id }
                assertNull(messages.getValue(10).parentId)
                assertEquals(10L, messages.getValue(11).parentId)
                // created later but saved again earlier: the order by id counts, not the time
                assertEquals(11L, messages.getValue(12).parentId)
                // other chats are not mixed in
                assertNull(
                    db
                        .messages()
                        .forChat(2)
                        .single()
                        .parentId,
                )
                assertNull(messages.getValue(12).selectedAt)

                db.messages().upsert(messages.getValue(12).copy(selectedAt = 99))
                assertEquals(
                    99L,
                    db
                        .messages()
                        .forChat(1)
                        .first { it.id == 12L }
                        .selectedAt,
                )
            } finally {
                db.close()
                file.delete()
            }
        }
    }

    @Test
    fun branchSqlIsSafeToRunTwice() {
        val first = BranchSql.statements(listOf("id", "parent_id"))
        assertEquals(listOf(BranchSql.ADD_SELECTED, BranchSql.LINK_PARENTS), first)
        // the column is there already: only links are filled, nothing is added twice
        assertEquals(listOf(BranchSql.LINK_PARENTS), BranchSql.statements(listOf("id", "selected_at")))
    }

    @Test
    fun newBranchesKeepTheirParents() {
        runTest {
            val file = File.createTempFile("hain-v4-branches", ".db").also { it.delete() }
            createVersion2(file)
            open(file).close()
            // a second start runs no migration and changes nothing
            val db = open(file)
            try {
                db.messages().upsert(message(13, chatId = 1, parent = 10, selectedAt = 5))
                val sibling = db.messages().forChat(1).first { it.id == 13L }
                assertEquals(10L, sibling.parentId)
                assertEquals(5L, sibling.selectedAt)
                assertEquals(
                    11L,
                    db
                        .messages()
                        .forChat(1)
                        .first { it.id == 12L }
                        .parentId,
                )
            } finally {
                db.close()
                file.delete()
            }
        }
    }

    private fun open(file: File): HainDatabase =
        Room
            .databaseBuilder(ApplicationProvider.getApplicationContext(), HainDatabase::class.java, file.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(
                HainDatabase.MIGRATION_1_2,
                HainDatabase.MIGRATION_2_3,
                HainDatabase.MIGRATION_3_4,
                HainDatabase.MIGRATION_4_5,
            ).build()

    private fun message(
        id: Long,
        chatId: Long,
        parent: Long?,
        selectedAt: Long?,
    ) = MessageEntity(
        id = id,
        chatId = chatId,
        role = MessageEntity.ROLE_USER,
        content = "neu",
        createdAt = id,
        parentId = parent,
        selectedAt = selectedAt,
    )

    private fun createVersion2(file: File) {
        val schema = JSONObject(File(SCHEMA_V2).readText()).getJSONObject("database")
        val statements = mutableListOf<String>()
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) statements += entitySql(entities.getJSONObject(i))
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) statements += setup.getString(i)
        statements += chatSql(1, "Alt")
        statements += chatSql(2, "Anderer")
        statements += messageSql(10, chat = 1, role = "user", text = "Frage", createdAt = 100)
        statements += messageSql(11, chat = 1, role = "assistant", text = "Antwort", createdAt = 300)
        statements += messageSql(12, chat = 1, role = "user", text = "Noch was", createdAt = 200)
        statements += messageSql(20, chat = 2, role = "user", text = "Hallo", createdAt = 150)
        statements += "PRAGMA user_version = 2"
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            statements.forEach { connection.execSQL(it) }
        } finally {
            connection.close()
        }
    }

    private fun chatSql(
        id: Long,
        title: String,
    ) = "INSERT INTO chat (id, title, created_at, updated_at, model_id, pinned, private, archived) " +
        "VALUES ($id, '$title', 1, 1, NULL, 0, 0, 0)"

    private fun messageSql(
        id: Long,
        chat: Long,
        role: String,
        text: String,
        createdAt: Long,
    ) = "INSERT INTO message (id, chat_id, role, content, created_at, status) " +
        "VALUES ($id, $chat, '$role', '$text', $createdAt, 'ok')"

    /** Table, indices and full-text triggers of one entity in the exported schema. */
    private fun entitySql(entity: JSONObject): List<String> {
        val table = entity.getString("tableName")
        val out = mutableListOf(entity.getString("createSql").replace(PLACEHOLDER, table))
        val indices = entity.optJSONArray("indices")
        if (indices != null) {
            for (j in 0 until indices.length()) {
                out += indices.getJSONObject(j).getString("createSql").replace(PLACEHOLDER, table)
            }
        }
        val triggers = entity.optJSONArray("contentSyncTriggers")
        if (triggers != null) for (j in 0 until triggers.length()) out += triggers.getString(j)
        return out
    }

    private companion object {
        const val SCHEMA_V2 = "schemas/app.hain.data.HainDatabase/2.json"
        const val PLACEHOLDER = "\${TABLE_NAME}"
    }
}
