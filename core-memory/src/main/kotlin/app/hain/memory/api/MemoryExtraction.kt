package app.hain.memory.api

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.hain.data.dao.ChatDao
import app.hain.data.dao.MessageDao
import app.hain.data.entities.MessageEntity
import app.hain.llm.api.ChatMessage
import app.hain.llm.api.EngineState
import app.hain.llm.api.GenerationEvent
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.Role
import app.hain.llm.api.SamplingParams
import app.hain.llm.api.StopReason
import app.hain.memory.internal.ChatLine
import app.hain.memory.internal.ExtractionBrain
import app.hain.memory.internal.ExtractionPrompts
import app.hain.memory.internal.FactExtractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/** When the background extraction may use the brain (MEMORY.md §2.3: on the charger or battery > 30 %). */
object ExtractionPolicy {
    const val MIN_BATTERY = 30

    fun canRun(
        charging: Boolean,
        batteryPercent: Int,
    ): Boolean = charging || batteryPercent > MIN_BATTERY

    /** New user messages since the last run; only then is a run worth the battery. */
    fun hasNew(
        lastUserMessageId: Long?,
        doneUpTo: Long,
    ): Boolean = lastUserMessageId != null && lastUserMessageId > doneUpTo
}

/**
 * Background extraction (T-114, MEMORY.md §2.3): two minutes after the last answer of a chat a
 * [MemoryExtractionWorker] lets the local brain look for lasting facts in it. They only become
 * suggestions on the memory page. Never in private chats, never while the memory is off, and it
 * steps aside as soon as the user writes again ([yieldToChat]).
 */
class MemoryExtraction(
    private val context: Context,
    private val store: MemoryStore,
    private val llm: LlmEngine,
    private val brainPath: suspend () -> String?,
    private val chats: ChatDao,
    private val messages: MessageDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Volatile private var running: Deferred<Unit>? = null

    /** Runs after the chat was quiet for [IDLE_MINUTES] minutes; a newer answer pushes it back. */
    fun schedule(chatId: Long) {
        if (!store.enabled.value) return
        val request =
            OneTimeWorkRequestBuilder<MemoryExtractionWorker>()
                .setInitialDelay(IDLE_MINUTES, TimeUnit.MINUTES)
                .setInputData(workDataOf(KEY_CHAT to chatId))
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(true).build())
                .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_PREFIX + chatId, ExistingWorkPolicy.REPLACE, request)
    }

    /** The user writes again: a running extraction stops at once, so the chat gets the brain. */
    fun yieldToChat() {
        val job = running ?: return
        if (job.isActive) {
            job.cancel()
            llm.cancel()
        }
    }

    /** One run for [chatId]; `false` when it should wait for the charger. */
    @Suppress("ReturnCount")
    internal suspend fun run(chatId: Long): Boolean {
        if (!store.enabled.value) return true
        if (!batteryAllows()) return false
        val chat = chats.byId(chatId) ?: return true
        if (chat.isPrivate) return true
        val rows = messages.forChat(chatId).filter { it.status == MessageEntity.STATUS_OK }
        val lastUser = rows.lastOrNull { it.role == MessageEntity.ROLE_USER }?.id
        val doneUpTo = prefs.getLong(KEY_DONE + chatId, 0L)
        if (!ExtractionPolicy.hasNew(lastUser, doneUpTo)) return true
        val lines =
            rows
                .filter { it.role == MessageEntity.ROLE_USER || it.role == MessageEntity.ROLE_ASSISTANT }
                .filter { it.content.isNotBlank() }
                .map { ChatLine(it.role == MessageEntity.ROLE_USER, it.content) }
        val path = brainPath() ?: return true
        val job =
            scope.async {
                // the brain the chat has loaded is used as it is (no second load, no other context size)
                if (llm.state.value !is EngineState.Ready && !llm.load(path)) return@async
                val outcome = FactExtractor(store, LlmExtractionBrain(llm), prompts()).run(lines, chatId)
                Log.i(TAG, "extraction: ${outcome.suggested.size} suggested, ${outcome.skipped} skipped")
            }
        running = job
        try {
            job.await()
            prefs.edit().putLong(KEY_DONE + chatId, lastUser ?: 0L).apply()
        } catch (e: CancellationException) {
            // stepped aside for the chat; the next answer schedules it again
            Log.i(TAG, "extraction stopped for the chat", e)
        } finally {
            running = null
        }
        return true
    }

    private fun prompts(): ExtractionPrompts {
        fun read(name: String) =
            context.assets
                .open("prompts/$name")
                .bufferedReader()
                .use { it.readText() }
                .trim()
        return ExtractionPrompts(read("extract_facts.txt"), read("compare_facts.txt"))
    }

    private fun batteryAllows(): Boolean {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return true
        val level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        val percent = if (level >= 0 && scale > 0) level * PERCENT / scale else PERCENT
        return ExtractionPolicy.canRun(charging, percent)
    }

    companion object {
        /** Set once by the app; the worker reads it (WorkManager creates workers by itself). */
        @Volatile var current: MemoryExtraction? = null

        const val KEY_CHAT = "chat_id"
        private const val TAG = "HainMemory"
        private const val PREFS = "hain_memory_extraction"
        private const val KEY_DONE = "done_"
        private const val WORK_PREFIX = "memory-extract-"
        private const val IDLE_MINUTES = 2L
        private const val PERCENT = 100
    }
}

