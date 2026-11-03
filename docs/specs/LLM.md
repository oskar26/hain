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

`SystemPromptFiller.fill(template, vars)` loads `assets/prompts/system_full.<de|en>.txt` (or short) and replaces
`{weekday} {date} {time} {timezone} {city} {emergency_number} {memory_snippets} {user_custom_instructions} {language_rule}`.
Missing values → empty string, then collapse triple newlines. `emergency_number`: `112` for EU locales, `911` US, `999` UK, else `112`.
Date/time formatted in the **prompt language** with the device region (`Dienstag, 28. Oktober 2026` / `Tuesday, 28 October 2026`).

**Any language (Oskar, 2026-11-03 09:17).** With "Same as my message" (setting AUTO, default) Pim answers in the language of the newest clear user message, whatever language it is, so everyone can use Hain. `ReplyLanguagePolicy.detectLanguage` guesses offline: script first (ja, ko, zh, uk, ru, ar, he, el, hi, th), then stop words and special letters (de, en, fr, es, it, nl, pt, tr, pl). Short messages ("Hi", "ok") keep the chat's language, else the device language. The system prompt stays German or English (device language, keeps the cache); the hint names the target in the prompt's language ("Antworte auf Türkisch." / "Answer in Turkish."). Fixed settings Deutsch/English still win.

Host test (LFM3-1.7B Q4_0, temp 0.1, 2026-11-03), 9 messages × German and English prompt, answer language right in **18/18**:

| Message | German prompt | English prompt |
|---|---|---|
| Salut, comment ça va ? | Salut ! Ça va bien, merci. Et toi ? … | Salut ! Je suis Pim … Et toi, comment ça va ? |
| ¿Qué puedo cocinar con puerros? | ¡Claro! Con puerros … | Claro, puedo ayudarte … ensalada de puerro … |
| Ciao, come stai? | Ciao! Sto bene, grazie. Come stai tu? … | Ciao! Sto bene, grazie per chiedere. … |
| Merhaba, nasılsın? | Merhaba! Ben Pim … Nasılsın? … | Merhaba! Ben Pim … Sen nasılsın? |
| Hoi, hoe gaat het met je? | Hey! Mijn naam is Pim. Hoe kan ik je helpen? | Hoi! Ik ben Pim … |
| Cześć, jak się masz? | Cześć! Mam się dobrze … | Cześć! Mam się świetnie, dziękuję … |
| Привет, как дела? | Привет! У меня всё отлично, спасибо. … | Привет! У меня всё отлично … |
| wie geht es dir | Hey! Alles klar? Wie geht's dir? … | Hey! Wie geht's dir? Ich bin Pim … |
| Hello, how are you? | Hey there! I'm doing great … | I'm doing great, thanks for asking! … |

Limit: in smaller languages the 1.7B model makes word mistakes (e.g. Dutch "groefsprout", Spanish "puerros (cebollas)"); the language itself is right.

**Language (T-080 fix, 2026-11-03).** LFM3-1.7B follows the language of its system prompt much more than a rule inside it:
with the old English prompt, German messages got English or mixed answers ("Sure! Lauch (leek) eignet sich…").
`ReplyLanguagePolicy` (`:core-llm`) therefore decides:

| `answer_language` | prompt file | language rule (last lines, `# Sprache` / `# Language`) | hint before each answer |
|---|---|---|---|
| `de` | `system_full.de.txt` | „Antworte immer nur auf Deutsch. Misch keine englischen Wörter …“ | `Antworte auf Deutsch.` |
| `en` | `system_full.en.txt` | "Always answer in English only. Never mix in German …" | `Answer in English.` |
| `auto` (default) | device language `de` → DE file, else EN file | "answer in the language of the last message, never mix" | language of the last user message (stop-word detector, umlauts ×2); unclear ("Hi", "ok") → device language |

The hint is a one-line `system` turn **after** the history (`…user<|im_end|>\n<|im_start|>system\nAntworte auf Deutsch.<|im_end|>\n<|im_start|>assistant\n`).
It is never stored, so the cached prompt start and the checkpoint at the end of the history (§1, T-033) stay valid; the next turn re-reads only the last answer.
`user_custom_instructions` holds the "no tools yet" line (until T-062) plus "about me" / "style" from the settings, labelled in the prompt language.
The context heading is `# Context` / `# Kontext` (end of the fixed part = first checkpoint).

Prompt rules for a 1.7B model (learned on the host, 03.11.): short bullet rules, no tool names while no tools are connected
(the old prompt made the model write a `weather` tool call and invent a "[1] Wikipedia" source), and don't *mention* things you
don't want literally: "at most one emoji / nature touch" gave a 😊 under every answer and "Das ist ein kleiner Natur-Hauch für dich".
Sampling stays `chat` (temp 0.1, top_k 50, repeat 1.1): the language problem was the prompt, not the sampler.

Default German prompt (`prompts/system_full.de.txt`; the English file is the same text in English):

```text
Du bist Pim, ein kleiner, freundlicher Spross aus dem Hain. Du bist eine KI in der App Hain und läufst komplett auf diesem Handy. Chats bleiben auf dem Gerät.

# So antwortest du
- Du hilfst bei allem: Schule, Kochen, Alltag, Wissen, Ideen.
- Duze den Nutzer. Klinge warm, natürlich und locker, wie ein netter Freund.
- Antworte meist in 2 bis 5 Sätzen. Bei Erklärungen, Rezepten oder Anleitungen darfst du ausführlicher sein und eine kurze Liste nutzen.
- Fang direkt mit dem Inhalt an, ohne „Hallo“. Nur wenn der Nutzer grüßt, grüß kurz zurück.
- Auf Smalltalk antworte herzlich und stell eine kleine Gegenfrage.
- Nutze einfache Wörter und kurze Sätze. Formatiere sparsam: **fett** für das Wichtigste, Listen mit „-“.
- Keine Floskeln. Wiederhole nicht die Frage.

# Ehrlich bleiben
- Erfinde nichts: keine Fakten, Zahlen, Links oder Zitate. Wenn du unsicher bist, sag das offen.
- Dein Wissen kann veraltet sein. Bei Aktuellem wie Wetter, Nachrichten oder Preisen sag ehrlich, dass du es nicht sicher weißt.
- Gesundheit, Recht, Geld: nur allgemeine Infos, empfiehl Fachleute. In Gefahr: sofort {emergency_number} anrufen.
- Schädliche Bitten lehnst du kurz und freundlich ab.

# Kontext
Jetzt: {weekday}, {date}, {time} ({timezone}). Ort: {city}.
Über den Nutzer: {memory_snippets}
{user_custom_instructions}

# Sprache
{language_rule}
```

## 10. Omni (optional pack, later tasks)

- Text mode: system = Hain prompt + last line `Respond with text.`, greedy.
- Voice mode: system exactly `Respond with interleaved text and audio.`; Hain short prompt prepended to first user message.
- Image input needs mmproj + llama.cpp `mtmd` API → separate native task T-150 (needs stronger model).
