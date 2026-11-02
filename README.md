<p align="center">
  <img src="assets/mascot/v2/pim_v2_sheet.png" alt="Pim, the Hain mascot" width="420">
</p>

<h1 align="center">Hain</h1>

<p align="center">
  <b>Your AI. Your phone. Nobody else.</b><br>
  A private, local-first ChatGPT alternative for Android, in German and English.
</p>

<p align="center">
  <a href="https://github.com/oskar26/hain/releases/tag/nightly">Download nightly APK</a> ·
  <a href="docs/workflow/STATUS.md">Status</a> ·
  <a href="docs/concept/KONZEPT.md">Concept (DE)</a> ·
  <a href="CONTRIBUTING.md">Contributing</a>
</p>

---

> [!WARNING]
> **Hain is in early development (alpha).** Things break, features are missing, and chats or settings may be reset between versions. Please don't rely on it for anything important yet. Feedback is very welcome!
> **Hain ist noch in früher Entwicklung (Alpha).** Es kann abstürzen, Funktionen fehlen noch, und Chats oder Einstellungen können zwischen Versionen verloren gehen. Freu mich über Feedback!

A *Hain* is a small, sheltered grove: a quiet place that belongs to you. Hain runs a language model **directly on your phone** with llama.cpp. You can chat, search, read photos and PDFs, talk, and let it remember things – **no cloud, no account, no API keys**. Hain only goes online when you ask for something from the internet, and it always shows you where the request goes.

Your guide is **Pim**, a small mint-green sprout.

## Principles

- **Local first.** The model, your chats and your memory stay on the device, encrypted.
- **Online only when visible.** Every network request goes through one gate and shows up in a log you can read.
- **Own code, no forks.** Libraries like llama.cpp are dependencies (git submodule), never copied or modified.
- **Light on storage.** A small base pack (~1.1 GB brain), everything big is an optional download.
- **Simple for everyone.** Plain language, no jargon ("knowledge pack" instead of "model"), large text, light and dark.
- **Honest.** Hain can be wrong and does not replace a doctor, a lawyer or therapy. Actions that reach other people (SMS, calls, mail) always need your OK.
- **No tracking.** No telemetry, no ads, no Google Play Services, no Firebase.

## Who it is for

People who care about privacy but are not tech experts, and people who are critical of power-hungry data centres. Teachers, students, seniors with their first smartphone, F-Droid fans – everyone who wants a helpful AI without handing their data to anyone.

## Features (version 1 goal)

| Area | What Hain does | State |
|---|---|---|
| Chat | Streaming answers, Markdown, code, thinking block, stop, regenerate, copy, share | ✅ basic |
| First start | Playful welcome tour with Pim, downloads the brain (LFM3-1.7B, 1.0 GB) with resume and checksum, celebration screen | ✅ |
| Chats | Saved and encrypted, search, rename, pin, delete, export | 🔨 in progress |
| Settings | Answer mode (fast / auto / thorough), theme, large text, own instructions, knowledge packs | 🚧 partly (packs screen; no picker yet) |
| Tools | Calculator & units, weather, Wikipedia, web search without API key, read web pages | 🗓 planned |
| Phone actions | Alarm, timer, calendar entry, navigation – always confirmed | 🗓 planned |
| News | Standard mix (incl. junge Welt), standard + own feeds, or only own feeds | 🗓 planned |
| Answer cards | Lists, tables, checklists, weather and source cards instead of plain text | 🗓 planned |
| Memory | "Remember that …", a memory page you can edit, forget anything | 🗓 planned |
| Documents | Ask your PDFs and photos of text (PP-OCRv5), answers with sources | 🗓 planned |
| Voice | Dictation (Moonshine), read aloud, Pim's own voice | 🗓 planned |
| On the phone | Share menu target, widget, quick tile, default assistant | 🗓 planned |
| Mini-apps | Many key-free integrations (e.g. KVB / Transitous), switchable, shown as trees in Pim's forest | 🗓 later |
| Images | Erase objects, remove background, upscale; optional image generation pack | 🗓 later |

## Knowledge packs

| Pack | Model | Size | Purpose |
|---|---|---|---|
| Brain (base) | LFM3-1.7B QAD Q4_0 | 1.0 GB | chat, tools, memory |
| Remember & files | LFM2.5-Embedding-350M | ~0.2 GB | search in memory, chats, files |
| Reading text | PaddleOCR PP-OCRv5 mobile (Latin) | 12 MB | PDFs and photos, without Google |
| Dictation | Moonshine Small | ~0.12–0.25 GB | microphone, DE + EN |
| See + talk (optional) | LFM3-Omni-2B | 1.65 GB | photos, voice mode |
| Image generation (optional) | SD 1.5 + DMD2 | 1.1 GB | only phones with 8 GB RAM+ |

Details: [docs/specs/MODELS.md](docs/specs/MODELS.md).

## Install

