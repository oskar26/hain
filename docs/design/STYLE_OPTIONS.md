# STYLE_OPTIONS.md — two style proposals, Oskar picks (03.11.2026)

Owner brief (Oskar, 03.11.2026, after merging #24): *"Ours looks too childish, and the 3D is a bit
over the top. I want green like before, not a Nothing look."* Wanted: less colourful and playful,
more professional and modern, **smaller radii**, **blur and gradients instead of glass**.

Both proposals are built into the app as one switchable theme:
**Settings → Look → "Style (test)"** (`ui_style`, default A). Same screens, only the theme differs.
Code: `core-ui/…/theme/HainStyle.kt` (`HainStyle.A` / `HainStyle.B`, `LocalHainStyle`, `look()`,
`shapesFor()`, `colorsFor()`), read by the shared primitives `glassTile`/`calmTile`, `hainSurface`,
`dome`, `Composer`, `Hain3DIcon`/`Hain3DBadge`, the onboarding bubbles and hero, and `JuicyPim`.

References: Oskar's Mobbin collection "hain" (Microsoft Copilot welcome, permissions, voice mode and
map answer; Mozi onboarding; Bevel and ChatGPT chat) and his own AI style images. The moodboards with
those foreign screens stay outside the repo (copyright); only Hain's own screenshots are here.

## Shared by both

- Hain green **#16A36B** stays the brand colour (dark mode #1FB877 / #22C17E).
- Grown-up, calm, modern. No colourful tiles, no glossy clay icons: **one Lucide line icon set**
  (stroke 1.75, `res/drawable/icl_*.xml`, ISC) for tools, suggestions, settings and onboarding.
- **Small radii**: cards 12–16 dp instead of 20–28 dp.
- 3D only as a hint: light, soft blur and gradients, no sheen, no glossy edges, no coloured floor glow.
- **Pim grown up**: drawn flat (no glossy jelly), no squish on tap; blinks; only celebrates by hopping.
- Confetti only in greens and white, fewer pieces.
- Light and dark. The chat history stays plain: no tiles behind answers.
- Kept from #24: brain picker inside the message box, long-text fixes (folding own messages,
  scrolling tables and code, composer that never pushes its buttons away).

## Stil A — "Lichtung" (clearing): clear and minimal

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

## Stil B — "Morgennebel" (morning mist): soft green light

Like the Copilot welcome gradient and voice pill, the health-app reference: a breath of green in the
page, surfaces that fade softly into mint, green light as a soft blur.

| Token | Light | Dark |
|---|---|---|
| bg | #FAFCFB | #0C1210 |
| surface | #F0F5F2 | #151F1A |
| surfaceHigh | #E6EEE9 | #1D2923 |
| border | #DDE6E1 | #24322B |
| text / textMuted | #0F1513 / #5A6660 | #ECEFED / #9AA39E |
| accent / accentSoft | #16A36B / #DCF1E5 | #22C17E / #133325 |
| icon | #66736C | #8B948F |

- Radii: chip/row 10 dp, card/bubble/menu/tile 16 dp, composer/sheet 20 dp, buttons as pills.
- Depth: tiles = vertical gradient white → 55 % accentSoft, a blurred green light (alpha 0.10, dark
  0.16) at the tile's light point, faint top edge, 4 dp green-grey shadow; menus 14 dp; buttons a
  gentle gradient (+10 % light on top) with a 3 dp shadow in their own colour.
- Message box: misty card lit from below, soft green rim (10 → 28 %), faint green floor glow.
- Icons: green line icons on pale green squares.
- Welcome: mint mist fading in from the top, discs that fade out at their edge with a green halo.

## Screenshots

Recorded by `*StyleScreens` (Roborazzi, workflow `screens.yml`) for both styles in light and dark:
empty chat, long answer, multi-line composer with the brain picker, drawer, settings, welcome.
Copies in `docs/design/screens/style_{a,b}_*.png`.

## Not changed yet

- App icon: waits for the pick.
- The glossy 3D files (`ic3d_*.webp`, `assets/illustrations/3d/`) stay in the repo for now; nothing
  draws them. After the pick the losing style and unused assets are removed.
