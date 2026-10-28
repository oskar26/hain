#!/usr/bin/env bash
# =====================================================================
#  Hain · lokaler Coding-Agent: Qwen3.6-35B-A3B + FrogNano-4B + pi
#  Fedora 43/44 · RX 7600 8 GB · 48 GB RAM · nutzt dein ~/llama.cpp
#
#  Aufruf IM hain-Ordner:   bash tools/setup/setup-local-agent.sh
#  Braucht daneben:         tools/setup/llm-models.ini
#  Anleitung:               docs/workflow/LOCAL_MODELS.md
#
#  Ergebnis: EIN llama.cpp-Router (Port 8080), der nie mehr als ein Modell
#  gleichzeitig lädt. Alte Dienste tiel-server/small-server werden
#  gestoppt und deaktiviert (Dateien bleiben liegen).
#
#  Stand 28.10.2026 · NICHT auf deinem PC getestet. Ausgaben lesen.
# =====================================================================
set -euo pipefail

LLAMA_DIR="${LLAMA_DIR:-$HOME/llama.cpp}"
PORT="${PORT:-8080}"
HAIN_DIR="${HAIN_DIR:-$(pwd)}"
RAM_LIMIT="${RAM_LIMIT:-36G}"
IDLE_SLEEP="${IDLE_SLEEP:-900}"     # Sekunden ohne Anfrage, dann Modell entladen
VRAM_MARGIN="${VRAM_MARGIN:-1536}"  # MiB, die der Router frei lässt

QWEN_REPO="unsloth/Qwen3.6-35B-A3B-GGUF"
QWEN_FILE="Qwen3.6-35B-A3B-UD-Q4_K_XL.gguf"   # 22,4 GB
FROG_REPO="bartowski/FrogNano-4B-2609-GGUF"
FROG_FILE="FrogNano-4B-2609-Q6_K.gguf"        # 3,8 GB

echo "==> 0/7 Checks"
[ -f "$HAIN_DIR/AGENTS.md" ] && [ -f "$HAIN_DIR/tools/setup/llm-models.ini" ] || {
  echo "Bitte im hain-Ordner starten (AGENTS.md oder tools/setup/llm-models.ini fehlt)"; exit 1; }
SERVER_BIN="$LLAMA_DIR/build/bin/llama-server"
[ -x "$SERVER_BIN" ] || { echo "llama-server fehlt unter $SERVER_BIN (erst setup-fedora.sh)"; exit 1; }
free_gb=$(df -BG --output=avail "$HOME" | tail -1 | tr -dc 0-9)
[ "$free_gb" -ge 30 ] || echo "    WARNUNG: nur ${free_gb} GB frei, Modelle brauchen ~27 GB"
mkdir -p "$HOME/.local/bin" "$HOME/.config/systemd/user" "$HOME/.config/llm"
export PATH="$HOME/.npm-global/bin:$HOME/.local/bin:$PATH"

echo "==> 1/7 llama.cpp: Router-Funktionen prüfen"
need_build=0
for flag in --models-preset --models-max --models-memory-margin --sleep-idle-seconds; do
  "$SERVER_BIN" --help 2>/dev/null | grep -q -- "$flag" || need_build=1
done
if [ "$need_build" = 1 ] || [ "${UPDATE_LLAMA:-0}" = 1 ]; then
  echo "    zu alt -> aktualisiere und baue neu (Vulkan)"
  git -C "$LLAMA_DIR" pull
  cmake -S "$LLAMA_DIR" -B "$LLAMA_DIR/build" -G Ninja -DGGML_VULKAN=ON -DCMAKE_BUILD_TYPE=Release
  cmake --build "$LLAMA_DIR/build" -j4
fi
echo "    $(git -C "$LLAMA_DIR" describe --tags --always 2>/dev/null)"

echo "==> 2/7 Modelle laden (überspringt vorhandene)"
command -v hf >/dev/null || pip install --user -U huggingface_hub
dl() { mkdir -p "$3"; [ -f "$3/$2" ] && echo "    $2 schon da" || hf download "$1" "$2" --local-dir "$3"; }
dl "$QWEN_REPO" "$QWEN_FILE" "$HOME/models/qwen3.6-35b-a3b"
dl "$FROG_REPO" "$FROG_FILE" "$HOME/models/frognano"

echo "==> 3/7 Presets nach ~/.config/llm/models.ini"
sed "s|@HOME@|$HOME|g" "$HAIN_DIR/tools/setup/llm-models.ini" > "$HOME/.config/llm/models.ini"

echo "==> 4/7 Alte Einzel-Dienste abschalten, Router-Dienst anlegen"
for s in tiel-server small-server vram-guard vram-guard-small; do
  systemctl --user disable --now "$s.service" 2>/dev/null || true
done
cat > "$HOME/.config/systemd/user/llm-router.service" <<UNIT
[Unit]
Description=llama.cpp Router (Qwen3.6-MoE / FrogNano, max. 1 Modell)

