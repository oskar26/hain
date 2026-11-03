# STYLE_OPTIONS.md — two styles to choose from (03.11.2026)

Owner feedback on the 3D redesign (#24, Oskar, 03.11.2026 21:17): *"Far too colourful and playful,
not professional and modern as I wanted. Look at Mobbin and my pictures again. A smaller border
radius. More of a Nothing look (no dot matrix, but the blur and so on). Blur and gradients rather
than glass. If unsure, work out two visual styles with screenshots and I'll decide."*

So this branch ships **two styles** behind one switch (`HainStyle.A` / `HainStyle.B` in
`:core-ui`, `app.hain.ui.theme.HainStyle`). Same screens, same components; only tokens and a few
surface recipes differ. Test switch in the app: **Settings → Look → Style (test)**. Default: A.
Once Oskar has chosen, the other style and the switch are removed.

## What the research said

**Mobbin collection "hain"** (Copilot ×4, Bevel, ChatGPT, Grok, Phantom, Opal, Mozi): the calm,
professional ones are Bevel and ChatGPT — near-monochrome shell, one accent, thin line icons, the
message box a floating rounded card (≈16–24 dp) with the chat fading/blurring under it, assistant
answers without a bubble, hierarchy by weight and grey rather than by size jumps, lots of air.
Copilot adds a frosted bottom pill and a soft glow. Playful outliers we do **not** copy: Phantom's
blobs, Opal's iridescent gem, Copilot's cloud sky, Bevel's blue glowing user bubble.

**Oskar's pictures** (sandbox only, not in the repo): what we keep is the calm, the white space,
depth through light and blur, soft gradients (the light Health app, the empty chat with its faint
green rim glow). What we drop: the colourful glowing tiles, glossy bubbles and coloured 3D icons.

**Nothing (OS 3, Nothing X, nothing.tech)**: strict monochrome, the brand colour is the absence of
colour; one accent used sparingly; depth through translucency and backdrop blur, never heavy
shadows; small radii (the site uses 6–8 px); hairlines instead of shadows; precise grotesk type
(NType 82, Geist on the site), light headline weights. The dot-matrix font and dot grids are
deliberately **not** used.

## Shared rules (A and B)

- Radii: nothing above 20 dp. Cards 12–16 dp, message box 14–16 dp, chips 8–10 dp.
- Near-monochrome plus Hain green `#16A36B` as the only accent. No coloured tiles; every icon is
  a Lucide line icon (ISC), stroke 1.8, one colour. The old 3D icon renders are no longer used.
- Depth through blur and soft gradients, not glass edges, inner light or thick shadows.
- The chat scrolls **under** the message box: progressive blur (Haze, Android 12+) plus a fade
  into the page colour (`hainBottomFade`); no solid bar behind the box.
- Kept from #24: brain button with Auto/Fast/Thorough inside the message box (popover), the
  long-text fixes (T-085), ConfirmCard.
- Pim: calmer — A draws the flat Pim v2.1, B a matte soft gradient; no jelly gloss, half the
  breathing.
- Light and dark for both.

## A — "Nothing Clean"

- White `#FFFFFF` / true black `#000000`, neutral greys without a green tint, ink text.
- Hairlines (`border`) instead of shadows on panels and the message box.
- Main buttons and send are **ink** (black / white); green only as accent (selection, glow).
- Font **Geist** (OFL, Nothing's own site uses it): regular headlines with tight tracking.
- Radii 8 / 10 / 12 / 14 / 16 dp; buttons are 12 dp rectangles.
- Blur only under the message box and popovers; one faint green glow rising from the bottom in
  the empty chat and while thinking (`hainAmbientGlow`, `ThinkingGlow`).

## B — "Soft Gradient"

- Page with large, very soft mint/grey blobs (`hainBackdrop`, mesh-like, low saturation); in dark
  a deep green-black with faint green light.
- Surfaces are translucent white layers over the mesh with a faint light edge (`hainPanel`).
- Main buttons and send in Hain green with a whisper of gradient and a small soft shadow.
- Font **Hanken Grotesk** (light headlines), slim pill buttons, radii 10 / 12 / 14 / 16 / 20 dp.
- Line icons sit in small mint-tinted squares.

## Where it lives

| Token / recipe | File |
|---|---|
| `HainStyle`, palettes `NothingLight/Dark`, `SoftLight/Dark` | `core-ui/.../theme/HainStyle.kt` |
| shapes `NothingShapes`, `SoftShapes` | `core-ui/.../theme/HainShapes.kt` |
| `hainPanel`, `hainBackdrop`, `hainAmbientGlow` | `core-ui/.../theme/GlassTile.kt` |
| `hainBottomFade` (blur + fade under the box) | `core-ui/.../effects/HainGlass.kt` |
| `ChatSurface` (chat under the box) | `feature-chat/.../api/ChatScreen.kt` |
| setting `ui_style_test` ("a"/"b") | `core-common/.../SettingsRepository.kt` |

Screenshots: `StyleScreens` in feature-chat, feature-settings and feature-onboarding
(`style_<A|B>_<screen>_<light|dark>.png`, workflow screens.yml); comparison boards in
`docs/design/screens/style-A.png` and `style-B.png`.

Not restyled yet (follows after the decision): the hero objects of tour pages 2–4, the success
screen confetti, the app icon.
