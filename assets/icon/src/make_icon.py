#!/usr/bin/env python3
"""Hain app icon (T-088, motif "Pim im Wald" since T-102): Pim in his app shape
(assets/mascot/v2/static/pim_idle.svg) standing in front of a soft forest of round trees.

Two looks, both pure SVG rendered with resvg (pip install resvg-py pillow):
  * "3d"   (default): glossy, see-through mint glass Pim, round shaded trees, mist, sky gradient.
  * "flat" : same scene in solid colours, no gradients, no gloss (Pim as in pim_idle.svg).

Scene units: the visible icon tile is 0..1024. Adaptive layers (108 dp) show more around it
(the launcher shows the inner 72 dp), so they are rendered with a wider viewBox.
Pim units as in pim_idle.svg (viewBox -12 -18 144 144).

Usage: python3 assets/icon/src/make_icon.py   (writes into assets/icon/ and app/src/*/res)
       python3 assets/icon/src/make_icon.py --no-res   (only assets/icon)
"""
from __future__ import annotations

import io
import math
import os
import sys

import resvg_py
from PIL import Image, ImageDraw

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
ICON = os.path.join(ROOT, "assets", "icon")
SRC = os.path.join(ICON, "src")

# Pim v2 app shape, 1:1 from pim_idle.svg. The stem starts inside the head, so the leaf is fixed to Pim.
BODY = "M60 34 C82 34 92 52 92 76 C92 96 80 106 60 106 C40 106 28 96 28 76 C28 52 38 34 60 34 Z"
STEM = "M60 42 C60 33 59.5 26 57.5 20.5"
LEAF = "M57.5 20.5 C54 11 44 7.5 36.5 11.5 C38 20 48 24.5 57.5 20.5 Z"
VEIN = "M56 19.5 C51 16 45 14 40 13"
EYES = [(45.0, 64.0), (68.0, 64.0)]  # x, y of 7x14 rounded rects
PIM_CX, PIM_CY = 60.0, 57.0  # centre of the union box (x 28..92, y 7.5..106)

# Flat palette (T-102 flat look)
FLAT = {
    "sky": "#E3EFD3",
    "far": "#B9D8AE",
    "mid": "#6FAE7C",
    "midTrunk": "#7A8466",
    "ground": "#3F8358",
    "front": "#2E6E47",
    "front2": "#3A7F53",
    "shadow": "#2A5E3E",
    "pim": "#7FD6A4",
    "leaf": "#3FAF77",
    "vein": "#7FD6A4",
    "eye": "#1F2A2E",
}


def squircle(cx: float, cy: float, r: float, n: float = 5.0, steps: int = 256) -> str:
    """Superellipse |x|^n + |y|^n = r^n, the macOS-like continuous corner."""
    pts = []
    for i in range(steps):
        t = 2 * math.pi * i / steps
        c, s = math.cos(t), math.sin(t)
        x = cx + r * math.copysign(abs(c) ** (2 / n), c)
        y = cy + r * math.copysign(abs(s) ** (2 / n), s)
        pts.append(f"{x:.2f} {y:.2f}")
    return "M" + " L".join(pts) + " Z"


# --- forest -------------------------------------------------------------------------------
# (x, y of the canopy centre, radius) in tile units. Lists reach past the tile for the
# adaptive layers (visible 0..1024, layer -256..1280).
FAR = [(-230, 470, 95), (-90, 440, 85), (40, 465, 80), (160, 430, 90), (300, 455, 78), (420, 425, 85),
       (560, 445, 80), (690, 430, 88), (820, 455, 82), (950, 435, 90), (1080, 460, 85), (1220, 440, 95)]
MID = [(-200, 560, 120), (-40, 590, 105), (110, 545, 100), (250, 600, 95), (790, 600, 95),
       (930, 545, 105), (1070, 590, 110), (1230, 560, 120)]
FRONT = [(-60, 560, 175), (40, 760, 165), (-120, 900, 200), (1085, 560, 175), (990, 760, 165),
         (1150, 900, 200)]
BUSH = [(150, 1050, 175), (880, 1040, 185), (-60, 1120, 200), (1100, 1120, 200)]


