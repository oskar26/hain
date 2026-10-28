# AGENT_PROMPTS.md — Prompt + Modell pro Aufgabe

Generiert von `tools/agent_prompts.py` aus `tools/tasks_src.py`. Nach Änderungen an den Aufgaben neu erzeugen:
`python3 tools/agent_prompts.py > docs/workflow/AGENT_PROMPTS.md`

Reihenfolge: Aufgaben der Reihe nach, nur wenn alle `depends` in TASKS.md `done` sind. Lane H = du selbst oder stärkstes Modell.

## Schwierigkeitsstufen

- **S · einfach:** scaffold, strings, ui und test in Größe XS/S. Lokal reicht.
- **M · mittel:** feature (reines Kotlin) und alles in Größe M.
- **L · schwer:** native (JNI, CMake, NDK), plan, Lane H. Stärkstes Modell oder du.

## Modelle und Thinking-Level

| Modell | S · einfach | M · mittel | L · schwer |
|---|---|---|---|
| Lokal: Qwen3.6-35B-A3B `qwen-moe` (pi) | Thinking medium | Thinking medium | - |
| Lokal: FrogNano-4B `frognano` (pi) | Thinking medium | - | - |
| Gemini 3.8 Flash | low | medium | high |
| Gemini 4 Argon | low | medium | high |
| DeepSeek Flash (V4.1, `deepseek-flash`) | low | high | max |
| DeepSeek V4-Pro | - | high | max |
| Claude Haiku 5.5 | low | medium | - |
| Claude Sonnet 5.5 | low | medium | high |
| Claude Opus 5.5 | - | medium | xhigh |
| GPT-6 Luna | low | medium | - |
| GPT-6.1 Sol | low | medium | xhigh |
| GPT-6 Astra | - | low | medium |

Hinweise:

- Gemini 3.8 Flash und Gemini 4 Argon: kein `minimal` (Fehler). Standard ist medium.
- DeepSeek: `none` schaltet Thinking aus, `medium` wird zu `high` gemappt. Laut DeepSeek ist V4.1-Flash stärker als V4-Pro.
- Claude/GPT: über `xhigh` bringt `max` beim Coden praktisch nichts, dauert aber viel länger.
- Lokal läuft immer nur EIN Modell (llama.cpp-Router, `--models-max 1`). Wechsel per `/model` in pi. Details: LOCAL_MODELS.md.
- Qwen3.6-35B-A3B lokal: Reasoning-Budget 12k ist gesetzt. Tiel-Coder wird nicht mehr genutzt.
- FrogNano-4B: bester kleiner Coding-Agent (Microsoft, unabhängig geprüfte Werte), aber auf Python trainiert. Für Kotlin nur kleine, klare Aufgaben.
- Spark-X2.6-4B: verworfen (Benchmarks unbelegt), FrogNano ist der bessere kleine.
- Lokale Modelle einrichten: `bash tools/setup/setup-local-agent.sh` (Router + pi + Projekt-Befehle /task, /next, /review).
- Reviewer immer ein **anderes** Modell als der Umsetzer.
- Hängt ein Modell zweimal am selben Fehler: eine Stufe höher (anderes Modell oder höheres Level).

## Reviewer-Prompt (nach jeder Aufgabe)

Modell: anderes als Umsetzer · Level: eine Stufe unter der Aufgabe, mindestens medium.

```
Read AGENTS.md. Review task {T-XXX}: compare the diff (git show) with the task card in
docs/workflow/TASKS.md and the spec sections it lists. Run its check command and
./gradlew testDebugUnitTest. Check: only listed files changed, no hard-coded strings,
DE+EN strings present, no material3 imports, no network call outside NetworkGate.
If ok: set status `done`. If not: set `todo`, add a "Review notes" bullet list under the
task card, and log it in HANDOFF_LOG.md.
```

## Planer-Prompt (alle ~10 Aufgaben)

Modell: Claude Opus 5.5 · xhigh, GPT-6.1 Sol · xhigh oder Gemini 3.8 Flash · high.

