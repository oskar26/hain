#!/usr/bin/env python3
"""Pim v2.1 generator — geometry, motion library and previews (single source of truth).

Outputs (python3 tools/pim_gen.py):
  assets/mascot/v2/static/pim_<pose>.svg        static poses (for docs, icons, tests)
  assets/mascot/v2/motion/pim_motion.json       ALL animations as keyframes (Compose reads this)
  assets/mascot/v2/preview/<name>.gif           GIF preview per animation (240 px, 25 fps)
  assets/mascot/v2/preview/pim_states.png       static pose sheet
  assets/mascot/v2/preview/pim_20px.png / 40px  small-size legibility check
  assets/mascot/v2/preview/pim_preview.html     open in a browser: every animation live + random idle

Coordinate system: Pim is drawn in a 120x120 box; files use viewBox "-12 -18 144 144" so hops/leaf never clip. Compose: draw with the same padding, y down. Angles in degrees, positive = clockwise.
Leaf: positive `leaf` perks the leaf UP, negative droops it (leaf hangs to the left).
Param defaults (neutral pose) are in NEUTRAL; a key only lists what differs.
"""
import io, json, math, os

BODY, LEAF, EYE, ACCENT, BG = "#7FD6A4", "#3FAF77", "#1F2A2E", "#3FAF77", "#F7F7F4"
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "assets", "mascot", "v2")

# ---------------- geometry (v2.1: stem grows out of the body, leaf sits ON the stem tip) -------------
BODY_D = "M60 34 C82 34 92 52 92 76 C92 96 80 106 60 106 C40 106 28 96 28 76 C28 52 38 34 60 34 Z"
STEM_D = "M60 42 C60 33 59.5 26 57.5 20.5"          # starts inside the body (hidden), ends at tip
STEM_BASE = (60, 36)                                 # pivot for stem bend
LEAF_TIP = (57.5, 20.5)                              # pivot for leaf; leaf base == stem tip
LEAF_D = "M57.5 20.5 C54 11 44 7.5 36.5 11.5 C38 20 48 24.5 57.5 20.5 Z"
LEAF_VEIN = "M56 19.5 C51 16 45 14 40 13"            # drawn only >= 64 dp (detail level)
EYE_L, EYE_R = (48.5, 71.0), (71.5, 71.0)
EYE_W, EYE_H = 7.0, 14.0

NEUTRAL = dict(x=0, y=0, sx=1, sy=1, tilt=0, bend=0, leaf=0, leafScale=1,
               eyes="pill", eyeH=1, eyeDX=0, eyeDY=0, eyeLS=1, eyeRS=1,
               fx="", fxA=0, fxX=0, fxY=0)
NUMERIC = [k for k, v in NEUTRAL.items() if not isinstance(v, str)]


# ---------------- easing (identical in pim_preview.html and in Compose PimEasing) -------------------
def ease(name, u):
    u = min(max(u, 0.0), 1.0)
    if name == "linear":
        return u
    if name == "in":
        return u ** 3
    if name == "out":
        return 1 - (1 - u) ** 3
    if name == "back":  # overshoot then settle
        c1, c3 = 1.70158, 2.70158
        return 1 + c3 * (u - 1) ** 3 + c1 * (u - 1) ** 2
    if name == "spring":  # damped spring, settles at 1
        return 1 - math.exp(-6 * u) * math.cos(10.5 * u)
    # default inOut (cubic)
    return 4 * u ** 3 if u < 0.5 else 1 - (-2 * u + 2) ** 3 / 2


