# DESIGN_SYSTEM.md — look & feel (ADR-08, ADR-09)

Mockups (style direction only, texts are placeholders): `docs/design/design1_klar_dark.png` … `design5_voice_buddy.png`.
Use 1 (dark) + 2 (light) for the app, 5 for voice mode, 4 for the optional home screen. 3 (glass) only as accent in news, never blur on low-end devices.

## 1. Rules

- Built on `androidx.compose.foundation` + **Compose Unstyled** primitives (behaviour/a11y), styled by us. Our components live in `:core-ui` (`app.hain.ui`). **No `androidx.compose.material3` components on screen.** Libraries and the full motion table: `UI_LIBRARIES.md`.
- Looks like ChatGPT/Claude: lots of white space, assistant answers as plain text without bubble, user messages in a grey bubble on the right, pill-shaped composer at the bottom, side drawer for chats.
- No shadows. Thin 1 dp borders. No gradients except Pim and the voice ring.

## 2. Tokens (`HainColors`, `HainType`, `HainShapes`, `HainSpacing`)

| Token | Light | Dark |
|---|---|---|
| `bg` | #FFFFFF | #0F1311 |
| `surface` (bubbles, cards, composer) | #F4F5F4 | #1B201D |
| `surfaceHigh` (pressed, menus) | #ECEEEC | #242A26 |
| `border` | #E4E7E5 | #2A302C |
| `text` | #0D0F0E | #ECEFED |
| `textMuted` | #5F6662 | #9AA39E |
| `accent` grove green | #16A36B | #22C17E |
| `onAccent` | #FFFFFF | #06130C |
| `accentSoft` (chips, selection) | #E3F5EC | #173326 |
| `online` (internet used indicator) | #E8833A | #F0964F |
| `danger` | #D14343 | #F07070 |

Typography: font **Inter** (bundled, OFL) — option "Gut lesbar" switches to **Atkinson Hyperlegible** (OFL).
| Style | size/line | weight |
|---|---|---|
| `display` | 28/34 | 600 |
| `title` | 20/26 | 600 |
| `body` (answers) | 16/24 | 400 |
| `bodySmall` | 14/20 | 400 |
| `label` | 13/16 | 500 |
| `code` | 14/20 JetBrains Mono (OFL) | 400 |
Respect system font scale up to 200 %.

Shapes (all **continuous/squircle** via Capsule `ContinuousRoundedRectangle`, composer = `ContinuousCapsule`): composer 28 dp, user bubble 20 dp, card 16 dp, chip 12 dp, button 12 dp. Spacing scale 4/8/12/16/24/32.
Icons: **Lucide** (ISC licence) as vector drawables, 20 dp, stroke 1.75.

## 3. Components (`:core-ui`, one file each, with `@Preview` light+dark)

`HainTheme`, `HainSurface`, `HainText`, `HainButton` (primary/secondary/ghost), `HainIconButton`, `HainTextField`,
`Composer` (pill: + attach, text, mic, send/stop), `UserBubble`, `AssistantBlock` (markdown + actions row: copy, speak, regenerate, share),
`ReasoningBlock` (collapsed, "Gedacht für 4 s"), `ToolChip` ("Suche im Web…" with orange dot when online), `HainChip`, `HainCard`,
`SegmentedControl`, `SettingsRow` (title, subtitle, trailing switch/value/chevron), `HainSwitch` (own drawn), `Drawer` (chat list), `TopBar` (model picker in the middle like ChatGPT), `BottomSheet`, `Toast`, `EmptyState`, `ProgressBar`.

Markdown: `com.mikepenz:multiplatform-markdown-renderer` (core + `-code`, not `-m3`) with `rememberStreamingMarkdownState()` for streaming, and our own colours/typography, code blocks with copy button, tables scrollable, LaTeX via own `MathText` (KaTeX not available natively → render with `jlatexmath-android`, check licence in LICENSES.md, else show source in code style).

## 4. Pim (mascot) — v2.1 "simple & cute" (2026-10-28)

Style reference: minimal AI mascots (OpenAI dots, Grok bot, Notion figures). v1 (outline, arms, legs, blush) is retired → `assets/mascot/legacy_mascot_v1.png`, do not use.

**Anatomy:** body = soft gumdrop, flat mint `#7FD6A4`; stem grows out of the body top and the single leaf sits exactly on the stem tip (`#3FAF77`, faint vein only ≥ 64 dp); two vertical pill eyes `#1F2A2E` (dark mode `#0E1513`). No outline, no mouth, no arms/legs, no gradient, no shine, no blush. Tiny accent effects only: `zz`, `!`, `?`, sparkle, heart, sweat drop.

