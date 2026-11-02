// Hain native session logic (LLM.md §1, T-033). No JNI here, so the same code runs in a host test.
#pragma once

#include <algorithm>
#include <atomic>
#include <chrono>
#include <cstdint>
#include <string>
#include <vector>

#include "llama.h"

#ifdef __ANDROID__
#include <android/log.h>
#define HAIN_LOGI(...) __android_log_print(ANDROID_LOG_INFO, "HainLlm", __VA_ARGS__)
#define HAIN_LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "HainLlm", __VA_ARGS__)
#else
#include <cstdio>
#define HAIN_LOGI(...) (std::fprintf(stderr, __VA_ARGS__), std::fputc('\n', stderr))
#define HAIN_LOGE(...) (std::fprintf(stderr, __VA_ARGS__), std::fputc('\n', stderr))
#endif

namespace hain {

constexpr int STOP_EOS = 0;
constexpr int STOP_MAX_TOKENS = 1;
constexpr int STOP_CANCELLED = 2;
constexpr int STOP_ERROR = 3;
constexpr int STOP_CONTEXT_FULL = 4;

// Hybrid models (LFM: attention + short convolutions) cannot cut their recurrent state at an
// arbitrary position. A checkpoint is a copy of that small recurrent part at a known token count,
// so the memory can go back there instead of re-reading the whole chat (like llama-server does).
struct Checkpoint {
    size_t nTokens = 0;
    std::vector<uint8_t> data;
};

constexpr size_t MAX_CHECKPOINTS = 6;

struct Stats {
    double loadMs = 0;
    int promptTokens = 0;   // tokens in the last prompt
    int reusedTokens = 0;   // of those, already in memory (not decoded again)
    double prefillMs = 0;
    int genTokens = 0;
    double genMs = 0;
};

struct Session {
    llama_model *model = nullptr;
    llama_context *ctx = nullptr;
    const llama_vocab *vocab = nullptr;
    std::vector<llama_token> cached;  // tokens currently in memory (seq 0)
    std::vector<Checkpoint> checkpoints;  // sorted by nTokens, all ≤ cached.size()
    bool kvReuse = true;
    std::atomic<bool> cancel{false};
    Stats stats;
};

inline double nowMs() {
    using namespace std::chrono;
    return duration<double, std::milli>(steady_clock::now().time_since_epoch()).count();
}

// Checked by ggml between graph nodes: a stop lands inside a long prompt batch, not after it.
inline bool abortCallback(void *data) { return static_cast<Session *>(data)->cancel.load(); }

inline void attachAbort(Session *s) { llama_set_abort_callback(s->ctx, abortCallback, s); }

inline std::vector<llama_token> tokenize(const llama_vocab *vocab, const std::string &text, bool addSpecial) {
    int n = -llama_tokenize(vocab, text.c_str(), (int32_t) text.size(), nullptr, 0, addSpecial, true);
    std::vector<llama_token> out(std::max(n, 0));
    if (n > 0) {
        llama_tokenize(vocab, text.c_str(), (int32_t) text.size(), out.data(), n, addSpecial, true);
    }
    return out;
}

inline std::string piece(const llama_vocab *vocab, llama_token token) {
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
inline size_t completeUtf8(const std::string &s) {
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

// Token counts of the byte prefixes in `cutBytes` whose tokens equal the start of `tokens`.
// A cut inside a word would tokenize differently; such cuts are dropped.
inline std::vector<size_t> cutTokens(const llama_vocab *vocab, const std::string &prompt,
                                     const std::vector<llama_token> &tokens, const std::vector<int> &cutBytes) {
    std::vector<size_t> out;
    for (int b : cutBytes) {
        if (b <= 0 || (size_t) b >= prompt.size()) continue;
        auto prefix = tokenize(vocab, prompt.substr(0, (size_t) b), false);
        if (prefix.empty() || prefix.size() >= tokens.size()) continue;
        if (std::equal(prefix.begin(), prefix.end(), tokens.begin())) out.push_back(prefix.size());
    }
    std::sort(out.begin(), out.end());
    out.erase(std::unique(out.begin(), out.end()), out.end());
    return out;
}

inline void clearMemory(Session *s) {
    llama_memory_clear(llama_get_memory(s->ctx), true);
    s->cached.clear();
    s->checkpoints.clear();
}

inline void saveCheckpoint(Session *s) {
    const size_t n = s->cached.size();
    if (n == 0) return;
    for (const auto &c : s->checkpoints) {
        if (c.nTokens == n) return;
    }
    Checkpoint cp;
    cp.nTokens = n;
    cp.data.resize(llama_state_seq_get_size_ext(s->ctx, 0, LLAMA_STATE_SEQ_FLAGS_PARTIAL_ONLY));
    if (!cp.data.empty()) {
        size_t got = llama_state_seq_get_data_ext(s->ctx, cp.data.data(), cp.data.size(), 0,
                                                  LLAMA_STATE_SEQ_FLAGS_PARTIAL_ONLY);
        if (got == 0) return;
        cp.data.resize(got);
    }
    s->checkpoints.push_back(std::move(cp));
    std::sort(s->checkpoints.begin(), s->checkpoints.end(),
              [](const Checkpoint &a, const Checkpoint &b) { return a.nTokens < b.nTokens; });
    // Keep the earliest one (end of the fixed system prompt, shared by all chats) and the newest.
    while (s->checkpoints.size() > MAX_CHECKPOINTS) s->checkpoints.erase(s->checkpoints.begin() + 1);
}

// Cuts the memory back to at most `n` tokens. Returns the token count kept (0 = memory cleared).
inline size_t truncateTo(Session *s, size_t n) {
    llama_memory_t mem = llama_get_memory(s->ctx);
    if (n >= s->cached.size()) return s->cached.size();
    if (n > 0 && llama_memory_seq_rm(mem, 0, (llama_pos) n, -1)) {
        s->cached.resize(n);
        while (!s->checkpoints.empty() && s->checkpoints.back().nTokens > n) s->checkpoints.pop_back();
        return n;
    }
    // Recurrent part cannot be cut: go back to the newest checkpoint at or below n.
    while (!s->checkpoints.empty()) {
        Checkpoint &cp = s->checkpoints.back();
        if (cp.nTokens <= n && cp.nTokens > 0) {
            bool ok = llama_state_seq_set_data_ext(s->ctx, cp.data.data(), cp.data.size(), 0,
                                                   LLAMA_STATE_SEQ_FLAGS_PARTIAL_ONLY) == cp.data.size() ||
                      cp.data.empty();
            if (ok && llama_memory_seq_rm(mem, 0, (llama_pos) cp.nTokens, -1)) {
                s->cached.resize(cp.nTokens);
                return cp.nTokens;
            }
            break;
        }
        s->checkpoints.pop_back();
    }
    clearMemory(s);
    return 0;
}

enum class DecodeResult { OK, CANCELLED, ERROR };

// Decodes tokens[from, to) in batches. On a stop or error the memory goes back to a safe point.
inline DecodeResult decodeRange(Session *s, const std::vector<llama_token> &tokens, size_t from, size_t to) {
    const size_t nBatch = llama_n_batch(s->ctx);
    for (size_t i = from; i < to; i += nBatch) {
        if (s->cancel) return DecodeResult::CANCELLED;
        size_t n = std::min(nBatch, to - i);
        llama_batch batch = llama_batch_get_one(const_cast<llama_token *>(tokens.data() + i), (int32_t) n);
        int rc = llama_decode(s->ctx, batch);
        if (rc != 0) {
            // Part of the batch may be in memory now: drop it so memory and `cached` agree again.
            s->cached.push_back(-1);  // marks the unknown tail; truncateTo cuts it off
            truncateTo(s, s->cached.size() - 1);
            return rc == 2 ? DecodeResult::CANCELLED : DecodeResult::ERROR;
        }
        s->cached.insert(s->cached.end(), tokens.begin() + (long) i, tokens.begin() + (long) (i + n));
    }
    return DecodeResult::OK;
}

// Brings the memory to `prompt`, reusing what is already there (T-033) and leaving checkpoints at
// `cuts` (token counts) for the next turn. With `checkpointEnd` the end of the prompt gets one too.
inline DecodeResult ingestPrompt(Session *s, const std::vector<llama_token> &prompt, const std::vector<size_t> &cuts,
                                 bool checkpointEnd) {
    size_t keep = 0;
    if (s->kvReuse) {
        while (keep < s->cached.size() && keep < prompt.size() && s->cached[keep] == prompt[keep]) ++keep;
        if (keep == prompt.size() && keep > 0 && !checkpointEnd) --keep;  // ≥ 1 token for fresh logits
        keep = truncateTo(s, keep);
    } else {
        clearMemory(s);
    }
    s->stats.promptTokens = (int) prompt.size();
    s->stats.reusedTokens = (int) keep;
    size_t at = keep;
    for (size_t cut : cuts) {
        if (cut <= at || cut >= prompt.size()) continue;
        DecodeResult r = decodeRange(s, prompt, at, cut);
        if (r != DecodeResult::OK) return r;
        saveCheckpoint(s);
        at = cut;
    }
    DecodeResult r = decodeRange(s, prompt, at, prompt.size());
    if (r == DecodeResult::OK && checkpointEnd) saveCheckpoint(s);
    return r;
}

inline llama_sampler *makeSampler(const llama_vocab *vocab, const float *p, int n, const char *grammar) {
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

// Sink for generated text: return false to stop.
template <typename Emit>
int generate(Session *s, const std::string &prompt, const std::vector<int> &cutBytes, const float *params, int nParams,
             const char *grammar, Emit emit) {
    const int maxTokens = nParams > 6 ? (int) params[6] : 1024;
    auto tokens = tokenize(s->vocab, prompt, false);
    const auto nCtx = (size_t) llama_n_ctx(s->ctx);
    if (tokens.empty()) return STOP_ERROR;
    if (tokens.size() + 8 >= nCtx) return STOP_CONTEXT_FULL;

    const double t0 = nowMs();
    auto cuts = cutTokens(s->vocab, prompt, tokens, cutBytes);
    DecodeResult ingest = ingestPrompt(s, tokens, cuts, false);
    s->stats.prefillMs = nowMs() - t0;
    s->stats.genTokens = 0;
    s->stats.genMs = 0;
    if (ingest == DecodeResult::CANCELLED) return STOP_CANCELLED;
    if (ingest == DecodeResult::ERROR) {
        clearMemory(s);
        return STOP_ERROR;
    }

    llama_sampler *smpl = makeSampler(s->vocab, params, nParams, grammar);
    const double t1 = nowMs();
    int reason = STOP_MAX_TOKENS;
    std::string pending;
    auto flush = [&](bool all) -> bool {
        size_t n = all ? pending.size() : completeUtf8(pending);
        if (n == 0) return true;
        bool keepGoing = emit(pending.data(), n);
        pending.erase(0, n);
        return keepGoing;
    };
    for (int i = 0; i < maxTokens; ++i) {
        if (s->cancel) { reason = STOP_CANCELLED; break; }
        if (s->cached.size() + 1 >= nCtx) { reason = STOP_CONTEXT_FULL; break; }
        llama_token tok = llama_sampler_sample(smpl, s->ctx, -1);
        if (llama_vocab_is_eog(s->vocab, tok)) { reason = STOP_EOS; break; }
        pending += piece(s->vocab, tok);
        s->stats.genTokens++;
        if (!flush(false)) { reason = STOP_CANCELLED; break; }
        std::vector<llama_token> one{tok};
        DecodeResult r = decodeRange(s, one, 0, 1);
        if (r == DecodeResult::CANCELLED) { reason = STOP_CANCELLED; break; }
        if (r == DecodeResult::ERROR) { reason = STOP_ERROR; break; }
    }
    if (reason != STOP_ERROR && reason != STOP_CANCELLED) flush(true);
    s->stats.genMs = nowMs() - t1;
    llama_sampler_free(smpl);
    const auto &st = s->stats;
    HAIN_LOGI("prompt %d tok (reused %d) in %.0f ms; %d tok in %.0f ms (%.1f t/s); checkpoints %zu",
              st.promptTokens, st.reusedTokens, st.prefillMs, st.genTokens, st.genMs,
              st.genMs > 0 ? st.genTokens * 1000.0 / st.genMs : 0.0, s->checkpoints.size());
    return reason;
}

// Reads `prompt` into memory without answering (warm-up after loading), checkpoint at the end.
inline bool prefill(Session *s, const std::string &prompt, const std::vector<int> &cutBytes) {
    auto tokens = tokenize(s->vocab, prompt, false);
    if (tokens.empty() || tokens.size() + 8 >= (size_t) llama_n_ctx(s->ctx)) return false;
    const double t0 = nowMs();
    auto cuts = cutTokens(s->vocab, prompt, tokens, cutBytes);
    DecodeResult r = ingestPrompt(s, tokens, cuts, true);
    HAIN_LOGI("warm-up %zu tok (reused %d) in %.0f ms", tokens.size(), s->stats.reusedTokens, nowMs() - t0);
    return r == DecodeResult::OK;
}

}  // namespace hain
