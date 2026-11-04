package app.hain.voice.internal

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MoonshineTokenizerTest {
    // a tiny tokenizer.json in the shape Moonshine ships (BPE, ▁ = space, <0xNN> byte fallback)
    private val json =
        """
        {"added_tokens":[{"id":0,"content":"<unk>","special":true},{"id":1,"content":"<s>","special":true},
          {"id":2,"content":"</s>","special":true}],
         "model":{"type":"BPE","byte_fallback":true,"vocab":{"<unk>":0,"<s>":1,"</s>":2,
           "<0xC3>":3,"<0xBC>":4,"▁Hallo":5,",":6,"▁ich":7,"▁bin":8,"▁P":9,"im":10,".":11,"▁gr":12,"n":13}}}
        """.trimIndent()

    private val tokenizer = MoonshineTokenizer.fromJson(json)

    @Test
    fun piecesBecomeWordsWithoutTheLeadingSpace() {
        assertEquals("Hallo, ich bin Pim.", tokenizer.decode(listOf(5, 6, 7, 8, 9, 10, 11)))
    }

    @Test
    fun byteFallbackBuildsUmlauts() {
        assertEquals("grün", tokenizer.decode(listOf(12, 3, 4, 13)))
    }

    @Test
    fun specialTokensAndUnknownIdsAreSkipped() {
        assertEquals("Hallo", tokenizer.decode(listOf(1, 5, 2, 0, 999)))
        assertEquals(14, tokenizer.size)
    }

    @Test
    fun emptyIsEmpty() {
        assertEquals("", tokenizer.decode(emptyList()))
    }

    @Test
    fun configReadsTheStreamingNumbers() {
        val config =
            MoonshineConfig.parse(
                """
                {"encoder_dim":620,"decoder_dim":512,"depth":10,"nheads":8,"head_dim":64,"vocab_size":12288,
                 "bos_id":1,"eos_id":2,"frame_len":80,"total_lookahead":16,
                 "frontend_state_shapes":{"sample_buffer":[1,79],"sample_len":[1],"conv1_buffer":[1,620,4],
                   "conv2_buffer":[1,1240,4],"frame_count":[1]}}
                """.trimIndent(),
            )
        assertEquals(10, config.depth)
        assertEquals(8, config.heads)
        assertEquals(64, config.headDim)
        assertEquals(1, config.bosId)
        assertEquals(2, config.eosId)
        assertEquals(5, config.stateShapes.size)
        assertArrayEquals(longArrayOf(1, 620, 4), config.stateShapes.getValue("conv1_buffer"))
    }

    @Test
    fun tokenLimitFollowsTheSpeechLength() {
        assertEquals(8, maxTokens(0.2))
        assertEquals(33, maxTokens(5.0))
        assertEquals(390, maxTokens(60.0))
    }
}
