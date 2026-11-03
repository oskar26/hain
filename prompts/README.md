# prompts/

| File | Use |
|---|---|
| `system_full.de.txt` | Default system prompt (German) for LFM3-1.7B (≈430 tokens filled): answer language `de`, or `auto` on a German device (LLM.md §9). |
| `system_full.en.txt` | Same prompt in English: answer language `en`, or `auto` on other devices. |
| `system_short.en.txt` | Battery saver / low-RAM devices (≈180 tokens). |
| `system_short.de.txt` | German variant, only for A/B test (T-090). |
| `lfm3-1.7b_chat_template.jinja` | Official template, reference only. `PromptBuilder` re-implements it in Kotlin. Golden tests must match its output. |
| `lfm3-omni-2b_chat_template.jinja` | Omni template, reference only. |
| `lfm3-1.7b_tokenizer_config.json` | Special tokens reference. |

Placeholders: `{weekday} {date} {time} {timezone} {city} {emergency_number} {memory_snippets} {user_custom_instructions} {language_rule}`.
Unfilled placeholders must be removed (empty string), never sent literally.

Omni voice mode: system prompt must be exactly `Respond with interleaved text and audio.`; put the short prompt at the top of the first user message instead.
These files are copied into `app/src/main/assets/prompts/` by Gradle task (see T-031). Edit them here only.
