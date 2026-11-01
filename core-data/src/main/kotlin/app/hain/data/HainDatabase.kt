package app.hain.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.hain.data.dao.ChatDao
import app.hain.data.dao.MessageDao
import app.hain.data.entities.ChatEntity
import app.hain.data.entities.ChatSummaryEntity
import app.hain.data.entities.MessageEntity
import app.hain.data.entities.MessageFts
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/** The app database `hain.db` (DATA.md), encrypted with SQLCipher. */
@Database(
    entities = [ChatEntity::class, MessageEntity::class, MessageFts::class, ChatSummaryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class HainDatabase : RoomDatabase() {
    abstract fun chats(): ChatDao

    abstract fun messages(): MessageDao

    companion object {
        const val NAME = "hain.db"

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
                .build()
        }
    }
}