def blob(x: float, y: float, r: float, fill: str, extra: str = "") -> str:
    """Round tree canopy: one big ball and two smaller shoulders, merged."""
    return (
        f'<g {extra}>'
        f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{r:.1f}" fill="{fill}"/>'
        f'<circle cx="{x - 0.55 * r:.1f}" cy="{y + 0.38 * r:.1f}" r="{0.72 * r:.1f}" fill="{fill}"/>'
        f'<circle cx="{x + 0.55 * r:.1f}" cy="{y + 0.42 * r:.1f}" r="{0.68 * r:.1f}" fill="{fill}"/>'
        f"</g>"
    )


def ball(x: float, y: float, r: float, grad: str) -> str:
    """3D tree: each ball shaded on its own (light from the upper left)."""
    return (
        f'<circle cx="{x - 0.55 * r:.1f}" cy="{y + 0.38 * r:.1f}" r="{0.72 * r:.1f}" fill="url(#{grad})"/>'
        f'<circle cx="{x + 0.55 * r:.1f}" cy="{y + 0.42 * r:.1f}" r="{0.68 * r:.1f}" fill="url(#{grad})"/>'
        f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{r:.1f}" fill="url(#{grad})"/>'
    )


def trunk(x: float, y: float, r: float, fill: str, h: float) -> str:
    w = max(10.0, r * 0.14)
    return f'<rect x="{x - w / 2:.1f}" y="{y + 0.5 * r:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{w / 2:.1f}" fill="{fill}"/>'


DEFS_3D = """
<linearGradient id="sky" gradientUnits="userSpaceOnUse" x1="0" y1="-260" x2="0" y2="760">
  <stop offset="0" stop-color="#F1F4DC"/>
  <stop offset="0.35" stop-color="#E2EFD2"/>
  <stop offset="0.75" stop-color="#C8E2C2"/>
  <stop offset="1" stop-color="#B4D6B1"/>
</linearGradient>
<radialGradient id="sun" gradientUnits="userSpaceOnUse" cx="230" cy="80" r="620">
  <stop offset="0" stop-color="#FCFBE6" stop-opacity="0.85"/>
  <stop offset="1" stop-color="#FCFBE6" stop-opacity="0"/>
</radialGradient>
<radialGradient id="tFar" cx="0.38" cy="0.32" r="0.75">
  <stop offset="0" stop-color="#CBE3C2"/>
  <stop offset="1" stop-color="#9CC59C"/>
</radialGradient>
<radialGradient id="tMid" cx="0.36" cy="0.3" r="0.78">
  <stop offset="0" stop-color="#8FC593"/>
  <stop offset="0.6" stop-color="#5E9F6C"/>
  <stop offset="1" stop-color="#40804F"/>
</radialGradient>
<radialGradient id="tFront" cx="0.36" cy="0.28" r="0.8">
  <stop offset="0" stop-color="#5BA466"/>
  <stop offset="0.55" stop-color="#2F7A45"/>
  <stop offset="1" stop-color="#1C5432"/>
</radialGradient>
<radialGradient id="tBush" cx="0.4" cy="0.25" r="0.8">
  <stop offset="0" stop-color="#4A9258"/>
  <stop offset="0.6" stop-color="#26663A"/>
  <stop offset="1" stop-color="#163F27"/>
</radialGradient>
<linearGradient id="ground" gradientUnits="userSpaceOnUse" x1="0" y1="640" x2="0" y2="1100">
  <stop offset="0" stop-color="#9CC49A"/>
  <stop offset="0.3" stop-color="#6E9F70"/>
  <stop offset="0.6" stop-color="#3F7650"/>
  <stop offset="1" stop-color="#22523A"/>
</linearGradient>
<linearGradient id="fog" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#F4F8EC"/>
  <stop offset="1" stop-color="#F4F8EC"/>
</linearGradient>
<filter id="fBlur14" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="14"/></filter>
<filter id="fBlur9" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="9"/></filter>
<filter id="fBlur4" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="4"/></filter>
<filter id="fBlur30" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="30"/></filter>
<filter id="fBlur2" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="2"/></filter>

<radialGradient id="body" gradientUnits="userSpaceOnUse" cx="56" cy="66" r="44" fx="50" fy="58">
  <stop offset="0" stop-color="#D9F8E8"/>
  <stop offset="0.55" stop-color="#B4EDD2"/>
  <stop offset="0.85" stop-color="#93DDBC"/>
  <stop offset="1" stop-color="#6CC69E"/>
</radialGradient>
<linearGradient id="bodyRim" gradientUnits="userSpaceOnUse" x1="60" y1="34" x2="60" y2="106">
  <stop offset="0" stop-color="#FFFFFF" stop-opacity="0.95"/>
  <stop offset="0.4" stop-color="#FFFFFF" stop-opacity="0.25"/>
  <stop offset="0.8" stop-color="#FFFFFF" stop-opacity="0.1"/>
  <stop offset="1" stop-color="#F0FFF7" stop-opacity="0.75"/>
</linearGradient>
<radialGradient id="bounce" gradientUnits="userSpaceOnUse" cx="60" cy="102" r="26">
  <stop offset="0" stop-color="#EFFFF7" stop-opacity="0.8"/>
  <stop offset="1" stop-color="#EFFFF7" stop-opacity="0"/>
</radialGradient>
<linearGradient id="leaf" gradientUnits="userSpaceOnUse" x1="38" y1="9" x2="57" y2="22">
  <stop offset="0" stop-color="#9AD0A9"/>
  <stop offset="0.5" stop-color="#78BB8B"/>
  <stop offset="1" stop-color="#5DA474"/>
</linearGradient>
<linearGradient id="stem" gradientUnits="userSpaceOnUse" x1="57" y1="0" x2="62" y2="0">
  <stop offset="0" stop-color="#8CCB9E"/>
  <stop offset="1" stop-color="#5FA676"/>
</linearGradient>
<linearGradient id="eye" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#2A3A36"/>
  <stop offset="1" stop-color="#101A18"/>
</linearGradient>
<filter id="blur6" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="6"/></filter>
<filter id="blur3" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="3"/></filter>
<filter id="blur2" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="2"/></filter>
<filter id="blur1" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="1"/></filter>
<filter id="blur05" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="0.5"/></filter>
<clipPath id="bodyClip"><path d="BODY"/></clipPath>
""".replace("BODY", BODY)


def forest_3d() -> str:
    """Sky, three tree layers with mist between them, ground. Covers -400..1424."""
    far = "".join(ball(x, y, r, "tFar") for x, y, r in FAR)
    mid_trunks = "".join(trunk(x, y, r, "#6E7A5E", 230) for x, y, r in MID)
    mid = "".join(ball(x, y, r, "tMid") for x, y, r in MID)
    front = "".join(ball(x, y, r, "tFront") for x, y, r in FRONT)
    bush = "".join(ball(x, y, r, "tBush") for x, y, r in BUSH)
    return f"""
<rect x="-400" y="-400" width="1824" height="1824" fill="url(#sky)"/>
<rect x="-400" y="-400" width="1824" height="1824" fill="url(#sun)"/>
<g filter="url(#fBlur9)" opacity="0.85">{far}</g>
<rect x="-400" y="505" width="1824" height="120" fill="#EEF5E4" opacity="0.75" filter="url(#fBlur30)"/>
<rect x="-400" y="640" width="1824" height="900" fill="url(#ground)"/>
<g filter="url(#fBlur4)">{mid_trunks}{mid}</g>
<rect x="-400" y="680" width="1824" height="130" fill="#E7F1DD" opacity="0.6" filter="url(#fBlur30)"/>
<g filter="url(#fBlur2)">{front}</g>
<g>{bush}</g>
<rect x="-400" y="820" width="1824" height="140" fill="#DDEBD4" opacity="0.18" filter="url(#fBlur30)"/>
"""


def forest_flat() -> str:
    c = FLAT
    far = "".join(blob(x, y, r, c["far"]) for x, y, r in FAR)
    mid_trunks = "".join(trunk(x, y, r, c["midTrunk"], 230) for x, y, r in MID)
    mid = "".join(blob(x, y, r, c["mid"]) for x, y, r in MID)
    front = "".join(blob(x, y, r, c["front2"] if i % 2 else c["front"]) for i, (x, y, r) in enumerate(FRONT))
    bush = "".join(blob(x, y, r, c["front"]) for x, y, r in BUSH)
    return f"""
<rect x="-400" y="-400" width="1824" height="1824" fill="{c['sky']}"/>
{far}
<rect x="-400" y="640" width="1824" height="900" fill="{c['ground']}"/>
{mid_trunks}{mid}
{front}
{bush}
"""


def pim_3d(scale: float, cx: float, cy: float) -> str:
    """Glossy, see-through mint glass Pim, centred so the body+leaf box sits at (cx, cy)."""
    tx, ty = cx - PIM_CX * scale, cy - PIM_CY * scale
    eyes = ""
    for ex, ey in EYES:
        eyes += (
            f'<rect x="{ex}" y="{ey + 0.6}" width="7" height="14" rx="3.5" fill="#0B3A26" opacity="0.25" filter="url(#blur05)"/>'
            f'<rect x="{ex}" y="{ey}" width="7" height="14" rx="3.5" fill="url(#eye)"/>'
            f'<ellipse cx="{ex + 2.3}" cy="{ey + 3.2}" rx="1.1" ry="1.6" fill="#FFFFFF" opacity="0.55"/>'
        )
    return f"""
<g transform="translate({tx:.2f} {ty:.2f}) scale({scale:.4f})">
  <!-- contact shadow -->
  <ellipse cx="60" cy="105.5" rx="33" ry="4.2" fill="#0E3522" opacity="0.55" filter="url(#blur2)"/>
  <ellipse cx="60" cy="106" rx="22" ry="2.2" fill="#0A2A1A" opacity="0.45" filter="url(#blur1)"/>
  <!-- stem + leaf (stem starts inside the head, so the leaf is fixed to Pim) -->
  <path d="{STEM}" fill="none" stroke="url(#stem)" stroke-width="4.6" stroke-linecap="round"/>
  <path d="M59 40 C59 33 58.6 28 57.6 24" fill="none" stroke="#C9EDD3" stroke-width="1" stroke-linecap="round" opacity="0.7"/>
  <path d="{LEAF}" fill="url(#leaf)"/>
  <path d="{VEIN}" fill="none" stroke="#C8E8CF" stroke-width="1" stroke-linecap="round" opacity="0.8"/>
  <path d="M41 13.5 C46 11.5 52 13.5 55.5 18" fill="none" stroke="#E4F6E8" stroke-width="0.8" stroke-linecap="round" opacity="0.6"/>
  <!-- body: slightly see-through glass -->
  <path d="{BODY}" fill="url(#body)" opacity="0.93"/>
  <g clip-path="url(#bodyClip)">
    <!-- thicker, greener glass along the edge -->
    <path d="{BODY}" fill="none" stroke="#4FB287" stroke-opacity="0.45" stroke-width="7" filter="url(#blur3)"/>
    <!-- white inner glow -->
    <path d="{BODY}" fill="none" stroke="#FFFFFF" stroke-opacity="0.6" stroke-width="3" filter="url(#blur2)"/>
    <!-- refraction: deeper mint at the lower right, bounce light at the bottom -->
    <ellipse cx="80" cy="90" rx="16" ry="18" fill="#3FAE7E" opacity="0.22" filter="url(#blur6)"/>
    <ellipse cx="60" cy="102" rx="22" ry="5" fill="url(#bounce)" opacity="0.7" filter="url(#blur2)"/>
    <!-- specular highlights: a long soft streak top left, a small one right -->
    <path d="M36 60 C37 48 44 40 54 37.5" fill="none" stroke="#FFFFFF" stroke-width="3.2" stroke-linecap="round" opacity="0.9" filter="url(#blur1)"/>
    <ellipse cx="45" cy="45" rx="7" ry="3.2" transform="rotate(-35 45 45)" fill="#FFFFFF" opacity="0.75" filter="url(#blur2)"/>
    <path d="M86.5 62 C88 68 88.4 74 88 80" fill="none" stroke="#FFFFFF" stroke-width="1.6" stroke-linecap="round" opacity="0.55" filter="url(#blur1)"/>
    <path d="M33 76 C33.5 84 36 91 41 96" fill="none" stroke="#FFFFFF" stroke-width="1.4" stroke-linecap="round" opacity="0.35" filter="url(#blur1)"/>
  </g>
  <!-- light rim -->
  <path d="{BODY}" fill="none" stroke="url(#bodyRim)" stroke-width="1.1"/>
  {eyes}
</g>"""


def pim_flat(scale: float, cx: float, cy: float) -> str:
    """Flat Pim, colours as in pim_idle.svg, plus a flat ground shadow."""
    c = FLAT
    tx, ty = cx - PIM_CX * scale, cy - PIM_CY * scale
    eyes = "".join(
        f'<rect x="{ex}" y="{ey}" width="7" height="14" rx="3.5" fill="{c["eye"]}"/>' for ex, ey in EYES
    )
    return f"""
<g transform="translate({tx:.2f} {ty:.2f}) scale({scale:.4f})">
  <ellipse cx="60" cy="105.5" rx="31" ry="3.6" fill="{c['shadow']}"/>
  <path d="{STEM}" fill="none" stroke="{c['leaf']}" stroke-width="3.8" stroke-linecap="round"/>
  <path d="{LEAF}" fill="{c['leaf']}"/>
  <path d="{VEIN}" fill="none" stroke="{c['vein']}" stroke-width="1.1" stroke-linecap="round"/>
  <path d="{BODY}" fill="{c['pim']}"/>
  {eyes}
</g>"""


def scene(look: str, size: int, shape: str, layer: str = "all", dev: bool = False, pim_k: float = 7.6) -> str:
    """look: '3d'|'flat'. shape: 'square' (full bleed), 'squircle' (macOS-like tile with
    margin + shadow) or 'adaptive' (108 dp layer, visible tile in the middle 2/3).
    layer: 'all' | 'bg' | 'fg'. pim_k: px per Pim unit in tile units (1024)."""
    if shape == "adaptive":
        vb = "-256 -256 1536 1536"
    else:
        vb = "0 0 1024 1024"
    defs = DEFS_3D if look == "3d" else ""
    bg = forest_3d() if look == "3d" else forest_flat()
    p = (pim_3d if look == "3d" else pim_flat)(pim_k, 512, 548)
    ribbon = dev_ribbon(shape) if dev else ""
    if shape == "squircle":
        r, c = 1024 * 0.4023, 512
        sq_clip = squircle(c, c, r)
        shadow = (
            '<filter id="bigShadow" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="14"/></filter>'
            f'<path d="{squircle(c, c + 12, r)}" fill="#0E3522" opacity="0.30" filter="url(#bigShadow)"/>'
        )
        rim = (
            f'<path d="{sq_clip}" fill="none" stroke="#FFFFFF" stroke-opacity="0.45" stroke-width="5"/>'
            if look == "3d"
            else ""
        )
        if look == "flat":
            shadow = f'<path d="{squircle(c, c + 8, r)}" fill="#0E3522" opacity="0.18"/>'
        # tile content is drawn in a 0.8047 scaled frame around the centre
        k = r / 512
        inner = f'<g transform="translate({c - 512 * k:.2f} {c - 512 * k:.2f}) scale({k:.4f})">'
        content = ""
        if layer in ("all", "bg"):
            content += bg
        if layer in ("all", "fg"):
            content += p + ribbon
        body = f'{shadow}<clipPath id="tile"><path d="{sq_clip}"/></clipPath><g clip-path="url(#tile)">{inner}{content}</g></g>{rim}'
    else:
        body = ""
        if layer in ("all", "bg"):
            body += f'<clipPath id="tile"><rect x="-400" y="-400" width="1824" height="1824"/></clipPath><g clip-path="url(#tile)">{bg}</g>'
        if layer in ("all", "fg"):
            body += p + ribbon
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" '
        f'viewBox="{vb}"><defs>{defs}</defs>{body}</svg>'
    )


