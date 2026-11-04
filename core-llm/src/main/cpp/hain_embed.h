// Hain embedder (MEMORY.md §3, T-110): one small model in llama.cpp's embedding mode that turns a
// short text into one vector. No JNI here, so the same code runs in a host test.
#pragma once

#include <cmath>
#include <string>
#include <vector>

#include "hain_session.h"
#include "llama.h"

namespace hain {

struct EmbedSession {
    llama_model *model = nullptr;
    llama_context *ctx = nullptr;
    const llama_vocab *vocab = nullptr;
    int nEmbd = 0;
    int nCtx = 0;
};

// Loads an embedding GGUF (mmap). Its own small context, separate from the brain's: `nCtx` tokens
// per text (LFM2.5-Embedding reads up to 512), the pooling the model file asks for.
inline EmbedSession *embedLoad(const char *path, int nCtx, int nThreads) {
    auto mparams = llama_model_default_params();
    mparams.n_gpu_layers = 0;
    mparams.load_mode = LLAMA_LOAD_MODE_MMAP;
    llama_model *model = llama_model_load_from_file(path, mparams);
    if (model == nullptr) {
        HAIN_LOGE("embedding model load failed");
        return nullptr;
    }
    auto cparams = llama_context_default_params();
    cparams.n_ctx = (uint32_t) nCtx;
    // the whole text in one batch: non-causal models need batch == ubatch
    cparams.n_batch = (uint32_t) nCtx;
    cparams.n_ubatch = (uint32_t) nCtx;
    cparams.n_seq_max = 1;
    cparams.n_threads = nThreads;
    cparams.n_threads_batch = nThreads;
    cparams.embeddings = true;
    llama_context *ctx = llama_init_from_model(model, cparams);
    if (ctx == nullptr) {
        HAIN_LOGE("embedding context init failed");
        llama_model_free(model);
        return nullptr;
    }
    if (llama_pooling_type(ctx) == LLAMA_POOLING_TYPE_NONE) {
        HAIN_LOGE("embedding model has no pooling");
        llama_free(ctx);
        llama_model_free(model);
        return nullptr;
    }
    auto *e = new EmbedSession();
    e->model = model;
    e->ctx = ctx;
    e->vocab = llama_model_get_vocab(model);
    e->nEmbd = llama_model_n_embd_out(model);
    if (e->nEmbd <= 0) e->nEmbd = llama_model_n_embd(model);
    e->nCtx = nCtx;
    return e;
}

inline void embedFree(EmbedSession *e) {
    if (e == nullptr) return;
    llama_free(e->ctx);
    llama_model_free(e->model);
    delete e;
}

// The vector of `text` (with the model's BOS), length 1 (L2-normalised), so a dot product is the
// cosine. Texts longer than the context are cut. Empty on failure.
inline std::vector<float> embed(EmbedSession *e, const std::string &text) {
    std::vector<float> out;
    if (e == nullptr || text.empty()) return out;
    auto tokens = tokenize(e->vocab, text, true);
    if (tokens.empty()) return out;
    if ((int) tokens.size() > e->nCtx) tokens.resize((size_t) e->nCtx);
    llama_memory_t mem = llama_get_memory(e->ctx);
    if (mem != nullptr) llama_memory_clear(mem, true);
    llama_batch batch = llama_batch_init((int32_t) tokens.size(), 0, 1);
    for (size_t i = 0; i < tokens.size(); ++i) {
        batch.token[i] = tokens[i];
        batch.pos[i] = (llama_pos) i;
        batch.n_seq_id[i] = 1;
        batch.seq_id[i][0] = 0;
        batch.logits[i] = 1;
    }
    batch.n_tokens = (int32_t) tokens.size();
    int rc = llama_decode(e->ctx, batch);
    llama_batch_free(batch);
    if (rc != 0) {
        HAIN_LOGE("embedding decode failed (%d)", rc);
        return out;
    }
    const float *v = llama_get_embeddings_seq(e->ctx, 0);
    if (v == nullptr) return out;
    out.assign(v, v + e->nEmbd);
    double sum = 0;
    for (float x : out) sum += (double) x * x;
    const double norm = std::sqrt(sum);
    if (norm > 0) {
        for (float &x : out) x = (float) (x / norm);
    }
    return out;
}

}  // namespace hain
