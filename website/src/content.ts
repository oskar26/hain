// All visible text of the website, English (default) and German.
// Keep both languages in sync. Plain language, German uses "du".

export type Lang = 'en' | 'de'

export const LINKS = {
  // T-269: the newest release with the APK (stable releases only, never the nightly)
  download: 'https://github.com/oskar26/hain/releases/latest',
  repo: 'https://github.com/oskar26/hain',
  issues: 'https://github.com/oskar26/hain/issues',
  newIssue: 'https://github.com/oskar26/hain/issues/new',
  contributing: 'https://github.com/oskar26/hain/blob/main/CONTRIBUTING.md',
  license: 'https://github.com/oskar26/hain/blob/main/LICENSE',
  models: 'https://github.com/oskar26/hain/blob/main/docs/specs/MODELS.md',
  concept: 'https://github.com/oskar26/hain/blob/main/docs/concept/KONZEPT.md',
  roadmap: 'https://github.com/oskar26/hain/blob/main/docs/workflow/ROADMAP.md',
  trademarks: 'https://github.com/oskar26/hain/blob/main/TRADEMARKS.md',
}

/**
 * T-269: where a feature stands, the same marks as the feature table in the root README.md.
 * - `since01`: already in the 0.1 test versions (state of 0.1.3.1, 04.11.2026)
 * - `new`:     added on the way to 0.2 (0.1.4 – 0.1.10.11)
 * - `planned`: in the concept (docs/concept/KONZEPT.md) or ROADMAP.md, not in the app yet
 */
export type Status = 'new' | 'since01' | 'planned'

/** One highlighted feature with its screenshot ([shot] = file id in public/screenshots/). */
export type Feature = { id: string; icon: string; status: Status; title: string; text: string; shot: string; shotAlt: string; wide?: boolean }

type Copy = {
  meta: { switchTo: string; switchLabel: string }
  nav: { why: string; features: string; roadmap: string; how: string; privacy: string; faq: string; download: string; menu: string; close: string; mainLabel: string; mobileLabel: string }
  hero: { badge: string; title: [string, string, string]; sub: string; cta: string; ctaHelp: string; note: string; installHint: string; pimAlt: string; iconAlt: string }
  marquee: string[]
  why: { kicker: string; title: string; sub: string; items: { title: string; text: string }[] }
  status: Record<Status, string>
  features: {
    kicker: string; title: string; sub: string
    legend: Record<Status, string>
    items: Feature[]
    moreTitle: string; moreSub: string
    more: { icon: string; status: Status; title: string; text: string }[]
  }
  how: {
    kicker: string; title: string; sub: string
    steps: { title: string; text: string }[]
    helpTitle: string; helpIntro: string; helpSteps: string[]; helpNote: string
    req: string
  }
  privacy: {
    kicker: string; title: string; sub: string
    stays: { title: string; items: string[] }
    online: { title: string; items: string[] }
    logTitle: string; logExample: string
    log: { what: string; where: string }[]
    never: string
  }
  compare: { kicker: string; title: string; sub: string; colHain: string; colCloud: string; rows: { label: string; hain: string; cloud: string; good?: 'hain' | 'cloud' | 'both' }[]; note: string }
  roadmap: {
    kicker: string; title: string; sub: string
    groups: { tag: string; note: string; items: { icon: string; title: string; text: string }[] }[]
    note: string; link: string; conceptLink: string
  }
  faq: { kicker: string; title: string; items: { q: string; a: string }[] }
  join: { kicker: string; title: string; sub: string; items: { title: string; text: string; cta: string; href: string }[] }
  ai: { title: string; text: string }
  footer: { tagline: string; source: string; license: string; models: string; imprint: string; imprintNote: string; noCookies: string; issues: string; concept: string; trademarks: string; support: string; soon: string }
}

