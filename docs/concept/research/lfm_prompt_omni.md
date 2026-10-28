# LFM3 für Hain: Omni oder 1.7B + Pakete, Chat-Template, System-Prompt
Stand: 2026-10-27 (LFM3 ist heute erschienen). Alle Fakten aus den unten verlinkten Quellen; Schlüsse von mir sind als **(Einschätzung)** markiert.

Volle Templates (wörtlich) liegen daneben:
- `/workspace/work/android-app/out/lfm3-1.7b_chat_template.jinja`
- `/workspace/work/android-app/out/lfm3-omni-2b_chat_template.jinja`
- `/workspace/work/android-app/out/lfm3-1.7b_tokenizer_config.json`

---

## 1. LFM3-Omni-2B vs. LFM3-1.7B + LFM3-VL-2B + LFM3-Audio-2B

| | **LFM3-Omni-2B** | **LFM3-1.7B** (+ VL-2B / Audio-2B als Pakete) |
|---|---|---|
| Parameter | 2.17B | 1.73B (VL 1.9B, Audio ~2B) |
| GGUF Q4_K_M | 1.1 GB + mmproj Q8_0 541 MB ≈ **1,65 GB** (Q4_0 1.05 GB) | 1.1 GB (QAD-Q4_0 1.05 GB, auf Q4-Verlust trainiert). VL und Audio je 1.1 GB + eigener mmproj (Größe nicht geprüft) |
| Kontext | **32K** | **256K** (128K Kontext = 1,6 GB KV-Cache) |
| Denken | **Nicht-Reasoning**, `enable_thinking=True` nicht unterstützt; jede Antwort beginnt mit leerem `<think></think>`; llama-server: `--reasoning off` | Reasoning **standardmäßig an**; `enable_thinking=False` geht, laut Karte „kann inkonsistent sein“ |
| Eingaben | Text, Bild, Video (+Tonspur), Audio 16 kHz | nur Text (Bild via VL, Sprache via Audio) |
| Ausgaben | Text, Sprache 24 kHz, interleaved | Text (Audio-2B: Text/Sprache) |
| Textsprachen (offiziell) | EN, AR, ZH, FR, **DE**, IT, JA, KO, PT, ES, VI, TH, ID, HI, RU, PL | gleiche 16 Sprachen |
| Sprach-Locales | 19 inkl. **Deutsch** (TTS weiblich/männlich) | Audio-2B: 18 Sprachen |
| Text-Benchmarks | **Keine Zahlen veröffentlicht**, nur ein Radar (IFEval, GSM8K, MMLU-Pro, BFCL v4, RULER). Blog: „behält die Kernfähigkeiten … inkl. Instruction Following“ | IFEval 96.7, IFBench 73.0, BFCL v4 71.4, ToolSandbox 80.4, Multi-IF 89.1, GPQA-D 63.4; AA-Omniscience: Genauigkeit nur 7 %, aber 92,9 % „non-hallucination“ (sagt eher „weiß nicht“) |
| Tool Calling | gleiches XML-Format; Tools aus Sprache **oder** Text; interne Voice-Tool-Eval 52.2 (Gemma 4 E4B 72.4) | stärkster Teil des Modells (Agenten-RL, Tool-Search/Deferred Tools) |
| Sehen | DocVQA 92.5, OCRBench 806, MMBench 73.7, Video-MME 57.7 | VL-2B klar stärker: Ø 71.4 über 62 Benchmarks (≈ Gemma 4 12B), Doc/OCR/Chart 83.9, mit Denken |
| Hören | EN-ASR WER 6.36, multilingual (inkl. de) 6.72, TTS UTMOSv2 3.80, VoiceBench 64.7 | Audio-2B: EN-WER 6.2, multilingual 6.0, VoiceBench 73.1, Tool-Calling 58.5 |
| Latenz | Audio-Encoder kausal → Prefill während man spricht, TTFT „ab 50 ms“; LiquidCodec 87× Echtzeit (1 CPU-Thread, M4 Max), streamend | Text: laut Liquid schnellstes Modell seiner Klasse (≥1,8× Decode, ≥2,5× Prefill bei 32K vs. MiniCPM5-2B/Qwen3.5-4B/Gemma 4 E4B, Q4_0, llama.cpp, inkl. iPhone 17 Pro). **Keine Android-Zahlen** veröffentlicht |
| llama.cpp | PR **#31257** (Audio+Omni) **am 27.10.2026 gemergt** (GGUF-Karte sagt noch „pending“ – veraltet). In einem getaggten Release noch nicht geprüft. **Video** und **Live-ASR für Omni**: Folge-PR. `/v1/realtime` (WebSocket-Voice) „coming soon“ | Text: „LFM3-kompatibler Build nötig“ (Merge-Status nicht geprüft). VL: PR **#31252 in Review**, ohne Video. Audio: in #31257 gemergt; Live-ASR für Audio im Folge-PR |
| Lizenz | LFM Open License v1.0 (Apache-2.0-Basis): kommerziell gratis unter **10 Mio. USD Jahresumsatz**, kein Copyleft, Lizenz/NOTICE mitliefern, Kündigung bei jedem Verstoß | gleich |

