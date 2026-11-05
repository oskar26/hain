# ADR-24 — Großes Gehirn: Underdog Poodle 4B als optionale Stufe für starke Handys

- **Datum:** 05.11.2026
- **Entschieden von:** Oskar (05.11. 06:53 „dann nehmen wir poodle“, korrigiert 06:55: nicht Standard,
  sondern die stärkere Stufe für starke Handys)
- **Status:** angenommen; Lizenzfrage offen (siehe unten)

## Entscheidung

1. **LFM3 1.7B bleibt das Standard-Gehirn für alle** (ADR-05 gilt weiter). Onboarding lädt nur LFM3,
   ohne Auswahl bleibt LFM3 aktiv, es gibt keine Migration und keinen Hinweis an LFM3-Nutzer.
2. **Underdog Poodle 4B (Q4_K_M, ~2,78 GB) ist ein freiwilliges „Großes Gehirn“** im Katalog
   (`tier: "strong"`). Es erscheint im Gehirn-Wähler und unter Modelle & Pakete → Gehirne nur auf Handys,
   auf denen es laufen kann, und wird nur dort „Empfohlen für dein Handy“ markiert, wo genug Speicher frei
   ist. Laden und Wechseln macht der Nutzer selbst.
3. **Name in der App: „Großes Gehirn · für starke Handys“**, nicht „Premium“. „Premium“ klingt nach
   Bezahlstufe; Hain hat kein Abo und soll nicht so wirken. Intern heißt die Stufe `strong`.
4. **Einordnung neben Auto / Schnell / Gründlich:** Das sind Antwort-Modi (Denken und Recherche-Budget)
   und bleiben unabhängig vom Gehirn. Das Gehirn bestimmt, *welches* Modell antwortet, der Modus, *wie lange*
   es nachdenkt und sucht. Poodle denkt nie (siehe 6); „Gründlich“ gibt ihm weiter das größere
   Recherche-Budget (mehr Suchen, mehr Seiten, eine Minute), nur ohne Denkblock.
5. **Speicher-Schwellen** (`min_ram_gb` 8, `recommend_ram_gb` 12):
   - Host-Messung mit Hains eigener Session (llama.cpp 39febd2, mmap, 2 Threads): Spitzen-RSS siehe
     `docs/research/poodle-host-test.md` (ctx 8192 = App-Standard, dazu 4096/2048). Davon sind ~2,8 GB
     gemappte Modelldatei (saubere Seiten, die Android bei Druck verwirft, was dann bremst).
   - Unter 7 GB (DeviceCheck `TOO_SMALL`): nicht angeboten. 7 GB: angeboten mit „könnte langsam sein“.
     Ab 8 GB: angeboten. Empfohlen erst ab 12 GB, weil Android einer Vordergrund-App auf 8-GB-Handys oft nur
     3–4 GB lässt und Poodle dann mit Kamera/Browser im Hintergrund um Seiten kämpft.
   - Rechenleistung wird nicht gemessen; RAM ist der Stellvertreter (Handys mit 12 GB haben fast immer
     aktuelle Kerne). Ein Tempo-Test auf dem Gerät ist ein offener Punkt.
6. **Chat-Format:** Qwen3.5-Template (ChatML, Tool-Calls als `<tool_call><function=…><parameter=…>`), in
   Kotlin nachgebaut (`QwenPromptBuilder`, Golden-Tests gegen das Jinja-Template aus der GGUF). Das Format wird
   beim Laden aus `general.architecture` der GGUF erkannt (`BrainFormat`), so nutzen Chat, Nachrichten-
   Zusammenfassung und Gedächtnis-Extraktion automatisch das richtige Template. **Denken aus** (leerer
   Think-Block, wie `enable_thinking=false`), Sampling laut Modellkarte: Temperatur 0,6, top-p 0,95,
   top-k 20, keine Wiederholungsstrafe.
7. **Schneller Start für alle Gehirne:** Der Speicher nach dem festen System-Prompt (Werkzeuge + Regeln) wird
   als Datei im Cache abgelegt (pro Modell, Kontextgröße und Prompt-Hash) und beim nächsten Start geladen,
   statt ~1.700 Token neu zu lesen. Nützt auch LFM3.

## Warum Poodle

Vergleichstest 05.11. (12 Modelle, 24 Prompts, Thinking aus, Sampling laut Modellkarte):

| | Poodle 4B Q4_K_M | LFM3 1.7B Q4_0 (Standard) |
|---|---|---|
| Werkzeug richtig gewählt | **11/11** | 9/11 |
| Argumente richtig | **11/11** | 7/8 |
| unnötige Werkzeug-Aufrufe | **0/13** | 4/13 |
| Deutsch (Blindnote) | **2** | 4, dazu englischer Denktext in Antworten |
| Datei | 2,78 GB | 1,05 GB |
| Handy-Schätzung PP/TG | ~65 / ~10 t/s | 177 / 29 t/s (gemessen) |

Poodle löst als eines von vier Modellen die Folgefrage „eine Viertelstunde früher“ richtig (6:45 → 6:30).
Preis: etwa dreimal langsamer, daher nur als freiwillige Stufe. Gemma 4 E4B (Deutsch-Note 1) bleibt ein
Kandidat für später, ist mit 5 GB Datei und ~6 GB RSS aber zu groß.

## Herkunft und Lizenz (offen, vor dem Merge klären)

- Datei: `mradermacher/Underdog-Poodle-4B-GGUF`, Revision `86a171fc122c3989f57826cfd8eb589d3b879b81`
  (25.10.2026), `Underdog-Poodle-4B.Q4_K_M.gguf`, 2.783.447.424 Bytes,
  sha256 `6068fc8fe09a94a8f53a815d06365c857847ec4966307acd7c6b0e4f625da686` (Download geprüft).
- Basismodell: `ConwayResearch/Underdog-Poodle-4B` (Feintuning von Qwen3.5-4B, Qwen: Apache-2.0).
- **Die Lizenz des Basismodells wurde am 05.11.2026 um 05:45 MEZ von Apache-2.0 auf CC BY-NC 4.0 umgestellt**
  (Commit `2ed6b9e` „Switch the license to CC BY-NC 4.0“). Die Quantisierung vom 25.10. stammt aus der
  Apache-2.0-Ausgabe (Commit `163db2f`, README „Apache-2.0, inherited from Qwen3.5-4B“, LICENSE = Apache 2.0);
  die GGUF-Metadaten sagen ebenfalls `apache-2.0`. Der Katalog pinnt genau diese Revision und verlinkt die
  Apache-Ausgabe.
- Einschätzung (keine Rechtsberatung): Apache-2.0 erlaubt die Nutzung der damals veröffentlichten Gewichte
  dauerhaft; neue Versionen fallen unter CC BY-NC 4.0. Hain lädt das Modell nur auf Wunsch herunter und
  bündelt es nicht. **Oskar entscheidet vor dem Merge**, ob das reicht oder ob ein anderes Modell (z. B.
  Jan-v3.5-4B, Apache-2.0, 10/11) die Stufe übernimmt.
