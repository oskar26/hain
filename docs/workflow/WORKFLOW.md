# WORKFLOW.md — how Hain is built with several agents and small models

## Idea

A strong model (or the human) **plans**; small/cheap models **implement one small task at a time**;
a second pass **reviews**. Everything an agent needs is written down, so no agent depends on chat history.

```
           ┌────────────┐   writes/updates   ┌──────────────┐
 Human ───►│  PLANNER   │──────────────────►│  TASKS.md     │
           │ (strong)   │                    │  specs/*.md   │
           └────────────┘                    └──────┬───────┘
                                                    │ one task
                                             ┌──────▼───────┐
                                             │ IMPLEMENTER  │  small model (7–32B, or cheap API tier)
                                             │ 1 task/commit│
                                             └──────┬───────┘
                                                    │ diff + checks
                                             ┌──────▼───────┐
                                             │  REVIEWER    │  other model or human
                                             └──────┬───────┘
                                                    │ ok → done / not ok → back to todo with notes
                                             STATUS.md + HANDOFF_LOG.md
```

## Roles

| Role | Who | Does | Does not |
|---|---|---|---|
| **Planner** | human or strongest model | writes/splits tasks, edits specs, answers "Questions for human", decides ADRs | write feature code |
| **Implementer** | any model, small is fine | exactly one task from TASKS.md, runs checks, updates status | change specs/ADRs, start 2nd task |
| **Reviewer** | different model or human | reads diff against task card + spec, runs checks, sets `done` or reopens | rewrite the feature |
| **Fixer** | any | tasks of type `fix` created by reviewer | — |

## Task card format (TASKS.md)

```
### T-043 · ToolCallParser
type: feature | size: S | owner: -
depends: T-040
read: docs/specs/LLM.md §5
files: core-llm/src/main/kotlin/app/hain/llm/toolcall/ToolCallParser.kt,
       core-llm/src/test/kotlin/app/hain/llm/toolcall/ToolCallParserTest.kt
do:
  1. ...
accept:
  - parses the 4 examples in LLM.md §5 exactly
check: ./gradlew :core-llm:testDebugUnitTest --tests "*ToolCallParser*"
```

- **status** (Overview table only): `todo` → `doing (<model>)` → `review` → `done` | `blocked: <reason>` | `split`
- **size**: `XS` (<50 lines), `S` (<150), `M` (<400). No `L` tasks: split them.
- **files**: the complete list. Small models must not wander.
- **accept**: testable statements. **check**: commands that prove them.

## Rules that make small models succeed

1. **One task, one context.** Give the model: AGENTS.md + the task card + the spec sections in `read`. Nothing else.
2. **Interfaces first.** Each module has a `api/` package with interfaces written early (T-01x). Later tasks implement against them, so tasks are independent.
3. **Pure Kotlin where possible.** Parsers, prompt builder, router, memory ranking are plain JVM code with unit tests. Android/JNI glue is thin.
4. **Golden files.** Expected outputs live in `src/test/resources/golden/`. The implementer makes the test pass; it does not invent the expectation.
5. **Fakes over mocks.** Every interface has a `Fake*` in `core-testing` so UI tasks don't need a real model.
6. **No refactors** outside the task. Note ideas under "Ideas" in STATUS.md.
7. **Fail loud.** If checks fail twice with the same error, stop, set `blocked`, describe the error in the handoff.
8. **Native/C++ tasks** (marked `native`) need a stronger model or the human. Small models skip them.

## Model suggestions per task type

| Type | Suitable |
|---|---|
| `scaffold`, `ui`, `strings`, `test`, `fix` | small local coder models (e.g. Qwen-Coder 7–14B class), cheap API tiers |
| `feature` (pure Kotlin) | 14–32B coder models, mid API tiers |
| `native` (JNI, CMake, NDK), `perf`, `security` | strongest available model or human |
| `review` | a different model than the implementer |
| `plan`, `spec` | strongest model or human |

## Parallel work

Tasks with no shared `files` and satisfied `depends` may run in parallel in different branches
(`task/T-042`). Merge order = task number. TASKS.md marks parallel lanes (A–F).

## Handoff entry format (HANDOFF_LOG.md, append at the bottom)

```
## 2026-11-02 · T-042 · done · harness: aider · model: qwen-coder-14b
- What: parser for <tool_call> XML, 6 tests
- Files: ToolCallParser.kt, ToolCallParserTest.kt
- Checks: all green
- Notes/risks: nested <parameter> with "<" in value is escaped by the model? unclear → Q-07
- Next: T-043
```

## When the plan is wrong

Implementers do not silently change the plan. Write it to STATUS.md → "Questions for human" with an ID (Q-xx).
The planner answers there, updates specs/tasks, and moves the answer into `docs/decisions/ADR.md` if it is a decision.
