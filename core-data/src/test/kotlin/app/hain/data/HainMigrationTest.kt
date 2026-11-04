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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.ConscryptMode
import java.io.File

/**
 * T-111: a database written by version 2 (built from the exported schema `schemas/…/2.json`) opens
 * with version 3 through [HainDatabase.MIGRATION_2_3]; Room checks every table against the entities.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@ConscryptMode(ConscryptMode.Mode.OFF)
class HainMigrationTest {
    @Test
    fun version2MigratesToVersion3() =
        runTest {
            val file = File.createTempFile("hain-v2", ".db").also { it.delete() }
            createVersion2(file)

            val db =
                Room
                    .databaseBuilder(
                        ApplicationProvider.getApplicationContext(),
                        HainDatabase::class.java,
                        file.absolutePath,
                    ).setDriver(BundledSQLiteDriver())
                    .addMigrations(HainDatabase.MIGRATION_1_2, HainDatabase.MIGRATION_2_3)
                    .build()
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

    private fun createVersion2(file: File) {
        val schema = JSONObject(File(SCHEMA_V2).readText()).getJSONObject("database")
        val statements = mutableListOf<String>()
        val entities = schema.getJSONArray("entities")
        for (i in 0 until entities.length()) statements += entitySql(entities.getJSONObject(i))
        val setup = schema.getJSONArray("setupQueries")
        for (i in 0 until setup.length()) statements += setup.getString(i)
        statements +=
            "INSERT INTO chat (id, title, created_at, updated_at, model_id, pinned, private, archived) " +
            "VALUES (1, 'Alt', 1, 1, NULL, 0, 0, 0)"
        statements += "PRAGMA user_version = 2"
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
