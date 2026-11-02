# Hain app icon (3D Pim)

Status: **design ready, not yet built into the app.** The website already uses it (favicon, header, hero, social preview).

![Hain icon](../../assets/icon/hain-icon-rounded-1024.png)

## What it shows

Pim as a soft, glossy 3D creature: a mint-green jelly drop with a small leaf sprout on top and two oval white eyes, lit from inside. Background: a calm gradient from Hain green `#16A36B` (top) to mint and a touch of turquoise (bottom). No text. Style follows Oskar's style reference "App-Icon" (glossy 3D creature, light-to-saturated gradient, inner glow), drawn new for Hain.

## Files (`assets/icon/`)

| File | Size | Use |
|---|---|---|
| `hain-icon-1024.png` | 1024 × 1024, full-bleed square | **Master.** Store listings, F-Droid, source for every other size |
| `hain-icon-512.png` | 512 × 512 | Play Store / F-Droid listing icon |
| `hain-icon-rounded-1024.png` | 1024 × 1024, transparent corners | Website, docs, README, social media |
| `hain-icon-foreground.png` | 1024 × 1024, transparent | Adaptive icon **foreground** layer (Pim only, ~58 % of the canvas, inside the 66 % safe zone) |
| `hain-icon-background.png` | 1024 × 1024 | Adaptive icon **background** layer (gradient only) |

How they were made: generated with an image model (Hark `generate_image`, several variants, the best one picked), then cut out and resized with Pillow. No third-party artwork is included.

## Building it into the app later (adaptive icon)

Do this in a separate task (the app module is `app/`). Don't change the launcher icon in the same PR as unrelated work.

1. **Export the layers** for each density. Adaptive icon layers are 108 dp; only the inner 72 dp (66 %) are always visible.

   | Folder | Layer size (108 dp) | Legacy icon (48 dp) |
   |---|---|---|
   | `mipmap-mdpi` | 108 px | 48 px |
   | `mipmap-hdpi` | 162 px | 72 px |
   | `mipmap-xhdpi` | 216 px | 96 px |
   | `mipmap-xxhdpi` | 324 px | 144 px |
   | `mipmap-xxxhdpi` | 432 px | 192 px |

   ```bash
   for d in mdpi:108:48 hdpi:162:72 xhdpi:216:96 xxhdpi:324:144 xxxhdpi:432:192; do
     IFS=: read name layer legacy <<< "$d"
     out=app/src/main/res/mipmap-$name; mkdir -p "$out"
     magick assets/icon/hain-icon-foreground.png -resize ${layer}x${layer} "$out/ic_launcher_foreground.webp"
     magick assets/icon/hain-icon-background.png -resize ${layer}x${layer} "$out/ic_launcher_background.webp"
     magick assets/icon/hain-icon-rounded-1024.png -resize ${legacy}x${legacy} "$out/ic_launcher.webp"
   done
   ```

   Optional: replace the background PNG with a vector gradient (`drawable/ic_launcher_background.xml`, `#16A36B` → `#79EBC5`/`#78DEE0`) to save space.

2. **Add** `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` (and the same as `ic_launcher_round.xml`):

   ```xml
   <adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
       <background android:drawable="@mipmap/ic_launcher_background" />
       <foreground android:drawable="@mipmap/ic_launcher_foreground" />
       <monochrome android:drawable="@drawable/ic_launcher_monochrome" />
   </adaptive-icon>
   ```

3. **Themed icon (Android 13+):** `ic_launcher_monochrome` should be a flat single-colour silhouette of Pim (drop + leaf, eyes cut out) as a vector drawable, sized like the foreground. The existing flat Pim SVGs in `assets/mascot/v2/static/` are a good starting point.

4. Make sure `AndroidManifest.xml` uses `android:icon="@mipmap/ic_launcher"` and `android:roundIcon="@mipmap/ic_launcher_round"`.

5. Check on a real phone with circle, squircle and rounded-square masks, light and dark wallpaper, and with themed icons on. Pim's eyes must stay fully visible in the circle mask.

## Website use

`website/public/img/hain-icon.webp` (512 px, from the rounded master), `website/public/favicon.png` (64 px), `website/public/apple-touch-icon.png` (180 px), `website/public/og.png` (1200 × 630 social preview), `website/public/img/pim-3d.webp` (the cut-out 3D Pim used in the hero).
