# ADR-24 — Großes Gehirn: Jan-v3.5-4B als optionale Stufe für starke Handys

- **Datum:** 05.11.2026
- **Entschieden von:** Oskar (05.11. 06:53 „dann nehmen wir poodle“, korrigiert 06:55: nicht Standard, sondern die
  stärkere Stufe für starke Handys; **05.11. 08:02: „Jan statt Poodle“**)
- **Status:** angenommen

## Entscheidung

1. **LFM3 1.7B bleibt das Standard-Gehirn für alle** (ADR-05 gilt weiter). Onboarding lädt nur LFM3, ohne Auswahl
   bleibt LFM3 aktiv, es gibt keine Migration und keinen Hinweis an LFM3-Nutzer.
2. **Jan-v3.5-4B (Q4_K_M, ~2,72 GB, Apache-2.0) ist ein freiwilliges „Großes Gehirn“** im Katalog (`tier: "strong"`).
   Es erscheint im Gehirn-Wähler und unter Modelle & Pakete → Gehirne nur auf Handys, auf denen es laufen kann, und wird
   nur dort „Empfohlen für dein Handy“ markiert, wo genug Speicher da ist. Laden und Wechseln macht der Nutzer selbst.
3. **Name in der App: „Großes Gehirn · für starke Handys“**, nicht „Premium“: Das klänge nach Bezahlstufe, und Hain soll
   nicht so wirken. Intern heißt die Stufe `strong`.
4. **Einordnung neben Auto / Schnell / Gründlich:** Das sind Antwort-Modi (Denken und Recherche-Budget) und bleiben
   unabhängig vom Gehirn. Das Gehirn bestimmt, *welches* Modell antwortet, der Modus, *wie lange* es nachdenkt und sucht.
   Jan denkt nie (siehe 6); „Gründlich“ gibt ihm weiter das größere Recherche-Budget, nur ohne Denkblock.
5. **Speicher-Schwellen** (`min_ram_gb` 8, `recommend_ram_gb` 12), geprüft am Jan-Spitzenwert:
   - Vergleichstest (llama-server, ctx 4096): Spitzen-RSS 5,36 GB. Host-Test mit Hains Prompt (ctx 8192 = App-Standard,
     2 Threads, mmap): 5,8–5,9 GB, davon ~2,7 GB gemappte Modelldatei (saubere Seiten, die Android bei Druck verwirft,
     was dann bremst). Details: `docs/research/jan-host-test.md`.
   - Das liegt nur ~0,3 GB unter Poodle (5,66 GB), daher bleiben die Schwellen: unter 7 GB (DeviceCheck `TOO_SMALL`)
     nicht angeboten, 7 GB angeboten mit „könnte langsam sein“, ab 8 GB angeboten, empfohlen erst ab 12 GB, weil Android
     einer Vordergrund-App auf 8-GB-Handys oft nur 3–4 GB lässt.
   - Rechenleistung wird nicht gemessen; RAM ist der Stellvertreter. Ein Tempo-Test auf dem Gerät ist offen.
6. **Chat-Format:** Jan hat die Qwen3-4B-Architektur (`general.architecture = qwen3`) und das Qwen3-Template ohne
   Denken: ChatML, Tool-Calls als JSON in `<tool_call>`. In Kotlin nachgebaut (`QwenPromptBuilder`, Golden-Tests gegen
   das Jinja-Template aus der gepinnten GGUF, `prompts/jan-v3.5-4b_chat_template.jinja`). Das Format wird beim Laden aus
   der GGUF erkannt (`BrainFormat`), so nutzen Chat, Nachrichten und Gedächtnis-Extraktion automatisch das richtige
   Template. Einzige Abweichung: Der Werkzeug-Block steht vor Hains System-Text, damit Werkzeuge + Regeln ein fester,
   speicherbarer Block bleiben (7). Der Host-Test hat beide Reihenfolgen geprüft (Hain-Reihenfolge 6/6 Werkzeug, 6/6
   Argumente, 0/4 unnötig; Template-Reihenfolge 5/6, 5/5, 0/4). **Kein Denken** (das Template kennt keins), Sampling laut
   Modellkarte: Temperatur 0,7, top-p 0,8, top-k 20 (min-p 0, keine Wiederholungsstrafe wie im Vergleichstest).
