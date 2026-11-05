# RELEASE_0.2_CHECKLIST.md — what the 0.2.0 release step needs

> Oskar, 05.11.2026 07:33: **0.2.0 is released only when every 0.2 feature is in.** Then README and website
> are reworked: the website gets a **new screenshot of every feature** and **one highlighted section per
> feature** (EN + /de), the README a **complete feature overview**. This file collects what that step
> needs. It is a work list, not release notes.

## How the screenshots are made

- All shots come from Roborazzi tests (`*/src/test/kotlin/**/screens/`), recorded by the CI workflow
  `screens.yml` (artifact `screens`). Phone: 392 × 850 dp (xxhdpi), tablet: 1280 × 800 dp.
- **Every shot in German and English, light and dark.** German = file name without suffix, English = `_en`
  (website: `<id>-light.webp`, `<id>-light-en.webp`, … 720 px wide, WebP q82).
- Sample texts: invented only. No real people, places or addresses of the team (T-252: the German shots use
  the same Paris example as the English ones).
- Status in the column "Shot": **da** = test exists in DE + EN, **DE** = only German so far, **neu** = test still
  to write.

## One line per feature

| # | Feature (website section) | Screen name (test → file) | Shot | Website text (EN / DE) |
|---|---|---|---|---|
| 1 | Welcome tour + language step | `OnboardingStyleScreens` → `onboarding` | da | Welcome / Willkommen |
| 2 | Brain download, skip, celebration | `OnboardingScreens` → `onboarding_success` | DE | Your brain, on your phone / Dein Gehirn auf dem Handy |
| 3 | Chat: start page with Pim and suggestions | `ChatStyleScreens` → `chat_empty` | da | Just ask / Frag einfach |
| 4 | Answer with Markdown, tables, code | `ChatStyleScreens` → `chat_long_answer` | da | Real answers / Richtige Antworten |
| 5 | Thinking level in the message box | `ChatStyleScreens` → `chat_composer_picker` | da | Fast or thorough / Schnell oder gründlich |
| 6 | One Pim status (waiting, thinking, looking up, writing) | `ChatStyleScreens` → `chat_pim_tools`; `ChatScreens` → `chat_status_*` | da / DE | Pim shows what he does / Pim zeigt, was er tut |
| 7 | Frosted glass top bar, message box, menus | `ChatScreens` → `menu_frosted_*` (core-ui `MenuBlurScreens`) | DE | Frosted glass / Milchglas |
| 8 | Saved chats: drawer, search, pin, rename, delete | `ChatStyleScreens` → `drawer` | da | Your chats stay / Deine Chats bleiben |
| 9 | Versions ‹ 2/3 › and waiting messages | `BranchQueueScreens` → `branches_queue` | DE | Nothing gets lost / Nichts geht verloren |
| 10 | Brain picker in the top bar | `ModelPickerScreens` → `chat_brain_picker_*` | DE | Choose the brain / Gehirn wählen |
| 11 | Tools: weather, Wikipedia, sources with site icons | `ChatScreens` → `chat_answer_*` | DE | Pim looks it up / Pim schaut nach |
| 12 | Web search + reading several pages (thorough) | neu: `chat_research_steps` | neu | Research / Recherche |
| 13 | Calculator and units | neu: `chat_calculator` | neu | Exact maths / Exakt rechnen |
| 14 | Phone actions with confirm card (alarm, timer, calendar, route) | neu: `chat_action_card` (core-cards) | neu | Always with your OK / Immer mit deinem OK |
| 15 | Answer cards: list, table, checklist, progress, buttons, charts | `CardScreens` → `cards_light`, `cards_dark`, `cards_list_light` | DE | Cards in the answer / Karten in der Antwort |
| 16 | Memory: page, topics, suggestions | `MemoryScreens` → `memory_*` | DE | Pim remembers – if you want / Pim merkt sich was – wenn du willst |
| 17 | Memory chips under the answer | `MemoryScreens` → `memory_answer_chips_light` | DE | (part of 16) |
| 18 | News: list with pictures + briefing | `NewsScreens` → `news` | da | Your news / Deine Nachrichten |
| 19 | News: reading view (Aa, sepia) | `NewsScreens` → `news_reader` | da | Read in Hain / In Hain lesen |
| 20 | News: ask Pim about the article | `NewsScreens` → `news_ask_light` | DE | Ask about the article / Frag zum Artikel |
| 21 | News: your briefing today + morning note | `NewsScreens` → `news_briefing_light`, `news_settings_light` | DE | Briefing |
| 22 | News: own feeds (3 modes) | `NewsScreens` → `news_feeds_light` | DE | Your sources / Deine Quellen |
| 23 | Documents: paper clip, file chip, answer with [S. 2] | neu: `chat_document_answer` | neu | Your PDFs and photos / Deine PDFs und Fotos |
| 24 | Text recognition question (OCR pack) | neu: `chat_ocr_ask` | neu | (part of 23) |
| 25 | My files | neu: `my_files` | neu | (part of 23) |
| 26 | Share into Hain (text, link, PDF, photo – T-253) | neu: `share_new_chat_file` | neu | From any app / Aus jeder App |
| 27 | Widget "Frag Pim" + quick tile | neu: widget preview (Glance) | neu | On your home screen / Auf dem Startbildschirm |
| 28 | Dictation (mic) | neu: `chat_dictation` | neu | Speak instead of typing / Sprechen statt tippen |
| 29 | Read aloud | neu: `chat_read_aloud` | neu | (part of 28) |
| 30 | Private chat | neu: `chat_private` | neu | Private chat / Privater Chat |
| 31 | App lock + block screenshots | `SettingsScreens` → privacy page, neu: `settings_privacy` | neu | Lock / Sperre |
| 32 | Export chat as PDF / Markdown | neu: `export_sheet` (+ PDF page as picture) | neu | Take your chats along / Chats mitnehmen |
| 33 | Network log + offline mode | neu: `settings_network_log` | neu | You see every request / Du siehst jede Anfrage |
| 34 | Settings hub | `SettingsStyleScreens` → `settings` | da | Settings / Einstellungen |
| 35 | Look: theme, app icon, font, vibration, reduce motion | `SettingsScreens` → `settings_look_dark` | DE | Your look / Dein Aussehen |
| 36 | Models & packs (Knowledge, Brains, Multimedia, Voice, Extras) | `SettingsScreens` → `packs_hub_light`, `packs_voice_dark` | DE | Models & packs / Modelle & Pakete |
| 37 | Wikipedia offline | `SettingsScreens` → `packs_knowledge_light` | DE | Wikipedia without internet / Wikipedia ohne Internet |
| 38 | Search by meaning (memory pack) | `MemoryScreens` → `memory_smart_search_dark` | DE | (part of 16) |
| 39 | About Hain, update check | `SettingsScreens` → `settings_about` | DE | About / Über Hain |
| 40 | Developer mode | `SettingsScreens` → `settings_dev` | DE | (README only) |
| 41 | Tablets: chat list, settings list-detail | `TabletScreens` → `tablet_chat_*`; `TabletSettingsScreens` → `tablet_settings_*` | DE | Tablets |
| 42 | Tablets: packs + memory in two columns (T-251) | `TabletSettingsScreens` → `tablet_packs_*`; `TabletMemoryScreens` → `tablet_memory_*` | DE | (part of 41) |
| 43 | Tablets: news list + article side by side | `NewsScreens` → `news_tablet_*` | DE | (part of 41) |
| 44 | Pim: reactions, 2048 easter egg | core-ui `pim/*` frames | DE | Meet Pim / Das ist Pim |

