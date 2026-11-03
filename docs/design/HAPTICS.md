# HAPTICS.md — premium haptics (T-096, 03.11.2026)

Oskar: *"smartes haptisches Feedback, das nicht so stark brummt, sondern eher taktil ist … hochwertig,
Premium … smart eingesetzt … verschiedene Stufen."*

## Rules

- Only **`VibrationEffect.Composition` primitives** (crisp clicks and ticks from the phone's tuned
  haptics engine), when the phone supports *all* primitives of a pattern
  (`Vibrator.areAllPrimitivesSupported`, Android 12+). Otherwise the matching
  `HapticFeedbackConstants` via `View.performHapticFeedback`. **Never `vibrate(long)`**, never a buzz.
- Every pattern is 1–2 primitives and at most ~100 ms (unit-tested in `HapticsTest`).
- Played with usage `USAGE_TOUCH`, and skipped completely when the phone's **touch vibration**
  (`Settings.System.HAPTIC_FEEDBACK_ENABLED`) is off.
- Code: `core-ui/…/motion/Haptics.kt` — `HainHaptics`, `HainHaptic.pattern()`, `HapticLevel`,
  `LocalHapticLevel`; get it with `rememberHainHaptics()`.

## Moments

| Moment | Where | Pattern (full strength) | Fallback |
|---|---|---|---|
| Send | send button | CLICK 0.7 | KEYBOARD_TAP |
| Toggle | switches, segmented choices | TICK 0.6 | CLOCK_TICK |
| Select | thinking level in the brain picker | LOW_TICK 0.8 | CLOCK_TICK |
| Done | an answer finished | TICK 0.45 + TICK 0.7 after 70 ms | CONTEXT_CLICK |
| Long press | long-press moments | THUD 0.8 | LONG_PRESS |
| Menu | a ⋯ / long-press menu opens (`HainPopupMenu`, chat list, chat) | TICK 0.55 | LONG_PRESS |
| Confirm | confirming a delete ("Delete" in the chat menu) | CLICK 0.8 | VIRTUAL_KEY |
| Pull | a pull gesture passes the point where Pim looks surprised (`PullPim`), once per pull | LOW_TICK 0.7 | CLOCK_TICK |
| Burst | Pim's easter egg explodes (after ≥ 5 s holding) | THUD 1.0 + TICK 0.5 after 60 ms | LONG_PRESS |
| Error | an answer failed | CLICK 0.6 + QUICK_FALL 0.5 after 50 ms | REJECT |
| Tick | tapping Pim, buttons, opening the brain picker | TICK 0.5 | CLOCK_TICK |
| Merge | 2048 tiles merge (bigger tiles a little firmer) | CLICK 0.5 × 0.5–1.0 | CLOCK_TICK |
| Charge | holding Pim (T-097): one short primitive per pulse, LOW_TICK → TICK → CLICK, strength 0.3 → 1.0, gap 260 → 45 ms (`chargeGapMs`, `chargeStrength`) – never one long vibration | | |

**No tick while text streams** — the answer only ticks once, when it is done. **No tick while scrolling**
— only the pull gesture ticks, once, when it passes its point. Plain ghost buttons and links stay silent.

## Where (checklist, Oskar 03.11. 23:26: "nicht zu viel, nicht zu wenig, nur subtil")

| Place | Moment | Code |
|---|---|---|
| Send button | Send | `ChatScreen` / `HainSendButton` |
| Switches in Settings | Toggle | `SettingsRows` |
| Segmented controls (theme, answer mode, vibration) | Toggle, only on a change | `SegmentedControl` |
| Thinking level (brain picker) | Tick on open, Select on a choice | `ThinkingPicker` |
| Answer finished | Done | `ChatScreen` (once per answer) |
| Answer failed | Error | `ChatScreen` |
| ⋯ menus (chat list, chat) | Menu | `HainPopupMenu` |
| Delete chat confirmed | Confirm | `ChatListDrawer.ConfirmDelete` |
| Pull gesture (chat list) | Pull | `PullPim` |
| Tap on Pim | Tick | `LivePim`, `HeroPim` |
| Holding Pim (easter egg) | Charge → Burst | `HeroPim`, `PimEgg` |
| Primary / secondary buttons, tool confirm card | Tick | `HainButton` |
| 2048: tiles merge / won / no moves | Merge (firmer for bigger tiles) / Done / Error | `Game2048Screen` |
| First start finished | Toggle | `SuccessScreen` |

## Levels

Settings → Look → **Vibration**: Off / Subtle (default) / Normal / Strong (`haptics`). The level scales
every primitive: 0 / 0.55 / 0.8 / 1.0. "Off" is silent, also for the fallbacks.
