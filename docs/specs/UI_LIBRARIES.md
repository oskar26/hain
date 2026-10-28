# UI_LIBRARIES.md — what we use instead of building from scratch (ADR-21)

Checked 2026-10-28. Rule: a library may handle **behaviour, effects, parsing**; the **look** stays ours (DESIGN_SYSTEM.md).
Every library below is added in T-018 with exact versions in `libs.versions.toml` and a row in LICENSES.md.

## 1. assistant-ui — what we take

assistant-ui (assistant-ui.com, MIT) is React / React Native / Ink / Vue. **Its code cannot run in our Kotlin/Compose app.**
Embedding it in a WebView would break native feel, performance and offline-first design. Switching Hain to React Native would throw away ADR-02/03.

We use it as the **UX blueprint**: same building blocks, same states, re-implemented in Kotlin (concepts only, no code copied).

| assistant-ui primitive | Hain component (Kotlin) | Task |
|---|---|---|
| `ThreadPrimitive.Viewport` (auto-scroll, stick to bottom unless user scrolls up) | `ChatViewport` | T-058 |
| `ThreadPrimitive.ScrollToBottom` | `ScrollToBottomPill` (fades in, shows "neue Antwort") | T-058 |
| `ThreadPrimitive.Suggestion` / Suggestions | `SuggestionChips` in empty state | T-052 |
| `ComposerPrimitive.Root/Input/Send/Cancel` + attachments | `Composer` (send ↔ stop morph, attachment chips) | T-012 |
| `ActionBarPrimitive` (copy, reload, speak, edit) | `MessageActions` (visible on last answer, long-press for others) | T-052 |
| `BranchPickerPrimitive` ("2 / 3" after edit or regenerate) | `BranchPicker` using `message.parent_id` | T-057 |
| Reasoning element (disclosure with duration) | `ReasoningBlock` ("Gedacht für 4 s") | T-013 |
| Tool UI / tool timeline | `ToolTimeline` (chips per tool call: running → done/failed) | T-059 |
| Approval card (human in the loop) | `ConfirmCard` for alarm/calendar/navigate | T-068 |
| Sources | `SourceChips` with favicons (cached) | T-059 |
| Message queue (send while generating) | `QueuedMessage` chip, sent after current answer | T-057 |
| Stopped / error state | "Gestoppt" label + "Weiter" button; error card with retry | T-057 |
| Typing indicator | Pim leaf wiggle + shimmer text | T-019 |

Read for behaviour details: https://www.assistant-ui.com/docs (llms.txt available) and the React Native elements list.

## 2. Libraries we build on

