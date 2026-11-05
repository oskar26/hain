package app.hain.llm.api

import app.hain.llm.internal.LfmPromptBuilder
import app.hain.llm.internal.QwenPromptBuilder

/**
 * T-241: chat format of the loaded brain, read from its GGUF (`general.architecture`). Decides the
 * prompt template, the sampling for chat answers and whether the brain may think.
 *
 * - [LFM]: LFM3 1.7B, the standard brain for every phone (own template, thinking in "Gründlich").
 * - [QWEN]: Qwen3.5 family, here the "Großes Gehirn" Underdog Poodle 4B. Thinking stays off (on a
 *   phone it would take minutes; the host test ran without it), sampling as on its model card
 *   (temperature 0.6, top-p 0.95, top-k 20, no repeat penalty).
 */
enum class BrainFormat(
    /** Sampling for chat answers before developer settings. */
    val chatSampling: SamplingParams,
    /** `false` = the brain never thinks, whatever the answer mode says. */
    val mayThink: Boolean,
) {
    LFM(SamplingParams.CHAT, mayThink = true),
    QWEN(
        SamplingParams(temperature = 0.6f, topK = 20, topP = 0.95f, minP = 0f, repeatPenalty = 1.0f),
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
        /** Architectures of the Qwen3 / Qwen3.5 families (ChatML with `<tool_call>` XML). */
        private val QWEN_ARCHS = setOf("qwen35", "qwen35moe", "qwen3", "qwen3moe", "qwen3next")

        /** Format for a GGUF `general.architecture`; unknown or `null` = [LFM] (Hain's default). */
        fun ofArchitecture(arch: String?): BrainFormat = if (arch?.lowercase() in QWEN_ARCHS) QWEN else LFM
    }
}