[Service]
Environment=GGML_VK_VISIBLE_DEVICES=0
ExecStart=$SERVER_BIN --models-preset %h/.config/llm/models.ini \\
  --models-max 1 --models-memory-margin $VRAM_MARGIN \\
  --sleep-idle-seconds $IDLE_SLEEP --host 127.0.0.1 --port $PORT
Restart=on-failure
RestartSec=10
# Harte RAM-Grenze für Router + Modell (Qwen-Experten liegen im RAM)
MemoryMax=$RAM_LIMIT
MemorySwapMax=0
Nice=5

[Install]
WantedBy=default.target
UNIT

cat > "$HOME/.local/bin/vram-guard-router" <<SH
#!/usr/bin/env bash
# Notbremse: weniger als 1 GB VRAM frei -> alle Modelle entladen.
MIN_FREE_MIB=\${MIN_FREE_MIB:-1024}
DEV=""
for d in /sys/class/drm/card*/device; do
  [ "\$(cat \$d/vendor 2>/dev/null)" = "0x1002" ] && [ -f \$d/mem_info_vram_total ] && DEV=\$d && break
done
[ -z "\$DEV" ] && { echo "Keine AMD-GPU gefunden"; exit 1; }
while sleep 2; do
  free=\$(( (\$(cat \$DEV/mem_info_vram_total) - \$(cat \$DEV/mem_info_vram_used)) / 1048576 ))
  if [ "\$free" -lt "\$MIN_FREE_MIB" ]; then
    "\$HOME/.local/bin/llm" off >/dev/null 2>&1
    notify-send "LLM entladen" "Nur noch \${free} MiB VRAM frei." 2>/dev/null || true
    sleep 20
  fi
done
SH
chmod +x "$HOME/.local/bin/vram-guard-router"
cat > "$HOME/.config/systemd/user/vram-guard-router.service" <<UNIT
[Unit]
Description=VRAM-Notbremse für llm-router
BindsTo=llm-router.service
After=llm-router.service

[Service]
ExecStart=%h/.local/bin/vram-guard-router

[Install]
WantedBy=llm-router.service
UNIT

echo "==> 5/7 Befehl 'llm'"
cat > "$HOME/.local/bin/llm" <<SH
#!/usr/bin/env bash
# llm start | stop | qwen | frog | off | status | log | vram
URL="http://127.0.0.1:$PORT"
loaded() { curl -s "\$URL/models" | python3 -c 'import sys,json
d=json.load(sys.stdin); d=d.get("data",d) if isinstance(d,dict) else d
for m in d:
    s=m.get("status",{}); s=s.get("value",s) if isinstance(s,dict) else s
    if s in ("loaded","loading","sleeping"): print(m.get("id"), s)' 2>/dev/null; }
load() { curl -s -X POST "\$URL/models/load" -H 'Content-Type: application/json' -d "{\"model\":\"\$1\"}"; echo; }
case "\${1:-status}" in
  start)  systemctl --user start llm-router && echo "Router läuft (lädt erst bei Bedarf)";;
  stop)   systemctl --user stop llm-router && echo "Router gestoppt, alles frei";;
  qwen)   load qwen-moe;;
  frog)   load frognano;;
  off)    for m in \$(loaded | cut -d' ' -f1); do curl -s -X POST "\$URL/models/unload" -H 'Content-Type: application/json' -d "{\"model\":\"\$m\"}" >/dev/null; echo "entladen: \$m"; done;;
  status) echo "Router: \$(systemctl --user is-active llm-router)"; loaded || true;;
  log)    journalctl --user -u llm-router -f;;
  vram)   for d in /sys/class/drm/card*/device; do [ -f \$d/mem_info_vram_used ] && echo "VRAM belegt: \$(( \$(cat \$d/mem_info_vram_used)/1048576 )) / \$(( \$(cat \$d/mem_info_vram_total)/1048576 )) MiB"; done;;
  *) echo "Nutzung: llm start|stop|qwen|frog|off|status|log|vram";;
esac
SH
chmod +x "$HOME/.local/bin/llm"
systemctl --user daemon-reload
systemctl --user enable llm-router.service vram-guard-router.service >/dev/null

echo "==> 6/7 pi installieren/aktualisieren + Modelle eintragen"
if ! node -e 'const [a,b]=process.versions.node.split(".").map(Number); process.exit(a>22||(a===22&&b>=19)?0:1)' 2>/dev/null; then
  echo "    Node fehlt oder < 22.19 -> nodejs24"
  sudo dnf install -y nodejs24 nodejs24-npm || sudo dnf install -y nodejs npm
fi
mkdir -p "$HOME/.npm-global"; npm config set prefix "$HOME/.npm-global"
npm install -g --ignore-scripts @earendil-works/pi-coding-agent@latest
grep -q npm-global "$HOME/.bashrc" || echo 'export PATH="$HOME/.npm-global/bin:$HOME/.local/bin:$PATH"' >> "$HOME/.bashrc"

