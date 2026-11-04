package app.hain.common.api.net

/** Keeps the network log across app restarts (DATA.md `network_log`). */
interface NetworkLogStore {
    /** Entries not older than [since], newest first. */
    suspend fun load(since: Long): List<NetworkLogEntry>

    suspend fun append(entry: NetworkLogEntry)

    /** Removes everything older than [before]. */
    suspend fun prune(before: Long)

    suspend fun clear()
}
