package app.hain.feature.chat.internal

import app.hain.llm.api.ChatMessage
import app.hain.llm.api.EngineState
import app.hain.llm.api.GenerationEvent
import app.hain.llm.api.LlmEngine
import app.hain.llm.api.Role
import app.hain.llm.api.SamplingParams
import app.hain.llm.api.StopReason
import app.hain.llm.internal.AnswerMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatEngineTest {
    private class FakeLlm(
        private val chunks: List<String>,
        private val end: StopReason = StopReason.END,
    ) : LlmEngine {
        var lastPrompt = ""
        var lastParams: SamplingParams? = null
        override val state: StateFlow<EngineState> = MutableStateFlow(EngineState.Ready("x"))
        override val isAvailable = true

        override suspend fun load(
            path: String,
            contextSize: Int,
        ) = true

        override fun generate(
            prompt: String,
            params: SamplingParams,
        ): Flow<GenerationEvent> {
            lastPrompt = prompt
            lastParams = params
            val events = chunks.map { GenerationEvent.Token(it) } + GenerationEvent.Done(end)
            return flowOf(*events.toTypedArray())
        }

        override fun cancel() = Unit

        override suspend fun unload() = Unit
    }

    @Test
    fun fastModeStreamsTextWithoutThinking() =
        runTest {
            val llm = FakeLlm(listOf("Pa", "ris", "."))
            val updates = ChatEngine(llm, { "SYS" }).respond(user("Hauptstadt?"), AnswerMode.FAST).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals("Paris.", last.output.text)
            assertEquals(null, last.thinkingMillis)
            assertTrue(llm.lastPrompt.startsWith("<|startoftext|><|im_start|>system\nSYS<|im_end|>"))
            assertTrue(llm.lastPrompt.endsWith("<|im_start|>assistant\n<think></think>"))
            assertEquals(1024, llm.lastParams?.maxTokens)
            assertEquals("Paris", (updates[1] as TurnUpdate.Streaming).text)
        }

    @Test
    fun thoroughModeSplitsReasoning() =
        runTest {
            val llm = FakeLlm(listOf("erst denken", "</think>", "Antwort"))
            val updates = ChatEngine(llm, { "S" }).respond(user("warum?"), AnswerMode.THOROUGH).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals("erst denken", last.output.reasoning)
            assertEquals("Antwort", last.output.text)
            assertTrue(llm.lastPrompt.endsWith("<|im_start|>assistant\n<think>"))
            assertEquals(4096, llm.lastParams?.maxTokens)
        }

    @Test
    fun stoppedWhileThinkingKeepsThinkingOutOfTheAnswer() =
        runTest {
            val llm = FakeLlm(listOf("noch am denken"), StopReason.CANCELLED)
            val updates = ChatEngine(llm, { "S" }).respond(user("warum?"), AnswerMode.THOROUGH).toList()
            val last = updates.last() as TurnUpdate.Finished
            assertEquals("", last.output.text)
            assertEquals("noch am denken", last.output.reasoning)
        }

    private fun user(text: String) = listOf(ChatMessage(Role.USER, text))
}
