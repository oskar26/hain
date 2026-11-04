# Hain app icon (T-088, motif "Pim als Sprechblase" since T-100i)

Status: **in the app** (launcher icon, adaptive + themed), on the website and in the README.

![Hain icon](../../assets/icon/hain-icon-rounded-1024.png)

## What it shows

**Motif: "Pim als Sprechblase"** — chosen by Oskar on 04.11.2026 (T-100i; reference sketch "Konzept F"). Pim's glossy mint glass body has the shape of a round chat bubble: a soft oval, a bit wider than tall, with a small rounded tail at the lower left, all one smooth path. Inside sit Pim's two dark dot eyes with a tiny catch-light. Pim's stem grows out of the top of the bubble (it starts inside the body, so the leaf is fixed to Pim) and carries the leaf. The bubble says "chat", the eyes and leaf say "Pim". Drawn like a modern macOS app icon:

- **Tile:** continuous-corner squircle (superellipse n = 5, 824/1024 like the macOS grid), soft vertical gradient `#5FDCA6` → Hain green `#16A36B`-ish `#22B47A` → `#0B7A4F`, a white glow top-left, a darker pool bottom-right, a thin light edge (glass rim) and a soft drop shadow.
- **Pim (bubble):** ellipse cx 62 cy 66 rx 36 ry 28.5 in Pim units plus the tail (tip at 25.5/97); glass look — radial mint gradient (almost white → `#7EDDB0` → `#3DBF85`), white inner glow along the edge, a specular highlight top-left, a light edge on top, a mint bounce light at the bottom, a contact shadow.
- Calm and adult: no text, no outlines, no extra props. Recognisable at 48 px (checked masked round and squircle).

**Debug build:** same icon with a small dark pill “dev” at the bottom (inside the safe zone), so the debug app (`app.hain.debug`) is easy to tell apart.

## Source

`assets/icon/src/make_icon.py` builds everything from one SVG description (pure vector, gradients + Gaussian blur, rendered with resvg). Units, eyes, stem and leaf follow `assets/mascot/v2/static/pim_idle.svg`; the bubble body is drawn in the script (`BODY`).

```bash
pip install resvg-py pillow
python3 assets/icon/src/make_icon.py          # assets/icon + app/src/main/res + app/src/debug/res
python3 assets/icon/src/make_icon.py --no-res # only assets/icon
```

Own work, no image model and no third-party artwork. The SVGs are in `assets/icon/src/` (`hain-icon.svg` full-bleed master, `hain-icon-macos.svg` squircle, `hain-icon-dev.svg`).

## Files (`assets/icon/`)

| File | Size | Use |
|---|---|---|
| `hain-icon-1024.png` | 1024², full-bleed square | Master; stores, F-Droid |
| `hain-icon-512.png` | 512², full-bleed | Play Store listing (Play masks it) |
| `hain-icon-rounded-1024.png` | 1024², squircle on transparent | Website, README, docs, social |
| `hain-icon-dev-1024.png` | 1024², squircle | Debug variant |
| `hain-icon-foreground.png` | 1024², transparent | Adaptive foreground (Pim inside the 66 dp safe zone) |
| `hain-icon-background.png` | 1024² | Adaptive background (gradient tile) |

## In the app (`app/src/main/res`, `app/src/debug/res`)

- `mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml`: adaptive icon with background, foreground and **monochrome** layer.
- `mipmap-{m,h,xh,xxh,xxxh}dpi/`: `ic_launcher_foreground.webp`, `ic_launcher_background.webp` (108 dp layers) and the legacy `ic_launcher.webp` (squircle) / `ic_launcher_round.webp` (48 dp).
- `drawable/ic_launcher_monochrome.xml`: themed icon (Android 13+), Pim bubble silhouette (body + stem + leaf) with the eyes cut out, vector.
- The debug source set only overrides the mipmaps (with the “dev” pill); the monochrome layer comes from `main`.
- `AndroidManifest.xml`: `android:icon="@mipmap/ic_launcher"`, `android:roundIcon="@mipmap/ic_launcher_round"`.

Website copies (`website/public/`): `img/hain-icon.webp` (rounded, 256 px), `favicon.png` (rounded, 64 px), `apple-touch-icon.png` (full-bleed, 180 px), `og.png` (rounded icon at 420 px, placed at 700/105). Update them whenever the icon changes.

## History

- 04.11.2026 (T-100i): "Pim als Sprechblase" (this icon).
- 03.11.2026 (T-088): glossy glass Pim as a mint drop (Pim v2.1 shape) on the same squircle.
- 02.11.2026: 3D icon from an image model, never built into the app.
