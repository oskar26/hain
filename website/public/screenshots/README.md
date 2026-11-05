App screenshots for the website (WebP, 720 px wide, tablet 1280 px): `<id>-<light|dark>[-en].webp`.
They come from the Roborazzi tests (CI workflow `screens.yml`, artifact "screens"); convert them with
`bash website/scripts/web_shots.sh <unzipped artifact>`. Which feature shows which id: `features.items[].shot`
in `src/content.ts`. Sizes other than 720 × 1561 go into `SHOT_SIZE` there.
