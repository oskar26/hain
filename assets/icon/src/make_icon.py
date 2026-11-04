#!/usr/bin/env python3
"""Hain app icon (T-088, motif "Pim als Sprechblase" since T-100i): glossy glass Pim
shaped like a round chat bubble, on a soft green squircle.

Pure SVG with gradients and blur, rendered with resvg (pip install resvg-py pillow).
Pim units as in assets/mascot/v2/static/pim_idle.svg (viewBox -12 -18 144 144); the body
is a chat bubble (tail lower left), eyes, stem and leaf keep Pim's style.

Usage: python3 assets/icon/src/make_icon.py   (writes into assets/icon/ and app/src/*/res)
"""
from __future__ import annotations

import io
import math
import os
import sys

import resvg_py
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "..", ".."))
ICON = os.path.join(ROOT, "assets", "icon")
SRC = os.path.join(ICON, "src")

# "Pim als Sprechblase" (T-100i, 04.11.2026): Pim's glass body is a round chat bubble
# (ellipse cx 62 cy 66 rx 36 ry 28.5, tail at the lower left), one smooth closed path.
# The stem starts inside the top of the bubble, so the leaf stays fixed to Pim.
BODY = (
    "M43.46 90.43 C50.51 93.78 58.79 95.12 66.93 94.23 C75.08 93.34 82.59 90.27 88.22 85.53 C93.85 80.79 97.27 74.66 97.90 68.17 C98.52 61.68 96.32 55.22 91.67 49.86 C87.01 44.49 80.18 40.55 72.30 38.69 C64.42 36.83 55.98 37.16 48.37 39.62 C40.76 42.09 34.45 46.54 30.48 52.24 C26.51 57.94 25.12 64.54 26.55 70.95 C28.81 81.13 22.98 93.64 25.50 97.00 C28.02 100.36 34.82 86.32 43.46 90.43 Z"
)
STEM = "M61 46 C61 35 60.5 29 58.5 23.5"
LEAF = "M58.5 23.5 C55 14 45 10.5 37.5 14.5 C39 23 49 27.5 58.5 23.5 Z"
VEIN = "M57 22.5 C52 19 46 17 41 16"
EYES = [(47.0, 58.5), (70.0, 58.5)]  # x, y of 7x14 rounded rects


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


DEFS = """
<linearGradient id="bg" x1="0" y1="0" x2="0.35" y2="1">
  <stop offset="0" stop-color="#5FDCA6"/>
  <stop offset="0.45" stop-color="#22B47A"/>
  <stop offset="1" stop-color="#0B7A4F"/>
</linearGradient>
<radialGradient id="bgGlow" cx="0.28" cy="0.18" r="0.75">
  <stop offset="0" stop-color="#FFFFFF" stop-opacity="0.38"/>
  <stop offset="0.55" stop-color="#FFFFFF" stop-opacity="0.06"/>
  <stop offset="1" stop-color="#FFFFFF" stop-opacity="0"/>
</radialGradient>
<radialGradient id="bgDeep" cx="0.75" cy="1.05" r="0.7">
  <stop offset="0" stop-color="#05452C" stop-opacity="0.45"/>
  <stop offset="1" stop-color="#05452C" stop-opacity="0"/>
</radialGradient>
<linearGradient id="rim" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#FFFFFF" stop-opacity="0.85"/>
  <stop offset="0.25" stop-color="#FFFFFF" stop-opacity="0.18"/>
  <stop offset="0.8" stop-color="#FFFFFF" stop-opacity="0"/>
  <stop offset="1" stop-color="#FFFFFF" stop-opacity="0.12"/>
</linearGradient>
<radialGradient id="body" gradientUnits="userSpaceOnUse" cx="52" cy="54" r="60" fx="48" fy="48">
  <stop offset="0" stop-color="#F4FFF9"/>
  <stop offset="0.35" stop-color="#BFF5DA"/>
  <stop offset="0.75" stop-color="#7EDDB0"/>
  <stop offset="1" stop-color="#3DBF85"/>
</radialGradient>
<linearGradient id="bodyRim" gradientUnits="userSpaceOnUse" x1="60" y1="37" x2="60" y2="102">
  <stop offset="0" stop-color="#FFFFFF" stop-opacity="0.95"/>
  <stop offset="0.45" stop-color="#FFFFFF" stop-opacity="0.2"/>
  <stop offset="0.85" stop-color="#FFFFFF" stop-opacity="0.05"/>
  <stop offset="1" stop-color="#E6FFF3" stop-opacity="0.6"/>
</linearGradient>
<radialGradient id="bounce" gradientUnits="userSpaceOnUse" cx="62" cy="96" r="30">
  <stop offset="0" stop-color="#E9FFF5" stop-opacity="0.75"/>
  <stop offset="1" stop-color="#E9FFF5" stop-opacity="0"/>
</radialGradient>
<linearGradient id="leaf" gradientUnits="userSpaceOnUse" x1="39" y1="12" x2="58" y2="25">
  <stop offset="0" stop-color="#F2FFF7"/>
  <stop offset="0.45" stop-color="#B4F2CF"/>
  <stop offset="1" stop-color="#5FD39C"/>
</linearGradient>
<linearGradient id="stem" gradientUnits="userSpaceOnUse" x1="0" y1="23" x2="0" y2="46">
  <stop offset="0" stop-color="#9BE9C1"/>
  <stop offset="1" stop-color="#C4F5DC"/>
</linearGradient>
<linearGradient id="eye" x1="0" y1="0" x2="0" y2="1">
  <stop offset="0" stop-color="#21403A"/>
  <stop offset="1" stop-color="#0C1D19"/>
</linearGradient>
<filter id="blur6" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="6"/></filter>
<filter id="blur3" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="3"/></filter>
<filter id="blur1" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="1.1"/></filter>
<filter id="blur05" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="0.5"/></filter>
<clipPath id="bodyClip"><path d="BODY"/></clipPath>
""".replace("BODY", BODY)


