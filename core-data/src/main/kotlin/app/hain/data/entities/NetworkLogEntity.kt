package app.hain.data.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** One line of the network log (DATA.md `network_log`); [url] is only set with the detailed log. */
@Entity(tableName = "network_log", indices = [Index("at")])
data class NetworkLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val at: Long,
    val host: String,
    val purpose: String,
    val bytes: Long,
    val code: Int?,
    val url: String?,
)