**Omni – Vorteile:** ein Download (~1,65 GB statt ~3,3 GB+ für drei Modelle), kein Modell-Wechsel/Entladen zwischen Foto, Sprache und Chat, gesprochene Frage zu einem Foto in einem Schritt, deutsche ASR+TTS eingebaut, keine Denk-Tokens (schneller zur ersten Antwort).
**Omni – Nachteile:** nur 32K Kontext, kein Reasoning, keine veröffentlichten Text-/Tool-Zahlen (Text-Qualität muss selbst gemessen werden), schwächer als VL-2B beim Sehen und als Audio-2B bei VoiceBench, Video/Live-ASR in llama.cpp noch nicht da, **Sprachmodus verlangt einen festen System-Prompt** (siehe 2.7), d. h. die Hain-Persona muss dann in die User-Nachricht.
**(Einschätzung)** Für Hain: LFM3-1.7B als „Gehirn“ behalten (Tools, Gedächtnis, langer Kontext sind der Kern) und Omni als **ein** optionales „Sehen+Sprechen“-Paket statt VL+Audio einzeln anbieten. Erst wenn der eigene deutsche Test (Konzept Phase 0) zeigt, dass Omni im Text-Chat mit Tools mithält, ganz auf Omni umstellen.

---

## 2. Chat-Template (aus `chat_template.jinja`, wörtlich)

### 2.1 Spezial-Tokens
- BOS `<|startoftext|>` (wird vom Template immer vorangestellt: `{{- bos_token -}}`), EOS `<|im_end|>`, PAD `<|pad|>` (tokenizer_config.json).
- Rollen: `<|im_start|>system`, `user`, `assistant`, `tool` … `<|im_end|>`.
- Medien: `<|vision_start|><|image|><|vision_end|>`, `<|vision_start|><|video|><|vision_end|>`; Omni zusätzlich `<|vision_start|><|audio|><|vision_end|>` und `<|audio_out|>`. Omni-Ausgabe beginnt mit `<|audio_start|>` (Sprache) bzw. `<|mixed_start|>` (interleaved).
- **Keine** Tokens `<|tool_call_start|>`, `<|tool_list_start|>` usw. mehr – das war LFM2/LFM2.5.

### 2.2 Standard-System-Prompt
**Keiner.** Das Template fügt nur einen System-Turn ein, wenn ein System-Prompt oder Tools übergeben werden. Das Beispiel auf der Modellkarte („You are a helpful assistant trained by Liquid AI.“) ist nur ein Beispiel. Omni-Karte: „The system turn is optional“.

Beispiel (Modellkarte LFM3-1.7B, wörtlich):
```
<|startoftext|><|im_start|>system
You are a helpful assistant trained by Liquid AI.<|im_end|>
<|im_start|>user
Explain how a heat pump works.<|im_end|>
<|im_start|>assistant
<think>
```

