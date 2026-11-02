# Hain – Design-Referenzen (Mobbin-Sammlung „hain", Oskar, 02.11.2026)

Entscheidung Oskar (02.11.2026): **leichter 3D-Touch** – einzelne weiche 3D-Objekte und Elemente mit Tiefe, Rest bleibt klar und flach (ChatGPT-ähnlich, Hain-Grün #16A36B, kein Material 3).

## Referenzen und was wir übernehmen
| App | Bereich | Übernehmen für Hain |
|---|---|---|
| Microsoft Copilot | Willkommen | Weicher Verlauf (Mint → helles Gelbgrün), große Begrüßung „Hi, ich bin Pim", ruhige Knöpfe unten |
| Microsoft Copilot | Berechtigungen | Liste mit kleinem Icon + 1 Satz Erklärung pro Punkt, schwarzer Pill-Knopf „Weiter" |
| Microsoft Copilot | Sprachmodus | Himmel-/Wolkenverlauf, „Ich höre zu", Pill-Leiste mit X / Mikro / Zahnrad (Häppchen 10) |
| Microsoft Copilot | Chat mit Ortskarte | Karten-Antwort mit Karte + Ort (Häppchen 6) |
| Phantom | Erfolg | Dunkler Screen, verspielte bunte Formen + Maskottchen, groß „Hi!" / „Alles bereit!", Pill-Knopf „Los geht's" → Vorbild Download-Erfolg |
| Bevel | Chat-Onboarding | Blob-Avatar (bei uns Pim) + Begrüßung + 3 Vorschlagszeilen (gleich breit, mit Icon) |
| Grok | Leerer Chat / Stimme | Viel Weißraum, blasses Logo mittig, Verlaufs-Wellenleiste (Häppchen 10) |
| ChatGPT | Chat | Saubere Antworten mit Überschrift, Listen, Schritten; dezente User-Bubble |
| Opal | Onboarding | Ein leuchtendes 3D-Objekt mittig als Held, kurzer Satz, „Tippen zum Weiterkommen" |
| Mozi | Onboarding-Flow (23 Screens) | Schrittweiser Flow mit Fortschritt, ein Gedanke pro Screen |

## Umsetzung „leichter 3D-Touch"
- Pim in 3D-Optik: radialer Verlauf (heller oben links), Glanzpunkt, weicher Kontaktschatten; Squash & Stretch beim Hüpfen, Wackeln wie Gelee, Blinzeln, reagiert auf Tippen. Alles in Compose (Canvas/Brush, Spring-Animationen), keine 3D-Engine, kein Rive.
- Held-Objekte pro Onboarding-Screen (Opal-Stil): glänzender Tropfen, Blatt, Samen, kleiner Baum (Wald/Mini-Apps), Schloss/Handy (alles bleibt am Handy). Als Compose-Gradients oder vorgerenderte WebP (transparent, ≤150 KB) in assets/illustrations/3d/.
- Elemente mit Tiefe: Senden-Knopf und Haupt-Pills mit leichtem Innenverlauf + weichem Schatten, drücken sich beim Tippen ein (Scale 0.96 + Haptik); Vorschlags-Chips mit kleinem 3D-Icon.
- Hintergründe: zarte Verläufe nur auf Onboarding/Erfolg; Chat bleibt weiß bzw. fast schwarz.
- Erfolg nach Download: Phantom-Stil + Pim jubelt + Konfetti + Haptik.
- „Bewegung reduzieren" schaltet Hüpfen/Wackeln/Konfetti ab; Dark Mode beachten; keine Dauer-Animationen im Chat außer beim Denken.
- Einstellungen (Oskar 02.11.): übersichtlicher, Gruppen mit Icon, Unterseiten.
- Denk-Verlauf (Oskar 02.11., wie Copilot): weicher, wandernder Grün/Mint-Verlauf von unten nach oben, oben transparent, nur solange Pim denkt/arbeitet.