/** The worker WorkManager starts for [MemoryExtraction]; waits for the charger when the battery is low. */
class MemoryExtractionWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    @Suppress("ReturnCount")
    override suspend fun doWork(): Result {
        // T-290: the app sets it up on a background thread at start; a worker that woke the app waits a moment
        val extraction =
            withTimeoutOrNull(WAIT_FOR_APP_MS) {
                while (MemoryExtraction.current == null) delay(POLL_MS)
                MemoryExtraction.current
            } ?: return Result.success()
        val chatId = inputData.getLong(MemoryExtraction.KEY_CHAT, -1L)
        if (chatId < 0) return Result.success()
        if (!extraction.run(chatId)) {
            // battery ≤ 30 % and not charging: try again once the phone is on the charger
            val again =
                OneTimeWorkRequestBuilder<MemoryExtractionWorker>()
                    .setInputData(inputData)
                    .setConstraints(Constraints.Builder().setRequiresCharging(true).build())
                    .build()
            WorkManager
                .getInstance(applicationContext)
                .enqueueUniqueWork(CHARGING_PREFIX + chatId, ExistingWorkPolicy.REPLACE, again)
        }
        return Result.success()
    }

    private companion object {
        const val CHARGING_PREFIX = "memory-extract-charging-"
    }

    private companion object {
        const val WAIT_FOR_APP_MS = 5_000L
        const val POLL_MS = 50L
    }
}

/** [ExtractionBrain] on the loaded brain: thinking off, low temperature, the answer forced by the grammar. */
internal class LlmExtractionBrain(
    private val llm: LlmEngine,
) : ExtractionBrain {
    override suspend fun complete(
        system: String,
        user: String,
        grammar: String,
        maxTokens: Int,
    ): String? {
        if (llm.state.value !is EngineState.Ready) return null
        // T-241: the loaded brain's own format (LFM or Qwen)
        val prompt =
            llm.format.promptBuilder().build(system, emptyList(), listOf(ChatMessage(Role.USER, user)), true, false)
        val params = SamplingParams(temperature = 0.1f, repeatPenalty = 1.0f, maxTokens = maxTokens, grammar = grammar)
        val out = StringBuilder()
        var reason = StopReason.ERROR
        llm.generate(prompt, params).collect { event ->
            when (event) {
                is GenerationEvent.Token -> out.append(event.text)
                is GenerationEvent.Done -> reason = event.reason
                is GenerationEvent.Reading -> Unit
            }
        }
        return out.toString().takeIf { reason == StopReason.END || reason == StopReason.MAX_TOKENS }
    }
}
