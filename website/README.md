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

## Placeholders to replace

1. **Screenshots** (section "A look inside"): real app screenshots in style A "Lichtung", light and dark
   (`public/screenshots/<id>-light.webp` / `<id>-dark.webp`, made from `../docs/design/screens/style_a_*.png`,
   720 px wide, WebP q82). The list is `SCREENSHOTS` at the end of `src/content.ts`, captions in `shots.items`.
   The page shows the dark file when the visitor's system is dark.
2. **Imprint (Impressum):** the footer shows "Imprint coming soon". A German website run by a person
   usually needs an Impressum: add a page or link it in `Footer` (`src/components/Sections.tsx`, marked
   `PLACEHOLDER`).
3. **Download link:** points to the `nightly` pre-release. When there are real releases, change
   `LINKS.download` to `https://github.com/oskar26/hain/releases/latest`.
4. **Network log** in the privacy section is an *example* (labelled as such), not live data.

## Style (since 03.11.2026): A "Lichtung"

Same look as the app's style A (`core-ui/.../HainStyle.kt`, `docs/design/STYLE_OPTIONS.md`): lots of white,
hairline borders (`.card`), Hain green only as an accent, radii 12–16 px, calm Lucide line icons (stroke 1.75),
a soft blurred mint light behind the hero (`.mist`), Hanken Grotesk (the app font, self-hosted).
Light and dark follow the system (`prefers-color-scheme`); all colours are CSS variables in `src/styles.css`.
Feature cards carry a status badge: `ready` = green "Available now / Schon da", `soon` = grey "Coming soon / Kommt bald"
(`features.items[].status` in `content.ts`, kept in sync with the feature table in the root README.md).
