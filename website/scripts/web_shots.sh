#!/bin/bash
# T-269: turns the showcase screenshots of the CI workflow screens.yml (artifact "screens",
# files web/web_<id>_<light|dark>[_en].png) into the website's WebPs:
# public/screenshots/<id>-<light|dark>[-en].webp, 720 px wide (tablet 1280), WebP q82, plus the
# older Lichtung style shots used on the page.
# Usage: bash website/scripts/web_shots.sh <unzipped screens artifact dir>
set -euo pipefail
SRC=$1
OUT="$(dirname "$0")/../public/screenshots"
IDS="tools research actions cards memory documents branches packs_brains news_briefing settings_privacy packs_knowledge packs_voice tablet"
for id in $IDS; do
  width=720
  [ "$id" = tablet ] && width=1280
  for m in light dark; do
    for lang in "" _en; do
      f=$(find "$SRC" -name "web_${id}_${m}${lang}.png" | head -1)
      [ -n "$f" ] || { echo "missing web_${id}_${m}${lang}.png" >&2; exit 1; }
      magick "$f" -resize "${width}x" -quality 82 -define webp:method=6 "$OUT/${id}-${m}${lang/_/-}.webp"
    done
  done
done
# The Lichtung style shots and the news reader (older tests, same artifact): <file prefix>:<id>
for pair in lichtung_onboarding:onboarding lichtung_chat_empty:chat_empty lichtung_chat_composer_picker:chat_composer_picker \
            lichtung_chat_long_answer:chat_long_answer lichtung_drawer:drawer lichtung_settings_look:settings_look news_reader:news_reader; do
  prefix=${pair%%:*}; id=${pair##*:}
  for m in light dark; do
    for lang in "" _en; do
      f=$(find "$SRC" -name "${prefix}_${m}${lang}.png" | head -1)
      [ -n "$f" ] || { echo "missing ${prefix}_${m}${lang}.png" >&2; exit 1; }
      magick "$f" -resize 720x -quality 82 -define webp:method=6 "$OUT/${id}-${m}${lang/_/-}.webp"
    done
  done
done
echo "done: $(ls "$OUT" | wc -l) files in public/screenshots"