def pim(scale: float, cx: float, cy: float, eyes: bool = True) -> str:
    """Glossy glass Pim, centred so the body+leaf box sits at (cx, cy)."""
    # union box of the bubble Pim: x 25..98, y 10.5..101 → centre (61.5, 56)
    tx, ty = cx - 61.5 * scale, cy - 56 * scale
    eye_svg = ""
    if eyes:
        for ex, ey in EYES:
            eye_svg += (
                f'<rect x="{ex}" y="{ey}" width="7" height="14" rx="3.5" fill="url(#eye)"/>'
                f'<circle cx="{ex + 2.4}" cy="{ey + 3.4}" r="1.35" fill="#FFFFFF" opacity="0.9"/>'
            )
    return f"""
<g transform="translate({tx:.2f} {ty:.2f}) scale({scale:.4f})">
  <!-- contact shadow -->
  <ellipse cx="60" cy="101" rx="34" ry="4.6" fill="#043D27" opacity="0.40" filter="url(#blur3)"/>
  <!-- stem + leaf (stem starts inside the head, so the leaf is fixed to Pim) -->
  <path d="{STEM}" fill="none" stroke="url(#stem)" stroke-width="4.4" stroke-linecap="round"/>
  <path d="{LEAF}" fill="url(#leaf)"/>
  <path d="{LEAF}" fill="none" stroke="#FFFFFF" stroke-opacity="0.8" stroke-width="0.9"/>
  <path d="{VEIN}" fill="none" stroke="#FFFFFF" stroke-width="1.1" stroke-linecap="round" opacity="0.7"/>
  <!-- body -->
  <path d="{BODY}" fill="url(#body)"/>
  <g clip-path="url(#bodyClip)">
    <!-- inner glow along the edge -->
    <path d="{BODY}" fill="none" stroke="#FFFFFF" stroke-opacity="0.55" stroke-width="5" filter="url(#blur3)"/>
    <!-- deeper green at the lower right for volume -->
    <ellipse cx="84" cy="86" rx="24" ry="15" fill="#1E9E68" opacity="0.28" filter="url(#blur6)"/>
    <ellipse cx="62" cy="96" rx="26" ry="7" fill="url(#bounce)" filter="url(#blur3)"/>
    <!-- specular highlight -->
    <ellipse cx="42" cy="49" rx="10" ry="5.2" transform="rotate(-26 42 49)" fill="#FFFFFF" opacity="0.92" filter="url(#blur1)"/>
    <ellipse cx="32" cy="66" rx="2.4" ry="5.2" transform="rotate(-8 32 66)" fill="#FFFFFF" opacity="0.55" filter="url(#blur1)"/>
  </g>
  <!-- light edge -->
  <path d="{BODY}" fill="none" stroke="url(#bodyRim)" stroke-width="1.1"/>
  {eye_svg}
</g>"""


