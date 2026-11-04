# PIM_MOTION.md — Pim, alive and everywhere (T-089, 03.11.2026)

Lichtung Pim: flat colours, calm, but charming. Code in `core-ui/…/pim/`.

## Rig (the leaf never comes off)

`PimRig` builds three plain 2D transforms (`Affine`), parent to child, like a skeleton:

1. **body** = move (x, y) · tilt around the bottom centre · squash (sx, sy) around the bottom centre
2. **stem** = body · own bend around the head anchor `STEM_BASE` (60, 36)
3. **leaf** = stem · own sway and scale around the stem tip `LEAF_TIP` (57.5, 20.5)

The stem and leaf start from the body's matrix (same pivot, same squash), their own wobble is only
relative to it. Before T-089 the leaf group only copied x/y and bend, so a squash, tilt or hop pulled
the head away from the leaf. `PimRigTest` samples **every move at 21 points in time in every state**
(with breathing) and checks at 160 dp that the leaf root stays within **1 dp** of the head and the leaf
within 1 dp of the stem tip. `PimFramesScreens` (CI, screens.yml) draws every move at five points in time.

## Moves (`PimMoves`, pure functions of t = 0…1)

| Kind | Moves |
|---|---|
| Always | breathing (very small squash + leaf sway) |
| Idle, random every 2.4–6 s, never twice in a row | blink, look around, wave (with the leaf), hop, nod, yawn, little heart |
| After 40 s without anything | doze with "Zz" |
| States | thinking (leaf wiggles, eyes up), sleeping (Zz), celebrate (hops), downloading (bob) |
| Moments | joy when an answer is done (hop, smile, sparkle), fright on an error (jump, wide eyes, "!") |
| Tap, a different one each time | squash & stretch with giggle, giggle, boing, leaf twirl, wink, heart |

`PimDirector` decides what plays (pure, tested). `LivePim` runs one frame clock and draws in the
draw phase only: no recomposition per frame. With "reduce motion" Pim stays still in his state's pose.

## Where Pim is

| Place | Size | Mood |
|---|---|---|
| Start page | 140 dp (64 dp with the keyboard) | idle, drag & long press (`HeroPim`) |
| "Pim denkt nach…" | 30 dp | thinking |
| Answer done | 28 dp, 2 s | joy |
| Error line | 28 dp | fright |
| Drawer header, empty search / no chats | 40 dp, 64 dp | idle, looking around |
| Pull the chat list down | 44 dp | peeks in, eyes wide when pulled far |
| Settings header, About page | 56 dp, 112 dp | idle, happy |
| Brain banner, onboarding, download, success | 32–112 dp | sleeping, downloading, celebrate |

Sizes are Pim's own size: since T-089 the canvas frames the 108 units around him, not the old
144-unit viewBox (a "28 dp" Pim used to look 19 dp).
