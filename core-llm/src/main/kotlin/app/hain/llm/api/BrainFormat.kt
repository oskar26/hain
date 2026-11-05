package app.hain.llm.api

import app.hain.llm.internal.LfmPromptBuilder
import app.hain.llm.internal.QwenPromptBuilder

/**
 * T-241: chat format of the loaded brain, read from its GGUF (`general.architecture`). Decides the
 * prompt template, the sampling for chat answers and whether the brain may think.
 *
 * - [LFM]: LFM3 1.7B, the former standard brain (own template, thinking in "Gründlich").
 * - [LFM_OMNI]: LFM3-Omni 2B (T-260), the standard brain for every phone since 0.1.10.7. Its text part is
 *   LFM3 1.7B (same architecture `lfm3`, same tokenizer, same chat template for text), so it uses the
 *   LFM prompt builder; but it is a non-reasoning model (the template opens every answer with an empty
 *   `<think></think>`) and its model card recommends greedy decoding for text, without repeat penalty.
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

    /**
     * T-260: near-greedy (temperature 0.1, top-k 50) instead of pure greedy, so "Nochmal" can still
     * give another wording; no repeat penalty (model card). Cheapest sampler chain of all formats.
     */
    LFM_OMNI(SamplingParams(temperature = 0.1f, topK = 50, repeatPenalty = 1.0f), mayThink = false),
    QWEN(
        SamplingParams(temperature = 0.7f, topK = 20, topP = 0.8f, minP = 0f, repeatPenalty = 1.0f),
        mayThink = false,
    ),
    ;

    /** Prompt builder for this format. */
    fun promptBuilder(): PromptBuilder =
        when (this) {
            LFM, LFM_OMNI -> LfmPromptBuilder()
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

        /**
         * T-260: format from architecture and model name (`general.name`, e.g. "LFM3 Omni 2B"): an LFM
         * file whose name says Omni is [LFM_OMNI]; everything else as [ofArchitecture].
         */
        fun of(
            arch: String?,
            name: String?,
        ): BrainFormat {
            val format = ofArchitecture(arch)
            val omni = name?.contains("omni", ignoreCase = true) == true
            return if (format == LFM && omni) LFM_OMNI else format
        }
    }
}