```
Read AGENTS.md, STATUS.md, the last 5 HANDOFF_LOG.md entries and open questions.
Split any task that is blocked for being too big (size M → two S).
Update tools/tasks_src.py, regenerate TASKS.md keeping existing statuses,
answer or escalate questions, and update STATUS.md "Now". Do not write feature code.
```

## M0 · Device check & setup (human)

### T-000 · Benchmark LFM3 on own phone

- Stufe: **L · schwer** · type `plan` · size `S` · lane `H` · depends -
- Empfohlen: **Claude Opus 5.5 · xhigh / GPT-6.1 Sol · xhigh** · Alternative: Gemini 3.8 Flash · high

Menschliche Aufgabe: selbst erledigen (oder mit stärkstem Modell als Helfer).

### T-001 · Create Git repo, add llama.cpp submodule

- Stufe: **S · einfach** · type `scaffold` · size `XS` · lane `A` · depends -
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-001 (Create Git repo, add llama.cpp submodule).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

## M1 · Project skeleton

### T-002 · Gradle multi-module skeleton

- Stufe: **M · mittel** · type `scaffold` · size `M` · lane `A` · depends T-001
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-002 (Gradle multi-module skeleton).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-003 · Lint & static checks

- Stufe: **S · einfach** · type `scaffold` · size `XS` · lane `A` · depends T-002
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-003 (Lint & static checks).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-004 · Hilt + App class + MainActivity

- Stufe: **S · einfach** · type `scaffold` · size `S` · lane `A` · depends T-002
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-004 (Hilt + App class + MainActivity).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-005 · Strings DE/EN + checker script

- Stufe: **S · einfach** · type `strings` · size `S` · lane `A` · depends T-002
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-005 (Strings DE/EN + checker script).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-006 · CI workflow

- Stufe: **S · einfach** · type `scaffold` · size `XS` · lane `A` · depends T-003,T-005
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-006 (CI workflow).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

## M2 · Design system & Pim

### T-010 · Design tokens

- Stufe: **S · einfach** · type `ui` · size `S` · lane `B` · depends T-002
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-010 (Design tokens).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-011 · Components: HainText, HainSurface, HainButton, HainIconButton

- Stufe: **S · einfach** · type `ui` · size `S` · lane `B` · depends T-018,T-019
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-011 (Components: HainText, HainSurface, HainButton, HainIconButton).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-012 · Components: HainTextField, Composer

- Stufe: **S · einfach** · type `ui` · size `S` · lane `B` · depends T-018,T-019
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-012 (Components: HainTextField, Composer).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-013 · Components: UserBubble, AssistantBlock (markdown via renderer core), ReasoningBlock

- Stufe: **M · mittel** · type `ui` · size `M` · lane `B` · depends T-018,T-019
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-013 (Components: UserBubble, AssistantBlock (markdown via renderer core), ReasoningBlock).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-014 · Components: HainChip, ToolChip, HainCard, SegmentedControl

- Stufe: **S · einfach** · type `ui` · size `S` · lane `B` · depends T-018,T-019
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-014 (Components: HainChip, ToolChip, HainCard, SegmentedControl).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-015 · Components: SettingsRow, HainSwitch, TopBar, Drawer, BottomSheet, Toast, EmptyState, ProgressBar

- Stufe: **S · einfach** · type `ui` · size `S` · lane `B` · depends T-018,T-019
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-015 (Components: SettingsRow, HainSwitch, TopBar, Drawer, BottomSheet, Toast, EmptyState, ProgressBar).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-018 · Add UI libraries + thin wrappers

- Stufe: **S · einfach** · type `scaffold` · size `S` · lane `B` · depends T-010
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-018 (Add UI libraries + thin wrappers).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-019 · Motion system HainMotion + MotionGovernor

- Stufe: **S · einfach** · type `ui` · size `S` · lane `B` · depends T-018
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-019 (Motion system HainMotion + MotionGovernor).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-016 · Pim v2 drawn in Compose (static states)

