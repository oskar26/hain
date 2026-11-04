package app.hain.data

import app.hain.common.api.net.NetworkLogEntry
import app.hain.common.api.net.NetworkLogStore
import app.hain.common.api.net.Purpose
import app.hain.data.dao.NetworkLogDao
import app.hain.data.entities.NetworkLogEntity

/** [NetworkLogStore] on the encrypted database. */
class RoomNetworkLogStore(
    private val dao: NetworkLogDao,
) : NetworkLogStore {
    override suspend fun load(since: Long): List<NetworkLogEntry> =
        dao.since(since, LIMIT).map {
            NetworkLogEntry(
                at = it.at,
                host = it.host,
                purpose = Purpose.entries.firstOrNull { p -> p.name == it.purpose } ?: Purpose.OTHER,
                bytes = it.bytes,
                code = it.code,
                url = it.url,
            )
        }

    override suspend fun append(entry: NetworkLogEntry) {
        dao.insert(
            NetworkLogEntity(
                at = entry.at,
                host = entry.host,
                purpose = entry.purpose.name,
                bytes = entry.bytes,
                code = entry.code,
                url = entry.url,
            ),
        )
    }

    override suspend fun prune(before: Long) = dao.prune(before)

    override suspend fun clear() = dao.clear()

    private companion object {
        const val LIMIT = 500
    }
}
