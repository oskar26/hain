// Hain JNI bridge to llama.cpp (LLM.md §1, T-030/T-033). Kept deliberately small: the logic lives in
// hain_session.h so it can be tested on a computer without Android.
#include <jni.h>

#include <algorithm>
#include <atomic>
#include <cstring>
#include <mutex>
#include <string>
#include <vector>

#include "ggml-backend.h"
#include "hain_embed.h"
#include "hain_session.h"
#include "llama.h"

using hain::Session;

namespace {

std::atomic<bool> g_backendReady{false};
std::mutex g_initLock;

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
    // brain and embedder (T-110) may both start first, on different threads: load the backends once
    std::lock_guard<std::mutex> lock(g_initLock);
    if (g_backendReady.load()) return;
    const char *dir = env->GetStringUTFChars(nativeLibDir, nullptr);
    ggml_backend_load_all_from_path(dir);
    env->ReleaseStringUTFChars(nativeLibDir, dir);
    llama_backend_init();
    g_backendReady = true;
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
    mparams.load_mode = LLAMA_LOAD_MODE_MMAP;
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

// [loadMs, promptTokens, reusedTokens, prefillMs, genTokens, genMs, restoredMs] of the last calls;
// restoredMs (T-242) is the time a snapshot from disk took, -1 when none was loaded.
JNIEXPORT jfloatArray JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeStats(JNIEnv *env, jobject, jlong handle) {
    Session *s = session(handle);
    jfloatArray out = env->NewFloatArray(7);
    if (s == nullptr) return out;
    const auto &st = s->stats;
    float v[7] = {(float) st.loadMs,    (float) st.promptTokens, (float) st.reusedTokens,
                  (float) st.prefillMs, (float) st.genTokens,    (float) st.genMs,
                  (float) s->restoredMs};
    env->SetFloatArrayRegion(out, 0, 7, v);
    return out;
}

// T-242: folder for memory snapshots of the system prompt (created by the caller); null = off.
JNIEXPORT void JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeSetStateDir(JNIEnv *env, jobject, jlong handle, jstring dir) {
    Session *s = session(handle);
    if (s == nullptr) return;
    if (dir == nullptr) {
        s->stateDir.clear();
        return;
    }
    const char *cdir = env->GetStringUTFChars(dir, nullptr);
    s->stateDir = cdir;
    env->ReleaseStringUTFChars(dir, cdir);
    s->modelKey = hain::modelKeyOf(s->model, llama_n_ctx(s->ctx));
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
                                                    jstring cardGrammar, jobject cb) {
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
    // T-133: grammar only inside ```card blocks
    std::string ccard;
    if (cardGrammar != nullptr) {
        const char *g = env->GetStringUTFChars(cardGrammar, nullptr);
        ccard = g;
        env->ReleaseStringUTFChars(cardGrammar, g);
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
                          cgrammar.empty() ? nullptr : cgrammar.c_str(), ccard.empty() ? nullptr : ccard.c_str(), emit);
}

// T-241: one GGUF metadata value (e.g. general.architecture) as UTF-8 bytes, or null.
JNIEXPORT jbyteArray JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeMeta(JNIEnv *env, jobject, jlong handle, jstring key) {
    Session *s = session(handle);
    if (s == nullptr) return nullptr;
    const char *ckey = env->GetStringUTFChars(key, nullptr);
    char buf[256];
    int32_t n = llama_model_meta_val_str(s->model, ckey, buf, sizeof(buf));
    env->ReleaseStringUTFChars(key, ckey);
    if (n < 0) return nullptr;
    const auto len = (jsize) std::min<int32_t>(n, (int32_t) sizeof(buf) - 1);
    jbyteArray out = env->NewByteArray(len);
    env->SetByteArrayRegion(out, 0, len, reinterpret_cast<const jbyte *>(buf));
    return out;
}

// T-211: chat template stored in the GGUF (tokenizer.chat_template) as UTF-8 bytes, or null.
JNIEXPORT jbyteArray JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeChatTemplate(JNIEnv *env, jobject, jlong handle) {
    Session *s = session(handle);
    if (s == nullptr) return nullptr;
    const char *tmpl = llama_model_chat_template(s->model, nullptr);
    if (tmpl == nullptr) return nullptr;
    const auto n = (jsize) std::strlen(tmpl);
    jbyteArray out = env->NewByteArray(n);
    env->SetByteArrayRegion(out, 0, n, reinterpret_cast<const jbyte *>(tmpl));
    return out;
}

// T-211: formats messages with llama.cpp's built-in template engine (own models in developer mode).
// Returns UTF-8 bytes, or null when llama.cpp does not know the template.
JNIEXPORT jbyteArray JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeApplyTemplate(JNIEnv *env, jobject, jbyteArray templateUtf8,
                                                         jobjectArray roles, jobjectArray contents,
                                                         jboolean addAssistant) {
    const std::string tmpl = bytesOf(env, templateUtf8);
    const jsize n = env->GetArrayLength(roles);
    std::vector<std::string> roleText(n);
    std::vector<std::string> contentText(n);
    std::vector<llama_chat_message> chat(n);
    for (jsize i = 0; i < n; ++i) {
        auto r = (jbyteArray) env->GetObjectArrayElement(roles, i);
        auto c = (jbyteArray) env->GetObjectArrayElement(contents, i);
        roleText[i] = bytesOf(env, r);
        contentText[i] = bytesOf(env, c);
        env->DeleteLocalRef(r);
        env->DeleteLocalRef(c);
    }
    for (jsize i = 0; i < n; ++i) chat[i] = {roleText[i].c_str(), contentText[i].c_str()};
    size_t total = 0;
    for (jsize i = 0; i < n; ++i) total += roleText[i].size() + contentText[i].size();
    std::vector<char> buf(total * 2 + 1024);
    int32_t len = llama_chat_apply_template(tmpl.c_str(), chat.data(), chat.size(), addAssistant == JNI_TRUE,
                                            buf.data(), (int32_t) buf.size());
    if (len < 0) return nullptr;
    if ((size_t) len > buf.size()) {
        buf.resize(len);
        len = llama_chat_apply_template(tmpl.c_str(), chat.data(), chat.size(), addAssistant == JNI_TRUE,
                                        buf.data(), (int32_t) buf.size());
        if (len < 0) return nullptr;
    }
    jbyteArray out = env->NewByteArray(len);
    env->SetByteArrayRegion(out, 0, len, reinterpret_cast<const jbyte *>(buf.data()));
    return out;
}

// T-110: embedder (MEMORY.md §3). Its own small context next to the brain, loaded on demand.
JNIEXPORT jlong JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeEmbedLoad(JNIEnv *env, jobject, jstring path, jint nCtx, jint nThreads) {
    const double t0 = hain::nowMs();
    const char *cpath = env->GetStringUTFChars(path, nullptr);
    hain::EmbedSession *e = hain::embedLoad(cpath, nCtx, nThreads);
    env->ReleaseStringUTFChars(path, cpath);
    if (e != nullptr) HAIN_LOGI("embedder loaded in %.0f ms (dim %d)", hain::nowMs() - t0, e->nEmbd);
    return reinterpret_cast<jlong>(e);
}

JNIEXPORT void JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeEmbedFree(JNIEnv *, jobject, jlong handle) {
    hain::embedFree(reinterpret_cast<hain::EmbedSession *>(handle));
}

// The normalised vector of a UTF-8 text, or null when it failed.
JNIEXPORT jfloatArray JNICALL
Java_app_hain_llm_internal_NativeLlm_nativeEmbed(JNIEnv *env, jobject, jlong handle, jbyteArray textUtf8) {
    auto *e = reinterpret_cast<hain::EmbedSession *>(handle);
    if (e == nullptr) return nullptr;
    std::vector<float> v = hain::embed(e, bytesOf(env, textUtf8));
    if (v.empty()) return nullptr;
    jfloatArray out = env->NewFloatArray((jsize) v.size());
    env->SetFloatArrayRegion(out, 0, (jsize) v.size(), v.data());
    return out;
}

}  // extern "C"
