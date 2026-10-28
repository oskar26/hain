# TESTING.md

## Levels

1. **JVM unit tests** (most): PromptBuilder (golden), ToolCallParser, StreamSplitter, ThinkingPolicy, Calculator, WeatherCodes, SearchRouter classifier, DDG HTML parser (saved HTML fixtures), Feed parser (fixtures for RSS2/Atom/RDF from each standard feed, saved 2026-10-xx), GermanOcrFixer, MemoryRanking, CardParser, SystemPrompt.fill, strings check.
2. **Robolectric/Compose UI tests** for screens with Fakes from `:core-testing`.
3. **Instrumented tests** only for: JNI load/generate with a tiny GGUF (e.g. a 1–10 MB test model, decided in T-032), Room migrations, SQLCipher open.
4. **Manual device checklist** per milestone (STATUS.md).

## Fixtures

`*/src/test/resources/fixtures/` — never fetch the internet in unit tests. Use OkHttp `MockWebServer`.

## German eval set (quality of model + prompt)

`eval/de_en_questions.jsonl` (T-090): 100 items `{id, lang, prompt, expect_tool?, must_contain?[], must_not_contain?[]}`:
30 tool-routing (weather, alarm, news, wiki, calc), 20 everyday German, 20 English, 10 memory, 10 safety/teen, 10 cards.
Runner: `tools/eval_runner.py` against `llama-server` on a PC with the same GGUF + prompt files + our tool schemas → report pass rate.
Used for: prompt changes, model switches, Omni decision (ADR-06 re-check).
