# Hain website

The homepage for Hain: https://oskar26.github.io/hain/ (English) and https://oskar26.github.io/hain/de/ (German).

Vite + React + TypeScript + Tailwind CSS. React only runs **at build time**: every page is prerendered to
static HTML, and the browser loads just one small script (`src/client.ts`, about 5 KB gzipped) for the
effects. No tracking, no cookies, no external fonts or CDNs.

## Develop

```bash
cd website
npm ci
npm run dev       # http://localhost:5173/hain/  (German: /hain/de/)
npm run build     # static site in website/dist/
npm run preview   # serve the build
```

`npm run build` runs: type check → client build → SSR build of `src/entry-server.tsx` →
`scripts/postbuild.mjs`, which writes `dist/index.html` (English), `dist/de/index.html` (German) and `dist/404.html`.

Deployment: `.github/workflows/pages.yml` builds and publishes to GitHub Pages on every push to `main`
that changes `website/**`.

## Where things are

| What | Where |
|---|---|
| All texts (English + German) | `src/content.ts` – keep both languages in sync, plain language, German uses "du" |
| Links (download, GitHub …) | `LINKS` in `src/content.ts` |
| Sections | `src/components/Hero.tsx` (nav + hero), `src/components/Sections.tsx` (everything else) |
| Effects (reveal on scroll, menu) | `src/client.ts` + data attributes, see the comment in `src/components/ui.tsx` |
| Styles, colours, animations | `src/styles.css` (Tailwind theme + custom CSS) |
| Images | `public/img/hain-icon.webp` (app icon, from `../assets/icon/`), `public/og.png`, favicons, app screenshots in `public/screenshots/` |
| Licences | `LICENSES.md` |

**Reduced motion:** with the system setting "reduce motion" all animations are off
(`@media (prefers-reduced-motion: reduce)` in `styles.css`, plus a check in `client.ts`).

## Screenshots and placeholders

1. **Screenshots** (T-269): one per feature in `features.items[].shot` (`src/content.ts`), files
   `public/screenshots/<id>-<light|dark>[-en].webp` (720 px wide, tablet 1280, WebP q82). They come from the
   Roborazzi tests in the CI workflow `screens.yml` (`WebShowcaseScreens`, `WebSettingsScreens`, the
   `web*` tests in `NewsScreens`, plus the Lichtung style tests). Download the "screens" artifact, unzip it
   and run `bash website/scripts/web_shots.sh <dir>`. Sample texts are invented (no real people or places).
2. **Imprint (Impressum):** the footer shows "Imprint coming soon". A German website run by a person
   usually needs an Impressum: add a page or link it in `Footer` (`src/components/Sections.tsx`, marked
   `PLACEHOLDER`).
3. **Download link:** `LINKS.download` = `https://github.com/oskar26/hain/releases/latest` (the newest
   stable release with the APK; the nightly is a pre-release and never "latest").
4. **Network log** in the privacy section is an *example* (labelled as such), not live data.

## Style (since 03.11.2026): A "Lichtung"

Same look as the app's style A (`core-ui/.../HainStyle.kt`, `docs/design/STYLE_OPTIONS.md`): lots of white,
hairline borders (`.card`), Hain green only as an accent, radii 12–16 px, calm Lucide line icons (stroke 1.75),
a soft blurred mint light behind the hero (`.mist`), Hanken Grotesk (the app font, self-hosted).
Light and dark follow the system (`prefers-color-scheme`); all colours are CSS variables in `src/styles.css`.
Every feature carries one of three calm marks (T-269, `Status` in `content.ts`, the same marks as the feature
table in the root README.md): `since01` = "Since 0.1 / Seit 0.1" (hairline: already in the 0.1 test versions,
state of 0.1.3.1), `new` = "New in 0.2 / Neu in 0.2" (green: added on the way to 0.2), `planned` =
"Planned / Geplant" (dashed: in KONZEPT.md or ROADMAP.md, not in the app yet; section "What comes next").
