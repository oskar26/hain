// Host test T-133: CardFence without a model. Build (no Gradle, no model needed):
//   c++ -std=c++17 -Icore-llm/src/main/cpp -Ithird_party/llama.cpp/include -Ithird_party/llama.cpp/ggml/include \
//       core-llm/src/test/cpp/card_fence_test.cpp -o /tmp/card_fence_test && /tmp/card_fence_test
#include "hain_card_grammar.h"
#include <cstdio>
#include <vector>
using namespace hain;
using E = CardFence::Event;
static int fails = 0;
static void expect(bool ok, const char *what) { if (!ok) { std::printf("FAIL %s\n", what); ++fails; } }
static std::vector<E> run(std::vector<std::string> pieces, std::vector<bool> on) {
    CardFence f; std::vector<E> out;
    for (size_t i = 0; i < pieces.size(); ++i) out.push_back(f.feed(pieces[i], i < on.size() ? (bool) on[i] : false));
    return out;
}
int main() {
    auto a = run({"Hier:\n", "```", "card", "\n", "List\n- a\n", "```", "\nFertig"}, {false, false, false, false, true, true, false});
    expect(a[3] == E::OPEN, "open after the newline token");
    expect(a[5] == E::CLOSE, "close at the fence");
    expect(a[6] == E::NONE, "free text after");
    auto b = run({"```card\nCard", " title=\"x\"\n"}, {});
    expect(b[0] == E::NONE, "fence inside a bigger token: no grammar");
    auto c = run({"```kotlin\n", "val a = 1\n", "```card\n", "```\n", "```card\n"}, {});
    expect(c[2] == E::NONE, "card fence inside another code block does not open");
    expect(c[4] == E::OPEN, "after that block a card opens");
    auto d = run({"```card\n"}, {});
    expect(d[0] == E::OPEN, "fence and newline in one token at the end opens");
    auto e = run({"  ```card  \n"}, {});
    expect(e[0] == E::OPEN, "spaces around the fence");
    auto g = run({"```card\n", "- ```x\n", "```"}, {false, true, true});
    expect(g[1] == E::NONE && g[2] == E::CLOSE, "backticks inside an item do not close");
    auto h = run({"```card\n", "List\n- a\n", "```\n", "Danke"}, {false, true, true, false});
    expect(h[2] == E::CLOSE, "fence with its newline in one token closes");
    expect(h[3] == E::NONE, "text after a closed card");
    std::string wrapped = cardGrammarWithNewline("root ::= \"```\"");
    expect(wrapped.find("hain-card-root ::= root") != std::string::npos, "wrapped root");
    std::printf(fails ? "%d FAILED\n" : "fence ok\n", fails);
    return fails ? 1 : 0;
}
