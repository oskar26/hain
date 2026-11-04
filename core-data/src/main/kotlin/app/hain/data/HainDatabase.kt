package app.hain.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    entities = [ChatEntity::class, MessageEntity::class, MessageFts::class, ChatSummaryEntity::class, NetworkLogEntity::class],
    version = 2,
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
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