def background(size: int, shape: str) -> str:
    """shape: 'square' (full bleed), 'squircle' (macOS-like with margin + shadow)."""
    if shape == "square":
        clip = f'<rect width="{size}" height="{size}"/>'
        r, c = size / 2, size / 2
    else:
        r, c = size * 0.4023, size / 2  # 824/1024 like macOS Big Sur grid
        clip = f'<path d="{squircle(c, c, r)}"/>'
    sq = squircle(c, c + (0 if shape == "square" else size * 0.012), r)
    shadow = (
        ""
        if shape == "square"
        else f'<path d="{sq}" fill="#03301F" opacity="0.32" filter="url(#bigShadow)"/>'
    )
    return f"""
<filter id="bigShadow" x="-20%" y="-20%" width="140%" height="140%"><feGaussianBlur stdDeviation="{size * 0.014:.1f}"/></filter>
<clipPath id="tile">{clip}</clipPath>
{shadow}
<g clip-path="url(#tile)">
  <rect width="{size}" height="{size}" fill="url(#bg)"/>
  <rect width="{size}" height="{size}" fill="url(#bgDeep)"/>
  <rect width="{size}" height="{size}" fill="url(#bgGlow)"/>
  <!-- soft blurred light pools -->
  <circle cx="{size * 0.86:.0f}" cy="{size * 0.2:.0f}" r="{size * 0.16:.0f}" fill="#B9F7DB" opacity="0.12" filter="url(#bigShadow)"/>
</g>
<g clip-path="url(#tile)">
  <path d="{squircle(c, c, r if shape != 'square' else size * 0.56)}" fill="none" stroke="url(#rim)" stroke-width="{size * 0.006:.1f}"/>
</g>"""


def svg(size: int, shape: str, pim_scale: float, with_bg: bool = True, dev: bool = False) -> str:
    body = background(size, shape) if with_bg else ""
    p = pim(pim_scale, size / 2, size * 0.515)
    ribbon = dev_ribbon(size, shape) if dev else ""
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{size}" height="{size}" '
        f'viewBox="0 0 {size} {size}"><defs>{DEFS}</defs>{body}{p}{ribbon}</svg>'
    )


def dev_ribbon(size: int, shape: str) -> str:
    """Small dark pill 'dev' at the bottom — subtle, inside the safe zone."""
    # square = adaptive foreground / legacy: keep the pill inside the 66 dp safe zone
    w, h = (size * 0.20, size * 0.075) if shape == "squircle" else (size * 0.16, size * 0.06)
    x, y = size / 2 - w / 2, size * (0.80 if shape == "squircle" else 0.72)
    return (
        f'<rect x="{x:.1f}" y="{y:.1f}" width="{w:.1f}" height="{h:.1f}" rx="{h / 2:.1f}" '
        f'fill="#0B2A20" fill-opacity="0.82" stroke="#FFFFFF" stroke-opacity="0.35" stroke-width="{size * 0.002:.1f}"/>'
        f'<text x="{size / 2:.1f}" y="{y + h * 0.71:.1f}" text-anchor="middle" font-family="DejaVu Sans" '
        f'font-weight="bold" font-size="{h * 0.62:.1f}" fill="#BFF5DA" letter-spacing="{size * 0.002:.1f}">dev</text>'
    )


def render(svg_str: str, px: int) -> Image.Image:
    data = resvg_py.svg_to_bytes(svg_string=svg_str, width=px, height=px)
    return Image.open(io.BytesIO(bytes(data))).convert("RGBA")


