package app.hain.tools.search

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.GateResponse
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.NetworkLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Network gate for tests: answers at once on the test thread, so virtual time and timeouts stay real. */
internal class FakeGate(
    private val answer: (GateRequest) -> GateResponse,
) : NetworkGate {
    val requests = mutableListOf<GateRequest>()
    private val entries = MutableStateFlow<List<NetworkLogEntry>>(emptyList())
    override val log: StateFlow<List<NetworkLogEntry>> = entries
    override val active: StateFlow<Int> = MutableStateFlow(0)

    override suspend fun fetch(request: GateRequest): GateResponse {
        requests += request
        val response = answer(request)
        entries.value = entries.value + NetworkLogEntry(0, request.url, request.purpose, 0, response.code)
        return response
    }

    override fun clearLog() {
        entries.value = emptyList()
    }
}