def sample(anim, t_ms):
    """Params at time t_ms. Keys: [{"t": ms, "ease": name, "p": {...}}] sorted by t, first at 0."""
    keys = anim["keys"]
    if anim.get("loop"):
        t_ms %= anim["duration"]
    full = []
    cur = dict(NEUTRAL)
    for k in keys:  # resolve each key to a full pose (carry forward)
        cur = dict(cur)
        cur.update(k["p"])
        full.append((k["t"], k.get("ease", "inOut"), cur))
    if t_ms <= full[0][0]:
        return dict(full[0][2])
    for (t0, _, a), (t1, e, b) in zip(full, full[1:]):
        if t_ms <= t1:
            u = ease(e, (t_ms - t0) / max(t1 - t0, 1))
            out = {k: a[k] + (b[k] - a[k]) * u for k in NUMERIC}
            for k in ("eyes", "fx"):  # discrete: switch at the half of the segment ("step")
                out[k] = b[k] if (t_ms - t0) / max(t1 - t0, 1) >= 0.5 else a[k]
            return out
    return dict(full[-1][2])


# ---------------- rendering ------------------------------------------------------------------------
def _eye(c, kind, s, p):
    cx, cy = c[0] + p["eyeDX"], c[1] + p["eyeDY"]
    if kind in ("pill", "wide"):
        w = (8.2 if kind == "wide" else EYE_W) * s
        h = (17 if kind == "wide" else EYE_H) * s * p["eyeH"]
        h = max(h, 2.2)
        r = min(w, h) / 2
        return f'<rect x="{cx-w/2:.2f}" y="{cy-h/2:.2f}" width="{w:.2f}" height="{h:.2f}" rx="{r:.2f}" fill="{EYE}"/>'
    if kind == "happy":   # ∩
        return f'<path d="M{cx-5:.2f} {cy+2:.2f} Q{cx:.2f} {cy-6.5:.2f} {cx+5:.2f} {cy+2:.2f}" fill="none" stroke="{EYE}" stroke-width="3.2" stroke-linecap="round"/>'
    if kind == "closed":  # ∪
        return f'<path d="M{cx-5:.2f} {cy-1:.2f} Q{cx:.2f} {cy+5:.2f} {cx+5:.2f} {cy-1:.2f}" fill="none" stroke="{EYE}" stroke-width="3.2" stroke-linecap="round"/>'
    if kind == "squeeze":  # > <
        d = 1 if c == EYE_L else -1
        return (f'<path d="M{cx-4*d:.2f} {cy-4:.2f} L{cx+3*d:.2f} {cy:.2f} L{cx-4*d:.2f} {cy+4:.2f}" fill="none" '
                f'stroke="{EYE}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>')
    raise ValueError(kind)


def _fx(p):
    if not p["fx"] or p["fxA"] <= 0.01:
        return ""
    a, x, y = p["fxA"], 96 + p["fxX"], 30 + p["fxY"]
    f = p["fx"]
    if f == "zz":
        return (f'<g opacity="{a:.2f}" fill="{ACCENT}" font-family="sans-serif" font-weight="700">'
                f'<text x="{x:.1f}" y="{y+8:.1f}" font-size="9">z</text><text x="{x+7:.1f}" y="{y:.1f}" font-size="12">z</text></g>')
    if f in ("!", "?"):
        return f'<text x="{x:.1f}" y="{y+6:.1f}" opacity="{a:.2f}" fill="{ACCENT}" font-family="sans-serif" font-weight="800" font-size="16">{f}</text>'
    if f == "sparkle":
        def star(cx, cy, r):
            return (f'M{cx} {cy-r} Q{cx} {cy} {cx+r} {cy} Q{cx} {cy} {cx} {cy+r} Q{cx} {cy} {cx-r} {cy} Q{cx} {cy} {cx} {cy-r} Z')
        return f'<path d="{star(x, y, 6*a)} {star(x-70, y+8, 4*a)} {star(x+4, y+26, 3*a)}" fill="{ACCENT}" opacity="{a:.2f}"/>'
    if f == "heart":
        return (f'<path transform="translate({x:.1f} {y:.1f}) scale({0.6+0.4*a:.2f})" opacity="{a:.2f}" fill="{ACCENT}" '
                f'd="M0 4 C-6 -2 -2 -7 0 -3 C2 -7 6 -2 0 4 Z"/>')
    if f == "drop":  # tiny sweat drop
        return f'<path transform="translate({x-8:.1f} {y+14:.1f})" opacity="{a:.2f}" fill="#8CC8E8" d="M0 -5 C3 -1 4 2 0 4 C-4 2 -3 -1 0 -5 Z"/>'
    return ""


