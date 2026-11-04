# VERSIONING.md — Versionsnummern von Hain

> Entschieden von Oskar am 04.11.2026. Gilt für `versionName`; die Zahl steht an genau einer Stelle:
> `hain.versionName` in `gradle.properties`.

## Schema

- **Ziel-Release bleibt 0.2.0.**
- **Jedes Plan-Häppchen N bekommt `0.1.N`:**

  | Häppchen | versionName |
  |---|---|
  | bis jetzt (main) | 0.1.3 |
  | 3b-2 + 4b-2 | 0.1.4 |
  | 5 | 0.1.5 |
  | 6 | 0.1.6 |
  | 7 | 0.1.7 |
  | 8 | 0.1.8 |
  | 9 | 0.1.9 |
  | 10 | 0.1.10 |
  | 11 (Release) | **0.2.0** |

- **Oskars Extras und Updates zwischendurch** hängen eine 4. Stelle an die aktuelle Stufe an:
  `0.1.N.x`. Beispiel: main ist 0.1.3, der Extras-PR setzt **0.1.3.1**, das nächste Extra 0.1.3.2.
  Mit dem nächsten Häppchen fällt die 4. Stelle wieder weg (0.1.4).

## versionCode

- `versionCode` ist eine reine Zählnummer für Android (immer größer = neuer). Die CI setzt sie auf die
  Laufnummer des APK-Workflows (`-Phain.versionCode=${{ github.run_number }}`), lokal ist sie 1.
- Die Update-Prüfung in der App vergleicht nur `versionCode` (siehe `version.json` am Release).

## Wo was steht

- `gradle.properties` → `hain.versionName`
- `app/build.gradle.kts` → liest `hain.versionName`, `hain.versionCode`, `hain.commit`, `hain.buildDate`
  (die letzten drei setzt die CI); `BuildConfig.GIT_SHA` und `BuildConfig.BUILD_DATE` zeigt
  „Über Hain“ an.
- Debug-Builds hängen `-debug` an (`0.1.3.1-debug`).
