package app.hain.docs.internal.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.hain.data.KeyProvider
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Hain's documents (T-123): their own small database `hain_docs.db`, encrypted with SQLCipher and
 * the same key as `hain.db`. Kept apart so the chat schema stays as it is (other parts of the app
 * change `hain.db` at the same time). Version 1.
 */
@Database(entities = [DocumentEntity::class, DocChunkEntity::class], version = 1, exportSchema = false)
abstract class DocsDatabase : RoomDatabase() {
    abstract fun docs(): DocsDao

    companion object {
        const val NAME = "hain_docs.db"

        fun open(
            context: Context,
            keys: KeyProvider,
        ): DocsDatabase {
            System.loadLibrary("sqlcipher")
            val hex = keys.databaseKey().joinToString("") { "%02x".format(it) }
            val factory = SupportOpenHelperFactory("x'$hex'".toByteArray())
            return Room
                .databaseBuilder(context.applicationContext, DocsDatabase::class.java, NAME)
                .openHelperFactory(factory)
                .build()
        }
    }
}