def render(p=None, bg=None, detail=True):
    q = dict(NEUTRAL)
    q.update(p or {})
    sb, lt = STEM_BASE, LEAF_TIP
    vein = f'<path d="{LEAF_VEIN}" fill="none" stroke="{BODY}" stroke-width="1.1" stroke-linecap="round" opacity=".55"/>' if detail else ""
    leaf = (f'<g transform="rotate({q["bend"]:.2f} {sb[0]} {sb[1]})">'
            f'<path d="{STEM_D}" fill="none" stroke="{LEAF}" stroke-width="3.6" stroke-linecap="round"/>'
            f'<g transform="translate({lt[0]} {lt[1]}) rotate({q["leaf"]:.2f}) scale({q["leafScale"]:.3f}) translate({-lt[0]} {-lt[1]})">'
            f'<path d="{LEAF_D}" fill="{LEAF}"/>{vein}</g></g>')
    fig = (f'<g transform="translate({q["x"]:.2f} {q["y"]:.2f}) rotate({q["tilt"]:.2f} 60 106) '
           f'translate(60 106) scale({q["sx"]:.4f} {q["sy"]:.4f}) translate(-60 -106)">'
           f'{leaf}<path d="{BODY_D}" fill="{BODY}"/>'
           f'{_eye(EYE_L, q["eyes"], q["eyeLS"], q)}{_eye(EYE_R, q["eyes"], q["eyeRS"], q)}</g>')
    bgr = f'<rect x="-12" y="-18" width="144" height="144" fill="{bg}"/>' if bg else ""
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="-12 -18 144 144">{bgr}{fig}{_fx(q)}</svg>'


# ---------------- motion library -------------------------------------------------------------------
def K(t, e="inOut", **p):
    return {"t": t, "ease": e, "p": p}


def A(name, duration, keys, kind="micro", loop=False, desc=""):
    return {"name": name, "kind": kind, "loop": loop, "duration": duration, "desc": desc, "keys": keys}


def breathe_keys(dur, depth=0.02, leaf_amp=3):
    """Loopable breath + leaf sway."""
    return [K(0, sy=1, sx=1, leaf=-leaf_amp * 0.3),
            K(dur * 0.5, sy=1 + depth, sx=1 - depth * 0.35, leaf=leaf_amp),
            K(dur, sy=1, sx=1, leaf=-leaf_amp * 0.3)]