### 2.3 Wie Tools deklariert werden (wörtlich aus dem Template)
```
{%- if tools -%}
    ...
        {%- if not _dyn -%}
            {%- set tools_ns.lines = tools_ns.lines + [tool_to_json(fn)] -%}
    ...
    {%- if tools_ns.lines -%}
        {%- set ns.system_prompt = ns.system_prompt + ("\n\n" if ns.system_prompt else "") + "<tools>\n" + (tools_ns.lines | join("\n")) + "\n</tools>" -%}
```
→ Eigener System-Prompt, dann Leerzeile, dann `<tools>` mit **einer JSON-Zeile pro Tool** (nur das `function`-Objekt: name, description, parameters; `defer_loading` wird entfernt), dann `</tools>`. **Kein** „List of tools: [...]“ (LFM2-Format).
Ergebnis (Liquid-Doku):
```
<|startoftext|><|im_start|>system
<tools>
{"name": "get_candidate_status", "description": "...", "parameters": {...}}
</tools><|im_end|>
```
Tools mit `"defer_loading": true` bleiben draußen und kommen erst über ein `search_tools`-Tool als `tool_reference` in `<additional_tools>…</additional_tools>` in die Tool-Antwort.

### 2.4 Tool-Aufruf-Format (XML, „Obermenge von qwen3_coder“)
```
<tool_call>
<function=get_weather>
<parameter=city>
Paris
</parameter>
</function>
</tool_call>
```
Mehrere Aufrufe getrennt durch Leerzeile. Nicht-String-Werte als JSON. Im Verlauf: assistant-Nachricht mit `tool_calls`, `arguments` als **Dict** (JSON-String → Template wirft Fehler).

### 2.5 Tool-Antwort
Rolle **`tool`**, vom Template eingewickelt:
```
<|im_start|>tool
<tool_response>
{"city": "Edinburgh", "condition": "light rain", "temperature_c": 11}
</tool_response><|im_end|>
```

### 2.6 Denken
- Template-Variablen: `enable_thinking` (1.7B: Default **true** → Generation beginnt mit `<think>`; false → `<think></think>`), `preserve_thinking` (Default false: Denken aus Turns vor der letzten User-Nachricht wird entfernt).
- Omni: Default `enable_thinking=false`, Reasoning nicht unterstützt.
- **Keine** `/think` / `/no_think`-Schalter im Template oder in der Doku gefunden.
- Bei Tool-Schleifen: Content + Reasoning des **aktuellen** Tool-Turns im Verlauf behalten.

### 2.7 Omni: System-Prompt steuert die Ausgabeart
- Text: `Respond with text.` / Sprache+Text: `Respond with interleaved text and audio.` (Ausgabe-Anweisung **ans Ende** des System-Prompts).
- Doku Spoken Chat: „Keep the system prompt **exactly** as shown, and give any further instructions in the user turn.“ → Für Hains Sprachmodus: System = genau dieser Satz, Persona/Regeln als Präfix in die User-Nachricht (oder: Text-Modus + separates TTS).
- Feste Aufgaben-Prompts: `Perform ASR in German.`, `Perform TTS in German using a female voice.`, `Please translate the following spoken German into English text.`, Bild: `Answer questions about the provided image. Base every answer only on what can be seen.`

### 2.8 Sampling
- **LFM3-1.7B allgemein** (Chat, Tools, Agenten): `temperature=0.1, top_k=50, repetition_penalty=1.1` (llama.cpp: `--temp 0.1 --top-k 50 --repeat-penalty 1.1`). Mathe/STEM: `temperature=1.0, top_p=0.95, top_k=20`. Kein min_p empfohlen.
- **LFM3-VL-2B**: `--temp 0.1 --top-p 1.0 --top-k 50 --min-p 0.0 --repeat-penalty 1.1 --no-reasoning-preserve`.
- **LFM3-Omni-2B**: Text **greedy**; Sprache `audio_temperature=0.4, audio_repetition_penalty=1.2` (Doku zusätzlich `audio_repetition_window=64`; in der GGUF hinterlegt).

---

## 3. System-Prompts für 1–2B-Modelle