def dev_ribbon(shape: str) -> str:
    """Small dark pill 'dev' at the lower right of Pim — subtle, inside the safe zone."""
    w, h = 190.0, 76.0
    x, y = 512 + 150, 760
    if shape == "adaptive":
        x, y = 512 + 110, 750
    return (
        f'<rect x="{x - w / 2:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{h / 2:.1f}" '
        f'fill="#0B2A20" fill-opacity="0.85" stroke="#FFFFFF" stroke-opacity="0.4" stroke-width="2.5"/>'
        f'<text x="{x:.1f}" y="{y + h * 0.71:.1f}" text-anchor="middle" font-family="DejaVu Sans" '
        f'font-weight="bold" font-size="{h * 0.62:.1f}" fill="#BFF5DA" letter-spacing="2">dev</text>'
    )


def render(svg_str: str, px: int) -> Image.Image:
    data = resvg_py.svg_to_bytes(svg_string=svg_str, width=px, height=px)
    return Image.open(io.BytesIO(bytes(data))).convert("RGBA")


def monochrome_vector() -> str:
    """Themed icon layer: Pim silhouette (body + stem + leaf), eyes cut out. 108 dp canvas."""
    s = 0.50
    tx, ty = 54 - PIM_CX * s, 54 - PIM_CY * s
    eyes = "".join(
        f"M{ex + 3.5},{ey} a3.5,3.5 0 0,1 3.5,3.5 v7 a3.5,3.5 0 0,1 -7,0 v-7 a3.5,3.5 0 0,1 3.5,-3.5 Z "
        for ex, ey in EYES
    )
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- T-102: themed (monochrome) launcher layer, used by both icons (3D and flat) — Pim silhouette, eyes cut out. Generated by assets/icon/src/make_icon.py -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <group
        android:translateX="{tx:.3f}"
        android:translateY="{ty:.3f}"
        android:scaleX="{s}"
        android:scaleY="{s}">
        <path
            android:fillColor="#FF000000"
            android:fillType="evenOdd"
            android:pathData="{BODY} {eyes}" />
        <path
            android:strokeColor="#FF000000"
            android:strokeWidth="3.8"
            android:strokeLineCap="round"
            android:pathData="{STEM}" />
        <path
            android:fillColor="#FF000000"
            android:pathData="{LEAF}" />
    </group>
