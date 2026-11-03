// Hain JNI bridge to llama.cpp (LLM.md §1, T-030/T-033). Kept deliberately small: the logic lives in
// hain_session.h so it can be tested on a computer without Android.
#include <jni.h>

#include <atomic>
#include <string>
#include <vector>

#include "ggml-backend.h"
#include "hain_session.h"
#include "llama.h"

using hain::Session;

namespace {

std::atomic<bool> g_backendReady{false};

Session *session(jlong handle) { return reinterpret_cast<Session *>(handle); }

std::string bytesOf(JNIEnv *env, jbyteArray array) {
    jsize len = env->GetArrayLength(array);
    std::string out(len, '\0');
    env->GetByteArrayRegion(array, 0, len, reinterpret_cast<jbyte *>(out.data()));
    return out;
}

std::vector<int> intsOf(JNIEnv *env, jintArray array) {
    if (array == nullptr) return {};
    jsize len = env->GetArrayLength(array);
    std::vector<int> out(len);
    env->GetIntArrayRegion(array, 0, len, reinterpret_cast<jint *>(out.data()));
    return out;
}

struct LoadProgress {
    JNIEnv *env;
    jobject cb;
    jmethodID method;
};

bool onLoadProgress(float progress, void *data) {
    auto *p = static_cast<LoadProgress *>(data);
    if (p->cb == nullptr) return true;
    jboolean keepGoing = p->env->CallBooleanMethod(p->cb, p->method, progress);
    return keepGoing == JNI_TRUE && !p->env->ExceptionCheck();
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
    HAIN_LOGI("backends loaded");
}

// Loads the model with mmap (weights are paged in from the file, not copied); `progress` gets
// onProgress(float 0..1) while the file is read.
JNIEXPORT jlong JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeLoad(JNIEnv *env, jobject, jstring path, jint nCtx, jint nThreads,
                                                jint nGpuLayers, jobject progress) {
    const double t0 = hain::nowMs();
    const char *cpath = env->GetStringUTFChars(path, nullptr);
    LoadProgress lp{env, progress, nullptr};
    if (progress != nullptr) lp.method = env->GetMethodID(env->GetObjectClass(progress), "onProgress", "(F)Z");
    auto mparams = llama_model_default_params();
    mparams.n_gpu_layers = nGpuLayers;
    mparams.use_mmap = true;
    mparams.progress_callback = onLoadProgress;
    mparams.progress_callback_user_data = &lp;
    llama_model *model = llama_model_load_from_file(cpath, mparams);
    env->ReleaseStringUTFChars(path, cpath);
    if (model == nullptr) {
        HAIN_LOGE("model load failed");
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
        HAIN_LOGE("context init failed");
        llama_model_free(model);
        return 0;
    }
    auto *s = new Session();
    s->model = model;
    s->ctx = ctx;
    s->vocab = llama_model_get_vocab(model);
    hain::attachAbort(s);
    s->stats.loadMs = hain::nowMs() - t0;
    HAIN_LOGI("model loaded in %.0f ms (%d threads, ctx %d)", s->stats.loadMs, nThreads, nCtx);
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
    auto tokens = hain::tokenize(s->vocab, ctext, addSpecial);
    env->ReleaseStringUTFChars(text, ctext);
    jintArray out = env->NewIntArray((jsize) tokens.size());
    env->SetIntArrayRegion(out, 0, (jsize) tokens.size(), reinterpret_cast<const jint *>(tokens.data()));
    return out;
}

// Sets (true) or clears (false) the stop flag. A set flag also aborts a running llama_decode.
JNIEXPORT void JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeSetCancel(JNIEnv *, jobject, jlong handle, jboolean cancel) {
    Session *s = session(handle);
    if (s != nullptr) s->cancel = cancel == JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeKvReuse(JNIEnv *, jobject, jlong handle, jboolean enabled) {
    Session *s = session(handle);
    if (s != nullptr) s->kvReuse = enabled;
}

// [loadMs, promptTokens, reusedTokens, prefillMs, genTokens, genMs] of the last calls.
JNIEXPORT jfloatArray JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeStats(JNIEnv *env, jobject, jlong handle) {
    Session *s = session(handle);
    jfloatArray out = env->NewFloatArray(6);
    if (s == nullptr) return out;
    const auto &st = s->stats;
    float v[6] = {(float) st.loadMs,    (float) st.promptTokens, (float) st.reusedTokens,
                  (float) st.prefillMs, (float) st.genTokens,    (float) st.genMs};
    env->SetFloatArrayRegion(out, 0, 6, v);
    return out;
}

// Reads a prompt prefix (e.g. the system prompt) into memory ahead of the first message.
JNIEXPORT jboolean JNICALL
Java_app_hain_llm_internal_NativeLlm_nativePrefill(JNIEnv *env, jobject, jlong handle, jbyteArray promptUtf8,
                                                   jintArray cutBytes) {
    Session *s = session(handle);
    if (s == nullptr) return JNI_FALSE;
    return hain::prefill(s, bytesOf(env, promptUtf8), intsOf(env, cutBytes)) ? JNI_TRUE : JNI_FALSE;
}

// Blocking loop on the caller thread. The prompt (UTF-8 bytes) is already fully templated, so it is
// tokenized with special-token parsing and without adding BOS. `cutBytes` are byte offsets where a
// checkpoint helps the next turn. Text goes to cb.onToken(byte[]) only in complete UTF-8 sequences;
// onToken returning false stops generation. The stop flag is not cleared here (see nativeSetCancel).
JNIEXPORT jint JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeGenerate(JNIEnv *env, jobject, jlong handle, jbyteArray promptUtf8,
                                                    jintArray cutBytes, jfloatArray params, jstring grammar,
                                                    jobject cb) {
    Session *s = session(handle);
    if (s == nullptr) return hain::STOP_ERROR;

    jsize nParams = env->GetArrayLength(params);
    std::vector<float> p(nParams);
    env->GetFloatArrayRegion(params, 0, nParams, p.data());
    std::string cgrammar;
    if (grammar != nullptr) {
        const char *g = env->GetStringUTFChars(grammar, nullptr);
        cgrammar = g;
        env->ReleaseStringUTFChars(grammar, g);
    }
    jmethodID onToken = env->GetMethodID(env->GetObjectClass(cb), "onToken", "([B)Z");
    auto emit = [&](const char *data, size_t n) -> bool {
        jbyteArray bytes = env->NewByteArray((jsize) n);
        env->SetByteArrayRegion(bytes, 0, (jsize) n, reinterpret_cast<const jbyte *>(data));
        jboolean keepGoing = env->CallBooleanMethod(cb, onToken, bytes);
        env->DeleteLocalRef(bytes);
        return keepGoing == JNI_TRUE && !env->ExceptionCheck();
    };
    return hain::generate(s, bytesOf(env, promptUtf8), intsOf(env, cutBytes), p.data(), (int) nParams,
                          cgrammar.empty() ? nullptr : cgrammar.c_str(), emit);
}

}  // extern "C"