| Need | Library | Licence | Why |
|---|---|---|---|
| Unstyled accessible primitives (bottom sheet, modal, dialog, drawer, dropdown, tooltip, toggle, slider, tabs, text field, scrollbars, focus ring) | **Compose Unstyled 2.x** (`com.composables:composeunstyled-*`, per-component modules) | MIT | built on Foundation, no Material, handles a11y/gestures → our `:core-ui` wraps them with our styling |
| Streaming markdown (LLM chunks), tables, code | **multiplatform-markdown-renderer** core + `-code` (syntax highlight) — `rememberStreamingMarkdownState()` | Apache-2.0 | append-only parsing of the tail, made for LLM streams; do **not** add the `-m3` artifact |
| Blur / frosted glass (top bar, composer over content, sheets) | **Haze 2.x** (`dev.chrisbanes.haze:haze`, `haze-blur`) | Apache-2.0 | GPU blur; Android 12+ native, older devices → solid fallback |
| Liquid-glass accents (voice orb, mode switch) | **Backdrop** (`io.github.kyant0:backdrop`) | Apache-2.0 | only for 1–2 hero elements, never lists |
| Smooth "squircle" corners (Apple-like, more premium than plain rounded) | **Capsule** (`io.github.kyant0:capsule`) | Apache-2.0 | `ContinuousRoundedRectangle`, `ContinuousCapsule` for composer, bubbles, cards |
| Shimmer (thinking text, loading cards, tool running) | **compose-shimmer** (`com.valentinilk.shimmer`) | Apache-2.0 | tiny, Modifier-based |
| Mascot animation | **Rive** Android runtime (`app.rive:rive-android`, Compose API) — *or* Compose-drawn Pim | MIT (runtime) | state machine idle/listening/thinking/speaking/happy; see Q-04 (export needs Rive paid plan, ~$9/month, otherwise splash screen) |
| Micro animations (success check, confetti for first chat) | **Lottie Compose** (`com.airbnb.android:lottie-compose`) | Apache-2.0 | free files from LottieFiles (check each file's licence) |
| Icons | Lucide (vector drawables) | ISC | already in spec |
| Image loading (thumbnails, favicons) | **Coil 3** | Apache-2.0 | |

Built into Compose (no library needed): `SharedTransitionLayout` (composer → bubble), `AnimatedContent`, `animateItem()` in LazyColumn, spring physics, `graphicsLayer` + `RenderEffect`, predictive back.

## 2b. Copy-paste sources (code we may take, with attribution in NOTICE)

Apache-2.0 and MIT code may be copied into our GPL-3.0 app (keep the original licence header + add to `NOTICE`).

| Source | Licence | What to take |
|---|---|---|
| **Composables UI** (composablehorizons/composables-ui) | MIT | shadcn-style "copy, paste, ship" components built on Compose Unstyled: buttons, cards, list items, forms, sheets. Copy the source into `:core-ui`, then restyle with our tokens. Best starting point for every T-011…T-015 component. |
| **SmolChat-Android** (shubham0204) | Apache-2.0 | **engine code only — its UI is rejected (owner decision 2026-10-28), never copy or imitate its screens.** llama.cpp + JNI + Kotlin wrapper (`SmolLM.kt`, CPU-feature `.so` selection for ARMv8.4/i8mm/dotprod), GGUF chat-template handling, Room chat storage → reference/copy for T-030…T-033 |
| **Google AI Edge Gallery** (google-ai-edge/gallery) | Apache-2.0 | Compose chat screen, model download manager with progress, benchmark screen → reference for T-052, T-081, T-000 (Material 3 there, so restyle) |
| prompt-bar / llm-typewriter | Apache-2.0 | composer state machine, prefix-stable streaming markdown idea |

**Not allowed:** GetStream `stream-chat-android-ai` — very good AI components (StreamingReasoning, ChatComposer, approval card, suggestions), but under the proprietary *Stream License* → incompatible with GPL-3.0. Look at its README for UX ideas only, never copy code.

## 2c-bis. Component base: primary + backup (owner decision 2026-10-28)

1. **Primary: Composables UI** (copy-paste, MIT) as described in §2b.
2. **Backup: Compose Unstyled** (https://composeunstyled.com, same authors, MIT) — headless primitives (Dialog, BottomSheet, Menu, Tabs, Slider, Checkbox, ToggleSwitch, TextField, Tooltip …) with accessibility and keyboard handling but **no styling at all**. Use when Composables UI has no ready component or its version fights our design; then style 100 % with Hain tokens. Composables UI is itself built on Compose Unstyled, so both mix without conflict. Verify the exact artifact coordinates and version in `libs.versions.toml` before use.

## 2d. From awesome-kotlin (checked 2026-10-28)

Most entries there are old View/XML libraries → not usable. Kept:

| Library | Licence | Use |
|---|---|---|
| **Konfetti** (`nl.dionsegijn:konfetti-compose` 2.2.0) | ISC | celebration bursts (first model installed, pack done) with custom shapes = tiny leaves in Hain colours; `burst(at = tapOffset)`; replaces Lottie confetti; skip when reduce_motion |
| **Showkase** (airbnb) | Apache-2.0 | debug-only catalogue of every `@Preview` component → reviewer and small models see all components in one place |
| Wavy Slider / saket squiggly-slider | Apache-2.0 | idea only (Material-based): download progress as a gentle wave line that flattens when done → own Canvas implementation in `HainProgress` |
| saket ExtendedSpans | idea | squiggly underline under spell/grammar hints in the composer → own implementation |
| Balloon, Spotlight, IntroShowCase | – | rejected (View-based or Material); own `Tooltip` on Compose Unstyled |

## 2e. Micro-details catalogue (the "many small things")

Every item respects `reduce_motion` and is owned by the named component.

- Composer: placeholder cycles through 3 example prompts with a soft vertical slide every 4 s (only when empty + focused off); send button grows out of the mic with a morph; character counter fades in only above 80 % of the context limit.
- Send: light haptic + bubble flies up from composer (shared bounds); Pim glances at the new message.
- Streaming: caret is a small soft mint dot that pulses; finished answer gives one subtle shimmer sweep across the action row.
- Copy: icon morphs to a check, tiny "Kopiert" pill rises 6 dp and fades.
- Code block: language label slides in on scroll-into-view; copy button only fully opaque on press.
- Tool chips: ticking elapsed time in tabular digits; success = check draws itself (path trim 0→1, 260 ms).
- Scroll: top bar gets a hairline + haze only after content scrolls under it; overscroll at bottom stretches the last message 2 %.
- Drawer: chat titles appear word by word when auto-named; swipe-to-delete reveals a red trash that scales with drag distance, haptic tick at threshold.
- Switches: thumb squishes horizontally while moving (own drawn `HainSwitch`).
- Numbers (download %, tokens/s): rolling digit animation (`AnimatedContent` per digit).
- Empty state: suggestion chips enter staggered 40 ms; Pim blinks.
- Errors: card shakes 2× by 4 dp, error haptic, Pim curious.
- Model download: wave progress line, leaf grows on Pim as progress rises; done = Konfetti leaves + happy Pim.
- Theme switch: circular reveal from the toggle position.
- Long press message: background dims and blurs (Haze), message lifts with scale 1.02, menu items stagger in.

## 2c. Workflow that makes building UI fast for small models

1. Copy the closest Composables UI component → restyle with tokens (never start from an empty file).
2. Every component gets `@Preview` light/dark + a **Roborazzi/Paparazzi screenshot test** (T-018 adds Roborazzi). The model renders, looks at the PNG, fixes, repeats.
3. Reference screenshots live in `docs/design/` – the reviewer compares side by side.

**Looked at, not used:** `prompt-bar` and `llm-typewriter` (Apache-2.0, nice ideas, but 0.x, one maintainer, Material-based) → use as reference only. GetStream AI components (tied to Stream SDK). eagle-render (Material 3 defaults). Composables UI (opinionated look; we only want Unstyled).

## 3. Motion system ("edel")

All values live in `HainMotion` (T-019). Springs instead of linear tweens. Everything respects `reduce_motion`.

| Moment | Animation | Spec |
|---|---|---|
| Send | text leaves composer and becomes the user bubble (shared element), composer shrinks back with spring | `spring(dampingRatio 0.82, stiffness 380)` |
| Answer starts | Pim leaf wiggles, "Pim denkt nach…" with shimmer, reasoning block grows in height | shimmer 1,400 ms |
| Tokens stream | new words fade + rise 2 dp (word-level, not char), soft green caret blinks at the end | 160 ms ease-out, max 30 animated words at once |
| Tool call | chip slides in, shimmer while running, morphs to ✓ with tiny scale bounce; orange dot if online | 220 ms |
| Card appears | scale 0.96 → 1 + fade, children stagger 40 ms | spring 0.85 |
| Send ↔ Stop | icon morphs (rotate + crossfade), button colour fades | 180 ms |
| Composer over content | frosted glass (Haze) once content scrolls under it | blur 24 dp, tint bg 70 % |
| Drawer open | chat scales to 0.94, corners round to 28 dp, background dims, drawer slides with parallax | spring 0.9 |
| Scroll-to-bottom pill | fade + slide 8 dp | 150 ms |
| Model switch | progress ring around model name, Pim sleeps → wakes | – |
| Voice mode | glass orb (Backdrop) pulses with mic RMS; Pim state machine | 60 fps |
| Long-press message | lift (scale 1.02, shadow 0 → 8 dp), menu pops from finger | spring |
| Haptics | send (light), tool confirm (medium), error (double) | `HapticFeedbackType` |

Performance budget: 60 fps on a 6 GB mid-range phone while streaming. Blur only on Android 12+; if frame time > 16 ms twice in a row, `MotionGovernor` turns blur off for the session. Never blur list items.