</vector>
"""


DENSITIES = [("mdpi", 108, 48), ("hdpi", 162, 72), ("xhdpi", 216, 96), ("xxhdpi", 324, 144), ("xxxhdpi", 432, 192)]

ADAPTIVE = """<?xml version="1.0" encoding="utf-8"?>
<!-- T-102: Hain launcher icon "Pim im Wald" (NAME). Layers generated by assets/icon/src/make_icon.py -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/PREFIX_background" />
    <foreground android:drawable="@mipmap/PREFIX_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""

LOOKS = {"3d": ("ic_launcher", "3D glass Pim"), "flat": ("ic_launcher_flat", "flat")}
FG_K = 7.4  # Pim scale on the adaptive foreground: whole Pim stays inside the 66 dp safe zone
TILE_K = 7.6  # Pim scale on the full-bleed / squircle tiles


def circle_mask(img: Image.Image) -> Image.Image:
    m = Image.new("L", img.size, 0)
    ImageDraw.Draw(m).ellipse((0, 0, img.size[0] - 1, img.size[1] - 1), fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), m)
    return out


def write_res(variant: str, dev: bool) -> None:
    res = os.path.join(ROOT, "app", "src", variant, "res")
    for look, (prefix, label) in LOOKS.items():
        for name, layer, legacy in DENSITIES:
            d = os.path.join(res, f"mipmap-{name}")
            os.makedirs(d, exist_ok=True)
            bg = render(scene(look, 1024, "adaptive", "bg", pim_k=FG_K), layer)
            fg = render(scene(look, 1024, "adaptive", "fg", dev=dev, pim_k=FG_K), layer)
            bg.convert("RGB").save(os.path.join(d, f"{prefix}_background.webp"), "WEBP", quality=92)
            fg.save(os.path.join(d, f"{prefix}_foreground.webp"), "WEBP", lossless=True)
            # legacy (API < 26): pre-masked
            full = render(scene(look, 1024, "square", dev=dev, pim_k=TILE_K), legacy)
            sq = render(scene(look, 1024, "squircle", dev=dev, pim_k=TILE_K), legacy)
            sq.save(os.path.join(d, f"{prefix}.webp"), "WEBP", lossless=True)
            circle_mask(full).save(os.path.join(d, f"{prefix}_round.webp"), "WEBP", lossless=True)
        a = os.path.join(res, "mipmap-anydpi-v26")
        os.makedirs(a, exist_ok=True)
        for f in (f"{prefix}.xml", f"{prefix}_round.xml"):
            with open(os.path.join(a, f), "w") as fh:
                fh.write(ADAPTIVE.replace("PREFIX", prefix).replace("NAME", label))
    if not dev:
        dd = os.path.join(res, "drawable")
        os.makedirs(dd, exist_ok=True)
        with open(os.path.join(dd, "ic_launcher_monochrome.xml"), "w") as fh:
            fh.write(monochrome_vector())


