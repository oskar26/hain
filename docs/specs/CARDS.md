# CARDS.md — answer cards (ADR-18)

Research: `docs/concept/research/openui.md`.

## 1. Two kinds

1. **Tool cards** (fixed, hand-built Compose): WeatherCard, NewsCard, SourcesCard, WikiCard, ConfirmCard (alarm/timer/calendar/navigate), MemoryChip. Data = `CardData` sealed class from `ToolResult`.
2. **Model cards** (OpenUI Lang subset) for: checklist, steps, table, comparison, progress, simple chart.

## 2. Model card syntax (subset, line-based)

The model writes a fenced block:
````
```card
Card title="Einkaufsliste"
Checklist
- Tomaten
- Basilikum [x]
Button label="In Notizen kopieren" action=copy
```
````
Components: `Card title=`, `List` (`- item`), `Table` (`| a | b |` rows, first row header), `Checklist` (`- item [x]?`), `Button label= action=copy|share|ask:"text"`, `Progress value=0..100 label=`, `Image src=` (only `attachment:N` or https from tool sources), `Chart type=bar|line` + `- label: number` lines.
Max 1 card per answer, max 30 lines.

Implemented in 0.1.6 (T-130/T-131): `List`, `Table`, `Checklist`, `Progress`, `Button` (+ `Card title=`).
`Image` and `Chart` are not rendered yet (the parser ignores those lines). The parser also takes German
keywords (`Karte`, `Liste`, `Checkliste`, `Tabelle`, `Fortschritt`, `Knopf`), items with `-`/`*`/`•`/`1.`,
check marks before or after the text (`[x]`, `[ ]`, `✅`, `☐`), German quotes `„…“`, a list or table without its
keyword line. More than one card per answer is shown (each counted from 0), but the prompt asks for one.
Limits: 30 lines per card, 20 items/rows, 5 columns, 3 buttons, 200 characters per text.

## 3. Enforcement

- When the model starts "```card" (detected by StreamSplitter), switch to a **GBNF grammar** for the rest of the block (`core-cards/src/main/assets/card.gbnf`, written in T-131). If grammar switching mid-generation is not supported by JNI → v1 parses leniently and falls back to markdown on error.
- Parser: pure Kotlin, line by line, streaming-tolerant; unknown lines → ignored; total failure → render as code block.
- **State 0.1.6:** the JNI bridge takes one grammar for a whole generation (`grammar` in `hain_llm.cpp`), not a switch in
  the middle of an answer, so v1 runs **without** the grammar at runtime: the system prompt (`# Karten` / `# Cards`
  in `prompts/system_full.*.txt`) shows one example card and the lenient parser reads it. `card.gbnf` is shipped and
  checked by `python3 tools/check_gbnf.py` (syntax, all rules defined, good/bad sample cards; stand-in for
  `llama-gbnf-validator`, which needs a C++ build) and `CardGrammarTest`. Mid-answer switching = later task.
- Code: `core-cards/…/api/CardParser.kt` (block → `ModelCard`), `AnswerParts.kt` (answer → text + cards, streaming:
  an open block grows, an empty one is held back, a half-written fence too; card fences inside other code blocks stay
  text), `CardText.kt` (plain text for copy/share), `CardState.kt` (ticks).

## 4. Renderer

`CardRenderer(card: ModelCard)` in `:core-cards` using `:core-ui` components. Checklist state is saved in `message.cards_json`. Buttons: `copy` → clipboard, `share` → share sheet, `ask:"…"` → sends that text as new user message.

Look (Lichtung, DESIGN_LANGUAGE.md / STYLE_OPTIONS.md): one `calmTile` with hairline, radius 12 dp (`shapes.card`),
padding 16 dp; title in `button` style (Medium, heading for TalkBack); bullets with a 6 dp green dot; table with a
hairline frame (radius 8), header on `surface`, columns share the width and wrap; checklist rows ≥ 40 dp, 20 dp box
(green + white check when done, text muted + struck through), `toggleable` with role Checkbox and "erledigt/offen",
`Toggle` haptic; progress = label + "40 %", 8 dp capsule track, fill grows in 600 ms (snaps with reduce motion),
`progressBarRangeInfo`; buttons all at the bottom, soft green fill (`accentSoft`), green Medium label, press-in + tick,
"Kopiert" for 1.5 s after copy, disabled while the answer still streams ("Pim schreibt noch…").
`cards_json` = `{"checked":{"card.block.item":true}}`, only items the user changed. Copy/share of the whole answer
use the plain text with the user's ticks.
