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
import app.hain.data.dao.MessageDao
import app.hain.data.dao.NetworkLogDao
import app.hain.data.entities.ChatEntity
import app.hain.data.entities.ChatSummaryEntity
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
    ],
    // 3 is taken by Häppchen 7 (memory, PR #38); 4 = T-057 branches (selected_at + parent_id filled in)
    version = 4,
    exportSchema = true,
)
abstract class HainDatabase : RoomDatabase() {
    abstract fun chats(): ChatDao

    abstract fun messages(): MessageDao

    abstract fun networkLog(): NetworkLogDao

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
         * Placeholder so a version-2 database reaches 3 on this branch. Häppchen 7 (memory, PR #38)
         * owns version 3: when both are merged, its MIGRATION_2_3 replaces this one (same name on
         * purpose, so keeping both does not compile).
         */
        @Suppress("MagicNumber") // schema versions
        val MIGRATION_2_3 =
            object : Migration(2, 3) {
                override fun migrate(db: SupportSQLiteDatabase) = Unit

                override fun migrate(connection: SQLiteConnection) = Unit
            }

        /**
         * Version 4 (T-057): edit and "ask again" keep the old versions as branches. Adds
         * `message.selected_at` (which sibling is shown) and links every older message to the one
         * before it (`parent_id`, by id, per chat), so old chats stay one straight line. Safe to run
         * twice: the column is only added when it is missing, only empty links are filled.
         */
        @Suppress("MagicNumber") // schema versions
        val MIGRATION_3_4 =
            object : Migration(3, 4) {
                override fun migrate(db: SupportSQLiteDatabase) {
                    val columns = mutableListOf<String>()
                    db.query("PRAGMA table_info(`message`)").use { c ->
                        val name = c.getColumnIndex("name")
                        while (c.moveToNext()) columns += c.getString(name)
                    }
                    BranchSql.statements(columns).forEach(db::execSQL)
                }

                // the JVM tests run Room on the bundled SQLite driver, which migrates through a connection
                override fun migrate(connection: SQLiteConnection) {
                    val columns = mutableListOf<String>()
                    connection.prepare("PRAGMA table_info(`message`)").use { st ->
                        while (st.step()) columns += st.getText(1)
                    }
                    BranchSql.statements(columns).forEach { connection.execSQL(it) }
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
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
        }
    }
}

/** SQL of [HainDatabase.MIGRATION_3_4]; [columns] = the columns `message` already has. */
internal object BranchSql {
    const val ADD_SELECTED = "ALTER TABLE `message` ADD COLUMN `selected_at` INTEGER"

    /** Every message without a parent gets the message before it in its chat (none for the first). */
    const val LINK_PARENTS =
        "UPDATE `message` SET `parent_id` = (SELECT p.`id` FROM `message` p " +
            "WHERE p.`chat_id` = `message`.`chat_id` AND p.`id` < `message`.`id` ORDER BY p.`id` DESC LIMIT 1) " +
            "WHERE `parent_id` IS NULL"

    fun statements(columns: List<String>): List<String> {
        val add = ADD_SELECTED.takeIf { "selected_at" !in columns }
        return listOfNotNull(add, LINK_PARENTS)
    }
}