- Stufe: **S · einfach** · type `ui` · size `S` · lane `B` · depends T-010
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-016 (Pim v2 drawn in Compose (static states)).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-017 · Lucide icons as vector drawables

- Stufe: **S · einfach** · type `ui` · size `S` · lane `B` · depends T-010
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-017 (Lucide icons as vector drawables).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

## M3 · LLM core (native + prompt)

### T-020 · Encrypted Room DB skeleton

- Stufe: **M · mittel** · type `feature` · size `M` · lane `C` · depends T-002
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-020 (Encrypted Room DB skeleton).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-030 · JNI bridge: load/generate/cancel

- Stufe: **L · schwer** · type `native` · size `M` · lane `C` · depends T-001,T-002
- Empfohlen: **Claude Opus 5.5 · xhigh / GPT-6.1 Sol · xhigh** · Alternative: Gemini 3.8 Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-030 (JNI bridge: load/generate/cancel).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-031 · Copy prompts into assets

- Stufe: **S · einfach** · type `scaffold` · size `XS` · lane `C` · depends T-002
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-031 (Copy prompts into assets).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-032 · LlmEngine implementation

- Stufe: **M · mittel** · type `feature` · size `S` · lane `C` · depends T-030
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-032 (LlmEngine implementation).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-033 · Prefix KV cache reuse

- Stufe: **L · schwer** · type `native` · size `S` · lane `C` · depends T-030
- Empfohlen: **Claude Opus 5.5 · xhigh / GPT-6.1 Sol · xhigh** · Alternative: Gemini 3.8 Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-033 (Prefix KV cache reuse).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-040 · Data classes ChatMessage/ToolSchema/ToolCall

- Stufe: **M · mittel** · type `feature` · size `XS` · lane `D` · depends T-002
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-040 (Data classes ChatMessage/ToolSchema/ToolCall).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-041 · Golden prompt files + render script

- Stufe: **S · einfach** · type `test` · size `S` · lane `D` · depends T-040
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-041 (Golden prompt files + render script).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-042 · PromptBuilder (pure Kotlin)

- Stufe: **M · mittel** · type `feature` · size `M` · lane `D` · depends T-041
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-042 (PromptBuilder (pure Kotlin)).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-043 · ToolCallParser

- Stufe: **M · mittel** · type `feature` · size `S` · lane `D` · depends T-040
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-043 (ToolCallParser).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-044 · StreamSplitter

- Stufe: **M · mittel** · type `feature` · size `S` · lane `D` · depends T-043
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-044 (StreamSplitter).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-045 · ThinkingPolicy + SystemPrompt.fill

- Stufe: **M · mittel** · type `feature` · size `S` · lane `D` · depends T-040
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-045 (ThinkingPolicy + SystemPrompt.fill).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

## M4 · Chat MVP (first usable app)

### T-050 · ChatEngine turn loop

- Stufe: **M · mittel** · type `feature` · size `M` · lane `E` · depends T-032,T-042,T-044,T-045,T-020
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-050 (ChatEngine turn loop).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-051 · ChatViewModel + UI state

- Stufe: **M · mittel** · type `feature` · size `S` · lane `E` · depends T-050
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-051 (ChatViewModel + UI state).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-052 · Chat screen

- Stufe: **M · mittel** · type `ui` · size `M` · lane `E` · depends T-051,T-011,T-012,T-013
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-052 (Chat screen).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-057 · Branches, message queue, stopped/error states

- Stufe: **M · mittel** · type `feature` · size `M` · lane `E` · depends T-051
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-057 (Branches, message queue, stopped/error states).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-058 · ChatViewport + scroll-to-bottom pill

- Stufe: **S · einfach** · type `ui` · size `S` · lane `E` · depends T-052
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-058 (ChatViewport + scroll-to-bottom pill).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-059 · ToolTimeline + SourceChips

- Stufe: **S · einfach** · type `ui` · size `S` · lane `E` · depends T-052,T-062
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-059 (ToolTimeline + SourceChips).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-053 · Drawer: chat list, search, rename, delete, pin