**Single source of truth:** `tools/pim_gen.py` (geometry, easing, every motion as keyframes) →
- `assets/mascot/v2/motion/pim_motion.json` — **Compose reads/ports this** (no hand-tuned numbers in Kotlin),
- `assets/mascot/v2/static/pim_*.svg` (10 poses), `preview/*.gif` (one per motion), `preview/pim_preview.html` (all motions live + random idle director; open in a browser),
- `preview/pim_20px.png`/`40px` legibility check. Concept sheet `pim_v2_sheet.png` is historical.

**Rig parameters** (neutral in brackets): `x,y` (0) · `sx,sy` squash around bottom centre (1) · `tilt` around bottom centre (0°) · `bend` stem+leaf around stem base (0°) · `leaf` around stem tip, + = perk up, − = droop (0°) · `leafScale` (1) · `eyes` ∈ pill|wide|happy ∩|closed ∪|squeeze >< · `eyeH` blink 0–1.1 (1) · `eyeDX/DY` look (0, max ±5) · `eyeLS/RS` per-eye size (1) · `fx` + `fxA/fxX/fxY`.
Easings: linear, in, out, inOut (cubic), back (overshoot), spring (damped `1−e^(−6u)·cos(10.5u)`). Discrete params switch at 50 % of a segment.

**33 motions.** States (loop or one-shot): idle, thinking, listening, speaking, sleeping, happy, celebrate, error, downloading. Micro-actions: blink, double_blink, look_left, look_right, look_around, look_up, hop, double_hop, leaf_flutter, leaf_wave (hello), shake (wet-puppy), yawn, wiggle, nod, headshake, surprised, giggle, shy, curious_tilt, dozing, sneeze, stretch_wide, peek_caret, tap_squish. Descriptions and timings: in the JSON.

**Layering:** a base state plays continuously; a micro-action is overlaid: additive for offsets/angles, multiplicative for scales, and its eyes/fx win while ≠ neutral (reference implementation: `pim_preview.html`, `frame()`).

**PimIdleDirector** (only in idle, `idleSchedule` in JSON): every 3.5–9 s pick a weighted random micro-action (blink most often, sneeze/dozing rare), never one of the last 2; independent blink every 3–6 s; > 60 s untouched → dozing → sleeping; any touch/typing wakes Pim (surprised small or blink).

**Event → motion map:** app start / new chat → leaf_wave · user types → peek_caret (eyes follow caret ≤ 2 dp) · send → nod · model thinking → thinking · streaming/TTS → speaking (bob from audio RMS) · mic open → listening (stretch from mic RMS) · success/confirm → happy · first model installed / long task done → celebrate + Konfetti leaves · error → error · "can't do that" → headshake · compliment/thanks detected → shy · question back to user → curious_tilt · download → downloading (`leafScale` = progress) · tap Pim → tap_squish + light haptic (3 taps in 1 s → giggle) · offline banner → sleeping (40 dp).

**Sizes:** empty chat 96 dp · voice mode 180 dp · errors 72 dp · offline 40 dp · avatar 20 dp (static, only blink + thinking, no fx, no vein).

**Rules:** max one Pim on screen except voice mode; never in settings lists; setting `show_pim` (default true); `reduce_motion` → only blink, no fx, no hops. Pim voice: see VOICE.md "Pim voice". Seasonal touches are v2.

## 5. Screens (v1)

1. Chat (default start): top bar (drawer, model picker, new chat), messages, composer. Empty state: Pim + "Hallo! Was kann ich für dich tun?" + 4 suggestion chips.
2. Drawer: search field, "Neuer Chat", list grouped Today/Yesterday/7 days/older, bottom: News, Gedächtnis, Bilder, Einstellungen.
3. News (NEWS.md). 4. Memory. 5. Image editor. 6. Settings (SETTINGS.md). 7. Onboarding (3 steps: welcome, language, download brain). 8. Voice mode (full screen). 9. Optional Home (bento, design 4).

## 6. Motion & haptics

Full motion system with springs and timings: `UI_LIBRARIES.md` §3 (source of truth). Short version:

Token fade-in 120 ms. Send button morphs to stop. Light haptic on send and on tool confirm. Setting `reduce_motion` disables all non-essential animation.
