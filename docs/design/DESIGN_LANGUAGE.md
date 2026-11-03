# DESIGN_LANGUAGE.md — one design for the whole app: calm, but with depth (T-011, 03.11.2026)

Owner brief (Oskar, 03.11.2026, after #20): *"Where is my 3D redesign? Where are my good design
decisions?"* — #20 took *"don't overdo it"* too literally and made the app almost flat. This
version keeps #20's calm order (one font, three radii, one motion language) and adds the **depth**
of his references (`REFERENCES_3D.md`, "AI style reference" 1–7): glass tiles with an inner coloured
light, small glossy 3D icons, domed buttons, a glossy Pim, a Wabi-like welcome cloud.

**Rule of thumb: calm, but with depth.** Every *object* of the app (a settings group, a tool card, a
pack, the message box, a menu, a button) is a soft 3D thing with light from the top left. The *chat
text itself* stays plain on white or almost black: no tiles behind answers, no colour in the
running text. Lots of white space. One design everywhere, no effect islands.

All tokens live in `:core-ui` (`app.hain.ui.theme`, `app.hain.ui.motion`, `app.hain.ui.components`).
Screens never pick their own colour, radius, shadow or duration — they use these.

| Before (#20) | Now (T-011) |
|---|---|
| raised cards = page colour + hairline + 2 dp shadow | glass tiles: white glass, coloured inner light, light edge, sheen, coloured floor glow |
| settings: same green dot on every tile | each group its own hue + its own 3D icon, the page itself sits on that glass |
| tool steps = grey chips | one glass tool card per answer, glowing in the first tool's colour, 3D icon per step |
| composer = white capsule | big white card, soft shadow, gentle green rim glow, round domed send |
| Auto/Fast/Thorough bar above the composer | brain button inside the composer → glass popover (T-084) |
| Pim: flat mint, dark dots, tiny gloss | glossy jelly Pim: pale → saturated, inner glow, light rim, white oval eyes |
| welcome tour on a mint wash | white page, cloud of shiny bubbles with motifs, round white Pim knob, two wide pills |

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

Four radii + capsule, all continuous squircles (Capsule library):

| Token | Radius | Use |
|---|---|---|
| `HainRadius.SMALL` | 12 dp | chips, rows in lists and menus (`shapes.chip`, `shapes.row`) |
| `HainRadius.MEDIUM` | 20 dp | cards, bubbles, menus, search (`shapes.card`, `userBubble`, `menu`) |
| `HainRadius.TILE` | 24 dp | glass tiles with inner light (`shapes.tile`, T-011) |
| `HainRadius.LARGE` | 28 dp | sheets, hero surfaces, the composer card (`shapes.sheet`, `shapes.composer`) |
| capsule | – | all buttons and pills, segmented control, switch, brain button |

## 4. Depth and light (T-011)

Light always comes **from the top left**. Four ways to give something depth, nothing else:

| Token | Looks like | Used for |
|---|---|---|
| `hainSurface(FLAT)` | soft grey fill, no edge | user bubble, search field, text fields, chips |
| `hainSurface(RAISED)` | page colour, light top edge, 6 dp soft shadow | secondary pills, small cards |
| `hainSurface(FLOATING)` | clear glass tile (below) with an 18 dp shadow, no floor glow | menus, popovers, the jump-down button |
| **`glassTile(hue)`** | white glass (dark: deep green-grey glass), the hue shines from inside around `lightAt`, a white light edge on top, a glossy sheen over the upper half, a soft shadow *and* floor glow in the same hue | settings groups and pages, tool cards, suggestion tiles, packs, the drawer, 3D badges |
| **`dome(color)`** | vertical gradient light → colour → deeper, white sheen, light top edge, coloured soft shadow | primary / dark pills, send + stop, the Pim knob |

Glow hues (`GlowHue`, soft and never neon): MINT (answers, suggestions, drawer), SKY (privacy, Wikipedia,
brain pack), LILAC (search), PEACH (look, calculator, plans), SUN (weather, packs), ROSE (about you),
TEAL (about, pages read, forest), CLEAR (menus). `glowStrength(dark)` holds the alphas: in dark mode the
inner light and shadow are stronger and the edge fainter, so the light really *glows* on dark glass.

Radii: SMALL 12, MEDIUM 20, **TILE 24** (glass tiles), LARGE 28 (sheets, **the composer card**), capsule
(buttons, pills). Real blur (Haze) stays limited to surfaces over scrolling content.

### 3D icons

`Hain3DIcon(Hain3D.X)` — 22 small glossy clay icons, one consistent set (own generation, see
`docs/specs/LICENSES.md`): weather, book, search, calculator, document, sparkles, phone, leaf, recycle,
gift, calendar, palette, two speech bubbles, heart, shield, tree, bolt, bulb, parcel, globe, head.
Light top left, transparent WebP, 192 px, 4–14 KB each (`core-ui/src/main/res/drawable-nodpi/ic3d_*.webp`,
copies in `assets/illustrations/3d/icons/`). `Hain3DBadge(icon, hue)` puts one in a small glowing glass tile.
Lucide stays for quiet action icons (copy, share, thumbs, menu, brain button).

### Pim (reference 7)

From 48 dp Pim is drawn glossy (`PimGloss.kt`): radial body gradient pale `#DDF9EA` → mint `#86DDAC` →
saturated `#27A56C` around the top-left light, an inner glow above the bottom, a thin light rim, a crisp
specular spot and **white oval eyes** (1.22× bigger) with a soft green shadow. Below 48 dp he stays flat
so he reads at icon size.

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

## 6. Components after T-011

- **Empty chat (reference 6):** small glossy Pim on the left, a big Light greeting by time of day ("Guten
  Morgen, / Hallo, / Guten Abend," + "was kann ich für dich tun?"), three suggestion glass tiles with a 3D
  icon whose colour shines from the icon (leaf/mint, recycle/teal, calendar/peach). With the keyboard open
  only Pim (48 dp) and the greeting stay.
- **Composer:** big white card (radius 28), soft shadow, green rim (brighter at the bottom) and a green floor
  glow; text grows to 6 lines, then scrolls inside; the bottom row holds the **brain button** (left) and the
  round green **send dome** (right), which becomes a dark stop dome while Pim answers.
- **Thinking picker (T-084):** brain icon (Lucide) in the composer, plus the level name when not Auto, on a
  soft green pill. Tap → glass popover above the composer (scale 0.9 → 1 + fade, 180 ms, from its bottom-left
  corner): Auto (sparkles) "Pim entscheidet selbst", Schnell (bolt) "antwortet sofort, ohne nachzudenken",
  Gründlich (bulb) "denkt länger nach, für schwierige Fragen", green check on the active one. Same DataStore
  value as Settings → Answers. TalkBack: "Denk-Stufe: Auto. Tippen zum Ändern"; rows are radio buttons.
- **Chat:** answers stay plain text. Tools of an answer = one glass tool card above it (3D icon, line, ✓/✕,
  orange online dot). Sources stay quiet chips. User bubble `FLAT`, max 85 % wide, folds after 14 lines.
- **Drawer:** a pane of glass lit mint from the top, rounded on the open side, glossy Pim + "Hain" on top,
  search, a raised "Neuer Chat" pill, rows as before; ⋯ menu = clear glass.
- **Settings:** hub = six glass tiles, each with its own hue and 3D icon (palette, bubble, heart, parcel,
  shield, tree); a group page shows its 3D badge + hint and puts all its controls on one glass tile lit from
  above in the group's colour.
- **Packs:** every pack is a glass tile (brain = sky + head, others = sun + parcel).
- **Buttons:** primary = green dome, DARK = almost-black dome, secondary = raised white pill; all press in to
  0.96 with a light tick.
- **Welcome tour (reference 1, Wabi):** white page with one faint mint glow, page 1 = a cloud of shiny
  see-through bubbles (weather, book, leaf, heart, forest in the middle + small empty ones, gently bobbing),
  the round white Pim knob overlapping its bottom, big calm headline (32 sp Light); pages 2–4 show their hero
  object inside a big glass bubble; two wide pills at the bottom (white "Überspringen", dark "Weiter", green
  "Los geht's" on the last page). Success: Pim cheers inside a big bubble, confetti + haptic stay.
- **Dark mode:** same structure; glass turns deep green-grey and the inner lights glow stronger.
- **Reduce motion:** every tween/spring snaps, Pim, bubbles and hero objects stand still, no confetti — the
  depth (glass, light, shadows) stays.

Screenshots of all of this (light + dark, long texts, 200 % font, 360 dp, landscape) are rendered by the CI
workflow `screens.yml` (Roborazzi) and kept in `docs/design/screens/`.

Follow-up suggestions under the last answer are **not** in yet: they need a second brain call per answer,
which costs time on the phone; revisit with a cheap prompt in a later Häppchen.