## README for 0.2.0

- **Feature overview**, one row per line above (status ✅ / 🚧 / 🗓️), grouped: Chat · Pim & look · Tools ·
  News · Memory · Documents · Voice · Privacy · On the phone · Tablets · Packs.
- **What's new in 0.2.0** in five bullets; the long per-version notes ("New in the latest test build") move
  into a `CHANGELOG.md` (DE + EN).
- **Install / update** from the 0.2.0 release instead of nightly; how signing works (see "Release step").
- Screenshot grid: one shot per area (from the table), light, DE and EN alternately or EN only.
- "Status and roadmap": 0.2.0 = released, next = 0.3 (mini-apps forest, images, voice mode, F-Droid).

## Release step (only after Oskar's go)

1. `hain.versionName=0.2.0` in `gradle.properties`, release notes DE + EN, README + website as above.
2. Signing: today `apk.yml` uploads the **debug-signed** APK (`app.hain.debug`, public debug key, updates
   over every nightly) to releases of `v*` tags; the R8 release build (`app.hain`) is **unsigned** and is only
   signed with the debug key for the CI smoke test. For a real release APK Oskar needs his own keystore as
   repository secrets plus a signing step for tags; until then `hain-debug.apk` is the release file.
3. Tag `v0.2.0` on main (Oskar), the workflow then creates the release with `hain-debug.apk` + `version.json`
   (channel "stable", read by the update check).