**Was Liquid selbst sagt (dünn):** System-Prompt optional; Tool-Beschreibungen kurz und klar, nur relevante Tools laden, bei vielen Tools Deferred Loading + im System-Prompt sagen, *wann* gesucht werden soll; Omni: Ausgabe-Anweisung ans Ende. LFM3-1.7B ist „nicht für komplexes mehrstufiges Denken“ und soll für Wissen Suche/Retrieval nutzen. RL mit Längenstrafe → antwortet von sich aus knapp. Kein offizieller Leitfaden zu Länge oder Prompt-Sprache gefunden.

**Praxisregeln (Einschätzung, allgemeine Erfahrung mit kleinen Modellen):**
1. 250–500 Tokens. Jede Regel kostet Aufmerksamkeit; lange Gedächtnisblöcke verschlechtern die Befolgung.
2. Kurze Abschnitte mit Überschriften und Stichpunkten, Imperativ, positive Formulierung („antworte in …“ statt „nie …“), eine Regel pro Zeile.
3. Wichtigste Regeln (Sprache, Werkzeug statt Raten) oben; dynamische Teile (Datum, Gedächtnis) **ganz unten**, damit der statische Anfang im KV-Cache wiederverwendet wird (Liquid nennt den stabilen Präfix ausdrücklich als Vorteil).
4. Tool-Schemas **nicht** im Text wiederholen – das Template hängt sie an. Im Prompt nur *wann* welches Tool.
5. Prompt-Sprache: Englischer Prompt + harte Regel „antworte in der Sprache der letzten Nachricht“ ist der übliche, robuste Weg; ein deutscher Prompt kann das Modell bei englischen Fragen ins Deutsche ziehen. **Beides A/B testen** mit den 50–100 deutschen Testfragen aus Phase 0 (Kurzvariante unten auch auf Deutsch).
6. Keine Few-Shot-Beispiele im Dauer-Prompt (Tokens); wenn nötig nur für Karten-Syntax, besser per GBNF-Grammatik erzwingen.
7. Persona dezent: ein Satz Ton, keine Rollenspiel-Anweisungen.

---

## 4. Empfohlener System-Prompt für Hain (≈470–520 Tokens, 2.100 Zeichen; mit echtem Tokenizer nachzählen)
Platzhalter `{…}` füllt die App. Kartenregel passt zum Konzept (OpenUI-Teilmenge, per GBNF erzwungen).

```
You are Hain, a friendly helper that runs fully on this phone. Chats stay on the device. Only tools that need the internet send a search query out.

# Language
- Always reply in the language of the user's last message (German or English).
- In German, use "du" and simple everyday words. Short sentences. No jargon.

# Style
- Be warm, calm and to the point: 1-4 sentences or a short list.
- You are a small, kind grove spirit. Show it lightly, at most one small nature touch per chat. Never let it get in the way.
- No filler phrases. Don't repeat the question.

# Tools
- Your own knowledge is limited and can be outdated. If a fact matters and you are not sure, use a tool first. Never invent facts, numbers, links or quotes.
- web_search: current info, prices, opening hours, anything recent. news: headlines and recent events. weather: weather and forecasts. wikipedia: facts about people, places, things.
- alarm, timer, calendar: when the user wants to set, change or check one. If a needed detail is missing (time, date), ask one short question. Use "Now" below for "tomorrow", "in 10 minutes" etc.
- memory: save only what the user asks you to remember or a clear, lasting preference. Never save health details, passwords or bank data unless asked. Use memory to recall facts about the user.
- After a tool result: answer from it and cite sources as [1], [2]. The app shows a card with the data, so add only the key point.
- If a tool fails or finds nothing, say so plainly and offer the next step.

# Cards
- If a checklist, steps, a table or a comparison clearly helps, answer with a card (Card, List, Table, Checklist, Button, Progress, Image, Chart). Otherwise use plain text.

# Care and honesty
- Health, legal or money questions: give general, careful information, say it does not replace a doctor, lawyer or advisor. In danger, tell the user to call {emergency_number} now.
- Say clearly when you don't know something or can't do it. You are an AI, not a person.
- Decline harmful requests briefly and kindly.

# Context
Now: {weekday}, {date}, {time} ({timezone}). Place: {city}.
About the user: {memory_snippets}
```

