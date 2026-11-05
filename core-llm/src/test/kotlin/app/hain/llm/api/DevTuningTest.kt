package app.hain.llm.api

import app.hain.common.api.DevSettings
import app.hain.common.api.ThinkingOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DevTuningTest {
    private val on = DevSettings(enabled = true)

    @Test
    fun `developer mode off keeps Hain's defaults whatever is saved`() {
        val saved = DevSettings(enabled = false, temperature = 1.5f, topK = 5, contextSize = 1024, systemPrompt = "x")
        assertEquals(SamplingParams.CHAT.copy(maxTokens = 1024), DevTuning.sampling(saved, false, 1024, 4096))
        assertEquals(SamplingParams.CHAT.copy(maxTokens = 4096), DevTuning.sampling(saved, true, 1024, 4096))
        assertEquals(CONTEXT_SIZE, DevTuning.contextSize(saved))
        assertEquals("hain", DevTuning.systemPrompt(saved, "hain"))
        assertTrue(DevTuning.thinking(saved.copy(thinking = ThinkingOverride.OFF), decided = true))
        assertNull(DevTuning.ownModel(saved.copy(customModelPath = "/m.gguf")) { true })
    }

    @Test
    fun `developer mode off uses the brain's own sampling`() {
        // T-241: the Großes Gehirn (Jan-v3.5-4B, Qwen3 format) samples as on its model card
        val qwen = DevTuning.sampling(DevSettings(), false, 1024, 4096, BrainFormat.QWEN.chatSampling)
        assertEquals(BrainFormat.QWEN.chatSampling.copy(maxTokens = 1024), qwen)
        assertEquals(0.8f, qwen.topP) // Jan-v3.5-4B model card
        // developer mode still wins over the brain's defaults
        assertEquals(1.5f, DevTuning.sampling(on.copy(temperature = 1.5f), false, 1024, 4096, qwen).temperature)
    }

    @Test
    fun `defaults of developer mode equal the app defaults`() {
        assertEquals(SamplingParams.CHAT.copy(maxTokens = 1024), DevTuning.sampling(on, false, 1024, 4096))
        assertEquals(SamplingParams.CHAT.copy(maxTokens = 4096), DevTuning.sampling(on, true, 1024, 4096))
        assertEquals(CONTEXT_SIZE, DevTuning.contextSize(on))
    }

    @Test
    fun `values are mapped and clamped`() {
        val dev =
            on.copy(
                temperature = 5f,
                topK = 1000,
                topP = 1.5f,
                minP = -1f,
                repeatPenalty = 0.5f,
                maxTokens = 10,
                thinkingBudget = 99_999,
                seed = -7,
            )
        val p = DevTuning.sampling(dev, thinking = false, defaultMax = 1, defaultMaxThinking = 1)
        assertEquals(2f, p.temperature)
        assertEquals(200, p.topK)
        assertEquals(1f, p.topP)
        assertEquals(0f, p.minP)
        assertEquals(1f, p.repeatPenalty)
        assertEquals(64, p.maxTokens)
        assertEquals(-1, p.seed)
        assertEquals(8192, DevTuning.sampling(dev, thinking = true, defaultMax = 1, defaultMaxThinking = 1).maxTokens)
        assertEquals(512, DevTuning.contextSize(on.copy(contextSize = 100)))
        assertEquals(8192, DevTuning.contextSize(on.copy(contextSize = 32_000)))
        assertEquals(2048, DevTuning.contextSize(on.copy(contextSize = 2048)))
    }

    @Test
    fun `thinking override, own prompt and own model`() {
        assertTrue(DevTuning.thinking(on.copy(thinking = ThinkingOverride.ON), decided = false))
        assertFalse(DevTuning.thinking(on.copy(thinking = ThinkingOverride.OFF), decided = true))
        assertTrue(DevTuning.thinking(on, decided = true))
        assertEquals("Sei kurz.", DevTuning.systemPrompt(on.copy(systemPrompt = "  Sei kurz. "), "hain"))
        assertEquals("hain", DevTuning.systemPrompt(on.copy(systemPrompt = "   "), "hain"))
        assertEquals("/m.gguf", DevTuning.ownModel(on.copy(customModelPath = "/m.gguf")) { true })
        assertNull(DevTuning.ownModel(on.copy(customModelPath = "/m.gguf")) { false })
    }

    @Test
    fun `memory warning only for big context on small phones`() {
        assertTrue(DevTuning.contextWarning(8192, 4))
        assertFalse(DevTuning.contextWarning(4096, 4))
        assertFalse(DevTuning.contextWarning(8192, 12))
    }

    @Test
    fun `debug line names model, settings and speed`() {
        val stats = GenerationStats(500, 300, 900, 120, 6000, 850)
        val line = DevTuning.debugLine("LFM3.gguf", on.copy(seed = 3), stats)
        assertTrue(line, line.startsWith("model=LFM3.gguf ctx=8192 temp=0.1 top_k=50"))
        assertTrue(line, line.contains("seed=3"))
        assertTrue(line, line.contains("ttft_ms=850 gen=120 tps=20.0"))
    }
}
