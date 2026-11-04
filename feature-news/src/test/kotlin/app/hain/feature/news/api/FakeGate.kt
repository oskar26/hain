package app.hain.feature.news.api

import app.hain.common.api.net.GateRequest
import app.hain.common.api.net.GateResponse
import app.hain.common.api.net.NetworkGate
import app.hain.common.api.net.NetworkLogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Network gate for the news tests: answers at once and remembers every request. */
internal class FakeGate(
    private val answer: (GateRequest) -> GateResponse = { GateResponse(404, "") },
) : NetworkGate {
    val requests = mutableListOf<GateRequest>()
    override val log: StateFlow<List<NetworkLogEntry>> = MutableStateFlow(emptyList())
    override val active: StateFlow<Int> = MutableStateFlow(0)

    override suspend fun fetch(request: GateRequest): GateResponse {
        requests += request
        return answer(request)
    }

    override fun clearLog() = Unit
}
