// Hain JNI bridge to llama.cpp (LLM.md §1, T-030/T-033). Kept deliberately small.
#include <jni.h>
#include <android/log.h>

#include <algorithm>
#include <atomic>
#include <string>
#include <vector>

#include "ggml-backend.h"
#include "llama.h"

#define TAG "HainLlm"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

namespace {

constexpr int STOP_EOS = 0;
constexpr int STOP_MAX_TOKENS = 1;
constexpr int STOP_CANCELLED = 2;
constexpr int STOP_ERROR = 3;
constexpr int STOP_CONTEXT_FULL = 4;

struct Session {
    llama_model *model = nullptr;
    llama_context *ctx = nullptr;
    const llama_vocab *vocab = nullptr;
    std::vector<llama_token> cached;  // tokens currently in the KV/recurrent memory (seq 0)
    bool kvReuse = true;
    std::atomic<bool> cancel{false};
};

std::atomic<bool> g_backendReady{false};

Session *session(jlong handle) { return reinterpret_cast<Session *>(handle); }

std::vector<llama_token> tokenize(const llama_vocab *vocab, const std::string &text, bool addSpecial) {
    int n = -llama_tokenize(vocab, text.c_str(), (int32_t) text.size(), nullptr, 0, addSpecial, true);
    std::vector<llama_token> out(std::max(n, 0));
    if (n > 0) {
        llama_tokenize(vocab, text.c_str(), (int32_t) text.size(), out.data(), n, addSpecial, true);
    }
    return out;
}

std::string piece(const llama_vocab *vocab, llama_token token) {
    char buf[256];
    int n = llama_token_to_piece(vocab, token, buf, sizeof(buf), 0, false);
    if (n < 0) {
        std::string big(-n, '\0');
        n = llama_token_to_piece(vocab, token, big.data(), (int32_t) big.size(), 0, false);
        return n > 0 ? big.substr(0, n) : std::string();
    }
    return std::string(buf, n);
}

// Length of the longest prefix of `s` that ends on a complete UTF-8 sequence.
size_t completeUtf8(const std::string &s) {
    size_t i = s.size();
    size_t back = 0;
    while (i > 0 && back < 4) {
        auto c = static_cast<unsigned char>(s[i - 1]);
        if ((c & 0xC0) != 0x80) {  // lead byte or ASCII
            size_t need = c < 0x80 ? 1 : (c >> 5) == 0x6 ? 2 : (c >> 4) == 0xE ? 3 : (c >> 3) == 0x1E ? 4 : 1;
            return (back + 1 >= need) ? s.size() : i - 1;
        }
        --i;
        ++back;
    }
    return s.size();
}

bool decodeTokens(Session *s, const std::vector<llama_token> &tokens, size_t from) {
    const size_t nBatch = llama_n_batch(s->ctx);
    for (size_t i = from; i < tokens.size(); i += nBatch) {
        size_t n = std::min(nBatch, tokens.size() - i);
        llama_batch batch = llama_batch_get_one(const_cast<llama_token *>(tokens.data() + i), (int32_t) n);
        if (llama_decode(s->ctx, batch) != 0) return false;
        s->cached.insert(s->cached.end(), tokens.begin() + (long) i, tokens.begin() + (long) (i + n));
    }
    return true;
}

// Brings the memory to `prompt`, reusing the shared prefix when possible (T-033).
bool ingestPrompt(Session *s, const std::vector<llama_token> &prompt) {
    llama_memory_t mem = llama_get_memory(s->ctx);
    size_t keep = 0;
    if (s->kvReuse) {
        while (keep < s->cached.size() && keep < prompt.size() && s->cached[keep] == prompt[keep]) ++keep;
        if (keep == prompt.size() && keep > 0) --keep;  // always decode ≥ 1 token for fresh logits
        // Hybrid/recurrent models (LFM) cannot drop a partial tail: fall back to a full re-decode.
        if (keep < s->cached.size() && !llama_memory_seq_rm(mem, 0, (llama_pos) keep, -1)) keep = 0;
    }
    if (keep == 0) llama_memory_clear(mem, true);
    s->cached.resize(keep);
    return decodeTokens(s, prompt, keep);
}

llama_sampler *makeSampler(const llama_vocab *vocab, const float *p, jsize n, const char *grammar) {
    // params = [temperature, topK, topP, minP, repeatPenalty, repeatLastN, maxTokens, seed]
    auto chain = llama_sampler_chain_init(llama_sampler_chain_default_params());
    if (grammar != nullptr && grammar[0] != '\0') {
        llama_sampler_chain_add(chain, llama_sampler_init_grammar(vocab, grammar, "root"));
    }
    const float temp = n > 0 ? p[0] : 0.1f;
    const int topK = n > 1 ? (int) p[1] : 50;
    const float topP = n > 2 ? p[2] : 1.0f;
    const float minP = n > 3 ? p[3] : 0.0f;
    const float repeat = n > 4 ? p[4] : 1.0f;
    const int lastN = n > 5 ? (int) p[5] : 64;
    const auto seed = n > 7 ? (uint32_t) p[7] : LLAMA_DEFAULT_SEED;
    if (repeat != 1.0f) {
        llama_sampler_chain_add(chain,
            llama_sampler_init_penalties(llama_vocab_n_tokens(vocab), lastN, repeat, 0.0f, 0.0f));
    }
    if (temp <= 0.0f) {
        llama_sampler_chain_add(chain, llama_sampler_init_greedy());
        return chain;
    }
    if (topK > 0) llama_sampler_chain_add(chain, llama_sampler_init_top_k(topK));
    if (topP < 1.0f) llama_sampler_chain_add(chain, llama_sampler_init_top_p(topP, 1));
    if (minP > 0.0f) llama_sampler_chain_add(chain, llama_sampler_init_min_p(minP, 1));
    llama_sampler_chain_add(chain, llama_sampler_init_temp(temp));
    llama_sampler_chain_add(chain, llama_sampler_init_dist(seed));
    return chain;
}

}  // namespace

