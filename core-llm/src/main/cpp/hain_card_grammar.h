// T-133: the card grammar (core-cards/src/main/assets/card.gbnf) only inside a ```card block of an answer.
// No JNI here, so the same code runs in a host test. See docs/specs/CARDS.md §3.
//
// The answer is sampled as before. When the text so far ends exactly with a line "```card\n", a grammar sampler
// is switched on for the next tokens; it is fed nothing of the text before, so the KV cache and the sampler chain
// (repeat penalty history, seed) stay as they are. When the grammar reaches its end (the closing "```"), it is
// switched off again and the answer goes on freely. A fence that arrives inside a bigger token ("```card\nCard")
// cannot be matched cleanly, so that card is read leniently by the app's parser instead (same as without grammar).
#pragma once

#include <cmath>
#include <string>
#include <vector>

#include "llama.h"

namespace hain {

// Watches the answer line by line and says when a ```card block opens and when the grammar should stop.
class CardFence {
public:
    enum class Event { NONE, OPEN, CLOSE };

    // [piece] is the text of one more token. OPEN: the grammar can start with the next token.
    // CLOSE (only while [grammarOn]): the token just closed the card with "```".
    Event feed(const std::string &piece, bool grammarOn) {
        Event event = Event::NONE;
        for (size_t i = 0; i < piece.size(); ++i) {
            const char c = piece[i];
            if (c != '\n') {
                line_ += c;
                continue;
            }
            const std::string done = trimmed(line_);
            line_.clear();
            if (!inBlock_) {
                if (done.rfind("```", 0) == 0) {
                    inBlock_ = true;
                    // only a fence that ends exactly at the end of this token can start the grammar
                    if (done == "```card" && i == piece.size() - 1) event = Event::OPEN;
                }
            } else if (done == "```") {
                inBlock_ = false;
            }
        }
        if (grammarOn && event == Event::NONE && trimmed(line_) == "```") event = Event::CLOSE;
        return event;
    }

    bool inBlock() const { return inBlock_; }

private:
    static std::string trimmed(const std::string &s) {
        size_t a = 0;
        size_t b = s.size();
        while (a < b && (s[a] == ' ' || s[a] == '\t' || s[a] == '\r')) ++a;
        while (b > a && (s[b - 1] == ' ' || s[b - 1] == '\t' || s[b - 1] == '\r')) --b;
        return s.substr(a, b - a);
    }

    std::string line_;
    bool inBlock_ = false;
};

// Samples one token with [chain], and with [grammar] when it is on (like llama.cpp's common sampler: try the
// chain first, check the token against the grammar, only on a miss filter the whole vocabulary). Returns
// LLAMA_TOKEN_NULL when the grammar allows no token at all; the caller then switches the grammar off.
inline llama_token sampleCard(llama_context *ctx, const llama_vocab *vocab, llama_sampler *chain,
                              llama_sampler *grammar, std::vector<llama_token_data> &cur) {
    const int nVocab = llama_vocab_n_tokens(vocab);
    const float *logits = llama_get_logits_ith(ctx, -1);
    if (logits == nullptr) return LLAMA_TOKEN_NULL;
    auto fill = [&]() {
        cur.resize(nVocab);
        for (llama_token id = 0; id < nVocab; ++id) cur[id] = llama_token_data{id, logits[id], 0.0f};
        return llama_token_data_array{cur.data(), cur.size(), -1, false};
    };
    // 1. as without grammar
    llama_token_data_array arr = fill();
    llama_sampler_apply(chain, &arr);
    if (arr.selected < 0) return LLAMA_TOKEN_NULL;
    llama_token token = arr.data[arr.selected].id;
    // 2. does the grammar take it?
    llama_token_data single{token, 1.0f, 0.0f};
    llama_token_data_array one{&single, 1, -1, false};
    llama_sampler_apply(grammar, &one);
    if (!std::isfinite(single.logit)) {
        // 3. no: only what the grammar allows, then the chain again
        arr = fill();
        llama_sampler_apply(grammar, &arr);
        bool any = false;
        for (size_t i = 0; i < arr.size && !any; ++i) any = std::isfinite(arr.data[i].logit);
        if (!any) return LLAMA_TOKEN_NULL;
        llama_sampler_apply(chain, &arr);
        if (arr.selected < 0 || !std::isfinite(arr.data[arr.selected].logit)) return LLAMA_TOKEN_NULL;
        token = arr.data[arr.selected].id;
    }
    llama_sampler_accept(chain, token);
    // an end-of-generation token is never fed to the grammar (llama.cpp aborts on a not finished one)
    if (!llama_vocab_is_eog(vocab, token)) llama_sampler_accept(grammar, token);
    return token;
}

}  // namespace hain
