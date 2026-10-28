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

## 3. Enforcement

- When the model starts "```card" (detected by StreamSplitter), switch to a **GBNF grammar** for the rest of the block (`core-cards/src/main/assets/card.gbnf`, written in T-131). If grammar switching mid-generation is not supported by JNI → v1 parses leniently and falls back to markdown on error.
- Parser: pure Kotlin, line by line, streaming-tolerant; unknown lines → ignored; total failure → render as code block.

## 4. Renderer

`CardRenderer(card: ModelCard)` in `:core-cards` using `:core-ui` components. Checklist state is saved in `message.cards_json`. Buttons: `copy` → clipboard, `share` → share sheet, `ask:"…"` → sends that text as new user message.