7. **Schneller Start für alle Gehirne:** Der Speicher nach dem festen System-Prompt (Werkzeuge + Regeln, alles vor
   „# Kontext“) wird als Datei im Cache abgelegt (pro Modell, Kontextgröße und Prompt-Hash) und beim nächsten Start
   geladen, statt ihn neu zu lesen. Bei Jan ~1.580 Token, Datei ~233 MB (Host). Nützt auch LFM3.

## Warum Jan (und nicht mehr Poodle)

Zuerst war Underdog Poodle 4B gewählt (bestes Tool-Verhalten im Vergleichstest). **Am 05.11.2026 um 05:45 MEZ wurde die
Lizenz des Poodle-Basismodells (`ConwayResearch/Underdog-Poodle-4B`) von Apache-2.0 auf CC BY-NC 4.0 umgestellt**
(nicht kommerziell). Auch wenn die zuvor veröffentlichte Apache-Ausgabe weiter nutzbar wäre, will Hain eine
eindeutige, GPL-verträgliche Lizenz ohne Streitfrage. Oskar hat deshalb um 08:02 Jan gewählt, die im Vergleichstest
schon genannte Alternative.

Vergleichstest 05.11. (12 Modelle, 24 Prompts, Thinking aus, Sampling laut Modellkarte):

| | Jan-v3.5-4B Q4_K_M | Poodle 4B Q4_K_M | LFM3 1.7B Q4_0 (Standard) |
|---|---|---|---|
| Werkzeug richtig gewählt | 10/11 | 11/11 | 9/11 |
| Argumente richtig | 9/10 | 11/11 | 7/8 |
| unnötige Werkzeug-Aufrufe | **0/13** | 0/13 | 4/13 |
| Deutsch (Blindnote) | 3 | 2 | 4 |
| Datei | 2,72 GB | 2,78 GB | 1,05 GB |
| Host PP/TG (Rebench) | **27,4 / 8,2 t/s** | 13,2 / 6,3 t/s | 73,3 / 21,7 t/s |
| Handy-Schätzung PP/TG | ~66 / ~11 t/s | ~65 / ~10 t/s | 177 / 29 t/s (gemessen) |
| Lizenz | **Apache-2.0** | seit 05.11. CC BY-NC 4.0 | LFM Open License v1.0 |

Jan ist der schnellste der 4B-Kandidaten und macht keine unnötigen Aufrufe. Schwächen aus dem Test: einzelne Ausrutscher
im Ton (Jan hat eine eingebaute lockere „Persönlichkeit“, Hains System-Prompt hält sie meist im Zaum) und die Folgefrage
„eine Viertelstunde früher“ (6:45 → 6:20 statt 6:30). Preis wie bei Poodle: etwa dreimal langsamer als LFM3, daher nur
als freiwillige Stufe. Gemma 4 E4B (Deutsch-Note 1) bleibt ein Kandidat für später, ist mit 5 GB Datei aber zu groß.

## Herkunft und Lizenz

- Datei: `janhq/Jan-v3.5-4B-gguf`, Revision `2b0fcab0066198e78f7c004a0815179fbce13985` (24.03.2026),
  `Jan-v3.5-4B-Q4_K_M.gguf`, 2.716.066.656 Bytes,
  sha256 `2f5305d93100ad9105eeb47c45ef1af99ba6d568c734fc267b0628b3be41c07a` (Download am 05.11. geprüft, gleich dem
  LFS-Wert der Hugging-Face-API).
- Lizenz: Apache-2.0 laut Modellkarte (`license: apache-2.0`) von `janhq/Jan-v3.5-4B-gguf` und `janhq/Jan-v3.5-4B`
  (geprüft 05.11.2026). Basis: `janhq/Jan-v3-4B-base-instruct` (Qwen3-4B-Architektur, Qwen: Apache-2.0).
- Hain lädt das Modell nur auf Wunsch herunter und bündelt es nicht; `licence` + `licence_url` stehen im Katalog und in
  Modelle & Pakete.
