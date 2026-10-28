# LOCAL_MODELS.md — lokale Modelle für Hain (Fedora, RX 7600 8 GB)

Stand 28.10.2026. Einrichtung: `bash tools/setup/setup-local-agent.sh` im hain-Ordner.

## Das Prinzip: ein Server, immer nur ein Modell

| | Qwen3.6-35B-A3B (`qwen-moe`) | FrogNano-4B (`frognano`) |
|---|---|---|
| Art | MoE, 35 B gesamt, ~3 B aktiv | dicht, 4,7 B |
| Datei | UD-Q4_K_XL, 22,4 GB | Q6_K, 3,8 GB |
| Wo es liegt | ~6 GB VRAM + Rest im RAM (`n-cpu-moe 35`) | komplett im VRAM (~5 GB) |
| Tempo (geschätzt) | ~25–35 tok/s | deutlich schneller |
| Stärke | Kotlin, Gradle, mehrere Dateien, Fehlersuche | kleine, klar umrissene Aufgaben |
| Schwäche | langsamer, denkt viel | auf Python trainiert, schwächer bei Kotlin |

Beide zusammen passen **nicht** in 8 GB VRAM. Deshalb läuft nur **ein** Dienst, `llm-router` auf Port 8080:

- `--models-max 1`: Er hält nie mehr als ein Modell. Wählst du ein anderes, entlädt er zuerst das alte.
- `--models-memory-margin 1536`: Er lässt 1,5 GB VRAM frei (für Desktop und Browser).
- `--sleep-idle-seconds 900`: Nach 15 min ohne Anfrage gibt er VRAM und RAM frei. Die nächste Anfrage lädt automatisch neu (dauert ein paar Sekunden).
- `MemoryMax=36G`: harte RAM-Grenze, damit Fedora nie einfriert.
- `vram-guard-router`: Notbremse. Unter 1 GB freiem VRAM werden alle Modelle entladen.

Die alten Dienste `tiel-server` und `small-server` sind abgeschaltet. Tiel-Coder wird nicht mehr genutzt.

## Alltag

```bash
llm start          # Router starten (lädt noch nichts)
cd ~/hain && pi    # pi startet mit qwen-moe (Projekt-Standard)
```

In pi:

| Befehl | Was passiert |
|---|---|
| `/task T-003` | genau diese Aufgabe nach AGENTS.md |
| `/next` | nächste offene Aufgabe (ohne Lane H und ohne `native`) |
| `/review T-003` | Review, ändert keinen Code |
| `/model` | Modell wechseln: `qwen-moe` ↔ `frognano`. Der Router tauscht automatisch. |
| `/new` | neue Sitzung, bitte nach jeder Aufgabe |

Im Terminal:

| Befehl | Zweck |
|---|---|
| `llm status` | läuft der Router, welches Modell ist geladen |
| `llm qwen` / `llm frog` | Modell vorab laden (sonst lädt die erste Anfrage) |
| `llm off` | Modell entladen, Router bleibt an (z. B. vor dem Zocken) |
| `llm stop` | Router ganz aus |
| `llm vram` | VRAM-Belegung |
| `llm log` | Log live ansehen |

## Welches Modell wofür

| Aufgabe (siehe AGENT_PROMPTS.md) | Lokal |
|---|---|
| S · einfach (scaffold XS/S, strings, kleine ui, test) | `frognano` probieren. Scheitert es zweimal: `qwen-moe` |
| M · mittel (feature, alles in Größe M) | `qwen-moe`, Thinking medium |
| L · schwer (native/JNI, plan, Lane H) | nicht lokal: Cloud-Modell (Opus 5.5 / GPT-6.1 Sol xhigh) |
| Review | anderes Modell als der Umsetzer, z. B. Umsetzer `frognano` → Review `qwen-moe` oder Gemini |

Pro Sitzung nur **ein** Modell. Nicht zwei pi-Fenster mit verschiedenen lokalen Modellen gleichzeitig: Der Router würde ständig hin- und herladen. Cloud-Modell + ein lokales Modell parallel ist kein Problem.

## Nicht gleichzeitig laufen lassen

- Spiele, Blender, Video-Export oder ein zweites LLM-Programm (Ollama, LM Studio): vorher `llm off`.
- Android-Emulator mit GPU: braucht selbst VRAM. Lieber am echten Handy testen (`adb`), sonst `llm off`.
- Gradle-Builds sind okay, brauchen aber RAM (bis ~6 GB). Bei 48 GB RAM und 36 GB Grenze für den Router bleibt genug.

## Feineinstellung

Alles in `tools/setup/llm-models.ini` (wird nach `~/.config/llm/models.ini` kopiert). Nach Änderungen das Setup-Skript erneut laufen lassen oder die Datei kopieren und `systemctl --user restart llm-router`.

- **VRAM knapp / Absturz beim Laden von Qwen:** `n-cpu-moe` erhöhen (36, 37, 38). Nie über 39.
- **Qwen zu langsam, VRAM noch frei:** `n-cpu-moe` senken (34, 33). Mit `llm vram` prüfen, Ziel ≥ 1,5 GB frei.
- **Denkt zu lange:** `reasoning-budget` senken (z. B. 8192).
- **Kontext:** 64k reicht dank Compaction. Mehr kostet VRAM und Tempo.
- **Schneller Ladevorgang nach Pause:** `IDLE_SLEEP=3600 bash tools/setup/setup-local-agent.sh` (1 h statt 15 min).

## Fehlerhilfe

| Problem | Lösung |
|---|---|
| pi: „connection refused“ | `llm start`, dann `llm status` |
| Modell lädt nicht | `llm log` lesen. Meist zu wenig VRAM → `n-cpu-moe` hoch |
| Antworten leer, nur Gedanken | llama.cpp aktualisieren: `UPDATE_LLAMA=1 bash tools/setup/setup-local-agent.sh` |
| Jede Antwort verarbeitet den ganzen Prompt neu | `ctx-checkpoints`/`checkpoint-min-step` stehen in der ini, Router neu starten |
| Desktop ruckelt | `llm off`, dann `margin` erhöhen: `VRAM_MARGIN=2048 bash tools/setup/setup-local-agent.sh` |
| Zurück zum alten Stand | `~/.pi/agent/models.json.bak` und `settings.json.bak` zurückkopieren |