- Stufe: **M · mittel** · type `ui` · size `M` · lane `E` · depends T-052,T-015
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-053 (Drawer: chat list, search, rename, delete, pin).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-054 · Export & share chat

- Stufe: **M · mittel** · type `feature` · size `S` · lane `E` · depends T-053
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-054 (Export & share chat).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-055 · Model picker in top bar

- Stufe: **S · einfach** · type `ui` · size `S` · lane `E` · depends T-052,T-081
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-055 (Model picker in top bar).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-056 · Private chat + app lock

- Stufe: **M · mittel** · type `feature` · size `S` · lane `E` · depends T-053
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-056 (Private chat + app lock).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

## M5 · Models & onboarding

### T-080 · Fill model catalogue with real URLs + sha256

- Stufe: **L · schwer** · type `plan` · size `S` · lane `F` · depends -
- Empfohlen: **Claude Opus 5.5 · xhigh / GPT-6.1 Sol · xhigh** · Alternative: Gemini 3.8 Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-080 (Fill model catalogue with real URLs + sha256).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-081 · Catalog + DeviceCheck

- Stufe: **M · mittel** · type `feature` · size `S` · lane `F` · depends T-002
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-081 (Catalog + DeviceCheck).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-082 · PackDownloader (WorkManager)

- Stufe: **M · mittel** · type `feature` · size `M` · lane `F` · depends T-081
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-082 (PackDownloader (WorkManager)).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-083 · Onboarding (welcome, language, download brain)

- Stufe: **M · mittel** · type `ui` · size `M` · lane `F` · depends T-082,T-016
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-083 (Onboarding (welcome, language, download brain)).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-084 · Packs screen in settings

- Stufe: **S · einfach** · type `ui` · size `S` · lane `F` · depends T-082
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-084 (Packs screen in settings).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

## M6 · Tools & internet

### T-060 · NetworkGate + network log

- Stufe: **M · mittel** · type `feature` · size `S` · lane `A` · depends T-020
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-060 (NetworkGate + network log).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-061 · Pim launcher icon + VectorDrawable export

- Stufe: **S · einfach** · type `ui` · size `S` · lane `B` · depends T-016
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-061 (Pim launcher icon + VectorDrawable export).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-062 · ToolRouter + schemas

- Stufe: **M · mittel** · type `feature` · size `S` · lane `A` · depends T-040,T-060
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-062 (ToolRouter + schemas).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-063 · Calculator + units

- Stufe: **M · mittel** · type `feature` · size `S` · lane `A` · depends T-062
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-063 (Calculator + units).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-064 · Weather tool + WeatherCard

- Stufe: **M · mittel** · type `feature` · size `M` · lane `A` · depends T-062,T-014
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-064 (Weather tool + WeatherCard).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-065 · Wikipedia tool + WikiCard

- Stufe: **M · mittel** · type `feature` · size `S` · lane `A` · depends T-062
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-065 (Wikipedia tool + WikiCard).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-066 · SearchRouter: SearXNG + DDG html + fallback

- Stufe: **M · mittel** · type `feature` · size `M` · lane `A` · depends T-062
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-066 (SearchRouter: SearXNG + DDG html + fallback).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-067 · read_page with Readability4K

- Stufe: **M · mittel** · type `feature` · size `S` · lane `A` · depends T-066
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-067 (read_page with Readability4K).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-068 · Intent tools (alarm, timer, calendar, navigate) + ConfirmCard

- Stufe: **M · mittel** · type `feature` · size `M` · lane `A` · depends T-062,T-014
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-068 (Intent tools (alarm, timer, calendar, navigate) + ConfirmCard).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-069 · Online indicator & network log screen

- Stufe: **S · einfach** · type `ui` · size `S` · lane `A` · depends T-060
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-069 (Online indicator & network log screen).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

## M7 · News (ADR-14)

### T-070 · Feed parser RSS2/Atom/RDF

- Stufe: **M · mittel** · type `feature` · size `S` · lane `B` · depends T-002
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-070 (Feed parser RSS2/Atom/RDF).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-071 · Standard feed list + repository

