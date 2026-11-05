package app.hain.llm.api

import app.hain.llm.internal.LfmPromptBuilder
import app.hain.llm.internal.QwenPromptBuilder

/**
 * T-241: chat format of the loaded brain, read from its GGUF (`general.architecture`). Decides the
 * prompt template, the sampling for chat answers and whether the brain may think.
 *
 * - [LFM]: LFM3 1.7B, the standard brain for every phone (own template, thinking in "Gründlich").
 * - [QWEN]: Qwen3 architecture, here the "Großes Gehirn" Jan-v3.5-4B (ADR-24). A non-thinking model
 *   (its template has no think block), sampling as on its model card (temperature 0.7, top-p 0.8,
 *   top-k 20; min-p 0 and no repeat penalty as in the comparison test).
 */
enum class BrainFormat(
    /** Sampling for chat answers before developer settings. */
    val chatSampling: SamplingParams,
    /** `false` = the brain never thinks, whatever the answer mode says. */
    val mayThink: Boolean,
) {
    LFM(SamplingParams.CHAT, mayThink = true),
    QWEN(
        SamplingParams(temperature = 0.7f, topK = 20, topP = 0.8f, minP = 0f, repeatPenalty = 1.0f),
        mayThink = false,
    ),
    ;

    /** Prompt builder for this format. */
    fun promptBuilder(): PromptBuilder =
        when (this) {
            LFM -> LfmPromptBuilder()
            QWEN -> QwenPromptBuilder()
        }

    companion object {
        /**
         * Qwen3 architectures (ChatML, tool calls as JSON in `<tool_call>`). Qwen3.5 (`qwen35`) writes
         * its calls differently and is not offered; such a file would get the LFM prompt.
         */
        private val QWEN_ARCHS = setOf("qwen3", "qwen3moe")

        /** Format for a GGUF `general.architecture`; unknown or `null` = [LFM] (Hain's default). */
        fun ofArchitecture(arch: String?): BrainFormat = if (arch?.lowercase() in QWEN_ARCHS) QWEN else LFM
    }
}
