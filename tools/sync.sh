#!/usr/bin/env bash
# Hain: keep this checkout identical to GitHub (oskar26/hain).
#
#   bash tools/sync.sh           pull main (rebase, autostash), update submodule, push local commits
#   bash tools/sync.sh --watch   same, every 5 minutes (Ctrl+C to stop)
#
# Rules for agents (AGENTS.md §1): run this at session start and after every commit.
# Never force-push. If a rebase conflicts, the script stops and leaves the repo untouched.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

sync_once() {
  local branch
  branch="$(git rev-parse --abbrev-ref HEAD)"
  git fetch --prune origin
  if git rev-parse --verify --quiet "origin/$branch" >/dev/null; then
    if ! git pull --rebase --autostash origin "$branch"; then
      git rebase --abort 2>/dev/null || true
      echo "!! Konflikt beim Rebase auf origin/$branch – nichts geändert. Bitte manuell lösen." >&2
      return 1
    fi
  fi
  git submodule update --init --recursive
  if [ -n "$(git log "origin/$branch..HEAD" --oneline 2>/dev/null || git log --oneline -1)" ]; then
    git push -u origin "$branch"
  fi
  if [ -n "$(git status --porcelain)" ]; then
    echo "Hinweis: uncommittete Änderungen (werden nicht gepusht):"
    git status --short
  fi
  echo "✓ $branch = origin/$branch ($(git rev-parse --short HEAD))"
}

if [ "${1:-}" = "--watch" ]; then
  while true; do sync_once || true; sleep 300; done
else
  sync_once
fi
