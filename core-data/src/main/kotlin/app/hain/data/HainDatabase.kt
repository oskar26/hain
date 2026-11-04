package app.hain.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL
import app.hain.data.dao.ChatDao
import app.hain.data.dao.MemoryDao
import app.hain.data.dao.MessageDao
import app.hain.data.dao.NetworkLogDao
import app.hain.data.entities.ChatEntity
import app.hain.data.entities.ChatSummaryEntity
import app.hain.data.entities.MemoryFactEntity
import app.hain.data.entities.MemoryFactFts
import app.hain.data.entities.MemoryFactVecEntity
import app.hain.data.entities.MemoryTopicEntity
import app.hain.data.entities.MessageEntity
import app.hain.data.entities.MessageFts
import app.hain.data.entities.NetworkLogEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/** The app database `hain.db` (DATA.md), encrypted with SQLCipher. */
@Database(
    entities = [
        ChatEntity::class,
        MessageEntity::class,
        MessageFts::class,
        ChatSummaryEntity::class,
        NetworkLogEntity::class,
        MemoryFactEntity::class,
        MemoryFactFts::class,
        MemoryTopicEntity::class,
        MemoryFactVecEntity::class,
    ],
    // 4 is taken by Häppchen 8b (T-057 branches); 5 = memory part 2 (vectors, `replaces`), Häppchen 7b
    version = 5,
    exportSchema = true,
)
abstract class HainDatabase : RoomDatabase() {
    abstract fun chats(): ChatDao

    abstract fun messages(): MessageDao

    abstract fun networkLog(): NetworkLogDao

    abstract fun memory(): MemoryDao

    companion object {
        const val NAME = "hain.db"

        /** Version 2 adds the persistent network log. */
        val MIGRATION_1_2 =
            object : Migration(1, 2) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        "CREATE TABLE IF NOT EXISTS `network_log` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "`at` INTEGER NOT NULL, `host` TEXT NOT NULL, `purpose` TEXT NOT NULL, " +
                            "`bytes` INTEGER NOT NULL, `code` INTEGER, `url` TEXT)",
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_network_log_at` ON `network_log` (`at`)")
                }
            }

        /**
         * Version 3 adds the memory (T-111, MEMORY.md): facts with their full-text index and topics.
         * The SQL is exactly what Room generates for the entities (checked by HainMigrationTest).
         */
        @Suppress("MagicNumber") // schema versions
        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    MEMORY_SQL.forEach(db::execSQL)
                }

                // the JVM tests run Room on the bundled SQLite driver, which migrates through a connection
                override fun migrate(connection: SQLiteConnection) {
                    MEMORY_SQL.forEach { connection.execSQL(it) }
                }
            }

        /**
         * Placeholder so a version-3 database reaches 4 on this branch. Häppchen 8b (T-057 branches)
         * owns version 4: when both are merged, its MIGRATION_3_4 replaces this one (same name on
         * purpose, so keeping both does not compile).
         */
        @Suppress("MagicNumber") // schema versions
        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) = Unit

                override fun migrate(connection: SQLiteConnection) = Unit
            }

        /**
         * Version 5 (7b, T-110/T-114): fact vectors in `memory_fact_vec` and `memory_fact.replaces`
         * for suggestions that would replace an older fact. Same SQL as Room generates (HainMigrationTest).
         */
        @Suppress("MagicNumber") // schema versions
        val MIGRATION_4_5 =
            object : Migration(4, 5) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    MEMORY_V5_SQL.forEach(db::execSQL)
                }

                override fun migrate(connection: SQLiteConnection) {
                    MEMORY_V5_SQL.forEach { connection.execSQL(it) }
                }
            }

        /** Opens the encrypted database. The key comes from [KeyProvider] and never leaves memory unwrapped. */
        fun open(
            context: Context,
            keys: KeyProvider,
        ): HainDatabase {
            System.loadLibrary("sqlcipher")
            // Raw-key form x'…' skips SQLCipher's slow passphrase KDF; the key is already random.
            val hex = keys.databaseKey().joinToString("") { "%02x".format(it) }
            val factory = SupportOpenHelperFactory("x'$hex'".toByteArray())
            return Room
                .databaseBuilder(context.applicationContext, HainDatabase::class.java, NAME)
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .build()
        }
    }
}

/** Tables, index and full-text triggers of the memory (DB version 3). */
internal val MEMORY_SQL =
    listOf(
        "CREATE TABLE IF NOT EXISTS `memory_fact` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`text` TEXT NOT NULL, `category` TEXT NOT NULL, `status` TEXT NOT NULL, " +
            "`sensitive` INTEGER NOT NULL, `importance` REAL NOT NULL, `origin_chat_id` INTEGER, " +
            "`created_at` INTEGER NOT NULL, `superseded_by` INTEGER)",
        "CREATE INDEX IF NOT EXISTS `index_memory_fact_status` ON `memory_fact` (`status`)",
        "CREATE VIRTUAL TABLE IF NOT EXISTS `memory_fact_fts` USING FTS4(`text` TEXT NOT NULL, " +
            "tokenize=unicode61, content=`memory_fact`)",
        "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_memory_fact_fts_BEFORE_UPDATE BEFORE UPDATE " +
            "ON `memory_fact` BEGIN DELETE FROM `memory_fact_fts` WHERE `docid`=OLD.`rowid`; END",
        "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_memory_fact_fts_BEFORE_DELETE BEFORE DELETE " +
            "ON `memory_fact` BEGIN DELETE FROM `memory_fact_fts` WHERE `docid`=OLD.`rowid`; END",
        "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_memory_fact_fts_AFTER_UPDATE AFTER UPDATE " +
            "ON `memory_fact` BEGIN INSERT INTO `memory_fact_fts`(`docid`, `text`) " +
            "VALUES (NEW.`rowid`, NEW.`text`); END",
        "CREATE TRIGGER IF NOT EXISTS room_fts_content_sync_memory_fact_fts_AFTER_INSERT AFTER INSERT " +
            "ON `memory_fact` BEGIN INSERT INTO `memory_fact_fts`(`docid`, `text`) " +
            "VALUES (NEW.`rowid`, NEW.`text`); END",
        "CREATE TABLE IF NOT EXISTS `memory_topic` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
            "`title` TEXT NOT NULL, `note` TEXT NOT NULL, `updated_at` INTEGER NOT NULL)",
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_memory_topic_title` ON `memory_topic` (`title`)",
    )

/** Vectors and the `replaces` link of the memory (DB version 5, 7b). */
internal val MEMORY_V5_SQL =
    listOf(
        "ALTER TABLE `memory_fact` ADD COLUMN `replaces` INTEGER",
        "CREATE TABLE IF NOT EXISTS `memory_fact_vec` (`fact_id` INTEGER NOT NULL, `model` TEXT NOT NULL, " +
            "`text_hash` INTEGER NOT NULL, `dim` INTEGER NOT NULL, `vector` BLOB NOT NULL, " +
            "PRIMARY KEY(`fact_id`), FOREIGN KEY(`fact_id`) REFERENCES `memory_fact`(`id`) " +
            "ON UPDATE NO ACTION ON DELETE CASCADE )",
    )
