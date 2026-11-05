package app.hain.llm.api

import app.hain.llm.internal.LfmPromptBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** T-260: LFM3-Omni 2B is told apart from LFM3 1.7B by its name; same template, no thinking. */
class BrainFormatTest {
    @Test
    fun omniByName() {
        assertEquals(BrainFormat.LFM_OMNI, BrainFormat.of("lfm3", "LFM3 Omni 2B"))
        assertEquals(BrainFormat.LFM, BrainFormat.of("lfm3", "LFM3 1.7B QAD"))
        assertEquals(BrainFormat.LFM, BrainFormat.of("lfm3", null))
        assertEquals(BrainFormat.QWEN, BrainFormat.of("qwen3", "Omni something"))
    }

    @Test
    fun omniNeverThinksAndHasNoRepeatPenalty() {
        assertFalse(BrainFormat.LFM_OMNI.mayThink)
        assertEquals(1.0f, BrainFormat.LFM_OMNI.chatSampling.repeatPenalty)
        assertTrue(BrainFormat.LFM_OMNI.chatSampling.temperature <= 0.1f)
        assertTrue(BrainFormat.LFM_OMNI.promptBuilder() is LfmPromptBuilder)
    }
}
