package app.hain.tools.api

import app.hain.common.api.net.OfflineException
import app.hain.llm.api.ToolCall
import app.hain.llm.api.ToolSchema
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.IOException

/**
 * Registry of the tools (T-062): which schemas the brain sees and how one call runs. Every call
 * ends in a [ToolResult]; failures become a short error the brain can explain (TOOLS.md §7).
 */
class ToolRouter(
    private val tools: List<Tool>,
    private val timeoutMs: Long = TIMEOUT_MS,
) {
    private val byName = tools.associateBy { it.schema.name }

    /** Schemas for the prompt; online tools are left out while offline (shorter prompt, no dead ends). */
    fun schemas(offline: Boolean): List<ToolSchema> = tools.filter { !offline || !it.online }.map { it.schema }

    fun isOnline(name: String): Boolean = byName[name]?.online == true

    // Any tool problem must become a short answer for the brain, never a crash; the code is the message.
    @Suppress("TooGenericExceptionCaught", "SwallowedException", "ReturnCount")
    suspend fun run(
        call: ToolCall,
        ctx: ToolContext,
    ): ToolResult {
        val tool = byName[call.name] ?: return failure("unknown_tool", "There is no tool called ${call.name}.")
        if (tool.online && ctx.offline) return failure("offline", OFFLINE_HINT)
        return try {
            withTimeout(timeoutMs) { tool.run(call.arguments, ctx) }
        } catch (e: TimeoutCancellationException) {
            failure("timeout", "The tool took too long.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: OfflineException) {
            failure("offline", OFFLINE_HINT)
        } catch (e: IllegalArgumentException) {
            failure("bad_arguments", e.message ?: "The arguments do not fit.")
        } catch (e: IOException) {
            failure("network", "The service could not be reached.")
        } catch (e: Exception) {
            failure("failed", "The tool did not work.")
        }
    }

    companion object {
        const val TIMEOUT_MS = 12_000L
        private const val OFFLINE_HINT = "Offline mode is on. Tell the user and offer to answer from memory."

        /** `{"error":"<code>","hint":"<one sentence>"}` (TOOLS.md §7). */
        fun failure(
            code: String,
            hint: String,
        ): ToolResult {
            val json = JsonObject(mapOf("error" to JsonPrimitive(code), "hint" to JsonPrimitive(hint)))
            return ToolResult(forModel = json.toString(), error = code)
        }
    }
}

/** Helpers for reading the brain's arguments, which often arrive as text. */
internal object Args {
    fun string(
        args: Map<String, Any?>,
        key: String,
    ): String? = args[key]?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

    fun required(
        args: Map<String, Any?>,
        key: String,
    ): String = string(args, key) ?: throw BadArgumentsException("Missing '$key'.")

    fun int(
        args: Map<String, Any?>,
        key: String,
    ): Int? =
        when (val v = args[key]) {
            is Number -> v.toInt()
            is String -> v.trim().toDoubleOrNull()?.toInt()
            else -> null
        }
}