1. Open the [nightly release](https://github.com/oskar26/hain/releases/tag/nightly) on your phone and download `hain-debug.apk`.
2. Allow installing from your browser, install, open Hain.
3. On first start Hain downloads the brain (1.1 GB, Wi-Fi recommended).

Every merge to `main` builds a new nightly. It is signed with the same debug key, so it installs as an update. Requires **Android 10 (API 29)** or newer; 6 GB RAM or more is recommended.

## Build from source

```bash
git clone --recursive https://github.com/oskar26/hain.git
cd hain
./gradlew assembleDebug                  # full build incl. llama.cpp (needs NDK 27 + CMake 3.31)
./gradlew assembleDebug -Phain.native=false   # Kotlin only, no native code
./gradlew testDebugUnitTest ktlintCheck detekt
python3 tools/check_strings.py           # every UI string in EN and DE
```

JDK 21 is required (see [KNOWN_ISSUES.md](docs/workflow/KNOWN_ISSUES.md)). CI builds the APK for every pull request (`.github/workflows/apk.yml`).

## Tech

Kotlin + Jetpack Compose (no Material 3, own design system), Hilt, Room (encrypted), WorkManager, OkHttp, llama.cpp via JNI with runtime CPU selection. Modules: `app`, `core-*` (llm, data, tools, ui, cards, memory, docs, voice, models), `feature-*` (chat, onboarding, settings, news, memory, image).
Architecture: [docs/specs/ARCHITECTURE.md](docs/specs/ARCHITECTURE.md) · Design: [docs/specs/DESIGN_SYSTEM.md](docs/specs/DESIGN_SYSTEM.md) · Libraries & licences: [docs/specs/LICENSES.md](docs/specs/LICENSES.md).

## Status and roadmap

| Version | What | When |
|---|---|---|
| nightly | Every merge to `main`, for testers | ongoing |
| **0.2.0** | First version for friends: chat that stays on your phone, settings, knowledge packs, tools (calculator, weather, Wikipedia, web search, timer), news, memory, private chats | **planned for 6 Nov 2026** |
| 1.0 | Everything in the [concept](docs/concept/KONZEPT.md): documents and photos, voice, mini-apps "forest", F-Droid | 2027, no fixed date |

Dates are goals, not promises. The detailed plan is in [docs/workflow/ROADMAP.md](docs/workflow/ROADMAP.md), the current state in [docs/workflow/STATUS.md](docs/workflow/STATUS.md).

## How AI is used

**In the app:** Hain runs an open language model (LFM3 1.7B by Liquid AI) on your phone. It can be wrong, make things up, or sound more sure than it is. Check anything important, especially health, money and legal questions. Your chats are not sent anywhere and are not used to train any model.

**Building the app:** Hain is made by one person (Oskar) together with AI coding agents. Large parts of the code, docs and graphics are written or drafted by AI (an AI assistant plus local models via llama.cpp). Every change goes through a pull request that a human reviews, tests and merges. If you find code that looks copied or wrong, please open an issue.

## Privacy

No account, no analytics, no ads, no Google Play Services, no Firebase. Hain only goes online when you ask for something from the internet (for example the weather or a search), or to download a knowledge pack. Details: [docs/specs/ARCHITECTURE.md](docs/specs/ARCHITECTURE.md).

## Contributing

Ideas, bug reports and pull requests are welcome. Start with **[CONTRIBUTING.md](CONTRIBUTING.md)**. The easiest way to help: install the nightly, use it, and [open an issue](https://github.com/oskar26/hain/issues) with what broke or what you'd like.

## Working on Hain

Humans and coding agents start with **[AGENTS.md](AGENTS.md)**: one task from [TASKS.md](docs/workflow/TASKS.md) per session, one commit per task (`T-XXX: title`), all checks green, status and handoff log updated.

## Auf Deutsch

Hain ist ein privater KI-Assistent fürs Android-Handy, der komplett auf dem Gerät läuft: chatten, suchen, Fotos und PDFs verstehen, sprechen, sich Dinge merken. Keine Cloud, kein Konto, keine API-Schlüssel. Ins Internet geht Hain nur, wenn du etwas suchst, und zeigt dir genau, wohin die Anfrage geht. Das ganze Konzept steht in [docs/concept/KONZEPT.md](docs/concept/KONZEPT.md).

**Stand:** Hain ist noch in Entwicklung. Version 0.2.0 für Freundinnen und Freunde ist für den 6. November 2026 geplant. Beim Programmieren helfen KI-Agenten, jede Änderung wird aber von einem Menschen geprüft. Die KI in der App kann sich irren, prüf wichtige Antworten lieber nach. Mitmachen: Fehler und Wünsche einfach als [Issue](https://github.com/oskar26/hain/issues) melden, siehe [CONTRIBUTING.md](CONTRIBUTING.md).

## Licence

[GPL-3.0](LICENSE). Model weights are not bundled and keep their own licences (e.g. LFM Open License v1.0).