def monochrome_vector() -> str:
    """Themed icon layer: Pim silhouette (body + stem + leaf), eyes cut out. 108 dp canvas."""
    # Pim units → 108 dp: foreground safe zone 66 dp; Pim box ~100 units tall → scale 0.54
    s = 0.50
    tx, ty = 54 - 61.5 * s, 54 - 56 * s
    eyes = "".join(
        f"M{ex + 3.5},{ey} a3.5,3.5 0 0,1 3.5,3.5 v7 a3.5,3.5 0 0,1 -7,0 v-7 a3.5,3.5 0 0,1 3.5,-3.5 Z "
        for ex, ey in EYES
    )
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- T-088/T-100i: themed (monochrome) launcher layer — Pim bubble silhouette, eyes cut out. Generated by assets/icon/src/make_icon.py -->
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
<!-- T-088/T-100i: Hain launcher icon (glossy Pim as chat bubble). Layers generated by assets/icon/src/make_icon.py -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@mipmap/ic_launcher_background" />
    <foreground android:drawable="@mipmap/ic_launcher_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
</adaptive-icon>
"""


def circle_mask(img: Image.Image) -> Image.Image:
    m = Image.new("L", img.size, 0)
    ImageDraw.Draw(m).ellipse((0, 0, img.size[0] - 1, img.size[1] - 1), fill=255)
    out = Image.new("RGBA", img.size, (0, 0, 0, 0))
    out.paste(img, (0, 0), m)
    return out


def write_res(variant: str, dev: bool) -> None:
    res = os.path.join(ROOT, "app", "src", variant, "res")
    # adaptive layers: 108 dp canvas, Pim inside the 66 dp safe zone
    for name, layer, legacy in DENSITIES:
        d = os.path.join(res, f"mipmap-{name}")
        os.makedirs(d, exist_ok=True)
        S = 1024
        bg = render(svg(S, "square", 0, True, False).replace(pim(0, S / 2, S * 0.515), ""), layer)
        fg = render(svg(S, "square", S * 0.0052, with_bg=False, dev=dev), layer)
        bg.convert("RGB").save(os.path.join(d, "ic_launcher_background.webp"), "WEBP", quality=92)
        fg.save(os.path.join(d, "ic_launcher_foreground.webp"), "WEBP", lossless=True)
        # legacy (API < 26): pre-masked
        full = render(svg(S, "square", S * 0.0072, True, dev), legacy)
        sq = render(svg(S, "squircle", S * 0.0072 * 0.8047, True, dev), legacy)
        sq.save(os.path.join(d, "ic_launcher.webp"), "WEBP", lossless=True)
        circle_mask(full).save(os.path.join(d, "ic_launcher_round.webp"), "WEBP", lossless=True)
    a = os.path.join(res, "mipmap-anydpi-v26")
    os.makedirs(a, exist_ok=True)
    for f in ("ic_launcher.xml", "ic_launcher_round.xml"):
        with open(os.path.join(a, f), "w") as fh:
            fh.write(ADAPTIVE)
    if not dev:
        dd = os.path.join(res, "drawable")
        os.makedirs(dd, exist_ok=True)
        with open(os.path.join(dd, "ic_launcher_monochrome.xml"), "w") as fh:
            fh.write(monochrome_vector())


def main() -> None:
    S = 1024
    k = 0.0072  # Pim units → px per px of canvas (bubble Pim ~73 x 91 units → ~53 x 65 % of the tile)
    master = svg(S, "square", S * k)
    mac = svg(S, "squircle", S * k * 0.8047)
    dev = svg(S, "squircle", S * k * 0.8047, dev=True)
    for name, s in (("hain-icon.svg", master), ("hain-icon-macos.svg", mac), ("hain-icon-dev.svg", dev)):
        with open(os.path.join(SRC, name), "w") as fh:
            fh.write(s)
    render(master, 1024).convert("RGB").save(os.path.join(ICON, "hain-icon-1024.png"))
    render(master, 512).convert("RGB").save(os.path.join(ICON, "hain-icon-512.png"))
    render(mac, 1024).save(os.path.join(ICON, "hain-icon-rounded-1024.png"))
    render(dev, 1024).save(os.path.join(ICON, "hain-icon-dev-1024.png"))
    render(svg(S, "square", 0).replace(pim(0, S / 2, S * 0.515), ""), 1024).convert("RGB").save(
        os.path.join(ICON, "hain-icon-background.png")
    )
    render(svg(S, "square", S * 0.0052, with_bg=False), 1024).save(os.path.join(ICON, "hain-icon-foreground.png"))
    if "--no-res" not in sys.argv:
        write_res("main", dev=False)
        write_res("debug", dev=True)


if __name__ == "__main__":
    main()