## 5. Kurzvariante (≈170–190 Tokens)

```
You are Hain, a friendly helper running fully on this phone.
- Reply in the language of the user's last message. In German use "du" and simple words.
- Be short and warm: 1-3 sentences or a short list.
- If you are not sure about a fact, use a tool (web_search, news, weather, wikipedia) instead of guessing. Never invent facts or links. Cite sources as [1].
- For alarms, timers and calendar entries use the matching tool; ask one question if the time or date is missing.
- Save to memory only what the user wants remembered.
- Health, legal, money: general info only, recommend a professional; emergency: {emergency_number}.
- Say plainly when you don't know or can't do something. You are an AI.
Now: {date}, {time}. Place: {city}. User: {memory_snippets}
```

Deutsche Kurzvariante für den A/B-Test:
```
Du bist Hain, ein freundlicher Helfer, der komplett auf diesem Handy läuft.
- Antworte in der Sprache der letzten Nachricht (Deutsch oder Englisch). Auf Deutsch: duzen, einfache Wörter.
- Kurz und herzlich: 1–3 Sätze oder eine kurze Liste.
- Bist du bei einem Fakt unsicher, nutze ein Werkzeug (web_search, news, weather, wikipedia), statt zu raten. Erfinde nichts. Quellen als [1].
- Wecker, Timer, Termine: passendes Werkzeug nutzen; fehlt Uhrzeit oder Datum, eine kurze Rückfrage.
- Ins Gedächtnis nur, was der Nutzer behalten haben will.
- Gesundheit, Recht, Geld: nur allgemeine Infos, Fachleute empfehlen; Notfall: {emergency_number}.
- Sag ehrlich, wenn du etwas nicht weißt oder nicht kannst. Du bist eine KI.
Jetzt: {date}, {time}. Ort: {city}. Nutzer: {memory_snippets}
```

**Omni-Sprachmodus:** System-Prompt exakt `Respond with interleaved text and audio.`; die Kurzvariante als erste Zeilen der User-Nachricht voranstellen (Omni-Doku). Für Omni-Text-Chat: Hain-Prompt + am Ende `Respond with text.`

---

## Offene Punkte
- Text-/Tool-Qualität von Omni ist nicht veröffentlicht → selbst messen.
- Keine Android-Geschwindigkeits- oder RAM-Messungen von Liquid; KV-Cache-Bedarf von Omni unbekannt.
- Ob #31257 (Omni/Audio) und die LFM3-Text-Unterstützung schon in einem getaggten llama.cpp-Release stecken, nicht geprüft; VL (#31252) noch in Review.
- mmproj-Größen von VL-2B und Audio-2B nicht geprüft.
- Kein offizieller Liquid-Rat zu Prompt-Sprache/Länge; Abschnitt 3 Punkte 1–7 sind Einschätzung.

## Quellen
- https://www.liquid.ai/blog/lfm3
- https://huggingface.co/blog/LiquidAI/lfm3
- https://huggingface.co/LiquidAI/LFM3-1.7B · https://huggingface.co/LiquidAI/LFM3-1.7B-GGUF
- https://huggingface.co/LiquidAI/LFM3-Omni-2B · https://huggingface.co/LiquidAI/LFM3-Omni-2B-GGUF (Dateiliste: /tree/main)
- https://huggingface.co/LiquidAI/LFM3-VL-2B-GGUF · https://huggingface.co/LiquidAI/LFM3-Audio-2B-GGUF
- https://huggingface.co/LiquidAI/LFM3-1.7B/raw/main/chat_template.jinja · https://huggingface.co/LiquidAI/LFM3-Omni-2B/raw/main/chat_template.jinja · tokenizer_config.json
- https://docs.liquid.ai/lfm/key-concepts/tool-use · https://docs.liquid.ai/lfm/key-concepts/omni-capabilities · https://docs.liquid.ai/lfm/key-concepts/text-generation-and-prompting · https://docs.liquid.ai/lfm/help/model-license
- https://github.com/ggml-org/llama.cpp/pull/31257 (merged 2026-10-27) · PR #31252 (VL, in Review, laut VL-GGUF-Karte)