def main() -> None:
    S = 1024
    outs = {
        "hain-icon.svg": scene("3d", S, "square", pim_k=TILE_K),
        "hain-icon-macos.svg": scene("3d", S, "squircle", pim_k=TILE_K),
        "hain-icon-dev.svg": scene("3d", S, "squircle", dev=True, pim_k=TILE_K),
        "hain-icon-flat.svg": scene("flat", S, "square", pim_k=TILE_K),
        "hain-icon-flat-macos.svg": scene("flat", S, "squircle", pim_k=TILE_K),
    }
    for name, s in outs.items():
        with open(os.path.join(SRC, name), "w") as fh:
            fh.write(s)
    render(outs["hain-icon.svg"], 1024).convert("RGB").save(os.path.join(ICON, "hain-icon-1024.png"))
    render(outs["hain-icon.svg"], 512).convert("RGB").save(os.path.join(ICON, "hain-icon-512.png"))
    render(outs["hain-icon-macos.svg"], 1024).save(os.path.join(ICON, "hain-icon-rounded-1024.png"))
    render(outs["hain-icon-dev.svg"], 1024).save(os.path.join(ICON, "hain-icon-dev-1024.png"))
    render(outs["hain-icon-flat.svg"], 1024).convert("RGB").save(os.path.join(ICON, "hain-icon-flat-1024.png"))
    render(outs["hain-icon-flat-macos.svg"], 1024).save(os.path.join(ICON, "hain-icon-flat-rounded-1024.png"))
    render(scene("3d", S, "adaptive", "bg", pim_k=FG_K), 1024).convert("RGB").save(
        os.path.join(ICON, "hain-icon-background.png")
    )
    render(scene("3d", S, "adaptive", "fg", pim_k=FG_K), 1024).save(os.path.join(ICON, "hain-icon-foreground.png"))
    render(scene("flat", S, "adaptive", "bg", pim_k=FG_K), 1024).convert("RGB").save(
        os.path.join(ICON, "hain-icon-flat-background.png")
    )
    render(scene("flat", S, "adaptive", "fg", pim_k=FG_K), 1024).save(
        os.path.join(ICON, "hain-icon-flat-foreground.png")
    )
    if "--no-res" not in sys.argv:
        write_res("main", dev=False)
        write_res("debug", dev=True)
        write_previews()


def write_previews() -> None:
    """T-103: small previews for the icon picker (onboarding + settings), in core-ui."""
    d = os.path.join(ROOT, "core-ui", "src", "main", "res", "drawable-nodpi")
    os.makedirs(d, exist_ok=True)
    for look, name in (("3d", "hain_app_icon_3d"), ("flat", "hain_app_icon_flat")):
        img = render(scene(look, 1024, "squircle", pim_k=TILE_K), 1024)
        # cut the margin around the squircle (keeps a little room for the shadow)
        m = int(1024 * 0.085)
        img.crop((m, m, 1024 - m, 1024 - m)).resize((288, 288), Image.LANCZOS).save(
            os.path.join(d, f"{name}.webp"), "WEBP", lossless=True
        )


if __name__ == "__main__":
    main()