- Stufe: **M · mittel** · type `feature` · size `S` · lane `B` · depends T-070,T-060
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-071 (Standard feed list + repository).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-072 · news tool + NewsCard

- Stufe: **M · mittel** · type `feature` · size `S` · lane `B` · depends T-071,T-062
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-072 (news tool + NewsCard).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-073 · News screen (3-mode switch, own feeds)

- Stufe: **M · mittel** · type `ui` · size `M` · lane `B` · depends T-072
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-073 (News screen (3-mode switch, own feeds)).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-074 · Article summary on demand

- Stufe: **M · mittel** · type `feature` · size `S` · lane `B` · depends T-073,T-067
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-074 (Article summary on demand).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

## M8 · Memory

### T-110 · Embedder (LFM2.5-Embedding via llama.cpp)

- Stufe: **L · schwer** · type `native` · size `S` · lane `C` · depends T-030
- Empfohlen: **Claude Opus 5.5 · xhigh / GPT-6.1 Sol · xhigh** · Alternative: Gemini 3.8 Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-110 (Embedder (LFM2.5-Embedding via llama.cpp)).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-111 · Memory tables + DAO

- Stufe: **M · mittel** · type `feature` · size `S` · lane `C` · depends T-020
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-111 (Memory tables + DAO).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-112 · Explicit remember/forget + remember tool

- Stufe: **M · mittel** · type `feature` · size `S` · lane `C` · depends T-111,T-062
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-112 (Explicit remember/forget + remember tool).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-113 · Retrieval & ranking

- Stufe: **M · mittel** · type `feature` · size `S` · lane `C` · depends T-111
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-113 (Retrieval & ranking).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-114 · Background extraction worker

- Stufe: **M · mittel** · type `feature` · size `M` · lane `C` · depends T-112,T-113,T-110
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-114 (Background extraction worker).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-115 · Memory screen

- Stufe: **M · mittel** · type `ui` · size `M` · lane `C` · depends T-113
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-115 (Memory screen).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

## M9 · Documents & OCR

### T-120 · PDF text layer + page render

- Stufe: **M · mittel** · type `feature` · size `S` · lane `D` · depends T-002
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-120 (PDF text layer + page render).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-121 · PP-OCRv5 Latin via ONNX Runtime

- Stufe: **L · schwer** · type `native` · size `M` · lane `D` · depends T-120
- Empfohlen: **Claude Opus 5.5 · xhigh / GPT-6.1 Sol · xhigh** · Alternative: Gemini 3.8 Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-121 (PP-OCRv5 Latin via ONNX Runtime).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-122 · GermanOcrFixer

- Stufe: **M · mittel** · type `feature` · size `S` · lane `D` · depends T-002
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-122 (GermanOcrFixer).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-123 · Chunking + doc search tool + attach in chat

- Stufe: **M · mittel** · type `feature` · size `M` · lane `D` · depends T-121,T-122,T-062
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-123 (Chunking + doc search tool + attach in chat).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

## M10 · Cards, voice, assistant

### T-130 · Card parser

- Stufe: **M · mittel** · type `feature` · size `S` · lane `E` · depends T-002
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-130 (Card parser).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-131 · Card renderer + GBNF

- Stufe: **M · mittel** · type `ui` · size `M` · lane `E` · depends T-130,T-014
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-131 (Card renderer + GBNF).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-140 · STT Moonshine + mic button

- Stufe: **M · mittel** · type `feature` · size `M` · lane `F` · depends T-082
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-140 (STT Moonshine + mic button).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-141 · TTS Piper + read aloud

- Stufe: **M · mittel** · type `feature` · size `M` · lane `F` · depends T-082
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-141 (TTS Piper + read aloud).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-143 · German Pim voice on device (owner-chosen engine)

- Stufe: **L · schwer** · type `native` · size `M` · lane `F` · depends T-082
- Empfohlen: **Claude Opus 5.5 · xhigh / GPT-6.1 Sol · xhigh** · Alternative: Gemini 3.8 Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-143 (German Pim voice on device (owner-chosen engine)).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-142 · Voice mode screen

