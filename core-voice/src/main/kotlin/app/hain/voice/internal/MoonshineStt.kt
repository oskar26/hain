package app.hain.voice.internal

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import app.hain.voice.api.AudioMath
import app.hain.voice.api.SpeechToText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.ceil
import kotlin.math.max

/** The numbers of `streaming_config.json` that the runtime needs. */
data class MoonshineConfig(
    val depth: Int,
    val heads: Int,
    val headDim: Int,
    val bosId: Int,
    val eosId: Int,
    val stateShapes: Map<String, LongArray>,
) {
    companion object {
        fun parse(json: String): MoonshineConfig {
            val o = Json.parseToJsonElement(json).jsonObject
            val shapes =
                o.getValue("frontend_state_shapes").jsonObject.mapValues { (_, v) ->
                    v.jsonArray.map { it.jsonPrimitive.long }.toLongArray()
                }
            return MoonshineConfig(
                depth = o.getValue("depth").jsonPrimitive.int,
                heads = o.getValue("nheads").jsonPrimitive.int,
                headDim = o.getValue("head_dim").jsonPrimitive.int,
                bosId = o.getValue("bos_id").jsonPrimitive.int,
                eosId = o.getValue("eos_id").jsonPrimitive.int,
                stateShapes = shapes,
            )
        }
    }
}

/** Upper bound for the text of [seconds] of speech (the model card's 6.5 tokens per second). */
internal fun maxTokens(seconds: Double): Int = max(MIN_TOKENS, ceil(seconds * TOKENS_PER_S).toInt())

private const val TOKENS_PER_S = 6.5
private const val MIN_TOKENS = 8

/**
 * Moonshine Streaming (Small, MIT) on ONNX Runtime (T-140). The pack holds the five graphs of the
 * official streaming runtime: frontend → encoder → adapter → cross K/V → decoder with a self K/V
 * cache. A dictation is short, so the whole utterance runs at once after you stop speaking, and the
 * decoder writes greedily until the end token.
 */
class MoonshineStt(
    dir: File,
    threads: Int = DEFAULT_THREADS,
) : SpeechToText {
    private val env = OrtEnvironment.getEnvironment()
    private val config = MoonshineConfig.parse(File(dir, "streaming_config.json").readText())
    private val tokenizer = MoonshineTokenizer.fromJson(File(dir, "tokenizer.json").readText())
    private val options =
        OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(threads)
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
        }
    private val frontend = open(dir, "frontend.onnx")
    private val encoder = open(dir, "encoder_int8.onnx")
    private val adapter = open(dir, "adapter_int8.onnx")
    private val crossKv = open(dir, "cross_kv_int8.onnx")
    private val decoder = open(dir, "decoder_kv_int8.onnx")

    private fun open(
        dir: File,
        name: String,
    ): OrtSession = env.createSession(File(dir, name).absolutePath, options)

    @Synchronized
    @Suppress("NestedBlockDepth") // every ONNX result is closed right after the next graph used it
    override fun transcribe(audio: FloatArray): String {
        if (audio.size < MIN_SAMPLES) return ""
        val ids = mutableListOf<Int>()
        val limit = maxTokens(audio.size.toDouble() / AudioMath.STT_RATE)
        features(audio).use { fe ->
            encoder.run(mapOf("features" to fe.tensor(0))).use { enc ->
                tensor(longArrayOf(0L), longArrayOf(1)).use { offset ->
                    adapter.run(mapOf("encoded" to enc.tensor(0), "pos_offset" to offset)).use { mem ->
                        crossKv.run(mapOf("memory" to mem.tensor(0))).use { kv ->
                            decode(kv.tensor(0), kv.tensor(1), limit, ids)
                        }
                    }
                }
            }
        }
        return tokenizer.decode(ids).trim()
    }

    private fun features(audio: FloatArray): OrtSession.Result {
        val inputs = mutableMapOf<String, OnnxTensor>()
        try {
            inputs["audio_chunk"] = tensor(audio, longArrayOf(1, audio.size.toLong()))
            for ((name, shape) in config.stateShapes) {
                val count = shape.fold(1L) { a, b -> a * b }.toInt()
                // sample_len and frame_count are int64 counters, the buffers are floats; all start at zero
                inputs[name] =
                    if (name.endsWith("_len") || name.endsWith("_count")) {
                        tensor(LongArray(count), shape)
                    } else {
                        tensor(FloatArray(count), shape)
                    }
            }
            return frontend.run(inputs)
        } finally {
            inputs.values.forEach { it.close() }
        }
    }

    private fun decode(
        kCross: OnnxTensor,
        vCross: OnnxTensor,
        limit: Int,
        out: MutableList<Int>,
    ) {
        val empty = longArrayOf(config.depth.toLong(), 1, config.heads.toLong(), 0, config.headDim.toLong())
        var kSelf: OnnxTensor = tensor(FloatArray(0), empty)
        var vSelf: OnnxTensor = tensor(FloatArray(0), empty)
        var last: OrtSession.Result? = null
        var token = config.bosId
        try {
            repeat(limit) {
                val next =
                    tensor(longArrayOf(token.toLong()), longArrayOf(1, 1)).use { tok ->
                        decoder.run(
                            mapOf(
                                "token" to tok,
                                "k_self" to kSelf,
                                "v_self" to vSelf,
                                "out_k_cross" to kCross,
                                "out_v_cross" to vCross,
                            ),
                        )
                    }
                if (last == null) {
                    kSelf.close()
                    vSelf.close()
                }
                last?.close()
                last = next
                kSelf = next.get("out_k_self").get() as OnnxTensor
                vSelf = next.get("out_v_self").get() as OnnxTensor
                token = argmaxLast(next.get("logits").get() as OnnxTensor)
                if (token == config.eosId) return
                out += token
            }
        } finally {
            if (last == null) {
                kSelf.close()
                vSelf.close()
            }
            last?.close()
        }
    }

    private fun argmaxLast(logits: OnnxTensor): Int {
        val shape = logits.info.shape
        val vocab = shape.last().toInt()
        val buffer = logits.floatBuffer
        val start = buffer.limit() - vocab
        var best = 0
        var bestValue = Float.NEGATIVE_INFINITY
        for (i in 0 until vocab) {
            val v = buffer.get(start + i)
            if (v > bestValue) {
                bestValue = v
                best = i
            }
        }
        return best
    }

    private fun tensor(
        data: FloatArray,
        shape: LongArray,
    ): OnnxTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(data), shape)

    private fun tensor(
        data: LongArray,
        shape: LongArray,
    ): OnnxTensor = OnnxTensor.createTensor(env, LongBuffer.wrap(data), shape)

    private fun OrtSession.Result.tensor(index: Int): OnnxTensor = get(index) as OnnxTensor

    override fun close() {
        listOf(frontend, encoder, adapter, crossKv, decoder).forEach { runCatching { it.close() } }
        options.close()
    }

    companion object {
        /** Two big cores are the sweet spot on phones; more threads mostly fight the UI. */
        const val DEFAULT_THREADS = 2
        private const val MIN_SAMPLES = 1_280
    }
}
