package app.hain.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import app.hain.data.entities.NetworkLogEntity

@Dao
interface NetworkLogDao {
    @Insert
    suspend fun insert(entry: NetworkLogEntity)

    @Query("SELECT * FROM network_log WHERE at >= :since ORDER BY at DESC LIMIT :limit")
    suspend fun since(
        since: Long,
        limit: Int,
    ): List<NetworkLogEntity>

    @Query("DELETE FROM network_log WHERE at < :before")
    suspend fun prune(before: Long)

    @Query("DELETE FROM network_log")
    suspend fun clear()
}
