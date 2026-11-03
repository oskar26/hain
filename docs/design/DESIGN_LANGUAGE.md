# DESIGN_LANGUAGE.md — one calm design for the whole app (T-010 redesign, 03.11.2026)

Owner brief (Oskar, 03.11.2026): *"Like the pictures I gave you, but don't overdo it."* and
*"Not single elements — everything should blend into one fitting app. Still minimalist and clean."*
Style reference: `REFERENCES_3D.md` (Wabi pills, soft glass tiles, light chat apps, Gemini message actions).

**Rule of thumb: when in doubt, less.** Calm, light, lots of white space, quiet accents.
No thick shadows, no colourful gradients outside the welcome tour and Pim, no busy animations.

All tokens live in `:core-ui` (`app.hain.ui.theme`, `app.hain.ui.motion`). Screens never pick their own
colour, radius, shadow or duration — they use these.

## 1. Font — Hanken Grotesk (decision 03.11.2026)

Oskar asked for a good, little-known, rather thin typeface instead of Inter.

| Considered | Why not / why |
|---|---|
| Inter (old) | everywhere, a bit heavy at small sizes |
| Geist, Manrope, DM Sans | well known; Manrope is wide |
| **Hanken Grotesk** ✅ | rare in apps, light and friendly, good umlauts (ä ö ü ß „ “), static weights 100–900, SIL OFL 1.1 |

| Weight | Use |
|---|---|
| Light 300 | big headings (`display`): greeting, tour, success |
| Regular 400 | all running text (`body`, `bodySmall`) |
| Medium 500 | titles, labels, buttons (`title`, `label`, `button`) |
| SemiBold 600 | only **bold** inside answers (markdown Bold maps to this file) |

- Files: `core-ui/src/main/res/font/hankengrotesk_{light,regular,medium,semibold}.ttf` (upstream v3.014,
  github.com/marcologous/hanken-grotesk). Licence text: `docs/specs/licenses/HankenGrotesk-OFL.txt`.
- "Gut lesbar" keeps **Atkinson Hyperlegible** (same sizes, bold headings). Code stays **JetBrains Mono**.
- Readability: running text 16 sp / 25 sp line height with +0.1 sp tracking (a thin font needs a hair of air);
  nothing the user must read is below 14 sp; text colours keep WCAG AA (`text` on `bg` ≈ 19:1,
  `textMuted` on `bg` ≈ 5.9:1 light / 7.2:1 dark, action icons ≥ 3:1). System font scale up to 200 % is respected.

| Style | size / line | weight |
|---|---|---|
| `display` | 30 / 38 | 300 |
| `title` | 20 / 28 | 500 |
| `body` | 16 / 25 | 400 |
| `bodySmall` | 14 / 20 | 400 |
| `label` | 13 / 18 | 500 |
| `button` | 15 / 20 | 500 |
| `code` | 14 / 20 JetBrains Mono | 400 |

## 2. Colours

The DESIGN_SYSTEM.md §2 palette stays (white / almost black, grove green `#16A36B`). New tokens:

| Token | Light | Dark | Use |
|---|---|---|---|
| `icon` | #7A817D | #8B948F | quiet grey action icons |
| `glass` | #FFFFFF @ 96 % | #1E2420 @ 95 % | floating tiles (menus) |
| `glassEdge` | #FFFFFF | #FFFFFF @ 12 % | thin light top edge of glass |
| `shadow` | #0D1F16 @ 12 % | #000000 @ 40 % | the one soft shadow colour |
| `scrim` | #0D0F0E @ 26 % | #000000 @ 55 % | behind drawer and dialogs |

Accent is used sparingly: send button, primary button, switches that are on, a chosen thumb, icon dots in Settings
(soft green circle `accentSoft` with a green icon — never solid green blocks).

## 3. Corners

Three radii + capsule, all continuous squircles (Capsule library):

| Token | Radius | Use |
|---|---|---|
| `HainRadius.SMALL` | 12 dp | chips, rows in lists and menus (`shapes.chip`, `shapes.row`) |
| `HainRadius.MEDIUM` | 20 dp | cards, bubbles, menus, tiles, search (`shapes.card`, `userBubble`, `menu`) |
| `HainRadius.LARGE` | 28 dp | sheets, hero surfaces (`shapes.sheet`) |
| capsule | – | composer, all buttons, segmented control, switch |

## 4. Depth — three levels only (`Modifier.hainSurface(level)`)

| Level | Looks like | Used for |
|---|---|---|
| `FLAT` | soft grey fill, no edge | user bubble, search field, chips, text fields, small buttons |
| `RAISED` | page colour + 1 dp hairline + 2 dp very soft shadow | composer, cards, settings tiles, suggestion cards, pack cards, secondary buttons |
| `FLOATING` | glass fill + light top edge + one 14 dp soft shadow | context menus / popovers |

No other shadows in the app. Dark mode uses the same levels (hairline carries the shape, the shadow is darker).
Real blur (Haze) stays limited to surfaces over scrolling content and is switched off by `MotionGovernor` on slow phones.

## 5. Motion — one language

| Token | Value | Use |
|---|---|---|
| `HainEase` | cubic (0.2, 0, 0, 1) | every tween |
| `FAST_MS` | 150 ms | small things appear/leave: message actions, highlight, list fade |
| `MENU_MS` | 180 ms | menus scale (0.9 → 1) + fade from their anchor corner, and back |
| `BASE_MS` | 240 ms | scrim, larger cross-fades |
| calm spring | damping 0.92, stiffness 500 | list item movement, switch thumb |
| drawer spring | damping 0.9, medium-low | drawer slide + chat scale |

Helpers: `hainTween(ms)`, `hainSpring()`, `motionSpec(moment)`. **All collapse to `snap()` when
`LocalReduceMotion` is on** (Settings → Look → "Weniger Bewegung", or system animations off). No endless
animation in the chat except the thinking glow while Pim works.

## 6. Components after the redesign

- **Chat:** answers as plain text; user bubble `FLAT`, tap → edit + copy fade in below it on the right (Gemini).
  Under each finished answer a quiet grey row, evenly spaced: copy (turns into a green check), share,
  thumbs up/down (kept on the phone), ask again. Read aloud joins once voice exists (Häppchen 10).
- **Composer:** raised white capsule, green round send button.
- **Drawer:** slides on the drawer spring; rows have 12 dp corners; the highlight covers the whole row incl. ⋯;
  ⋯ opens a compact glass menu right below it (scale + fade 180 ms from the ⋯ corner), the row stays marked.
- **Settings:** raised tiles with a soft green icon dot; choices = `SegmentedControl`; on/off = own `HainSwitch`.
- **Onboarding:** lighter mint wash (16 % top / 12 % bottom), success screen on the same wash (no dark island).

Follow-up suggestions under the last answer are **not** in yet: they need a second brain call per answer,
which costs time on the phone; revisit with a cheap prompt in a later Häppchen.