MOTIONS = [
    # ---------- loops / states ----------
    A("idle", 2400, breathe_keys(2400), "state", True, "breathing + gentle leaf sway; micro-actions are played on top of this"),
    A("thinking", 1800, [K(0, eyeDX=2, eyeDY=-4, tilt=-3, leaf=-6, bend=-2),
                         K(450, eyeDX=3.2, eyeDY=-4.6, leaf=8, bend=2),
                         K(900, eyeDX=1.6, eyeDY=-3.6, leaf=-6, bend=-2, sy=1.01),
                         K(1350, eyeDX=2.6, eyeDY=-4.4, leaf=8, bend=2),
                         K(1800, eyeDX=2, eyeDY=-4, leaf=-6, bend=-2)], "state", True, "eyes look up-right and drift, leaf ticks like a metronome"),
    A("listening", 1600, [K(0, eyes="wide", sx=0.97, sy=1.04, leaf=4),
                          K(250, "out", eyes="wide", sx=0.95, sy=1.08, leaf=8, eyeH=1.05),
                          K(600, eyes="wide", sx=0.97, sy=1.04, leaf=4),
                          K(800, "out", eyes="wide", sx=0.955, sy=1.065, leaf=7),
                          K(1600, eyes="wide", sx=0.97, sy=1.04, leaf=4)], "state", True, "big eyes, stretches with mic level (in app: driven by RMS, this loop is the fallback)"),
    A("speaking", 1200, [K(0, sy=1, y=0, leaf=0), K(150, "out", sy=1.03, sx=0.99, y=-1.5, leaf=4),
                         K(300, sy=0.99, y=0, leaf=-2), K(480, "out", sy=1.025, y=-1, leaf=3, eyeH=0.92),
                         K(640, sy=1, y=0, leaf=-1), K(820, "out", sy=1.035, y=-2, leaf=5),
                         K(1000, sy=0.99, y=0, leaf=-2), K(1200, sy=1, y=0, leaf=0)], "state", True, "bobs per syllable (in app: driven by TTS amplitude)"),
    A("sleeping", 4000, [K(0, eyes="closed", sx=1.12, sy=0.84, leaf=-25, bend=-4, fx="zz", fxA=0, fxY=4),
                         K(2000, eyes="closed", sx=1.14, sy=0.82, leaf=-22, bend=-3, fx="zz", fxA=1, fxY=-4),
                         K(4000, eyes="closed", sx=1.12, sy=0.84, leaf=-25, bend=-4, fx="zz", fxA=0, fxY=-12)], "state", True, "flat and slow, leaf hangs, little zz"),
    A("happy", 1400, [K(0, eyes="happy", sx=1.08, sy=0.92, leaf=0),
                      K(260, "out", eyes="happy", sx=0.95, sy=1.07, y=-7, leaf=16, fx="sparkle", fxA=1),
                      K(520, "in", eyes="happy", sx=1.06, sy=0.94, y=0, leaf=6, fx="sparkle", fxA=0.6),
                      K(900, "spring", eyes="happy", sx=1, sy=1, leaf=10, fx="sparkle", fxA=0),
                      K(1400, eyes="happy", leaf=10)], "state", False, "squash, jump, sparkles; used after success"),
    A("celebrate", 2000, [K(0, eyes="happy", sx=1.1, sy=0.9),
                          K(220, "out", eyes="happy", y=-10, sx=0.94, sy=1.08, tilt=-6, leaf=18, fx="sparkle", fxA=1),
                          K(440, "in", eyes="happy", y=0, sx=1.08, sy=0.92, tilt=0, leaf=4),
                          K(660, "out", eyes="happy", y=-10, sx=0.94, sy=1.08, tilt=6, leaf=18, fx="sparkle", fxA=1),
                          K(880, "in", eyes="happy", y=0, sx=1.08, sy=0.92, tilt=0, leaf=4, fx="sparkle", fxA=0.4),
                          K(1300, "spring", eyes="happy", sx=1, sy=1, leaf=12, fx="sparkle", fxA=0),
                          K(2000, eyes="happy", leaf=10)], "state", False, "double jump with tilt, pairs with Konfetti leaves"),
    A("error", 1600, [K(0, eyes="pill"), K(120, "out", eyes="wide", y=-3, sy=1.04, fx="drop", fxA=0),
                      K(300, eyes="pill", eyeLS=1.2, eyeRS=0.85, tilt=6, leaf=-8, y=0, fx="drop", fxA=1),
                      K(1200, eyes="pill", eyeLS=1.2, eyeRS=0.85, tilt=6, leaf=-10, fx="drop", fxA=1, fxY=6),
                      K(1600, eyes="pill", eyeLS=1.2, eyeRS=0.85, tilt=6, leaf=-10, fx="drop", fxA=0, fxY=10)], "state", False, "startle, then puzzled tilt with a sweat drop"),
    A("downloading", 3000, [K(0, leafScale=0.2, leaf=-10, eyeDY=-3, eyeDX=-2),
                            K(1500, leafScale=0.65, leaf=4, eyeDY=-4, eyeDX=-1, sy=1.01),
                            K(2600, "out", leafScale=1.15, leaf=14, eyeDY=-4, sy=1.02),
                            K(3000, "spring", leafScale=1, leaf=8)], "state", False, "leaf grows with download progress (in app: leafScale = progress)"),
    # ---------- micro-actions (played randomly on top of idle) ----------
    A("blink", 260, [K(0), K(90, "in", eyeH=0.12), K(260, "out", eyeH=1)], desc="single blink"),
    A("double_blink", 600, [K(0), K(80, "in", eyeH=0.12), K(180, "out", eyeH=1), K(320), K(400, "in", eyeH=0.12), K(600, "out", eyeH=1)], desc="two quick blinks"),
    A("look_left", 1500, [K(0), K(220, "back", eyeDX=-4, tilt=-2), K(1100, eyeDX=-4, tilt=-2), K(1250, "in", eyeDX=-4, eyeH=0.12, tilt=-1), K(1500, "out", eyeH=1)], desc="glance left, blink back"),
    A("look_right", 1500, [K(0), K(220, "back", eyeDX=4, tilt=2), K(1100, eyeDX=4, tilt=2), K(1250, "in", eyeDX=4, eyeH=0.12, tilt=1), K(1500, "out", eyeH=1)], desc="glance right, blink back"),
    A("look_around", 2600, [K(0), K(260, "back", eyeDX=-4, eyeDY=-1, tilt=-3), K(900, eyeDX=-4, eyeDY=-1, tilt=-3),
                            K(1250, "back", eyeDX=4, eyeDY=-1, tilt=3, leaf=4), K(1900, eyeDX=4, eyeDY=-1, tilt=3, leaf=4),
                            K(2250, "out", eyeDX=0, eyeDY=0, tilt=0, leaf=0), K(2350, "in", eyeH=0.12), K(2600, "out", eyeH=1)], desc="left, right, back to you"),
    A("look_up", 1800, [K(0), K(300, "back", eyeDY=-5, sy=1.03, leaf=6), K(1300, eyeDY=-5, sy=1.03, leaf=6), K(1800, eyeDY=0, sy=1, leaf=0)], desc="looks up at its own leaf"),
    A("hop", 900, [K(0), K(140, "out", sx=1.1, sy=0.9), K(330, "out", sx=0.94, sy=1.08, y=-12, leaf=-10, bend=-3),
                   K(520, "in", sx=0.98, sy=1.02, y=0, leaf=8, bend=2), K(620, "out", sx=1.08, sy=0.92, leaf=6), K(900, "spring", sx=1, sy=1, leaf=0, bend=0)], desc="anticipation squash, hop, land squash"),
    A("double_hop", 1300, [K(0), K(110, "out", sx=1.08, sy=0.92), K(260, "out", y=-8, sx=0.95, sy=1.06, leaf=-8),
                           K(410, "in", y=0, sx=1.06, sy=0.94, leaf=6), K(560, "out", y=-11, sx=0.94, sy=1.07, leaf=-10),
                           K(720, "in", y=0, sx=1.08, sy=0.92, leaf=8), K(1300, "spring", sx=1, sy=1, leaf=0)], desc="two little hops"),
    A("leaf_flutter", 1200, [K(0), K(100, "out", leaf=12), K(200, leaf=-6), K(300, leaf=10), K(400, leaf=-4), K(500, leaf=7), K(650, leaf=-2), K(1200, "spring", leaf=0)], desc="leaf flutters in a breeze"),
    A("leaf_wave", 1600, [K(0), K(200, "out", leaf=24, bend=6, tilt=2, eyes="happy"), K(400, leaf=4, bend=2, eyes="happy"),
                          K(600, leaf=24, bend=6, eyes="happy"), K(800, leaf=4, bend=2, eyes="happy"), K(1000, leaf=22, bend=5, eyes="happy"),
                          K(1600, "spring", leaf=0, bend=0, tilt=0, eyes="pill")], desc="waves hello with its leaf"),
    A("shake", 1100, [K(0), K(120, "in", sx=1.04, sy=0.96, eyes="squeeze"),
                      K(220, tilt=-7, x=-2, leaf=18, bend=-6, eyes="squeeze"), K(320, tilt=7, x=2, leaf=-14, bend=6, eyes="squeeze"),
                      K(420, tilt=-6, x=-2, leaf=14, bend=-5, eyes="squeeze"), K(520, tilt=5, x=2, leaf=-10, bend=4, eyes="squeeze"),
                      K(620, tilt=-2, x=-1, leaf=6, bend=-2, eyes="squeeze"), K(1100, "spring", tilt=0, x=0, leaf=0, bend=0, sx=1, sy=1, eyes="pill")], desc="shakes like a wet puppy, leaf flops"),
    A("yawn", 2400, [K(0), K(500, "out", sy=1.08, sx=0.95, eyes="closed", leaf=10, bend=2),
                     K(1300, sy=1.1, sx=0.94, eyes="closed", leaf=12, tilt=-2), K(1700, "in", sy=0.94, sx=1.06, eyes="closed", leaf=-6, tilt=0),
                     K(2000, "out", sy=1, sx=1, eyes="pill", eyeH=0.5, leaf=0), K(2400, eyeH=1)], desc="big stretch with closed eyes, sleepy squish"),
    A("wiggle", 1400, [K(0), K(200, tilt=-5, sx=1.02, leaf=-6), K(450, tilt=5, sx=1.02, leaf=6), K(700, tilt=-5, leaf=-6), K(950, tilt=5, leaf=6), K(1400, "spring", tilt=0, sx=1, leaf=0)], desc="happy side-to-side wiggle"),
    A("nod", 1000, [K(0), K(150, "out", sy=0.95, sx=1.03, eyeDY=2, leaf=6), K(320, sy=1.02, eyeDY=0, leaf=-3),
                    K(470, "out", sy=0.95, sx=1.03, eyeDY=2, leaf=6), K(1000, "spring", sy=1, sx=1, eyeDY=0, leaf=0)], desc="nods yes (for confirmations)"),
    A("headshake", 1100, [K(0), K(150, eyeDX=-3, tilt=-4, leaf=6), K(330, eyeDX=3, tilt=4, leaf=-6), K(510, eyeDX=-3, tilt=-4, leaf=6),
                          K(690, eyeDX=3, tilt=4, leaf=-6), K(1100, "spring", eyeDX=0, tilt=0, leaf=0)], desc="shakes no (for 'I can't do that')"),
    A("surprised", 1500, [K(0), K(80, "out", eyes="wide", eyeH=1.1, y=-8, sy=1.08, sx=0.94, leaf=22, bend=4, fx="!", fxA=1),
                          K(260, "in", eyes="wide", y=0, sy=0.96, sx=1.03, leaf=10, fx="!", fxA=1),
                          K(1100, eyes="wide", sy=1, sx=1, leaf=8, fx="!", fxA=1), K(1500, eyes="pill", leaf=0, bend=0, fx="!", fxA=0)], desc="jumps, big eyes, '!' "),
    A("giggle", 1300, [K(0, eyes="happy"), K(100, "out", eyes="happy", y=-2.5, sy=1.03), K(200, "in", eyes="happy", y=0, sy=0.97),
                       K(300, "out", eyes="happy", y=-2.5, sy=1.03, tilt=-2), K(400, "in", eyes="happy", y=0, sy=0.97),
                       K(500, "out", eyes="happy", y=-2, sy=1.02, tilt=2), K(600, "in", eyes="happy", y=0, sy=0.98),
                       K(1300, "spring", eyes="pill", sy=1, tilt=0)], desc="tiny fast bounces with happy eyes"),
    A("shy", 2200, [K(0), K(400, "out", eyeDY=4, eyeDX=-2, tilt=-6, sx=1.03, sy=0.96, leaf=-14, bend=-3, fx="heart", fxA=0),
                    K(800, eyeDY=4, eyeDX=-2, tilt=-6, leaf=-14, fx="heart", fxA=1, fxY=0),
                    K(1700, eyeDY=4, eyeDX=-2, tilt=-6, leaf=-12, fx="heart", fxA=0, fxY=-10),
                    K(2200, "spring", eyeDY=0, eyeDX=0, tilt=0, sx=1, sy=1, leaf=0, bend=0)], desc="looks down, squishes, small heart (after a compliment)"),
    A("curious_tilt", 1800, [K(0), K(300, "back", tilt=8, eyeLS=1.2, eyeRS=0.9, leaf=10, fx="?", fxA=1),
                             K(1300, tilt=8, eyeLS=1.2, eyeRS=0.9, leaf=10, fx="?", fxA=1),
                             K(1800, "spring", tilt=0, eyeLS=1, eyeRS=1, leaf=0, fx="?", fxA=0)], desc="head tilt with '?'"),
    A("dozing", 3200, [K(0), K(900, eyeH=0.5, leaf=-6), K(1700, eyeH=0.15, tilt=4, sy=0.97, leaf=-16, bend=-3),
                       K(2300, eyeH=0.12, tilt=7, sy=0.95, leaf=-20, bend=-4),
                       K(2420, "out", eyes="wide", eyeH=1, tilt=0, sy=1.06, y=-4, leaf=14, bend=3),
                       K(3200, "spring", eyes="pill", sy=1, y=0, leaf=0, bend=0)], desc="nods off, jolts awake"),
    A("sneeze", 1600, [K(0), K(300, "in", sy=1.06, sx=0.96, tilt=-4, eyeH=0.5, leaf=8),
                       K(600, sy=1.08, sx=0.95, tilt=-6, eyes="squeeze", leaf=10),
                       K(700, "out", sy=0.9, sx=1.1, tilt=6, y=2, eyes="squeeze", leaf=-24, bend=-6),
                       K(1000, sy=1, sx=1, tilt=0, y=0, eyes="pill", eyeH=0.6, leaf=4, bend=0),
                       K(1600, "spring", eyeH=1, leaf=0)], desc="achoo — leaf flips"),
    A("stretch_wide", 1800, [K(0), K(500, "out", sx=1.12, sy=0.9, eyes="closed", leaf=-4),
                             K(1100, sx=1.13, sy=0.89, eyes="closed"), K(1800, "spring", sx=1, sy=1, eyes="pill", leaf=0)], desc="stretches sideways"),
    A("peek_caret", 900, [K(0), K(250, "back", eyeDX=-3, eyeDY=4, tilt=-2), K(900, eyeDX=-3, eyeDY=4, tilt=-2)], desc="looks down at the composer (hold while user types; eye offset follows caret in app)"),
    A("tap_squish", 600, [K(0), K(80, "out", sx=1.12, sy=0.88, eyes="squeeze"), K(600, "spring", sx=1, sy=1, eyes="pill")], desc="reaction when the user taps Pim"),
]

