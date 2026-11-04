package app.hain.llm.api

import app.hain.common.api.DevSettings
import app.hain.common.api.ThinkingOverride

/**
 * Turns the developer settings (T-211) into what the engine uses for the next message. Pure, so it is
 * unit-tested. With developer mode off everything is Hain's default; out-of-range values are clamped.
 */
object DevTuning {
    const val MAX_TEMPERATURE = 2f
    const val MAX_TOP_K = 200
    const val MAX_REPEAT = 2f
    const val MIN_TOKENS = 64
    const val MAX_TOKENS = 8192

    /** Context above this needs a lot of memory on phones with little RAM. */
    const val BIG_CONTEXT = 4096

    /** Sampling for one answer; [thinking] answers get the thinking budget as their token limit. */
    fun sampling(
        dev: DevSettings,
        thinking: Boolean,
        defaultMax: Int,
        defaultMaxThinking: Int,
    ): SamplingParams {
        if (!dev.enabled) return SamplingParams.CHAT.copy(maxTokens = if (thinking) defaultMaxThinking else defaultMax)
        return SamplingParams(
            temperature = dev.temperature.coerceIn(0f, MAX_TEMPERATURE),
            topK = dev.topK.coerceIn(0, MAX_TOP_K),
            topP = dev.topP.coerceIn(0f, 1f),
            minP = dev.minP.coerceIn(0f, 1f),
            repeatPenalty = dev.repeatPenalty.coerceIn(1f, MAX_REPEAT),
            maxTokens = (if (thinking) dev.thinkingBudget else dev.maxTokens).coerceIn(MIN_TOKENS, MAX_TOKENS),
            seed = dev.seed.coerceAtLeast(-1),
        )
    }

    /** Whether the brain thinks: Hain's own choice [decided], unless developer mode forces it. */
    fun thinking(
        dev: DevSettings,
        decided: Boolean,
    ): Boolean =
        if (!dev.enabled) {
            decided
        } else {
            when (dev.thinking) {
                ThinkingOverride.AUTO -> decided
                ThinkingOverride.ON -> true
                ThinkingOverride.OFF -> false
            }
        }

    /** Context size the brain is loaded with; a change loads the model again. */
    fun contextSize(dev: DevSettings): Int =
        if (!dev.enabled) CONTEXT_SIZE else dev.contextSize.coerceIn(DevSettings.MIN_CONTEXT, DevSettings.MAX_CONTEXT)

    /** The developer's own system prompt when set, else Hain's. */
    fun systemPrompt(
        dev: DevSettings,
        default: String,
    ): String = if (dev.enabled && dev.systemPrompt.isNotBlank()) dev.systemPrompt.trim() else default

    /** The own GGUF file when developer mode uses one that exists, else `null` (= Hain's brain). */
    fun ownModel(
        dev: DevSettings,
        exists: (String) -> Boolean,
    ): String? = dev.customModelPath.takeIf { dev.enabled && it.isNotBlank() && exists(it) }

    /** Warn about memory: big context on a phone with up to 6 GB RAM. */
    fun contextWarning(
        contextSize: Int,
        ramGb: Int,
    ): Boolean = contextSize > BIG_CONTEXT && ramGb in 1..RAM_TIGHT_GB

    /** One line for "copy debug info": model, settings and speed of an answer. */
    fun debugLine(
        model: String,
        dev: DevSettings,
        stats: GenerationStats?,
    ): String =
        buildString {
            append("model=").append(model)
            append(" ctx=").append(contextSize(dev))
            val p = sampling(dev, thinking = false, defaultMax = SamplingParams.CHAT.maxTokens, defaultMaxThinking = 0)
            append(" temp=").append(p.temperature)
            append(" top_k=").append(p.topK)
            append(" top_p=").append(p.topP)
            append(" min_p=").append(p.minP)
            append(" repeat=").append(p.repeatPenalty)
            append(" max=").append(p.maxTokens)
            append(" seed=").append(p.seed)
            append(" thinking=").append(if (dev.enabled) dev.thinking.name.lowercase() else "auto")
            if (stats != null) {
                append(" | prompt=").append(stats.promptTokens)
                append(" reused=").append(stats.reusedTokens)
                append(" prefill_ms=").append(stats.prefillMs)
                append(" ttft_ms=").append(stats.firstTokenMs)
                append(" gen=").append(stats.genTokens)
                append(" tps=").append("%.1f".format(java.util.Locale.ROOT, stats.tokensPerSecond))
            }
        }

    private const val RAM_TIGHT_GB = 6
}