mkdir -p "$HOME/.pi/agent"
[ -f "$HOME/.pi/agent/models.json" ] && cp "$HOME/.pi/agent/models.json" "$HOME/.pi/agent/models.json.bak"
PORT="$PORT" python3 - <<'PY'
import json, os
p = os.path.expanduser("~/.pi/agent/models.json")
d = json.load(open(p)) if os.path.exists(p) else {}
d["$schema"] = "https://pi.dev/schemas/models.schema.json"
prov = d.setdefault("providers", {})
for old in ("local", "local-small"):          # Tiel/Einzel-Server von früher
    prov.pop(old, None)
samp = {"temperature": 0.6, "top_p": 0.95, "top_k": 20}
prov["llm"] = {
    "baseUrl": f"http://127.0.0.1:{os.environ['PORT']}/v1",
    "api": "openai-completions", "apiKey": "none",
    "compat": {"thinkingFormat": "qwen-chat-template"},
    "models": [
        {"id": "qwen-moe", "name": "Qwen3.6-35B-A3B (lokal, gründlich)", "reasoning": True,
         "contextWindow": 65536, "maxTokens": 16384, "samplingParams": samp},
        {"id": "frognano", "name": "FrogNano-4B (lokal, schnell)", "reasoning": True,
         "contextWindow": 65536, "maxTokens": 8192, "samplingParams": samp},
    ],
}
json.dump(d, open(p, "w"), indent=2)
print("    pi-Provider:", ", ".join(prov))
PY
s="$HOME/.pi/agent/settings.json"
if [ -f "$s" ] && grep -q '"tiel-coder"' "$s"; then
  cp "$s" "$s.bak"
  sed -i 's/"defaultProvider": *"local"/"defaultProvider": "llm"/; s/"tiel-coder"/"qwen-moe"/' "$s"
  echo "    globale pi-Settings: tiel-coder -> qwen-moe"
fi

echo "==> 7/7 Projekt-Konfiguration $HAIN_DIR/.pi/"
mkdir -p "$HAIN_DIR/.pi/prompts"
cat > "$HAIN_DIR/.pi/settings.json" <<'JSON'
{
  "$schema": "https://pi.dev/schemas/settings.schema.json",
  "defaultProvider": "llm",
  "defaultModel": "qwen-moe",
  "defaultThinkingLevel": "medium",
  "compaction": { "enabled": true, "reserveTokens": 12288, "keepRecentTokens": 12000 },
  "httpIdleTimeoutMs": 900000
}
JSON
cat > "$HAIN_DIR/.pi/APPEND_SYSTEM.md" <<'MD'
Hain project rules for local models:
- AGENTS.md is the law. One task per session, only the files the task card lists.
- Edit in small hunks. Never rewrite a whole file you did not create.
- After every edit run the task's "check" command and read the output.
- Same error twice: stop, set the task to `blocked: <reason>` in TASKS.md, explain in HANDOFF_LOG.md.
- Look at existing Kotlin/Gradle files first and copy their style.
MD
cat > "$HAIN_DIR/.pi/prompts/task.md" <<'MD'
---
description: Do one Hain task from TASKS.md
argument-hint: "<T-XXX>"
---
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Do exactly one task: $1.
Read only the spec sections listed under "read" for that task in docs/workflow/TASKS.md.
Change only the files listed under "files". Run the "check" command until it passes.
Then update TASKS.md (status), STATUS.md (Now, Last sessions) and append to HANDOFF_LOG.md.
If something is unclear, write a question into STATUS.md → "Questions for human",
set the task to `blocked: <reason>` and stop. Do not guess product decisions.
MD
cat > "$HAIN_DIR/.pi/prompts/next.md" <<'MD'
---
description: Do the next open Hain task
---
You are working on the Hain Android app. First read AGENTS.md completely and follow its
session protocol. Then read docs/workflow/STATUS.md.
Pick the first `todo` task in docs/workflow/TASKS.md whose dependencies are all `done`,
whose lane is not H and whose type is not `native`. Say which one, then do exactly that task.
Read only its listed spec sections, change only its listed files, run its "check" until it passes.
Then update TASKS.md, STATUS.md and append to HANDOFF_LOG.md.
If something is unclear: question into STATUS.md, set `blocked: <reason>`, stop.
MD
cat > "$HAIN_DIR/.pi/prompts/review.md" <<'MD'
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
MD
touch "$HAIN_DIR/.gitignore"
grep -qxF ".pi/settings.json" "$HAIN_DIR/.gitignore" || echo ".pi/settings.json" >> "$HAIN_DIR/.gitignore"

echo
echo "FERTIG. Weiter mit docs/workflow/LOCAL_MODELS.md"
echo "  source ~/.bashrc && llm start && cd $HAIN_DIR && pi"
