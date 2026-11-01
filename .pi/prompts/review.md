---
description: Review one Hain task
argument-hint: "<T-XXX>"
---
Read AGENTS.md. Review task $1: compare the diff (git show) with the task card in
docs/workflow/TASKS.md and the spec sections it lists. Run its check command and
./gradlew testDebugUnitTest. Check: only listed files changed, no hard-coded strings,
DE+EN strings present, no material3 imports, no network call outside NetworkGate.
Do not change code. If ok: set status `done`. If not: set `todo`, add a "Review notes"
bullet list under the task card, and log it in HANDOFF_LOG.md.
