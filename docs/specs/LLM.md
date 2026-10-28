# LLM.md — runtime, prompt format, tool calls

Source of truth for the format: `prompts/lfm3-1.7b_chat_template.jinja` (official, copied 2026-10-27).
Research notes: `docs/concept/research/lfm_prompt_omni.md`.

## 1. Native layer (`:core-llm`, tasks marked `native`)

```
core-llm/
  src/main/cpp/
    CMakeLists.txt        add_subdirectory(${REPO}/third_party/llama.cpp) ; build hain_llm.so
    hain_llm.cpp          JNI functions (below), ~400 lines
  src/main/kotlin/app/hain/llm/internal/NativeLlm.kt   external funs
```

- llama.cpp pinned in `third_party/llama.cpp` (git submodule). Record the commit in `docs/workflow/STATUS.md`.
  Needs a commit **after** LFM3 support (text: upstream; audio/omni PR #31257 merged 2026-10-27; VL PR #31252 in review).
- CMake flags: `-DGGML_OPENMP=OFF -DLLAMA_CURL=OFF -DBUILD_SHARED_LIBS=OFF -DGGML_CPU_ARM_ARCH=armv8.2-a+dotprod+i8mm` (fallback build without i8mm),
  optional `-DGGML_VULKAN=ON` behind Gradle flag `hain.vulkan=true` (off by default; test per device).
- ABI: `arm64-v8a` only. Linker flag `-Wl,-z,max-page-size=16384`.

JNI surface (keep it this small):

| Kotlin `external fun` | Does |
|---|---|
| `nativeLoad(path: String, nCtx: Int, nThreads: Int, nGpuLayers: Int): Long` | load model + context, return handle (0 = error) |
| `nativeFree(handle: Long)` | free all |
| `nativeTokenize(handle: Long, text: String, addSpecial: Boolean): IntArray` | for token counting |
| `nativeGenerate(handle, prompt: String, params: FloatArray, grammar: String?, cb: TokenCallback): Int` | blocking loop on caller thread; calls `cb.onToken(String)`; returns stop reason |
| `nativeCancel(handle: Long)` | sets atomic flag checked each token |
| `nativeKvReuse(handle, enabled: Boolean)` | keep KV cache for shared prefix |

- `params` = `[temperature, topK, topP, minP, repeatPenalty, repeatLastN, maxTokens, seed]`.
- Sampler chain: penalties → top_k → top_p → min_p → temp → dist (greedy if temperature == 0).
- Grammar: if non-null, add `llama_sampler_init_grammar` first.
- **UTF-8 safety:** buffer bytes until a complete UTF-8 sequence before calling `onToken` (German umlauts!).
- Stop on EOS `<|im_end|>` or `maxTokens`.
- **Prefix cache:** compare new prompt tokens with tokens in KV; only decode the new suffix (big speed-up in chats).

## 2. Prompt format (LFM3 ChatML)

```
<|startoftext|><|im_start|>system
{SYSTEM_PROMPT}

<tools>
{"name": "weather", "description": "...", "parameters": {...}}
{"name": "web_search", "description": "...", "parameters": {...}}
</tools><|im_end|>
<|im_start|>user
Wie wird das Wetter morgen in Köln?<|im_end|>
<|im_start|>assistant
<think>
```

Rules for `PromptBuilder` (pure Kotlin, must equal the Jinja output byte-for-byte for the golden cases):

1. Always start with `<|startoftext|>`.
2. System turn only if system prompt or tools exist. Tools block = system prompt + `"\n\n"` (only if system prompt non-empty) + `"<tools>\n"` + one compact JSON object per line (`{"name","description","parameters"}`, key order as given) + `"\n</tools>"`.
3. Each message: `<|im_start|>{role}\n{content}<|im_end|>\n`.
4. Assistant messages **before the last user message**: content = `<think></think>` + text after last `</think>` (reasoning dropped). Assistant messages after the last user message (current tool loop): `<think>{reasoning}</think>` + content + tool calls.
5. Assistant tool calls rendered as in §3, appended after content.
6. Tool messages: `<|im_start|>tool\n<tool_response>\n{content}\n</tool_response><|im_end|>\n`. Consecutive tool results for one call are merged by the template; we send one tool message per call.
7. Generation prompt: `<|im_start|>assistant\n` + (`<think>` if thinking enabled else `<think></think>`).
8. JSON of tool schemas: `kotlinx.serialization` with `prettyPrint=false`, separators `", "` and `": "` **like Python `json.dumps` default** (the template uses `tojson`, check golden file G-03 and adjust).

Golden files (create in T-041 by running the Jinja template with Python `jinja2` once, commit outputs):
`core-llm/src/test/resources/golden/G-01_plain.txt`, `G-02_system.txt`, `G-03_tools.txt`, `G-04_toolloop.txt`, `G-05_multiturn_thinking_dropped.txt`, `G-06_nothink.txt`.
Script: `tools/render_golden.py` (T-041).

## 3. Tool call output (what the model writes)

```
<tool_call>
<function=weather>
<parameter=city>
Köln
</parameter>
<parameter=days>
2
</parameter>
</function>
</tool_call>
```

- Several calls separated by a blank line.
- Values: string as-is (trim one leading/trailing `\n`); numbers/bools/objects/arrays are JSON → try `Json.parseToJsonElement`, fall back to string.
- Arguments are kept as `Map<String, Any?>` (template needs a dict, never a JSON string).

## 4. StreamSplitter (live, while tokens arrive)

States: `REASONING` (inside `<think>` … `</think>`), `TEXT`, `TOOL` (from `<tool_call>` to `</tool_call>`).
- Generation starts in `REASONING` if thinking enabled (the `<think>` is in the prompt, not in the output).
- Emit `UiDelta.Reasoning(text)`, `UiDelta.Text(text)`, `UiDelta.ToolCallStarted(name?)`.
- Hold back up to 12 chars when a partial tag (`<`, `<t`, `<to`…) could start; release when disambiguated.
- At end: `ToolCallParser.parse(fullOutput)` → `ParsedOutput(reasoning, text, toolCalls, errors)`.
- Malformed tool XML → show text, log, do **not** execute.

## 5. Parser test cases (must pass, T-042)

| # | Input (after `<think>…</think>`) | Expected |
|---|---|---|
| 1 | one call, one string param | name=weather, args={city:"Köln"} |
| 2 | one call, int param `2` | args={days:2 (Int)} |
| 3 | two calls separated by blank line | 2 calls in order |
| 4 | text before call ("Ich schaue nach.\n<tool_call>…") | text="Ich schaue nach.", 1 call |
| 5 | missing `</tool_call>` | 0 calls, error, text kept |
| 6 | param value with `<` inside (`a < b`) | value kept literally |
| 7 | JSON object param `{"a":1}` | parsed map |
| 8 | no think block at all (thinking off) | text only |

## 6. Sampling presets

| Preset | temp | top_k | top_p | min_p | rep. pen. | when |
|---|---|---|---|---|---|---|
| `chat` (default) | 0.1 | 50 | 1.0 | 0.0 | 1.1 | normal chat + tools |
| `math` | 1.0 | 20 | 0.95 | 0.0 | 1.1 | calculator-like questions with thinking on |
| `creative` | 0.7 | 50 | 0.95 | 0.05 | 1.1 | user picks "creative" |
| `omni_text` | 0 (greedy) | – | – | – | 1.0 | Omni text |
| `extract` | 0.0 | – | – | – | 1.0 | memory extraction JSON with grammar |

`maxTokens`: 1024 (thinking off), 4096 (thinking on). Context `nCtx`: 8192 default; 4096 on ≤6 GB RAM; user up to 32768.

## 7. Thinking modes (setting `answer_mode`)

- `fast`: `enableThinking=false`.
- `thorough`: `enableThinking=true`, reasoning shown collapsed ("Pim denkt nach…").
- `auto` (default): thinking on if message > 200 chars, contains digits+operators, or words like "warum/why/erkläre/explain/vergleiche/compare/plan"; else off. Pure function `ThinkingPolicy.decide(msg)` with tests.

## 8. Tool loop

- Max 3 tool rounds per user turn, then force answer: rebuild prompt with tools removed.
- Keep current-turn reasoning + tool calls in history (template rule).
- Tool timeout 12 s; on timeout send `{"error":"timeout"}` as tool result.

## 9. System prompt

`SystemPrompt.fill(template, vars)` loads `assets/prompts/system_full.en.txt` (or short) and replaces
`{weekday} {date} {time} {timezone} {city} {emergency_number} {memory_snippets} {user_custom_instructions}`.
Missing values → empty string, then collapse triple newlines. `emergency_number`: `112` for EU locales, `911` US, `999` UK, else `112`.
Date/time formatted in the **app language** (`Dienstag, 28. Oktober 2026` / `Tuesday, 28 October 2026`).

## 10. Omni (optional pack, later tasks)

- Text mode: system = Hain prompt + last line `Respond with text.`, greedy.
- Voice mode: system exactly `Respond with interleaved text and audio.`; Hain short prompt prepended to first user message.
- Image input needs mmproj + llama.cpp `mtmd` API → separate native task T-150 (needs stronger model).
