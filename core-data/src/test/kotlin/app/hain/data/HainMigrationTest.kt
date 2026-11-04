package app.hain.data

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import app.hain.data.dao.FtsQuery
import app.hain.data.entities.MemoryFactEntity
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
 * T-111 + T-057: a database written by version 2 (built from the exported schema `schemas/…/2.json`)
 * opens with version 4 through [HainDatabase.MIGRATION_2_3] (memory, #38) and
 * [HainDatabase.MIGRATION_3_4] (branches, #47); Room checks every table against the entities.
 * A version-3 database with memories keeps them on the way to 4.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class HainMigrationTest {
    @Test
    fun version2MigratesToVersion4() =
        runTest {
            val file = File.createTempFile("hain-v2", ".db").also { it.delete() }
            createVersion2(file)

            val db = open(file)
            try {
                assertEquals("Alt", db.chats().byId(1)!!.title)
                val id =
                    db.memory().insert(
                        MemoryFactEntity(
                            text = "mag keine Pilze",
                            category = "preference",
                            status = MemoryFactEntity.ACTIVE,
                            sensitive = false,
                            importance = 0.5,
                            originChatId = 1,
                            createdAt = 5,
                            supersededBy = null,
                        ),
                    )
                assertEquals(listOf(id), db.memory().search(FtsQuery.of("pilze")!!).map { it.id })
            } finally {
                db.close()
                file.delete()
            }
        }

    @Test
    fun version3WithMemoriesMigratesToVersion4() =
        runTest {
            val file = File.createTempFile("hain-v3", ".db").also { it.delete() }
            createVersion2(
                file,
                extra =
                    MEMORY_SQL +
                        listOf(
                            "INSERT INTO memory_fact (id, text, category, status, sensitive, importance, " +
                                "origin_chat_id, created_at, superseded_by) " +
                                "VALUES (7, 'spielt Gitarre', 'preference', 'active', 0, 0.5, 1, 5, NULL)",
                            "INSERT INTO message (id, chat_id, role, content, created_at, status) " +
                                "VALUES (10, 1, 'user', 'Frage', 100, 'ok')",
                            "INSERT INTO message (id, chat_id, role, content, created_at, status) " +
                                "VALUES (11, 1, 'assistant', 'Antwort', 200, 'ok')",
                        ),
                version = 3,
            )
            val db = open(file)
            try {
                assertEquals(listOf(7L), db.memory().search(FtsQuery.of("gitarre")!!).map { it.id })
                val messages = db.messages().forChat(1).associateBy { it.id }
                assertEquals(10L, messages.getValue(11).parentId)
                assertNull(messages.getValue(11).selectedAt)
            } finally {
                db.close()
                file.delete()
            }
        }

    private fun open(file: File): HainDatabase =
        Room
            .databaseBuilder(ApplicationProvider.getApplicationContext(), HainDatabase::class.java, file.absolutePath)
            .setDriver(BundledSQLiteDriver())
            .addMigrations(HainDatabase.MIGRATION_1_2, HainDatabase.MIGRATION_2_3, HainDatabase.MIGRATION_3_4)
            .build()

    private fun createVersion2(
        file: File,
        extra: List<String> = emptyList(),
        version: Int = 2,
    ) {
        val schema = JSONObject(File(SCHEMA_V2).readText()).getJSONObject("database")
        val statements = mutableListOf<String>()
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) statements += entitySql(entities.getJSONObject(i))
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) statements += setup.getString(i)
        statements +=
            "INSERT INTO chat (id, title, created_at, updated_at, model_id, pinned, private, archived) " +
            "VALUES (1, 'Alt', 1, 1, NULL, 0, 0, 0)"
        statements += extra
        statements += "PRAGMA user_version = $version"
        val connection = BundledSQLiteDriver().open(file.absolutePath)
        try {
            statements.forEach { connection.execSQL(it) }
        } finally {
            connection.close()
        }
    }

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
