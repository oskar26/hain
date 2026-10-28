# PROMPTS_FOR_AGENTS.md — copy-paste starters

## Implementer (any harness, small model)

```
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: {T-XXX} (or, if none given, the first `todo` task in
docs/workflow/TASKS.md whose dependencies are all `done` and whose lane is not H).
Read only the spec sections listed under "read" for that task.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
```

## Reviewer

```
Read AGENTS.md. Review task {T-XXX}: compare the diff (git show) with the task card in
docs/workflow/TASKS.md and the spec sections it lists. Run its check command and
./gradlew testDebugUnitTest. Check: only listed files changed, no hard-coded strings,
DE+EN strings present, no material3 imports, no network call outside NetworkGate.
If ok: set status `done`. If not: set `todo`, add a "Review notes" bullet list under the
task card, and log it in HANDOFF_LOG.md.
```

## Planner (strong model)

```
Read AGENTS.md, STATUS.md, the last 5 HANDOFF_LOG.md entries and open questions.
Split any task that is blocked for being too big (size M → two S).
Update tools/tasks_src.py, regenerate TASKS.md keeping existing statuses,
answer or escalate questions, and update STATUS.md "Now". Do not write feature code.
```

## Tips for small local models

- Context: give AGENTS.md + task card + listed spec sections only (≈ 6–10k tokens).
- Aider: `aider AGENTS.md docs/workflow/TASKS.md <spec> <files...>` and paste the implementer prompt.
- If the model loops on a failing check twice, stop and hand the task to a stronger model.
