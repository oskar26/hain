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
| Effects (reveal, parallax, tilt, menu) | `src/client.ts` + data attributes, see the comment in `src/components/ui.tsx` |
| Styles, colours, animations | `src/styles.css` (Tailwind theme + custom CSS) |
| Images | `public/img/` (bubbles, 3D Pim, icon), `public/og.png`, favicons |
| Licences | `LICENSES.md` |

**Reduced motion:** with the system setting "reduce motion" all animations, parallax and tilt are off
(`@media (prefers-reduced-motion: reduce)` in `styles.css`, plus a check in `client.ts`).

## Placeholders to replace

1. **Screenshots** (section "A look inside"): 4 phone frames currently show "Screenshot coming soon".
   Put real screenshots (portrait, about 1080 × 2340, WebP preferred, < 150 KB each) into
   `public/screenshots/` and set `file` in `SCREENSHOTS` at the end of `src/content.ts`:

   ```ts
   export const SCREENSHOTS = [
     { id: '01-welcome', file: '01-welcome.webp', de: '01-welcome-de.webp' }, // welcome tour with Pim
     { id: '02-chat', file: '02-chat.webp' },                                 // chat
     { id: '03-settings', file: '03-settings.webp' },                         // settings
     { id: '04-packs', file: '04-packs.webp' },                               // knowledge packs
   ]
   ```

   `de` is optional (German screenshot for the German page). Captions are in `shots.items` in `content.ts`.
2. **Imprint (Impressum):** the footer shows "Imprint coming soon". A German website run by a person
   usually needs an Impressum: add a page or link it in `Footer` (`src/components/Sections.tsx`, marked
   `PLACEHOLDER`).
3. **Download link:** points to the `nightly` pre-release. When there are real releases, change
   `LINKS.download` to `https://github.com/oskar26/hain/releases/latest`.
4. **Network log** in the privacy section is an *example* (labelled as such), not live data.