- Stufe: **M · mittel** · type `ui` · size `M` · lane `F` · depends T-140,T-141,T-016
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-142 (Voice mode screen).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-143 · Default assistant + screen context

- Stufe: **M · mittel** · type `feature` · size `M` · lane `F` · depends T-052
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-143 (Default assistant + screen context).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-144 · Widget + quick settings tile + share target

- Stufe: **M · mittel** · type `feature` · size `S` · lane `F` · depends T-052
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-144 (Widget + quick settings tile + share target).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

## M11 · Settings, images, polish

### T-100 · Settings DataStore + screens

- Stufe: **M · mittel** · type `feature` · size `M` · lane `B` · depends T-015
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-100 (Settings DataStore + screens).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-101 · View/edit system prompt, custom instructions

- Stufe: **S · einfach** · type `ui` · size `S` · lane `B` · depends T-100,T-045
- Empfohlen: **Lokal `frognano` (bei 2× Scheitern `qwen-moe`)** · Alternative: Gemini 3.8 Flash · low

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-101 (View/edit system prompt, custom instructions).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-160 · Image edit kit

- Stufe: **M · mittel** · type `feature` · size `M` · lane `D` · depends T-082
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-160 (Image edit kit).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-090 · German/English eval set + runner

- Stufe: **L · schwer** · type `test` · size `M` · lane `H` · depends T-042,T-062
- Empfohlen: **Claude Opus 5.5 · xhigh / GPT-6.1 Sol · xhigh** · Alternative: Gemini 3.8 Flash · high

Menschliche Aufgabe: selbst erledigen (oder mit stärkstem Modell als Helfer).

### T-150 · Multimodal: Omni/VL image input via mtmd

- Stufe: **L · schwer** · type `native` · size `M` · lane `C` · depends T-030
- Empfohlen: **Claude Opus 5.5 · xhigh / GPT-6.1 Sol · xhigh** · Alternative: Gemini 3.8 Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-150 (Multimodal: Omni/VL image input via mtmd).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-163 · Pim animated (Compose: blink, breathe, squash, eye tracking)

- Stufe: **M · mittel** · type `ui` · size `M` · lane `B` · depends T-061,T-019
- Empfohlen: **Lokal `qwen-moe` · medium** · Alternative: Gemini 3.8 Flash · medium / DeepSeek Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-163 (Pim animated (Compose: blink, breathe, squash, eye tracking)).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-170 · Image generation pack (SD1.5+DMD2, MNN)

- Stufe: **L · schwer** · type `native` · size `M` · lane `D` · depends T-160
- Empfohlen: **Claude Opus 5.5 · xhigh / GPT-6.1 Sol · xhigh** · Alternative: Gemini 3.8 Flash · high

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: T-170 (Image generation pack (SD1.5+DMD2, MNN)).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

### T-190 · Accessibility & performance pass

- Stufe: **L · schwer** · type `fix` · size `M` · lane `H` · depends T-052,T-100
- Empfohlen: **Claude Opus 5.5 · xhigh / GPT-6.1 Sol · xhigh** · Alternative: Gemini 3.8 Flash · high

Menschliche Aufgabe: selbst erledigen (oder mit stärkstem Modell als Helfer).

### T-195 · Trademark check name 'Hain'

- Stufe: **L · schwer** · type `plan` · size `XS` · lane `H` · depends -
- Empfohlen: **Claude Opus 5.5 · xhigh / GPT-6.1 Sol · xhigh** · Alternative: Gemini 3.8 Flash · high

Menschliche Aufgabe: selbst erledigen (oder mit stärkstem Modell als Helfer).

### T-199 · Beta release (GitHub + F-Droid metadata)

- Stufe: **L · schwer** · type `scaffold` · size `S` · lane `H` · depends T-190
- Empfohlen: **Claude Opus 5.5 · xhigh / GPT-6.1 Sol · xhigh** · Alternative: Gemini 3.8 Flash · high

Menschliche Aufgabe: selbst erledigen (oder mit stärkstem Modell als Helfer).

