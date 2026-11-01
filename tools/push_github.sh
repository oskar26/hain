#!/usr/bin/env bash
# Hain -> GitHub
# Nutzung (im hain-Ordner):  bash tools/push_github.sh [--public] [--name hain]
# Standard: privates Repo "hain" unter deinem GitHub-Konto.
set -euo pipefail

NAME="hain"
VIS="--private"
while [[ $# -gt 0 ]]; do
  case "$1" in
    --public) VIS="--public"; shift ;;
    --name)   NAME="$2"; shift 2 ;;
    *) echo "Unbekannte Option: $1"; exit 1 ;;
  esac
done

cd "$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
[[ -f AGENTS.md ]] || { echo "Bitte im hain-Ordner starten."; exit 1; }

# 1) gh installieren und anmelden
if ! command -v gh >/dev/null; then
  echo "Installiere GitHub CLI ..."
  sudo dnf install -y gh
fi
if ! gh auth status >/dev/null 2>&1; then
  gh auth login --hostname github.com --git-protocol https --web
fi
gh auth setup-git

# 2) Git vorbereiten
[[ -d .git ]] || git init -b main
git config user.name  >/dev/null || git config user.name  "$(gh api user -q .login)"
git config user.email >/dev/null || git config user.email "$(gh api user -q .id)+$(gh api user -q .login)@users.noreply.github.com"

# 3) .gitignore absichern (Modelle, Builds, lokale Pfade, Schluessel)
for p in ".gradle/" "build/" "local.properties" "*.gguf" "*.onnx" "*.mnn" "*.bin" \
         "/models/" ".idea/" "*.iml" ".DS_Store" "captures/" ".externalNativeBuild/" \
         ".cxx/" "*.jks" "*.keystore" "keystore.properties" ".env" "__pycache__/"; do
  grep -qxF "$p" .gitignore 2>/dev/null || echo "$p" >> .gitignore
done

# 4) Schon versehentlich getrackte Dateien rausnehmen (bleiben lokal liegen)
git rm -r --cached --ignore-unmatch -q local.properties .gradle build models .idea \
  $(git ls-files '*.gguf' '*.onnx' '*.mnn' '*.jks' '*.keystore' '**/__pycache__/*' 2>/dev/null) || true

# 5) Grosse Dateien pruefen (GitHub-Limit 100 MB)
BIG=$(git ls-files -z | xargs -0 -I{} sh -c '[ -f "{}" ] && [ $(stat -c%s "{}") -gt 52428800 ] && echo "{}"' || true)
if [[ -n "$BIG" ]]; then
  echo "Diese Dateien sind groesser als 50 MB, bitte in .gitignore oder Git LFS:"; echo "$BIG"; exit 1
fi

# 6) Submodule (llama.cpp) muessen auf einen oeffentlichen Commit zeigen
if [[ -f .gitmodules ]]; then
  git submodule status | grep -q '^+' && echo "Hinweis: Submodule hat lokale Aenderungen, die nicht gepusht werden."
fi

# 7) Commit, falls noch etwas offen ist
git add -A
git diff --cached --quiet || git commit -m "chore: prepare GitHub push"

# 8) Repo anlegen bzw. verbinden und pushen
if git remote get-url origin >/dev/null 2>&1; then
  git push -u origin HEAD:main --follow-tags
elif gh repo view "$NAME" >/dev/null 2>&1; then
  git remote add origin "https://github.com/$(gh api user -q .login)/$NAME.git"
  git push -u origin HEAD:main --follow-tags
else
  gh repo create "$NAME" $VIS --source=. --remote=origin \
    --description "Hain - private, local AI chat for Android" --push
fi

echo
echo "Fertig: $(gh repo view --json url -q .url)"
echo "CI laeuft jetzt unter Actions: $(gh repo view --json url -q .url)/actions"
