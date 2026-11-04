# Tool eval (4d)

`tool_eval.jsonl`: 52 questions (DE + EN, colloquial, typos, follow-ups with history, questions without a
tool). Each line: `id`, `lang`, `q`, `kind` (fact, current, knowledge, calc, action, chat, time),
`expect.tools` (allowed first tool, empty = no tool), `expect.args` (value substrings; `seconds`,
`time`, `start` are compared as numbers/clock/date), optional `ok_none`, `must` (one of these values has
to be in the answer after the tool), `mock` (tool result fed back), `history`.

`tool_eval.py` runs them through the real LFM3 on the host (no network, mocks only):

```bash
python3 tools/eval/tool_eval.py --defs legacy --rev 345fa73 --out before.json   # prompts as before 4d
python3 tools/eval/tool_eval.py --defs repo --out after.json                    # prompts of this tree
python3 tools/eval/tool_eval.py --rescore after.json                            # re-check saved answers
```

Needs the host runner `h2` (llama.cpp, model kept loaded) and the gguf; never run it while Gradle runs
(shared RAM). Results: `results/baseline.json` (before 4d), `results/after.json`.
