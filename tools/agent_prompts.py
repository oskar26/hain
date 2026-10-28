# Generates docs/workflow/AGENT_PROMPTS.md: model routing + one copy-paste prompt per task.
# Run: python3 tools/agent_prompts.py > docs/workflow/AGENT_PROMPTS.md
import contextlib, io, os

src = os.path.join(os.path.dirname(__file__), "tasks_src.py")
ns = {}
with contextlib.redirect_stdout(io.StringIO()):
    exec(open(src, encoding="utf-8").read(), ns)
T, M = ns["T"], ns["M"]


def tier(x):
    if x["type"] in ("native", "plan") or x["lane"] == "H":
        return "L"
    if x["type"] == "feature" or x["size"] == "M":
        return "M"
    return "S"


# model, tier S, tier M, tier L   ("-" = do not use for this tier)
MODELS = [
    ("Lokal: Qwen3.6-35B-A3B `qwen-moe` (pi)", "Thinking medium", "Thinking medium", "-"),
    ("Lokal: FrogNano-4B `frognano` (pi)", "Thinking medium", "-", "-"),
    ("Gemini 3.8 Flash", "low", "medium", "high"),
    ("Gemini 4 Argon", "low", "medium", "high"),
    ("DeepSeek Flash (V4.1, `deepseek-flash`)", "low", "high", "max"),
    ("DeepSeek V4-Pro", "-", "high", "max"),
    ("Claude Haiku 5.5", "low", "medium", "-"),
    ("Claude Sonnet 5.5", "low", "medium", "high"),
    ("Claude Opus 5.5", "-", "medium", "xhigh"),
    ("GPT-6 Luna", "low", "medium", "-"),
    ("GPT-6.1 Sol", "low", "medium", "xhigh"),
    ("GPT-6 Astra", "-", "low", "medium"),
]
PICK = {
    "S": ("Lokal `frognano` (bei 2× Scheitern `qwen-moe`)", "Gemini 3.8 Flash · low"),
    "M": ("Lokal `qwen-moe` · medium", "Gemini 3.8 Flash · medium / DeepSeek Flash · high"),
    "L": ("Claude Opus 5.5 · xhigh / GPT-6.1 Sol · xhigh", "Gemini 3.8 Flash · high"),
}
TIERNAME = {"S": "S · einfach", "M": "M · mittel", "L": "L · schwer"}

o = ["# AGENT_PROMPTS.md — Prompt + Modell pro Aufgabe", "",
     "Generiert von `tools/agent_prompts.py` aus `tools/tasks_src.py`. Nach Änderungen an den Aufgaben neu erzeugen:",
     "`python3 tools/agent_prompts.py > docs/workflow/AGENT_PROMPTS.md`", "",
     "Reihenfolge: Aufgaben der Reihe nach, nur wenn alle `depends` in TASKS.md `done` sind. Lane H = du selbst oder stärkstes Modell.", "",
     "## Schwierigkeitsstufen", "",
     "- **S · einfach:** scaffold, strings, ui und test in Größe XS/S. Lokal reicht.",
     "- **M · mittel:** feature (reines Kotlin) und alles in Größe M.",
     "- **L · schwer:** native (JNI, CMake, NDK), plan, Lane H. Stärkstes Modell oder du.", "",
     "## Modelle und Thinking-Level", "",
     "| Modell | S · einfach | M · mittel | L · schwer |", "|---|---|---|---|"]
o += [f"| {m} | {a} | {b} | {c} |" for m, a, b, c in MODELS]
o += ["",
      "Hinweise:", "",
      "- Gemini 3.8 Flash und Gemini 4 Argon: kein `minimal` (Fehler). Standard ist medium.",
      "- DeepSeek: `none` schaltet Thinking aus, `medium` wird zu `high` gemappt. Laut DeepSeek ist V4.1-Flash stärker als V4-Pro.",
      "- Claude/GPT: über `xhigh` bringt `max` beim Coden praktisch nichts, dauert aber viel länger.",
      "- Lokal läuft immer nur EIN Modell (llama.cpp-Router, `--models-max 1`). Wechsel per `/model` in pi. Details: LOCAL_MODELS.md.",
      "- Qwen3.6-35B-A3B lokal: Reasoning-Budget 12k ist gesetzt. Tiel-Coder wird nicht mehr genutzt.",
      "- FrogNano-4B: bester kleiner Coding-Agent (Microsoft, unabhängig geprüfte Werte), aber auf Python trainiert. Für Kotlin nur kleine, klare Aufgaben.",
      "- Spark-X2.6-4B: verworfen (Benchmarks unbelegt), FrogNano ist der bessere kleine.",
      "- Lokale Modelle einrichten: `bash tools/setup/setup-local-agent.sh` (Router + pi + Projekt-Befehle /task, /next, /review).",
      "- Reviewer immer ein **anderes** Modell als der Umsetzer.",
      "- Hängt ein Modell zweimal am selben Fehler: eine Stufe höher (anderes Modell oder höheres Level).", "",
      "## Reviewer-Prompt (nach jeder Aufgabe)", "",
      "Modell: anderes als Umsetzer · Level: eine Stufe unter der Aufgabe, mindestens medium.", "", "```",
      "Read AGENTS.md. Review task {T-XXX}: compare the diff (git show) with the task card in",
      "docs/workflow/TASKS.md and the spec sections it lists. Run its check command and",
      "./gradlew testDebugUnitTest. Check: only listed files changed, no hard-coded strings,",
      "DE+EN strings present, no material3 imports, no network call outside NetworkGate.",
      "If ok: set status `done`. If not: set `todo`, add a \"Review notes\" bullet list under the",
      "task card, and log it in HANDOFF_LOG.md.", "```", "",
      "## Planer-Prompt (alle ~10 Aufgaben)", "",
      "Modell: Claude Opus 5.5 · xhigh, GPT-6.1 Sol · xhigh oder Gemini 3.8 Flash · high.", "", "```",
      "Read AGENTS.md, STATUS.md, the last 5 HANDOFF_LOG.md entries and open questions.",
      "Split any task that is blocked for being too big (size M → two S).",
      "Update tools/tasks_src.py, regenerate TASKS.md keeping existing statuses,",
      "answer or escalate questions, and update STATUS.md \"Now\". Do not write feature code.", "```", ""]

for i, x in enumerate(T):
    if i in M:
        o += [f"## {M[i][0]} · {M[i][1]}", ""]
    tr = tier(x)
    a, b = PICK[tr]
    o += [f"### {x['id']} · {x['title']}", "",
          f"- Stufe: **{TIERNAME[tr]}** · type `{x['type']}` · size `{x['size']}` · lane `{x['lane']}` · depends {x['depends']}",
          f"- Empfohlen: **{a}** · Alternative: {b}", ""]
    if x["lane"] == "H":
        o += ["Menschliche Aufgabe: selbst erledigen (oder mit stärkstem Modell als Helfer).", ""]
        continue
    o += ["```",
          "You are working on the Hain Android app. First read AGENTS.md completely and follow its",
          "session protocol. Then read docs/workflow/STATUS.md.",
          f"Do exactly one task: {x['id']} ({x['title']}).",
          "Read only the spec sections listed under \"read\" for that task.",
          "Change only the files listed under \"files\". Run the \"check\" command until it passes.",
          "Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.",
          "If something is unclear, write a question into STATUS.md → \"Questions for human\",",
          "set the task to `blocked: <reason>` and stop. Do not guess product decisions.", "```", ""]
print("\n".join(o))
