# Host-Test Jan-v3.5-4B mit Hains Prompt (T-243, 05.11.2026)

> **Sandbox-Werte, keine Handymessung.** ARM Neoverse-V2, 2 Threads, 7 GB RAM, llama.cpp-Build aus dem
> Vergleichstest (`llm-test/upstream`, llama-server). Ein Sample pro Prompt. Gerätetests sind offen (unten).

## Aufbau

- Modell: `Jan-v3.5-4B-Q4_K_M.gguf` aus `janhq/Jan-v3.5-4B-gguf` Rev. `2b0fcab0` (sha256 `2f5305d9…c07a`, geprüft).
- Prompt: Hains deutscher System-Prompt (`system_full.de` mit Werkzeug-Regeln) + 15 Werkzeuge aus `tools.json`,
  gerendert mit dem Jinja-Template der GGUF. Zwei Reihenfolgen:
  - **template:** wie das Jinja (System-Text, dann Werkzeuge);
  - **hain:** Werkzeug-Block zuerst, System-Text danach – genau das, was `QwenPromptBuilder` baut (Golden-Tests).
- Rohes `/completion` (kein Server-Template, kein Server-Parser), ctx 8192 (App-Standard), Sampling wie
  `BrainFormat.QWEN`: Temperatur 0,7, top-p 0,8, top-k 20, min-p 0, keine Wiederholungsstrafe; kein Denken.
- Tool-Calls gelesen wie `RepairingToolCallParser` (JSON in `<tool_call>`); Bewertung nur der ersten Antwort, Regeln
  wie im Vergleichstest (`llm-test/score.py`: Werkzeugname, Argument-Muster, Umlaute).
- Prompts aus dem Vergleichstest: 6 mit Werkzeug (a1 Suche, a2 Wecker, a3 Wetter, a4 Rechnen, a6 Erinnerung, a8 Termin)
  und 4 ohne (b1–b4). Skript, Rohdaten und Logs: Sandbox `jan-host/` (`host_test.py`, `out_host_*.jsonl`, `run_*.log`),
  nicht im Repo.

## Ergebnis

| | hain (App) | template |
|---|---|---|
| Werkzeug richtig | **6/6** | 5/6 (a4 rechnet selbst: 409,50 € stimmt, aber ohne `calculate`) |
| Argumente richtig | **6/6** | 5/5 |
| unnötige Aufrufe | **0/4** | 0/4 |
| Prompt gesamt | 1.916 Token | 1.915 Token |
| erster Turn kalt (nichts gespeichert) | 83 s | 97 s |
| fester Teil (Snapshot) | 1.579 Token, **233 MB** | (ganzer Prefix gespeichert: 280 MB) |
| Snapshot laden + Rest lesen | 2,9 s + 337 Token in 18,6 s → **~21,5 s** | 3,7 s + 18 Token ¹ |
| Generierung | 6,2–7,4 t/s | 6,0–7,0 t/s |
| Spitzen-RSS (VmHWM, Server) | **5,79 GB** | 5,94 GB |

¹ Im template-Lauf wurde der ganze Prefix inkl. Kontext gespeichert, das ist zu optimistisch. In der App läge der
feste Teil bei dieser Reihenfolge nur vor „# Kontext“ (~1.800 von 6.480 Zeichen, geschätzt ~530 Token); der Werkzeug-
Block (~3.640 Zeichen, ~1.300 Token) stünde dahinter und müsste bei jedem neuen Chat neu gelesen werden (Host ~75 s
statt ~19 s). Deshalb baut `QwenPromptBuilder` die Werkzeuge nach vorn (ADR-24 §6) – die Werkzeug-Werte sind dabei
nicht schlechter (6/6 statt 5/6, ein Sample).

Die 337 Token nach dem Snapshot sind der Teil ab „# Kontext“ plus Frage. Im Test stehen dort auch die Werkzeug-Regeln aus
dem Vergleichstest (`user_custom_instructions`), daher ist der Rest hier größer als in der App mit leeren Notizen.
Auf dem Handy rechnet der Vergleichstest mit ~2,4× Host-PP (Schätzung ~66 t/s): Snapshot-Rest ~5 s, kalt ~30 s.

### Antworten (Auszug, hain)

- a2 Wecker: `{"time": "07:30", "label": "Samstag und Sonntag", "days": "Sa, So"}`; a8 Termin mit Ort und Umlauten
  korrekt (`Zülpicher Straße 12`, `Jörg`), dazu ein selbst erfundenes Ende 20:00.
- b1 „Hey, und dir? Köln ist ein bisschen kühl heute …“ – erfindet das Wetter ohne Werkzeug, Ton locker.
- b2 Katzenwitz ohne Pointe, b3 Himmelblau inhaltlich ungenau („das Rote und Blau wird …“), b4 Wetter/Klima gut.
- Wie im Vergleichstest: Tool-Verhalten stark, freie Antworten kurz und locker, mit einzelnen inhaltlichen Fehlern.

## Offen (Gerät)

- Tempo auf echten 8-/12-GB-Handys: Laden, kalter erster Turn, Snapshot-Start, t/s; Hitze/Akku bei längeren Chats.
- Speicher unter Last (Kamera/Browser im Hintergrund) auf 8 GB: wird Jan verdrängt, bricht das Laden ab?
- Snapshot-Datei (~233 MB im Cache) auf dem Gerät: Größe, Ladezeit, Verhalten nach „Cache leeren“.
- Antwortqualität im echten Chat (Werkzeugschleife mit echten Ergebnissen, Karten, Gedächtnis, Nachrichten-Briefing).