export const COPY: Record<Lang, Copy> = {
  en: {
    meta: { switchTo: 'Deutsch', switchLabel: 'Diese Seite auf Deutsch' },
    nav: { why: 'Why Hain', features: 'Features', roadmap: 'Planned', how: 'Install', privacy: 'Privacy', faq: 'FAQ', download: 'Download', menu: 'Open menu', close: 'Close menu', mainLabel: 'Main', mobileLabel: 'Menu' },
    hero: {
      badge: 'Version 0.2 · early version',
      title: ['Your AI.', 'Your phone.', 'Nobody else.'],
      sub: 'Hain is a free AI assistant for Android that runs on your phone. No account, no cloud, no ads, no tracking.',
      cta: 'Download the APK',
      ctaHelp: 'How to install',
      note: 'Free · Open source · Android 10 or newer',
      installHint: 'Hain is not in the Play Store. You install the APK from GitHub and allow your browser once to install apps (“unknown sources”).',
      pimAlt: 'Hain’s start page: Pim says hello and suggests a few questions',
      iconAlt: 'Hain app icon: Pim, a glossy green glass figure, in a soft forest',
    },
    marquee: ['No account', 'No cloud', 'No ads', 'No tracking', 'No Google services', 'Works offline', 'Open source', 'Free', 'German & English'],
    why: {
      kicker: 'Why Hain',
      title: 'A helpful AI that stays with you.',
      sub: 'Most AI apps send everything you type to big data centres. Hain thinks right on your phone.',
      items: [
        { title: 'Private', text: 'Your questions and chats stay on your phone. Nobody reads along, and nothing is used to train an AI.' },
        { title: 'Works offline', text: 'Chatting works without internet, even on a plane. Hain only goes online when you ask for something from the web.' },
        { title: 'Free', text: 'No ads, no hidden costs. Hain is open source and belongs to everyone.' },
        { title: 'Green', text: 'No data centre has to run for your questions. Hain uses the phone you already own.' },
      ],
    },
    status: { new: 'New in 0.2', since01: 'Since 0.1', planned: 'Planned' },
    features: {
      kicker: 'Features',
      title: 'What Hain 0.2 can do.',
      sub: 'Every feature once, with a real screenshot of the app. The examples in the pictures are made up.',
      legend: {
        since01: 'was already in the 0.1 test versions',
        new: 'came on the way to 0.2',
        planned: 'not in the app yet, see below',
      },
      items: [
        {
          id: 'welcome', icon: 'sprout', status: 'since01', shot: 'onboarding',
          title: 'Hello, I’m Pim',
          text: 'A short welcome tour with Pim, then Hain downloads its brain once (about 1 GB, Wi-Fi is best). The download can wait, picks up where it stopped and runs in the background. After that, chatting works without internet.',
          shotAlt: 'Welcome screen with Pim',
        },
        {
          id: 'chat', icon: 'chat', status: 'since01', shot: 'chat_long_answer',
          title: 'Chat that stays on your phone',
          text: 'Answers appear as they are written, with lists, tables and code. Stop at once, copy, share, rate or ask again. The AI runs on your phone, even in flight mode. Small models know less than the big cloud AIs, so check important answers.',
          shotAlt: 'A long answer with a table and code',
        },
        {
          id: 'thinking', icon: 'brain', status: 'since01', shot: 'chat_composer_picker',
          title: 'Fast, Auto or Thorough',
          text: 'Pick in the message box how much effort Pim puts in. Fast answers at once, Thorough takes longer and searches more for hard questions, Auto decides for you.',
          shotAlt: 'The thinking level picker in the message box',
        },
        {
          id: 'brain', icon: 'cpu', status: 'new', shot: 'packs_brains',
          title: 'Choose the brain',
          text: 'The standard brain is LFM3 1.7B (about 1 GB), which thinks out loud when a question needs it. LFM3-Omni 2B (also about 1 GB) is optional: it does not think out loud and calls fewer unneeded tools. Phones with 8 GB of memory or more can add the optional big brain Jan-v3.5 4B (2.7 GB): it picks tools more reliably but answers clearly slower. Each brain says plainly what it is good at; switch any time with a tap on “Hain” at the top of the chat.',
          shotAlt: 'Models & packs, Brains: LFM3 1.7B, LFM3-Omni 2B and Jan-v3.5 4B, each with a short assessment',
        },
        {
          id: 'tools', icon: 'search', status: 'since01', shot: 'tools',
          title: 'Pim looks it up',
          text: 'Weather from Open-Meteo, Wikipedia in German and English and a web search without account or API key. Each step shows under the answer, folded into one quiet line (“3 tools used”) that opens with a tap, with the sources. New in 0.2: weather and Wikipedia cards, site icons at the sources, and tools only come up when your question asks for them.',
          shotAlt: 'Answer with weather card, steps and sources',
        },
        {
          id: 'research', icon: 'book', status: 'since01', shot: 'research',
          title: 'Research with every step',
          text: 'For harder questions Pim searches the web, reads several pages at once and writes from them. You see what it searched and which pages it read, and you can stop at any time. Exact maths and units are calculated, not guessed.',
          shotAlt: 'Pim searched the web and read three pages, the answer is being written',
        },
        {
          id: 'actions', icon: 'alarm', status: 'since01', shot: 'actions',
          title: 'Phone actions, always with your OK',
          text: 'Alarm (also every weekday), timer, calendar entry and directions. Pim only prepares a card; your clock, calendar or map app opens after your tap. New in 0.2: repeating alarms, and if you like, a 3-second countdown with Undo instead of the tap.',
          shotAlt: 'Cards for a weekday alarm and a timer, waiting for a tap',
        },
        {
          id: 'cards', icon: 'cards', status: 'new', shot: 'cards',
          title: 'Cards in the answer',
          text: 'For plans, lists and comparisons Pim answers with a calm card: table, checklist to tick off, progress bar, bar or line chart and buttons. Ticks stay with the chat.',
          shotAlt: 'A study plan as a card with a table, a checklist and a progress bar',
        },
        {
          id: 'memory', icon: 'memory', status: 'new', shot: 'memory',
          title: 'Pim remembers – if you want',
          text: '“Remember that I’m vegetarian.” Pim keeps what you say about yourself on your phone and uses it when it fits. Under the answer you see which memories it used, and “Remember: …” offers new ones with one tap. See, change or delete everything in Settings → Memory.',
          shotAlt: 'Answer with the chips “Remember: is learning Spanish” and “2 memories used”',
        },
        {
          id: 'documents', icon: 'file', status: 'new', shot: 'documents',
          title: 'Ask your PDFs and photos',
          text: 'Attach a PDF, a photo of a page or a text file with the paper clip, or share it from another app. Hain reads the text on your phone (text recognition for scans as an optional pack) and Pim answers with the page.',
          shotAlt: 'A manual attached as PDF, Pim answers with page numbers',
        },
        {
          id: 'news', icon: 'news', status: 'new', shot: 'news_briefing',
          title: 'Your news, calmly',
          text: 'A news reader with pictures: 19 sources in German and English, plus your own feeds if you like. Your briefing of the day is picked and summed up on your phone, and you see each step while it is made.',
          shotAlt: 'News with the briefing card loading feeds 7 of 19',
        },
        {
          id: 'reader', icon: 'reader', status: 'new', shot: 'news_reader',
          title: 'Read in Hain and ask Pim',
          text: 'Articles open in a clean reading view with adjustable text. Ask Pim to sum up or explain an article; it answers only from that article, on your phone. Paywalls stay paywalls.',
          shotAlt: 'An article in the reading view',
        },
        {
          id: 'branches', icon: 'branch', status: 'new', shot: 'branches',
          title: 'Nothing gets lost',
          text: 'Edit a message or ask again and the old version stays: switch with ‹ 2/3 ›. Send your next question while Pim is still answering; it waits its turn.',
          shotAlt: 'Versions 2/2 and 3/3 and a waiting question',
        },
        {
          id: 'chats', icon: 'lock', status: 'since01', shot: 'drawer',
          title: 'Your chats stay – encrypted',
          text: 'Chats are stored encrypted, only on your phone. Search, rename, pin and delete them, with automatic titles. New in 0.2: export one chat as Markdown or PDF, or all at once as a ZIP.',
          shotAlt: 'The chat list in the side panel',
        },
        {
          id: 'privacy', icon: 'shield', status: 'new', shot: 'settings_internet',
          title: 'Private chat, app lock, network log',
          text: 'A private chat is never saved and gone when you leave it. Lock Hain with fingerprint or PIN and block screenshots. Every request to the internet is listed, and one switch on the Internet page turns all of it off. The network log has been there since 0.1; the rest is new.',
          shotAlt: 'The Internet page: one main switch and what may go online',
        },
        {
          id: 'packs', icon: 'package', status: 'new', shot: 'packs_knowledge',
          title: 'Wikipedia without internet',
          text: 'Models & packs sorts everything you can download: Wikipedia for your phone (German from about 140 MB), brains, text recognition, search by meaning and voice. Each pack says what it brings and whether it fits your phone.',
          shotAlt: 'Wikipedia packs in Models & packs',
        },
        {
          id: 'voice', icon: 'mic', status: 'new', shot: 'packs_voice',
          title: 'Speak instead of typing',
          text: 'Tap or hold the mic and speak; while Pim listens, the message box turns into a soft green bar whose lines move with your voice. Dictation runs fully on your phone (German and English, a small pack per language). Pim can read answers aloud, for now with your phone’s own voice.',
          shotAlt: 'Dictation packs for German and English and the read-aloud note',
        },
        {
          id: 'look', icon: 'sliders', status: 'since01', shot: 'settings_look',
          title: 'Your look',
          text: 'Light or dark, an easy-to-read font, app icon, vibration and reduce motion. Answer style and a few words about you are in Settings too.',
          shotAlt: 'Settings page Look',
        },
        {
          id: 'tablet', icon: 'tablet', status: 'new', shot: 'tablet', wide: true,
          title: 'Tablets and wide screens',
          text: 'On a tablet the chat list stays open on the left. Settings, Models & packs, Memory and News use the extra room.',
          shotAlt: 'Hain on a tablet: chat list on the left, chat on the right',
        },
      ],
      moreTitle: 'Also in 0.2',
      moreSub: 'Smaller things without their own picture.',
      more: [
        { icon: 'share', status: 'new', title: 'Share, widget & quick tile', text: 'Share text, links, PDFs or photos from any app into a new chat. A home screen widget “Ask Pim” and a quick tile “New chat”.' },
        { icon: 'languages', status: 'since01', title: 'Speaks your language', text: 'Pim answers in the language you write in, or always in German or English.' },
        { icon: 'update', status: 'new', title: 'Update check (optional)', text: 'Off until you say yes. Then Hain asks GitHub once a day for a new version and shows a calm hint. Nothing installs by itself.' },
        { icon: 'gauge', status: 'new', title: 'Quicker start', text: 'Pim’s fixed instructions are kept after the first read, so the first answer after opening Hain comes sooner.' },
        { icon: 'file', status: 'new', title: 'Formatted answers', text: 'Headings, lists, task lists, quotes, tables you can scroll sideways, code blocks with their language and a copy button, and maths formulas as readable text instead of raw LaTeX.' },
        { icon: 'package', status: 'new', title: 'Downloads that keep going', text: 'Packs keep downloading when you close Hain, with progress in the notification. Pause and resume later; Hain picks up where it stopped.' },
        { icon: 'code', status: 'new', title: 'Developer mode', text: 'Hidden for tinkerers: tap the version 7 times. Try your own GGUF model, tune sampling, see tokens per second.' },
        { icon: 'file', status: 'new', title: 'Open-source licences', text: 'Settings → About Hain lists every library, AI model, font and icon set Hain uses, with version and licence. A tap shows the full licence text, right in the app and offline.' },
        { icon: 'sprout', status: 'since01', title: 'A secret', text: 'Hold Pim on the start page for 5 seconds … (hint: 2048).' },
      ],
    },
    how: {
      kicker: 'Install',
      title: 'Ready in three steps.',
      sub: 'Hain is not in the Play Store. You install it directly from GitHub, which takes about two minutes.',
      steps: [
        { title: 'Download the APK', text: 'Open the download page on your phone. Under “Assets”, tap the file that ends in “.apk”.' },
        { title: 'Install it', text: 'Open the file. Android asks if your browser may install apps. Allow it once, then tap “Install”.' },
        { title: 'Get the brain', text: 'On first start Hain downloads its brain (about 1 GB). Wi-Fi is best. After that, chatting works offline.' },
      ],
      helpTitle: 'Help: “Install unknown apps”, step by step',
      helpIntro: 'Android protects you from apps that don’t come from the Play Store. That is good. To install Hain, you allow it once for your browser:',
      helpSteps: [
        'Tap the downloaded APK file (in the browser or in your “Downloads”).',
        'Android shows a message like “For your security, your phone is not allowed to install unknown apps from this source.” Tap “Settings”.',
        'Turn on “Allow from this source”.',
        'Go back and tap “Install”. Done!',
        'Optional: turn the switch off again afterwards. Hain keeps working.',
      ],
      helpNote: 'The exact words differ a little between phone brands. If Google Play Protect warns you, tap “More details” and “Install anyway”. Hain is open source, so anyone can check the code. Already have a test version? Install the new APK over it; your chats stay.',
      req: 'You need Android 10 or newer and at least 4 GB of RAM; 6 GB or more is recommended.',
    },
    privacy: {
      kicker: 'Privacy',
      title: 'Privacy, explained simply.',
      sub: 'No fine print. Here is exactly what happens with your data.',
      stays: {
        title: 'Always stays on your phone',
        items: ['Your questions and Hain’s answers', 'Your saved chats (encrypted)', 'What Hain remembers about you', 'Your documents (only their text, encrypted)', 'Your settings'],
      },
      online: {
        title: 'Goes online only when you ask',
        items: ['Weather, Wikipedia, web search, news', 'Downloading the brain and packs', 'Every request is listed, so you can see where it went'],
      },
      logTitle: 'Where requests go',
      logExample: 'Example',
      log: [
        { what: 'Weather tomorrow', where: 'open-meteo.com' },
        { what: 'Wikipedia: Eiffel Tower', where: 'wikipedia.org' },
        { what: 'Brain download', where: 'huggingface.co' },
      ],
      never: 'Never: no account, no analytics, no ads, no Google Play Services, no Firebase.',
    },
    compare: {
      kicker: 'Compared',
      title: 'Hain or a cloud AI?',
      sub: 'An honest comparison. Cloud AIs are smarter and know more. Hain is private and yours.',
      colHain: 'Hain',
      colCloud: 'Typical cloud AI',
      rows: [
        { label: 'Where the AI thinks', hain: 'On your phone', cloud: 'In a data centre', good: 'hain' },
        { label: 'Account needed', hain: 'No', cloud: 'Usually yes', good: 'hain' },
        { label: 'Who sees your chats', hain: 'Only you', cloud: 'The company can store them', good: 'hain' },
        { label: 'Ads and tracking', hain: 'None', cloud: 'Depends on the company', good: 'hain' },
        { label: 'Works without internet', hain: 'Yes, for chatting', cloud: 'No', good: 'hain' },
        { label: 'Price', hain: 'Free', cloud: 'Free with limits, or paid', good: 'hain' },
        { label: 'Knowledge and smarts', hain: 'Smaller, looks things up online', cloud: 'Much bigger', good: 'cloud' },
        { label: 'Speed', hain: 'Depends on your phone', cloud: 'Usually fast', good: 'cloud' },
      ],
      note: 'Hain uses a small open language model. It knows less than the big ones, so for facts it looks things up on the web when you allow it.',
    },
    roadmap: {
      kicker: 'Planned',
      title: 'What comes next.',
      sub: 'None of this is in the app yet. It is in Hain’s concept and plan, without dates.',
      groups: [
        {
          tag: 'Next: 0.3',
          note: 'Planned for the next version',
          items: [
            { icon: 'trees', title: 'Mini-apps in Pim’s forest', text: 'Small helpers without keys, for example local transport, that grow as trees in Pim’s forest.' },
            { icon: 'image', title: 'Edit photos', text: 'Erase objects, remove the background, make photos sharper – on your phone.' },
            { icon: 'mic', title: 'Voice mode', text: 'Talk with Pim hands-free, back and forth.' },
            { icon: 'package', title: 'F-Droid', text: 'Hain in the F-Droid app store, so updates come by themselves.' },
          ],
        },
        {
          tag: 'Later',
          note: 'In the concept, no version yet',
          items: [
            { icon: 'eye', title: 'Photos and audio with Omni', text: '“What is this?” or “Explain this maths problem”: the Omni brain can understand pictures and sound with an extra pack.' },
            { icon: 'voice', title: 'Pim’s own voice', text: 'Read aloud in Pim’s own voice instead of the phone’s voice.' },
            { icon: 'cards', title: 'Pictures in answers', text: 'Answer cards that can show pictures, too.' },
            { icon: 'sparkles', title: 'Create images (optional)', text: 'An optional pack for phones with 8 GB of memory or more. Nice for sketches and ideas, far weaker than the big cloud AIs.' },
            { icon: 'smartphone', title: 'Hain as your phone assistant', text: 'Start Hain like a phone assistant and ask about what is on your screen.' },
          ],
        },
      ],
      note: 'Plans can change.',
      link: 'Roadmap on GitHub',
      conceptLink: 'Concept (German)',
    },
    faq: {
      kicker: 'FAQ',
      title: 'Questions? Answers.',
      items: [
        { q: 'Why do I have to allow “unknown sources”?', a: 'Hain is not in the Play Store, so Android asks once if your browser may install it. That is a normal safety question. The step-by-step help above shows how. You can turn the switch off again afterwards.' },
        { q: 'Is Hain safe?', a: 'Hain is open source: anyone can read the code on GitHub. It has no ads, no trackers and no Google services, and your chats stay on your phone. It is still an early version, so it can crash, and things may change between versions.' },
        { q: 'Does Hain need the internet?', a: 'Only once to download the brain (about 1 GB). After that, chatting works offline. Hain only goes online when you ask for something from the web, like the weather or a search, and it shows you where the request goes.' },
        { q: 'Which phone do I need?', a: 'An Android phone with Android 10 or newer and at least 4 GB of RAM; 6 GB or more is recommended, 8 GB or more for the optional big brain. The newer your phone, the faster Hain answers. iPhones are not supported.' },
        { q: 'What does it cost?', a: 'Nothing. Hain is free and has no ads. The app is open source under the GPL-3.0 licence.' },
        { q: 'Which licences do the brains have?', a: 'The standard brain LFM3 1.7B and the optional LFM3-Omni 2B are under the LFM Open License v1.0, the optional big brain Jan-v3.5 4B under Apache-2.0. Wikipedia packs are CC BY-SA 4.0. Every licence text is in the app under Settings → About Hain → Open-source licences, and in the repository.' },
        { q: 'How do I update Hain?', a: 'Download the newest APK from the same download page and install it over the old one; your chats stay. If you like, Hain can check once a day whether a new version is out (off until you say yes).' },
        { q: 'Can the AI make mistakes?', a: 'Yes. Like every AI, Hain can be wrong, make things up or sound more sure than it is. Please check important answers, especially about health, money or law. Hain does not replace a doctor, a lawyer or therapy.' },
        { q: 'Is Hain made with AI?', a: 'Partly. Hain is made by one person, Oskar, together with AI coding agents. Every change is checked, tested and merged by a human.' },
      ],
    },
    join: {
      kicker: 'Get involved',
      title: 'Help Hain grow.',
      sub: 'You don’t need to code. Trying Hain and telling us what you think helps a lot.',
      items: [
        { title: 'Test and report', text: 'Install Hain, use it, and tell us what broke or what you wish for.', cta: 'Open an issue', href: LINKS.newIssue },
        { title: 'Translate', text: 'Hain speaks German and English. Help with wording or add a new language.', cta: 'How to translate', href: LINKS.contributing },
        { title: 'Code along', text: 'All code is on GitHub. Start with the contributing guide.', cta: 'View on GitHub', href: LINKS.repo },
      ],
    },
    ai: {
      title: 'About AI',
      text: 'Hain’s AI can be wrong. Check important answers. Hain is developed with the help of AI coding agents, and every change is reviewed by a human.',
    },
    footer: {
      tagline: 'Your AI. Your phone. Nobody else.',
      source: 'Source code',
      license: 'App licence: GPL-3.0',
      models: 'Brains: LFM Open License v1.0 (LFM3 1.7B, LFM3-Omni 2B), Apache-2.0 (Jan-v3.5 4B)',
      imprint: 'Imprint',
      imprintNote: 'Imprint coming soon',
      noCookies: 'This website uses no cookies and no tracking.',
      issues: 'Report a problem',
      concept: 'Concept (German)',
      trademarks: 'Trademarks',
      support: 'Support Hain',
      soon: 'coming soon',
    },
  },

  de: {
    meta: { switchTo: 'English', switchLabel: 'This page in English' },
    nav: { why: 'Warum Hain', features: 'Funktionen', roadmap: 'Geplant', how: 'Installieren', privacy: 'Datenschutz', faq: 'Fragen', download: 'Laden', menu: 'Menü öffnen', close: 'Menü schließen', mainLabel: 'Hauptnavigation', mobileLabel: 'Menü' },
    hero: {
      badge: 'Version 0.2 · frühe Version',
      title: ['Deine KI.', 'Dein Handy.', 'Sonst niemand.'],
      sub: 'Hain ist ein kostenloser KI-Assistent für Android, der auf deinem Handy läuft. Kein Konto, keine Cloud, keine Werbung, kein Tracking.',
      cta: 'APK laden',
      ctaHelp: 'So installierst du Hain',
      note: 'Kostenlos · Open Source · ab Android 10',
      installHint: 'Hain ist nicht im Play Store. Du lädst die APK von GitHub und erlaubst deinem Browser einmal, Apps zu installieren („unbekannte Quellen“).',
      pimAlt: 'Startseite von Hain: Pim sagt Hallo und schlägt ein paar Fragen vor',
      iconAlt: 'App-Symbol von Hain: Pim, eine glänzende grüne Glasfigur, in einem weichen Wald',
    },
    marquee: ['Kein Konto', 'Keine Cloud', 'Keine Werbung', 'Kein Tracking', 'Keine Google-Dienste', 'Läuft offline', 'Open Source', 'Kostenlos', 'Deutsch & Englisch'],
    why: {
      kicker: 'Warum Hain',
      title: 'Eine hilfreiche KI, die bei dir bleibt.',
      sub: 'Die meisten KI-Apps schicken alles, was du tippst, an große Rechenzentren. Hain denkt direkt auf deinem Handy.',
      items: [
        { title: 'Privat', text: 'Deine Fragen und Chats bleiben auf deinem Handy. Niemand liest mit, und nichts wird zum Trainieren einer KI benutzt.' },
        { title: 'Läuft offline', text: 'Chatten klappt ohne Internet, sogar im Flugzeug. Ins Netz geht Hain nur, wenn du etwas aus dem Internet willst.' },
        { title: 'Kostenlos', text: 'Keine Werbung, keine versteckten Kosten. Hain ist Open Source und gehört allen.' },
        { title: 'Grün', text: 'Für deine Fragen muss kein Rechenzentrum laufen. Hain nutzt das Handy, das du schon hast.' },
      ],
    },
    status: { new: 'Neu in 0.2', since01: 'Seit 0.1', planned: 'Geplant' },
    features: {
      kicker: 'Funktionen',
      title: 'Das kann Hain 0.2.',
      sub: 'Jede Funktion einmal, mit einem echten Bildschirmfoto der App. Die Beispiele auf den Bildern sind ausgedacht.',
      legend: {
        since01: 'war schon in den 0.1-Testversionen',
        new: 'kam auf dem Weg zu 0.2 dazu',
        planned: 'noch nicht in der App, siehe unten',
      },
      items: [
        {
          id: 'welcome', icon: 'sprout', status: 'since01', shot: 'onboarding',
          title: 'Hallo, ich bin Pim',
          text: 'Eine kurze Willkommenstour mit Pim, dann lädt Hain einmal sein Gehirn (etwa 1 GB, am besten im WLAN). Der Download kann warten, macht nach einer Pause weiter und läuft im Hintergrund. Danach klappt das Chatten ohne Internet.',
          shotAlt: 'Willkommensseite mit Pim',
        },
        {
          id: 'chat', icon: 'chat', status: 'since01', shot: 'chat_long_answer',
          title: 'Chat, der auf dem Handy bleibt',
          text: 'Die Antwort entsteht live, mit Listen, Tabellen und Code. Sofort stoppen, kopieren, teilen, bewerten oder nochmal fragen. Die KI läuft auf deinem Handy, sogar im Flugmodus. Kleine Modelle wissen weniger als die großen Cloud-KIs, prüf wichtige Antworten also nach.',
          shotAlt: 'Eine lange Antwort mit Tabelle und Code',
        },
        {
          id: 'thinking', icon: 'brain', status: 'since01', shot: 'chat_composer_picker',
          title: 'Schnell, Auto oder Gründlich',
          text: 'Im Eingabefeld wählst du, wie viel Mühe sich Pim gibt. Schnell antwortet sofort, Gründlich braucht länger und sucht bei schweren Fragen mehr, Auto entscheidet für dich.',
          shotAlt: 'Die Denk-Stufe im Eingabefeld',
        },
        {
          id: 'brain', icon: 'cpu', status: 'new', shot: 'packs_brains',
          title: 'Gehirn wählen',
          text: 'Standard ist LFM3 1.7B (etwa 1 GB), das bei schwierigen Fragen laut nachdenkt. LFM3-Omni 2B (auch etwa 1 GB) gibt es freiwillig: Es denkt nicht laut nach und ruft seltener unnötige Werkzeuge auf. Handys ab 8 GB Arbeitsspeicher können dazu das freiwillige große Gehirn Jan-v3.5 4B laden (2,7 GB): Es wählt Werkzeuge zuverlässiger, antwortet aber deutlich langsamer. Jedes Gehirn sagt ehrlich, was es gut kann; wechseln kannst du jederzeit mit einem Tipp auf „Hain“ oben im Chat.',
          shotAlt: 'Modelle & Pakete, Gehirne: LFM3 1.7B, LFM3-Omni 2B und Jan-v3.5 4B, jeweils mit kurzer Einschätzung',
        },
        {
          id: 'tools', icon: 'search', status: 'since01', shot: 'tools',
          title: 'Pim schaut nach',
          text: 'Wetter von Open-Meteo, Wikipedia auf Deutsch und Englisch und eine Websuche ohne Konto und ohne API-Schlüssel. Jeder Schritt steht unter der Antwort, eingeklappt zu einer ruhigen Zeile („3 Werkzeuge genutzt“), die ein Tipp öffnet, mit den Quellen. Neu in 0.2: Wetter- und Wikipedia-Karten, Website-Symbole an den Quellen, und Werkzeuge kommen nur, wenn deine Frage danach fragt.',
          shotAlt: 'Antwort mit Wetterkarte, Schritten und Quellen',
        },
        {
          id: 'research', icon: 'book', status: 'since01', shot: 'research',
          title: 'Recherche mit jedem Schritt',
          text: 'Bei schwereren Fragen sucht Pim im Netz, liest mehrere Seiten gleichzeitig und schreibt daraus. Du siehst, was gesucht und welche Seiten gelesen wurden, und kannst jederzeit stoppen. Rechnen und Einheiten werden genau gerechnet, nicht geraten.',
          shotAlt: 'Pim hat im Web gesucht und drei Seiten gelesen, die Antwort entsteht',
        },
        {
          id: 'actions', icon: 'alarm', status: 'since01', shot: 'actions',
          title: 'Handy-Aktionen, immer mit deinem OK',
          text: 'Wecker (auch jeden Werktag), Timer, Kalendereintrag und Route. Pim bereitet nur eine Karte vor; Uhr, Kalender oder Karten-App öffnen sich erst nach deinem Tipp. Neu in 0.2: wiederkehrende Wecker und, wenn du willst, 3 Sekunden Countdown mit „Rückgängig“ statt des Tipps.',
          shotAlt: 'Karten für einen Werktags-Wecker und einen Timer, die auf einen Tipp warten',
        },
        {
          id: 'cards', icon: 'cards', status: 'new', shot: 'cards',
          title: 'Karten in der Antwort',
          text: 'Für Pläne, Listen und Vergleiche antwortet Pim mit einer ruhigen Karte: Tabelle, Checkliste zum Abhaken, Fortschrittsbalken, Balken- oder Liniendiagramm und Knöpfe. Die Haken bleiben beim Chat.',
          shotAlt: 'Ein Lernplan als Karte mit Tabelle, Checkliste und Fortschrittsbalken',
        },
        {
          id: 'memory', icon: 'memory', status: 'new', shot: 'memory',
          title: 'Pim merkt sich was – wenn du willst',
          text: '„Merk dir, dass ich vegetarisch esse.“ Pim behält, was du über dich sagst, auf deinem Handy und nutzt es, wenn es passt. Unter der Antwort siehst du, welche Erinnerungen genutzt wurden, und „Merken: …“ bietet Neues mit einem Tipp an. Alles sehen, ändern und löschen unter Einstellungen → Gedächtnis.',
          shotAlt: 'Antwort mit den Chips „Merken: lernt Spanisch“ und „2 Erinnerungen genutzt“',
        },
        {
          id: 'documents', icon: 'file', status: 'new', shot: 'documents',
          title: 'Frag deine PDFs und Fotos',
          text: 'Häng mit der Büroklammer ein PDF, ein Foto einer Seite oder eine Textdatei an, oder teil sie aus einer anderen App. Hain liest den Text auf deinem Handy (Texterkennung für Scans als freiwilliges Paket), und Pim antwortet mit Seitenangabe.',
          shotAlt: 'Eine Anleitung als PDF angehängt, Pim antwortet mit Seitenzahlen',
        },
        {
          id: 'news', icon: 'news', status: 'new', shot: 'news_briefing',
          title: 'Deine Nachrichten, in Ruhe',
          text: 'Ein Nachrichten-Reader mit Bildern: 19 Quellen auf Deutsch und Englisch, auf Wunsch mit deinen eigenen Feeds. Dein Briefing des Tages wird auf dem Handy ausgewählt und zusammengefasst, und du siehst jeden Schritt, während es entsteht.',
          shotAlt: 'Nachrichten mit der Briefing-Karte, die Feeds 7 von 19 lädt',
        },
        {
          id: 'reader', icon: 'reader', status: 'new', shot: 'news_reader',
          title: 'In Hain lesen und Pim fragen',
          text: 'Artikel öffnen sich in einer ruhigen Leseansicht mit einstellbarer Schrift. Lass Pim einen Artikel zusammenfassen oder erklären; er antwortet nur aus diesem Artikel, auf deinem Handy. Bezahlschranken bleiben Bezahlschranken.',
          shotAlt: 'Ein Artikel in der Leseansicht',
        },
        {
          id: 'branches', icon: 'branch', status: 'new', shot: 'branches',
          title: 'Nichts geht verloren',
          text: 'Bearbeite eine Nachricht oder frag nochmal, die alte Version bleibt: wechseln mit ‹ 2/3 ›. Deine nächste Frage kannst du schon schicken, während Pim noch antwortet; sie wartet, bis sie dran ist.',
          shotAlt: 'Versionen 2/2 und 3/3 und eine wartende Frage',
        },
        {
          id: 'chats', icon: 'lock', status: 'since01', shot: 'drawer',
          title: 'Deine Chats bleiben – verschlüsselt',
          text: 'Chats liegen verschlüsselt nur auf deinem Handy. Suchen, umbenennen, anheften und löschen, mit automatischem Titel. Neu in 0.2: einen Chat als Markdown oder PDF exportieren, oder alle auf einmal als ZIP.',
          shotAlt: 'Die Chat-Liste in der Seitenleiste',
        },
        {
          id: 'privacy', icon: 'shield', status: 'new', shot: 'settings_internet',
          title: 'Privater Chat, App-Sperre, Netzwerk-Log',
          text: 'Ein privater Chat wird nie gespeichert und ist beim Verlassen weg. Hain mit Fingerabdruck oder PIN sperren und Bildschirmfotos verhindern. Jede Anfrage ins Internet wird aufgelistet, ein Schalter auf der Seite Internet macht alles aus. Das Netzwerk-Log gibt es seit 0.1, der Rest ist neu.',
          shotAlt: 'Die Internet-Seite: ein Hauptschalter und was ins Internet darf',
        },
        {
          id: 'packs', icon: 'package', status: 'new', shot: 'packs_knowledge',
          title: 'Wikipedia ohne Internet',
          text: 'Unter Modelle & Pakete liegt alles, was du laden kannst: Wikipedia fürs Handy (Deutsch ab etwa 140 MB), Gehirne, Texterkennung, Suche nach Bedeutung und Sprache. Jedes Paket sagt, was es bringt und ob es zu deinem Handy passt.',
          shotAlt: 'Wikipedia-Pakete unter Modelle & Pakete',
        },
        {
          id: 'voice', icon: 'mic', status: 'new', shot: 'packs_voice',
          title: 'Sprechen statt tippen',
          text: 'Mikro tippen oder halten und sprechen; während Pim zuhört, wird das Nachrichtenfeld zu einer sanft grünen Leiste, deren Balken sich mit deiner Stimme bewegen. Das Diktat läuft komplett auf deinem Handy (Deutsch und Englisch, ein kleines Paket pro Sprache). Pim kann Antworten vorlesen, vorerst mit der Stimme deines Handys.',
          shotAlt: 'Diktat-Pakete für Deutsch und Englisch und der Hinweis zum Vorlesen',
        },
        {
          id: 'look', icon: 'sliders', status: 'since01', shot: 'settings_look',
          title: 'Dein Aussehen',
          text: 'Hell oder dunkel, gut lesbare Schrift, App-Symbol, Vibration und weniger Bewegung. Antwortstil und ein paar Worte über dich stehen auch in den Einstellungen.',
          shotAlt: 'Einstellungsseite Aussehen',
        },
        {
          id: 'tablet', icon: 'tablet', status: 'new', shot: 'tablet', wide: true,
          title: 'Tablets und große Bildschirme',
          text: 'Auf dem Tablet bleibt die Chat-Liste links offen. Einstellungen, Modelle & Pakete, Gedächtnis und Nachrichten nutzen den Platz.',
          shotAlt: 'Hain auf dem Tablet: links die Chat-Liste, rechts der Chat',
        },
      ],
      moreTitle: 'Außerdem in 0.2',
      moreSub: 'Kleinere Dinge ohne eigenes Bild.',
      more: [
        { icon: 'share', status: 'new', title: 'Teilen, Widget & Schnellkachel', text: 'Teil Text, Links, PDFs oder Fotos aus jeder App in einen neuen Chat. Dazu ein Widget „Frag Pim“ und eine Schnellkachel „Neuer Chat“.' },
        { icon: 'languages', status: 'since01', title: 'Spricht deine Sprache', text: 'Pim antwortet in der Sprache, in der du schreibst, oder immer auf Deutsch oder Englisch.' },
        { icon: 'update', status: 'new', title: 'Update-Prüfung (freiwillig)', text: 'Aus, bis du Ja sagst. Dann fragt Hain einmal am Tag bei GitHub nach einer neuen Version und zeigt einen ruhigen Hinweis. Nichts installiert sich von selbst.' },
        { icon: 'gauge', status: 'new', title: 'Schnellerer Start', text: 'Pims feste Anweisungen werden nach dem ersten Lesen behalten, dadurch kommt die erste Antwort nach dem Öffnen früher.' },
        { icon: 'file', status: 'new', title: 'Schön gesetzte Antworten', text: 'Überschriften, Listen, Aufgabenlisten, Zitate, Tabellen zum Seitwärtswischen, Code-Blöcke mit Sprache und Kopieren-Knopf und Formeln als lesbarer Text statt rohem LaTeX.' },
        { icon: 'package', status: 'new', title: 'Downloads laufen weiter', text: 'Pakete laden weiter, wenn du Hain schließt, mit Fortschritt in der Benachrichtigung. Anhalten und später fortsetzen; Hain macht dort weiter, wo es aufgehört hat.' },
        { icon: 'code', status: 'new', title: 'Entwicklermodus', text: 'Versteckt für Bastler: 7× auf die Version tippen. Eigene GGUF-Modelle ausprobieren, Sampling einstellen, Token pro Sekunde sehen.' },
        { icon: 'file', status: 'new', title: 'Open-Source-Lizenzen', text: 'Unter Einstellungen → Über Hain stehen alle Bibliotheken, KI-Modelle, Schriften und Icons, die Hain nutzt, mit Version und Lizenz. Ein Tipp zeigt den vollen Lizenztext – direkt in der App, auch offline.' },
        { icon: 'sprout', status: 'since01', title: 'Ein Geheimnis', text: 'Halte Pim auf der Startseite 5 Sekunden lang … (Tipp: 2048).' },
      ],
    },
    how: {
      kicker: 'Installieren',
      title: 'In drei Schritten bereit.',
      sub: 'Hain ist nicht im Play Store. Du installierst die App direkt von GitHub, das dauert etwa zwei Minuten.',
      steps: [
        { title: 'APK laden', text: 'Öffne die Download-Seite auf deinem Handy. Tippe unter „Assets“ auf die Datei, die auf „.apk“ endet.' },
        { title: 'Installieren', text: 'Öffne die Datei. Android fragt, ob dein Browser Apps installieren darf. Erlaub es einmal und tippe auf „Installieren“.' },
        { title: 'Gehirn laden', text: 'Beim ersten Start lädt Hain sein Gehirn (etwa 1 GB). Am besten im WLAN. Danach klappt das Chatten offline.' },
      ],
      helpTitle: 'Hilfe: „Unbekannte Apps installieren“ Schritt für Schritt',
      helpIntro: 'Android schützt dich vor Apps, die nicht aus dem Play Store kommen. Das ist gut so. Damit du Hain installieren kannst, erlaubst du es einmal für deinen Browser:',
      helpSteps: [
        'Tippe auf die geladene APK-Datei (im Browser oder unter „Downloads“).',
        'Android zeigt etwa: „Aus Sicherheitsgründen darf dein Smartphone keine unbekannten Apps aus dieser Quelle installieren.“ Tippe auf „Einstellungen“.',
        'Schalte „Dieser Quelle vertrauen“ bzw. „Von dieser Quelle zulassen“ ein.',
        'Geh zurück und tippe auf „Installieren“. Fertig!',
        'Wenn du magst, schaltest du den Schalter danach wieder aus. Hain läuft trotzdem weiter.',
      ],
      helpNote: 'Die Wörter sind je nach Handy-Marke etwas anders. Falls Google Play Protect warnt, tippe auf „Weitere Details“ und „Trotzdem installieren“. Hain ist Open Source, jede und jeder kann den Code prüfen. Du hast schon eine Testversion? Installier die neue APK einfach darüber; deine Chats bleiben.',
      req: 'Du brauchst Android 10 oder neuer und mindestens 4 GB Arbeitsspeicher (RAM); empfohlen sind 6 GB oder mehr.',
    },
    privacy: {
      kicker: 'Datenschutz',
      title: 'Datenschutz, einfach erklärt.',
      sub: 'Kein Kleingedrucktes. So geht Hain mit deinen Daten um.',
      stays: {
        title: 'Bleibt immer auf deinem Handy',
        items: ['Deine Fragen und Hains Antworten', 'Deine gespeicherten Chats (verschlüsselt)', 'Was Hain sich über dich merkt', 'Deine Dokumente (nur der Text, verschlüsselt)', 'Deine Einstellungen'],
      },
      online: {
        title: 'Geht nur online, wenn du es willst',
        items: ['Wetter, Wikipedia, Websuche, Nachrichten', 'Gehirn und Pakete herunterladen', 'Jede Anfrage wird aufgelistet, du siehst, wohin sie ging'],
      },
      logTitle: 'Wohin Anfragen gehen',
      logExample: 'Beispiel',
      log: [
        { what: 'Wetter morgen', where: 'open-meteo.com' },
        { what: 'Wikipedia: Eiffelturm', where: 'wikipedia.org' },
        { what: 'Gehirn laden', where: 'huggingface.co' },
      ],
      never: 'Niemals: kein Konto, keine Analyse, keine Werbung, keine Google Play-Dienste, kein Firebase.',
    },
    compare: {
      kicker: 'Vergleich',
      title: 'Hain oder Cloud-KI?',
      sub: 'Ein ehrlicher Vergleich. Cloud-KIs sind schlauer und wissen mehr. Hain ist privat und gehört dir.',
      colHain: 'Hain',
      colCloud: 'Typische Cloud-KI',
      rows: [
        { label: 'Wo die KI denkt', hain: 'Auf deinem Handy', cloud: 'Im Rechenzentrum', good: 'hain' },
        { label: 'Konto nötig', hain: 'Nein', cloud: 'Meistens ja', good: 'hain' },
        { label: 'Wer deine Chats sieht', hain: 'Nur du', cloud: 'Die Firma kann sie speichern', good: 'hain' },
        { label: 'Werbung und Tracking', hain: 'Keine', cloud: 'Je nach Firma', good: 'hain' },
        { label: 'Ohne Internet', hain: 'Ja, zum Chatten', cloud: 'Nein', good: 'hain' },
        { label: 'Preis', hain: 'Kostenlos', cloud: 'Kostenlos mit Grenzen oder kostenpflichtig', good: 'hain' },
        { label: 'Wissen und Können', hain: 'Kleiner, schlägt online nach', cloud: 'Viel größer', good: 'cloud' },
        { label: 'Tempo', hain: 'Hängt von deinem Handy ab', cloud: 'Meist schnell', good: 'cloud' },
      ],
      note: 'Hain nutzt ein kleines offenes Sprachmodell. Es weiß weniger als die großen, deshalb schlägt es Fakten im Internet nach, wenn du das erlaubst.',
    },
    roadmap: {
      kicker: 'Geplant',
      title: 'Was noch kommt.',
      sub: 'Nichts davon ist schon in der App. Es steht in Hains Konzept und Plan, ohne Termine.',
      groups: [
        {
          tag: 'Als Nächstes: 0.3',
          note: 'Für die nächste Version geplant',
          items: [
            { icon: 'trees', title: 'Mini-Apps in Pims Wald', text: 'Kleine Helfer ohne Schlüssel, etwa Bus und Bahn, die als Bäume in Pims Wald wachsen.' },
            { icon: 'image', title: 'Fotos bearbeiten', text: 'Objekte wegradieren, Hintergrund entfernen, Fotos schärfer machen – auf dem Handy.' },
            { icon: 'mic', title: 'Sprachmodus', text: 'Freihändig mit Pim reden, hin und her.' },
            { icon: 'package', title: 'F-Droid', text: 'Hain im App-Store F-Droid, damit Updates von selbst kommen.' },
          ],
        },
        {
          tag: 'Später',
          note: 'Im Konzept, noch ohne Version',
          items: [
            { icon: 'eye', title: 'Bilder und Ton mit Omni', text: '„Was ist das?“ oder „Erklär mir diese Matheaufgabe“: Das Omni-Gehirn kann mit einem Zusatzpaket Bilder und Ton verstehen.' },
            { icon: 'voice', title: 'Pims eigene Stimme', text: 'Vorlesen mit Pims eigener Stimme statt der Handy-Stimme.' },
            { icon: 'cards', title: 'Bilder in Antworten', text: 'Antwort-Karten, die auch Bilder zeigen können.' },
            { icon: 'sparkles', title: 'Bilder erzeugen (freiwillig)', text: 'Ein freiwilliges Paket für Handys ab 8 GB Arbeitsspeicher. Nett für Skizzen und Ideen, deutlich schwächer als die großen Cloud-KIs.' },
            { icon: 'smartphone', title: 'Hain als Handy-Assistent', text: 'Hain wie einen Handy-Assistenten starten und zu dem fragen, was gerade auf dem Bildschirm ist.' },
          ],
        },
      ],
      note: 'Pläne können sich ändern.',
      link: 'Fahrplan auf GitHub',
      conceptLink: 'Konzept',
    },
    faq: {
      kicker: 'Fragen',
      title: 'Fragen? Antworten.',
      items: [
        { q: 'Warum muss ich „unbekannte Quellen“ erlauben?', a: 'Hain ist nicht im Play Store. Deshalb fragt Android einmal, ob dein Browser die App installieren darf. Das ist eine normale Sicherheitsfrage. Die Schritt-für-Schritt-Hilfe oben zeigt, wie es geht. Danach kannst du den Schalter wieder ausmachen.' },
        { q: 'Ist Hain sicher?', a: 'Hain ist Open Source: Jede und jeder kann den Code auf GitHub lesen. Es gibt keine Werbung, keine Tracker und keine Google-Dienste, und deine Chats bleiben auf deinem Handy. Es ist aber noch eine frühe Version: Sie kann abstürzen, und zwischen Versionen kann sich etwas ändern.' },
        { q: 'Braucht Hain Internet?', a: 'Nur einmal, um das Gehirn zu laden (etwa 1 GB). Danach klappt das Chatten offline. Ins Internet geht Hain nur, wenn du etwas aus dem Netz willst, zum Beispiel das Wetter oder eine Suche, und zeigt dir, wohin die Anfrage geht.' },
        { q: 'Welches Handy brauche ich?', a: 'Ein Android-Handy mit Android 10 oder neuer und mindestens 4 GB Arbeitsspeicher; empfohlen sind 6 GB oder mehr, für das freiwillige große Gehirn 8 GB oder mehr. Je neuer das Handy, desto schneller antwortet Hain. iPhones werden nicht unterstützt.' },
        { q: 'Was kostet Hain?', a: 'Nichts. Hain ist kostenlos und ohne Werbung. Die App ist Open Source unter der Lizenz GPL-3.0.' },
        { q: 'Welche Lizenzen haben die Gehirne?', a: 'Das Standard-Gehirn LFM3 1.7B und das freiwillige LFM3-Omni 2B stehen unter der LFM Open License v1.0, das freiwillige große Gehirn Jan-v3.5 4B unter Apache-2.0. Die Wikipedia-Pakete sind CC BY-SA 4.0. Alle Lizenztexte stehen in der App unter Einstellungen → Über Hain → Open-Source-Lizenzen und im Repository.' },
        { q: 'Wie bekomme ich Updates?', a: 'Lade die neueste APK von derselben Download-Seite und installiere sie über die alte; deine Chats bleiben. Wenn du willst, prüft Hain einmal am Tag, ob es eine neue Version gibt (aus, bis du Ja sagst).' },
        { q: 'Kann sich die KI irren?', a: 'Ja. Wie jede KI kann Hain sich irren, etwas erfinden oder sicherer klingen, als es ist. Prüf wichtige Antworten lieber nach, besonders bei Gesundheit, Geld und Recht. Hain ersetzt keine Ärztin, keinen Anwalt und keine Therapie.' },
        { q: 'Wird Hain mit KI gemacht?', a: 'Teilweise. Hain wird von einer Person, Oskar, zusammen mit KI-Agenten entwickelt. Jede Änderung wird von einem Menschen geprüft, getestet und übernommen.' },
      ],
    },
    join: {
      kicker: 'Mitmachen',
      title: 'Hilf Hain beim Wachsen.',
      sub: 'Programmieren musst du nicht. Hain ausprobieren und sagen, was du denkst, hilft schon sehr.',
      items: [
        { title: 'Testen und melden', text: 'Installier Hain, benutz es und erzähl, was kaputt war oder was du dir wünschst.', cta: 'Issue aufmachen', href: LINKS.newIssue },
        { title: 'Übersetzen', text: 'Hain spricht Deutsch und Englisch. Hilf bei den Texten oder bring eine neue Sprache mit.', cta: 'So geht Übersetzen', href: LINKS.contributing },
        { title: 'Mitprogrammieren', text: 'Der ganze Code liegt auf GitHub. Fang mit der Anleitung zum Mitmachen an.', cta: 'Auf GitHub ansehen', href: LINKS.repo },
      ],
    },
    ai: {
      title: 'Zur KI',
      text: 'Die KI in Hain kann sich irren. Prüf wichtige Antworten nach. Hain wird mit Hilfe von KI-Agenten entwickelt, jede Änderung prüft ein Mensch.',
    },
    footer: {
      tagline: 'Deine KI. Dein Handy. Sonst niemand.',
      source: 'Quellcode',
      license: 'App-Lizenz: GPL-3.0',
      models: 'Gehirne: LFM Open License v1.0 (LFM3 1.7B, LFM3-Omni 2B), Apache-2.0 (Jan-v3.5 4B)',
      imprint: 'Impressum',
      imprintNote: 'Impressum folgt',
      noCookies: 'Diese Website nutzt keine Cookies und kein Tracking.',
      issues: 'Problem melden',
      concept: 'Konzept',
      trademarks: 'Marke',
      support: 'Hain unterstützen',
      soon: 'kommt bald',
    },
  },
}

// Screenshots (T-269): from the Roborazzi tests (screens.yml), converted to WebP, 720 px wide (tablet
// 1280). Each id has <id>-light.webp, <id>-dark.webp and the English <id>-light-en.webp,
// <id>-dark-en.webp in public/screenshots/. The page shows the one matching the visitor's colour scheme.
// Made with website/scripts/web_shots.sh. Pixel size of each screenshot (all four files of an id have the
// same size); phone ids not listed are 720 × 1561, wide ones (`wide: true`) 1280 × 800.
export const SHOT_SIZE: Record<string, [number, number]> = {}