extern "C" {

JNIEXPORT void JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeInit(JNIEnv *env, jobject, jstring nativeLibDir) {
    if (g_backendReady.exchange(true)) return;
    const char *dir = env->GetStringUTFChars(nativeLibDir, nullptr);
    ggml_backend_load_all_from_path(dir);
    env->ReleaseStringUTFChars(nativeLibDir, dir);
    llama_backend_init();
    LOGI("backends loaded");
}

JNIEXPORT jlong JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeLoad(JNIEnv *env, jobject, jstring path, jint nCtx, jint nThreads,
                                                jint nGpuLayers) {
    const char *cpath = env->GetStringUTFChars(path, nullptr);
    auto mparams = llama_model_default_params();
    mparams.n_gpu_layers = nGpuLayers;
    llama_model *model = llama_model_load_from_file(cpath, mparams);
    env->ReleaseStringUTFChars(path, cpath);
    if (model == nullptr) {
        LOGE("model load failed");
        return 0;
    }
    auto cparams = llama_context_default_params();
    cparams.n_ctx = (uint32_t) nCtx;
    cparams.n_batch = 512;
    cparams.n_ubatch = 512;
    cparams.n_threads = nThreads;
    cparams.n_threads_batch = nThreads;
    llama_context *ctx = llama_init_from_model(model, cparams);
    if (ctx == nullptr) {
        LOGE("context init failed");
        llama_model_free(model);
        return 0;
    }
    auto *s = new Session();
    s->model = model;
    s->ctx = ctx;
    s->vocab = llama_model_get_vocab(model);
    return reinterpret_cast<jlong>(s);
}

JNIEXPORT void JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeFree(JNIEnv *, jobject, jlong handle) {
    Session *s = session(handle);
    if (s == nullptr) return;
    llama_free(s->ctx);
    llama_model_free(s->model);
    delete s;
}

JNIEXPORT jintArray JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeTokenize(JNIEnv *env, jobject, jlong handle, jstring text,
                                                    jboolean addSpecial) {
    Session *s = session(handle);
    const char *ctext = env->GetStringUTFChars(text, nullptr);
    auto tokens = tokenize(s->vocab, ctext, addSpecial);
    env->ReleaseStringUTFChars(text, ctext);
    jintArray out = env->NewIntArray((jsize) tokens.size());
    env->SetIntArrayRegion(out, 0, (jsize) tokens.size(), reinterpret_cast<const jint *>(tokens.data()));
    return out;
}

JNIEXPORT void JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeCancel(JNIEnv *, jobject, jlong handle) {
    Session *s = session(handle);
    if (s != nullptr) s->cancel = true;
}

JNIEXPORT void JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeKvReuse(JNIEnv *, jobject, jlong handle, jboolean enabled) {
    Session *s = session(handle);
    if (s != nullptr) s->kvReuse = enabled;
}

// Blocking loop on the caller thread. The prompt (UTF-8 bytes) is already fully templated, so it is
// tokenized with special-token parsing and without adding BOS. Text goes to cb.onToken(byte[]) only in
// complete UTF-8 sequences; onToken returning false stops generation.
JNIEXPORT jint JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeGenerate(JNIEnv *env, jobject, jlong handle, jbyteArray promptUtf8,
                                                    jfloatArray params, jstring grammar, jobject cb) {
    Session *s = session(handle);
    if (s == nullptr) return STOP_ERROR;
    s->cancel = false;

    jsize promptLen = env->GetArrayLength(promptUtf8);
    std::string prompt(promptLen, '\0');
    env->GetByteArrayRegion(promptUtf8, 0, promptLen, reinterpret_cast<jbyte *>(prompt.data()));

    jsize nParams = env->GetArrayLength(params);
    std::vector<float> p(nParams);
    env->GetFloatArrayRegion(params, 0, nParams, p.data());
    const int maxTokens = nParams > 6 ? (int) p[6] : 1024;

    const char *cgrammar = grammar != nullptr ? env->GetStringUTFChars(grammar, nullptr) : nullptr;
    llama_sampler *smpl = makeSampler(s->vocab, p.data(), nParams, cgrammar);
    if (cgrammar != nullptr) env->ReleaseStringUTFChars(grammar, cgrammar);

    jclass cbClass = env->GetObjectClass(cb);
    jmethodID onToken = env->GetMethodID(cbClass, "onToken", "([B)Z");

    auto tokens = tokenize(s->vocab, prompt, false);
    const auto nCtx = (size_t) llama_n_ctx(s->ctx);
    if (tokens.empty() || tokens.size() + 8 >= nCtx) {
        llama_sampler_free(smpl);
        return tokens.empty() ? STOP_ERROR : STOP_CONTEXT_FULL;
    }
    if (!ingestPrompt(s, tokens)) {
        llama_sampler_free(smpl);
        s->cached.clear();
        llama_memory_clear(llama_get_memory(s->ctx), true);
        return STOP_ERROR;
    }

    int reason = STOP_MAX_TOKENS;
    std::string pending;
    auto flush = [&](bool all) -> bool {
        size_t n = all ? pending.size() : completeUtf8(pending);
        if (n == 0) return true;
        jbyteArray bytes = env->NewByteArray((jsize) n);
        env->SetByteArrayRegion(bytes, 0, (jsize) n, reinterpret_cast<const jbyte *>(pending.data()));
        jboolean keepGoing = env->CallBooleanMethod(cb, onToken, bytes);
        env->DeleteLocalRef(bytes);
        pending.erase(0, n);
        return keepGoing == JNI_TRUE && !env->ExceptionCheck();
    };

    for (int i = 0; i < maxTokens; ++i) {
        if (s->cancel) { reason = STOP_CANCELLED; break; }
        if (s->cached.size() + 1 >= nCtx) { reason = STOP_CONTEXT_FULL; break; }
        llama_token tok = llama_sampler_sample(smpl, s->ctx, -1);
        if (llama_vocab_is_eog(s->vocab, tok)) { reason = STOP_EOS; break; }
        pending += piece(s->vocab, tok);
        if (!flush(false)) { reason = STOP_CANCELLED; break; }
        std::vector<llama_token> one{tok};
        if (!decodeTokens(s, one, 0)) { reason = STOP_ERROR; break; }
    }
    if (reason != STOP_ERROR) flush(true);
    llama_sampler_free(smpl);
    return reason;
}

}  // extern "C"