# idle scheduler (documented in DESIGN_SYSTEM §4, implemented in PimIdleDirector)
IDLE_SCHEDULE = {
    "gap_ms": [3500, 9000],
    "no_repeat_last": 2,
    "weights": {"blink": 30, "double_blink": 8, "look_left": 7, "look_right": 7, "look_around": 6, "look_up": 4,
                "leaf_flutter": 7, "hop": 4, "double_hop": 2, "wiggle": 4, "yawn": 2, "shake": 2, "giggle": 2,
                "curious_tilt": 3, "stretch_wide": 2, "sneeze": 1, "dozing": 1},
    "after_idle_s_to_sleep": 60,
    "notes": "blink also fires on its own every 3-6 s; micro-actions never run while thinking/listening/speaking; reduce_motion => only blink",
}

STATIC = {
    "idle": {}, "blink": dict(eyeH=0.15), "happy": dict(eyes="happy", sx=1.04, sy=0.97, leaf=12),
    "thinking": dict(eyeDX=2, eyeDY=-4, leaf=8, tilt=-3), "listening": dict(eyes="wide", sx=0.97, sy=1.04, leaf=4),
    "curious": dict(eyeLS=1.2, eyeRS=0.9, tilt=8, leaf=10, fx="?", fxA=1),
    "surprised": dict(eyes="wide", eyeH=1.1, leaf=20, fx="!", fxA=1),
    "shy": dict(eyeDY=4, eyeDX=-2, tilt=-6, sx=1.03, sy=0.96, leaf=-14, fx="heart", fxA=1),
    "squeeze": dict(eyes="squeeze", sx=1.08, sy=0.92),
    "sleeping": dict(eyes="closed", sx=1.12, sy=0.84, leaf=-25, bend=-4, fx="zz", fxA=1),
}


