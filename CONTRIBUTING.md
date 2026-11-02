# Contributing to Hain

Thanks for wanting to help! Hain is a small project in early development, so every bug report and idea counts.

*Deutsch weiter unten.*

## Ways to help

1. **Test it.** Install the [nightly APK](https://github.com/oskar26/hain/releases/tag/nightly) and use it like a normal app.
2. **Report bugs.** Open an [issue](https://github.com/oskar26/hain/issues) with: what you did, what happened, what you expected, your phone model and Android version, and a screenshot if possible.
3. **Suggest ideas.** Also as an issue. Explain the problem you want solved, not only the solution.
4. **Translate.** All texts live in `values/strings.xml` (English) and `values-de/strings.xml` (German) in each module. New languages are welcome.
5. **Write code.** See below.

## Writing code

- Read **[AGENTS.md](AGENTS.md)** first. It explains the structure, the rules and what is being worked on. This applies to humans and AI coding agents.
- Pick a task from [docs/workflow/TASKS.md](docs/workflow/TASKS.md), or comment on an issue that you want to take it.
- One task per pull request, one commit per task: `T-XXX: Short title`.
- Before you open the PR, run:

  ```bash
  ./gradlew ktlintFormat
  ./gradlew --continue assembleDebug testDebugUnitTest ktlintCheck detekt
  python3 tools/check_strings.py
  ```

- Every visible text needs an English and a German version, in plain language (German uses "du").
- Update the docs in the same PR: `README.md` (feature status), `docs/workflow/TASKS.md`, `STATUS.md`, `HANDOFF_LOG.md`.

## Rules that keep Hain private

- No telemetry, analytics, ads, Google Play Services or Firebase.
- Every internet request goes through `NetworkGate` and must be visible to the user.
- Actions that leave the phone (sending, booking, deleting) always ask the user first.
- New libraries only if they are compatible with GPL-3.0 and listed in [docs/specs/LICENSES.md](docs/specs/LICENSES.md).
- `third_party/llama.cpp` is a submodule. Don't edit it.

## Using AI to contribute

AI coding tools are fine and used a lot here. You are still responsible for what you submit: build it, test it, understand it, and say in the PR which tool you used. No code copied from projects with incompatible licences.

## Behaviour

Be kind and patient. Many people here are new to open source. Criticise code, not people.

## Licence

By contributing you agree that your contribution is licensed under [GPL-3.0](LICENSE).

---

## Auf Deutsch

Schön, dass du helfen willst! Hain ist noch ganz am Anfang, jede Rückmeldung hilft.

- **Testen:** [Nightly-APK](https://github.com/oskar26/hain/releases/tag/nightly) installieren und einfach benutzen.
- **Fehler melden:** Ein [Issue](https://github.com/oskar26/hain/issues) aufmachen: was du gemacht hast, was passiert ist, was du erwartet hast, Handy-Modell, Android-Version und am besten ein Screenshot.
- **Ideen:** Auch als Issue. Beschreib, welches Problem du lösen willst.
- **Übersetzen:** Texte liegen in `values/strings.xml` (Englisch) und `values-de/strings.xml` (Deutsch).
- **Code:** Erst [AGENTS.md](AGENTS.md) lesen, dann eine Aufgabe aus [TASKS.md](docs/workflow/TASKS.md) nehmen. Ein Pull Request pro Aufgabe, alle Checks grün, Texte immer auf Englisch und Deutsch.
- **KI-Tools** sind erlaubt. Du bist trotzdem verantwortlich für deinen Code und schreibst im PR dazu, welches Tool du benutzt hast.
- **Umgang:** Freundlich bleiben. Kritik am Code, nicht an Menschen.
