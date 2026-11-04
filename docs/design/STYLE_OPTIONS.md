# STYLE_OPTIONS.md — Hain's style: **Lichtung** (decided 03.11.2026)

> **Decision (Oskar, 03.11.2026, after merging #25): Stil A „Lichtung“.** It is now the only style.
> Stil B „Morgennebel“, the "Style (test)" switch (`ui_style`), the glossy 3D icons and the glossy
> jelly Pim of #24 are removed (T-087). Kept: the brain picker inside the message box and the
> long-text fixes. The app icon and the website follow in a separate PR (`hark/lichtung-web`).

Owner brief (Oskar, 03.11.2026, after merging #24): *"Ours looks too childish, and the 3D is a bit
over the top. I want green like before, not a Nothing look."* Wanted: less colourful and playful,
more professional and modern, **smaller radii**, **blur and gradients instead of glass**.

Code: `core-ui/…/theme/Lichtung.kt` (`Lichtung` constants, `LichtungShapes`, `lichtungColors()`),
always provided by `HainTheme` and read by the shared primitives `glassTile`/`calmTile`,
`hainSurface`, `dome`, `Composer`, `Hain3DIcon`/`Hain3DBadge`, the onboarding bubbles and hero.
History: #25 built two proposals (A „Lichtung“, B „Morgennebel“) behind a test switch; this file
keeps only the winner.

References: Oskar's Mobbin collection "hain" (Microsoft Copilot welcome, permissions, voice mode and
map answer; Mozi onboarding; Bevel and ChatGPT chat) and his own AI style images. The moodboards with
those foreign screens stay outside the repo (copyright); only Hain's own screenshots are here.

## Principles

- Hain green **#16A36B** stays the brand colour (dark mode #1FB877 / #22C17E).
- Grown-up, calm, modern. No colourful tiles, no glossy clay icons: **one Lucide line icon set**
  (stroke 1.75, `res/drawable/icl_*.xml`, ISC) for tools, suggestions, settings and onboarding.
- **Small radii**: chips 8, cards 12, sheets 16 dp instead of 20–28 dp.
- No 3D: hairlines instead of shadows, no sheen, no glossy edges, no coloured floor glow.
- **Pim flat and calm, but charming** (T-089): flat colours, the leaf always attached to the head, many small idle moods (docs/design/PIM_MOTION.md).
- Confetti only in greens and white, fewer pieces.
- Light and dark. The chat history stays plain: no tiles behind answers.
- Kept from #24: brain picker inside the message box, long-text fixes (folding own messages,
  scrolling tables and code, composer that never pushes its buttons away).

## Lichtung (clearing): clear and minimal

Like the Copilot permission list and map answer, ChatGPT's composer: white or near-black pages,
a hairline instead of shadows, green only where you act.

| Token | Light | Dark |
|---|---|---|
| bg | #FFFFFF | #0E1110 |
| surface | #F5F6F5 | #171B19 |
| surfaceHigh | #EDEFEE | #202523 |
| border | #E5E8E6 | #272C2A |
| text / textMuted | #0E1110 / #5E6561 | #ECEFED / #9AA39E |
| accent / accentSoft | #16A36B / #E8F5EE | #1FB877 / #14271E |
| icon | #6E7571 | #8B948F |

- Radii: chip/row 8 dp, card/bubble/menu/tile 12 dp, composer/sheet 16 dp, buttons 12 dp (no pills).
- Depth: tiles 1 dp shadow + 1 px border; menus 10 dp soft shadow; buttons flat, no shadow.
- Icons: line icons in the text colour (82 % alpha); badges on a soft grey square.
- Welcome: plain page, discs with a hairline, icons in the text colour.

## Screenshots

Recorded by `*StyleScreens` (Roborazzi, workflow `screens.yml`) in light and dark: empty chat, long
answer, multi-line composer with the brain picker, drawer, settings, welcome
(`build/outputs/roborazzi/styles/lichtung_*.png`). Copies in `docs/design/screens/`.