def main():
    import cairosvg
    from PIL import Image
    for d in ("static", "motion", "preview"):
        os.makedirs(os.path.join(ROOT, d), exist_ok=True)
    # remove outputs of the previous generator version
    import shutil
    shutil.rmtree(os.path.join(ROOT, "animated"), ignore_errors=True)
    for f in os.listdir(os.path.join(ROOT, "preview")):
        os.remove(os.path.join(ROOT, "preview", f))
    for f in os.listdir(os.path.join(ROOT, "static")):
        os.remove(os.path.join(ROOT, "static", f))
    if os.path.exists(os.path.join(ROOT, "pim_idle.svg")):
        os.remove(os.path.join(ROOT, "pim_idle.svg"))

    for name, p in STATIC.items():
        open(os.path.join(ROOT, "static", f"pim_{name}.svg"), "w").write(render(p))
    motion = {"version": "2.1", "viewBox": [-12, -18, 144, 144], "drawBox": 120, "colors": {"body": BODY, "leaf": LEAF, "eye": EYE, "accent": ACCENT},
              "geometry": {"body": BODY_D, "stem": STEM_D, "stemBase": STEM_BASE, "leaf": LEAF_D, "leafTip": LEAF_TIP,
                           "leafVein": LEAF_VEIN, "eyeL": EYE_L, "eyeR": EYE_R, "eyeW": EYE_W, "eyeH": EYE_H},
              "neutral": NEUTRAL, "easings": ["linear", "in", "out", "inOut", "back", "spring"],
              "idleSchedule": IDLE_SCHEDULE, "motions": MOTIONS}
    open(os.path.join(ROOT, "motion", "pim_motion.json"), "w").write(json.dumps(motion, indent=1))

    fps = 25
    for m in MOTIONS:
        dur = m["duration"] + (0 if m["loop"] else 400)  # hold a moment at the end of one-shots
        n = max(int(dur / 1000 * fps), 2)
        frames = []
        for i in range(n):
            png = cairosvg.svg2png(bytestring=render(sample(m, i * 1000 / fps), bg=BG).encode(), output_width=240, output_height=240)
            frames.append(Image.open(io.BytesIO(png)).convert("P", palette=Image.ADAPTIVE, colors=48))
        frames[0].save(os.path.join(ROOT, "preview", f"{m['name']}.gif"), save_all=True, append_images=frames[1:],
                       duration=int(1000 / fps), loop=0, disposal=2)
    names = list(STATIC)
    sheet = Image.new("RGB", (180 * 5, 180 * 2), BG)
    for i, name in enumerate(names):
        png = cairosvg.svg2png(bytestring=render(STATIC[name], bg=BG).encode(), output_width=180, output_height=180)
        sheet.paste(Image.open(io.BytesIO(png)).convert("RGB"), (180 * (i % 5), 180 * (i // 5)))
    sheet.save(os.path.join(ROOT, "preview", "pim_states.png"))
    for s in (20, 40):
        cairosvg.svg2png(bytestring=render(bg=BG, detail=False).encode(), output_width=s, output_height=s,
                         write_to=os.path.join(ROOT, "preview", f"pim_{s}px.png"))
    tpl = open(os.path.join(HERE, "pim_preview_template.html")).read()
    open(os.path.join(ROOT, "preview", "pim_preview.html"), "w").write(tpl.replace("/*MOTION_JSON*/", json.dumps(motion)))
    print(f"{len(MOTIONS)} motions, {len(STATIC)} static poses written to {os.path.normpath(ROOT)}")


if __name__ == "__main__":
    main()
