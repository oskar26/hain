# Website licences

The website code in `website/` is part of Hain and licensed under **GPL-3.0** (see `../LICENSE`).
The app icon (`public/img/hain-icon.webp`, the favicons, `public/og.png`, `../assets/icon/`) is own work:
drawn as SVG with gradients in `../assets/icon/src/make_icon.py` (Pim's shapes from `../assets/mascot/v2/`),
rendered with resvg and Pillow. No image model, no third-party artwork. The screenshots in `public/screenshots/`
are real screenshots of the app (style A "Lichtung", `../docs/design/screens/`).

## Dependencies

| Package | Version | Licence | Ships to visitors? |
|---|---|---|---|
| motion (framer-motion DOM helper `inView`) | 14.9 | MIT | yes (a few KB) |
| Hanken Grotesk 3.014 (the app font, self-hosted WOFF2 in `src/assets/fonts/`, text in `../docs/specs/licenses/HankenGrotesk-OFL.txt`) | 3.014 | OFL-1.1 | yes (font files) |
| lucide-react (icons, rendered to inline SVG at build time) | 1.66 | ISC | yes (as SVG markup) |
| React, React DOM (only used at build time to prerender HTML) | 19.3 | MIT | no |
| Tailwind CSS, @tailwindcss/vite | 4.3 | MIT | generated CSS only |
| Vite, @vitejs/plugin-react | 8.3 / 6.2 | MIT | no (build tool) |
| TypeScript | 7.0 | Apache-2.0 | no (build tool) |

All are compatible with GPL-3.0. No Google Fonts, no CDN, no analytics, no cookies.

The GitHub mark in the header/footer is GitHub's logo, used only to link to the repository
(per GitHub's logo guidelines).

## Aceternity UI and 21st.dev

Some effects were **inspired by** patterns shown on [Aceternity UI](https://ui.aceternity.com) and
[21st.dev](https://21st.dev) (bento grid, infinite moving cards / marquee, spotlight card, 3D card tilt,
text reveal, tracing beam timeline, aurora-style background, FAQ accordion).
**No code was copied from either site.** Everything here is written from scratch for Hain.

Reason (checked 02.11.2026):
- The Aceternity licence (https://ui.aceternity.com/licence) allows using items in end products but forbids
  redistributing the item's source files. A public GPL repository would redistribute the source, so copying
  their component code into this repo would not be allowed.
- The 21st.dev terms (https://21st.dev/terms) say components belong to their authors with their own
  licences and require a visible link back when redistributing. Each component would need its own
  licence check.

If someone later wants to add a component from these sites, check that component's own licence first,
add a row above, and keep the link back.
