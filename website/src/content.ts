// All visible text of the website, English (default) and German.
// Keep both languages in sync. Plain language, German uses "du".

export type Lang = 'en' | 'de'

export const LINKS = {
  download: 'https://github.com/oskar26/hain/releases/tag/nightly',
  repo: 'https://github.com/oskar26/hain',
  issues: 'https://github.com/oskar26/hain/issues',
  newIssue: 'https://github.com/oskar26/hain/issues/new',
  contributing: 'https://github.com/oskar26/hain/blob/main/CONTRIBUTING.md',
  license: 'https://github.com/oskar26/hain/blob/main/LICENSE',
  concept: 'https://github.com/oskar26/hain/blob/main/docs/concept/KONZEPT.md',
  roadmap: 'https://github.com/oskar26/hain/blob/main/docs/workflow/ROADMAP.md',
  trademarks: 'https://github.com/oskar26/hain/blob/main/TRADEMARKS.md',
}

export type Status = 'ready' | 'soon'

type Copy = {
  meta: { switchTo: string; switchLabel: string }
  nav: { why: string; features: string; how: string; privacy: string; faq: string; download: string; menu: string; close: string; mainLabel: string; mobileLabel: string }
  hero: { badge: string; title: [string, string, string]; sub: string; cta: string; ctaHelp: string; note: string; pimAlt: string; iconAlt: string }
  marquee: string[]
  why: { kicker: string; title: string; sub: string; items: { title: string; text: string }[] }
  status: Record<Status, string>
  features: { kicker: string; title: string; sub: string; readyTitle: string; soonTitle: string; items: { icon: string; title: string; text: string; status: Status }[] }
  how: {
    kicker: string; title: string; sub: string
    steps: { title: string; text: string }[]
    helpTitle: string; helpIntro: string; helpSteps: string[]; helpNote: string
    req: string
  }
  shots: { kicker: string; title: string; sub: string; placeholder: string; items: string[] }
  privacy: {
    kicker: string; title: string; sub: string
    stays: { title: string; items: string[] }
    online: { title: string; items: string[] }
    logTitle: string; logExample: string
    log: { what: string; where: string }[]
    never: string
  }
  compare: { kicker: string; title: string; sub: string; colHain: string; colCloud: string; rows: { label: string; hain: string; cloud: string; good?: 'hain' | 'cloud' | 'both' }[]; note: string }
  roadmap: { kicker: string; title: string; sub: string; items: { tag: string; when: string; text: string; now?: boolean }[]; note: string; link: string }
  faq: { kicker: string; title: string; items: { q: string; a: string }[] }
  join: { kicker: string; title: string; sub: string; items: { title: string; text: string; cta: string; href: string }[] }
  ai: { title: string; text: string }
  footer: { tagline: string; source: string; license: string; imprint: string; imprintNote: string; noCookies: string; issues: string; concept: string; trademarks: string; support: string; soon: string }
}

export const COPY: Record<Lang, Copy> = {
  en: {
    meta: { switchTo: 'Deutsch', switchLabel: 'Diese Seite auf Deutsch' },
    nav: { why: 'Why Hain', features: 'Features', how: 'Install', privacy: 'Privacy', faq: 'FAQ', download: 'Download', menu: 'Open menu', close: 'Close menu', mainLabel: 'Main', mobileLabel: 'Menu' },
    hero: {
      badge: 'Alpha · in development',
      title: ['Your AI.', 'Your phone.', 'Nobody else.'],
      sub: 'Hain is a free ChatGPT alternative for Android that runs completely on your phone. No account, no cloud, no ads, no tracking.',
      cta: 'Download for Android',
      ctaHelp: 'How to install',
      note: 'Free · Open source · Android 10 or newer',
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
        { title: 'Free', text: 'No subscription, no ads, no hidden costs. Hain is open source and belongs to everyone.' },
        { title: 'Green', text: 'No data centre has to run for your questions. Hain uses the phone you already own.' },
      ],
    },
    status: { ready: 'Available now', soon: 'Coming soon' },
    features: {
      kicker: 'Features',
      title: 'Everything you need. Nothing you don’t.',
      sub: 'Hain is still growing. Green means it is in the test version today, grey means it is coming soon.',
      readyTitle: 'Available now',
      soonTitle: 'Coming soon',
      items: [
        { icon: 'chat', title: 'Chat on your phone', text: 'Answers appear as they are written. Stop at once, copy, share, thumbs up or down, ask again. The AI runs on your phone, even in flight mode.', status: 'ready' },
        { icon: 'lock', title: 'Encrypted chats', text: 'Chats are stored encrypted, only on your phone. Search, rename, pin, delete, with automatic titles.', status: 'ready' },
        { icon: 'brain', title: 'Thinking level', text: 'Auto, Fast or Thorough, right in the message box. Fast answers at once, Thorough thinks longer for hard questions.', status: 'ready' },
        { icon: 'search', title: 'Web search without a key', text: 'Pim searches the web without an account or API key, reads the best pages at once and shows its sources. Or use your own SearXNG.', status: 'ready' },
        { icon: 'book', title: 'Wikipedia & weather', text: 'Wikipedia summaries in German and English and the weather from Open-Meteo, as a small card under the answer. Weather without a place uses your location, if you allow it.', status: 'ready' },
        { icon: 'calc', title: 'Exact maths', text: 'A real calculator and unit converter instead of guessing: “15% of 80”, “5 km in miles”.', status: 'ready' },
        { icon: 'link', title: 'Read a page', text: 'Send Pim a link and it reads the page and sums it up for you.', status: 'ready' },
        { icon: 'alarm', title: 'Phone actions with your OK', text: 'Alarm (also every weekday), timer, calendar entry, directions. Pim prepares them, nothing happens until you tap. Or let them start after 3 seconds, with Undo.', status: 'ready' },
        { icon: 'shield', title: 'Network log & offline mode', text: 'Every request goes through one gate and is listed, the brain download too. Web search on, ask first or off. One switch turns all of it off.', status: 'ready' },
        { icon: 'sliders', title: 'Settings your way', text: 'Light or dark, an easy-to-read font, answer style, a few words about you, reduce motion.', status: 'ready' },
        { icon: 'languages', title: 'Speaks your language', text: 'Pim answers in the language you write in, or always in German or English.', status: 'ready' },
        { icon: 'package', title: 'Knowledge packs', text: 'See, download and remove packs, with hints on what fits your phone.', status: 'ready' },
        { icon: 'update', title: 'Update check (optional)', text: 'If you want, Hain asks GitHub once a day whether a newer version is out and shows a calm hint. Off until you say yes, nothing about you is sent, nothing installs by itself.', status: 'ready' },
        { icon: 'code', title: 'Developer mode', text: 'Hidden for tinkerers: tap the version 7 times. Try your own GGUF model from Hugging Face, tune sampling and thinking, see tokens per second.', status: 'ready' },
        { icon: 'news', title: 'News', text: 'Headlines from a fixed mix of 14 sources in German and English, plus your own feeds if you like. Pim sums up an article in 5 points, on your phone.', status: 'ready' },
        { icon: 'memory', title: 'Memory', text: '“Remember that I’m vegetarian.” Pim keeps it on your phone and uses it when it fits. See, change and delete everything in Settings → Memory.', status: 'ready' },
        { icon: 'private', title: 'Private chat & app lock', text: 'A chat that is never saved, and a fingerprint lock for the app.', status: 'soon' },
        { icon: 'cards', title: 'Answer cards', text: 'Lists, tables, checklists, progress bars and buttons right in the answer. Tick items off, copy, share or ask the next question with one tap.', status: 'ready' },
        { icon: 'file', title: 'Documents', text: 'Ask your PDFs and photos of text, with sources.', status: 'soon' },
        { icon: 'mic', title: 'Voice', text: 'Dictate your message and let Pim read answers aloud.', status: 'soon' },
        { icon: 'share', title: 'Export & share', text: 'Save a chat as Markdown or PDF, share text into Hain, a widget and a quick tile.', status: 'soon' },
        { icon: 'image', title: 'Images', text: 'Erase objects, remove backgrounds, sharpen photos, all on your phone.', status: 'soon' },
        { icon: 'trees', title: 'Pim’s forest', text: 'Mini-apps without keys, like local transport, that grow as trees in Pim’s forest.', status: 'soon' },
      ],
    },
    how: {
      kicker: 'Install',
      title: 'Ready in three steps.',
      sub: 'Hain is not in the Play Store yet. You install it directly, which takes about two minutes.',
      steps: [
        { title: 'Download the app', text: 'Open the download page on your phone and tap “hain-debug.apk”.' },
        { title: 'Install it', text: 'Open the file. Android asks if your browser may install apps. Allow it once, then tap “Install”.' },
        { title: 'Get the brain', text: 'On first start Hain downloads its brain (about 1 GB). Wi-Fi is best. After that, chatting works offline.' },
      ],
      helpTitle: 'Help: “Install unknown apps”, step by step',
      helpIntro: 'Android protects you from apps that don’t come from the Play Store. That is good. To install Hain, you allow it once for your browser:',
      helpSteps: [
        'Tap the downloaded file “hain-debug.apk” (in the browser or in your “Downloads”).',
        'Android shows a message like “For your security, your phone is not allowed to install unknown apps from this source.” Tap “Settings”.',
        'Turn on “Allow from this source”.',
        'Go back and tap “Install”. Done!',
        'Optional: turn the switch off again afterwards. Hain keeps working.',
      ],
      helpNote: 'The exact words differ a little between phone brands. If Google Play Protect warns you, tap “More details” and “Install anyway”. Hain is open source, so anyone can check the code.',
      req: 'You need Android 10 or newer. We recommend 6 GB of RAM or more.',
    },
    shots: {
      kicker: 'A look inside',
      title: 'Calm, clear, and a little green.',
      sub: 'Lots of white, fine lines, green only where it matters. In light and dark.',
      placeholder: 'Screenshot coming soon',
      items: ['Welcome', 'Start page', 'Thinking level', 'Long answers', 'Your chats', 'Settings'],
    },
    privacy: {
      kicker: 'Privacy',
      title: 'Privacy, explained simply.',
      sub: 'No fine print. Here is exactly what happens with your data.',
      stays: {
        title: 'Always stays on your phone',
        items: ['Your questions and Hain’s answers', 'Your saved chats (encrypted)', 'What Hain remembers about you', 'Your settings'],
      },
      online: {
        title: 'Goes online only when you ask',
        items: ['Weather, Wikipedia, web search, news', 'Downloading the brain and knowledge packs', 'Every request is listed, so you can see where it went'],
      },
      logTitle: 'Where requests go',
      logExample: 'Example',
      log: [
        { what: 'Weather tomorrow', where: 'open-meteo.com' },
        { what: 'Wikipedia: Rhine', where: 'wikipedia.org' },
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
        { label: 'Price', hain: 'Free', cloud: 'Free with limits, or a subscription', good: 'hain' },
        { label: 'Knowledge and smarts', hain: 'Smaller, looks things up online', cloud: 'Much bigger', good: 'cloud' },
        { label: 'Speed', hain: 'Depends on your phone', cloud: 'Usually fast', good: 'cloud' },
      ],
      note: 'Hain uses a small open language model. It knows less than the big ones, so for facts it looks things up on the web when you allow it.',
    },
    roadmap: {
      kicker: 'Roadmap',
      title: 'Where Hain is today.',
      sub: 'Hain is in early development (alpha). Things can break, and chats may be reset between versions.',
      items: [
        { tag: 'Nightly', when: 'Now', text: 'A new test version after every change. For curious testers.', now: true },
        { tag: '0.2.0', when: 'Planned for 6 Nov 2026', text: 'First version for friends: chat that stays on your phone, settings, knowledge packs, tools, news, memory, private chats.' },
        { tag: '1.0', when: '2027, no fixed date', text: 'Documents and photos, voice, mini-apps in Pim’s forest, and F-Droid.' },
      ],
      note: 'Dates are goals, not promises.',
      link: 'Detailed roadmap on GitHub',
    },
    faq: {
      kicker: 'FAQ',
      title: 'Questions? Answers.',
      items: [
        { q: 'Why do I have to allow “unknown sources”?', a: 'Hain is not in the Play Store yet, so Android asks once if your browser may install it. That is a normal safety question. The step-by-step help above shows how. You can turn the switch off again afterwards.' },
        { q: 'Is Hain safe?', a: 'Hain is open source: anyone can read the code on GitHub. It has no ads, no trackers and no Google services, and your chats stay on your phone. It is still an alpha version, so it can crash or lose chats between versions.' },
        { q: 'Does Hain need the internet?', a: 'Only once to download the brain (about 1 GB). After that, chatting works offline. Hain only goes online when you ask for something from the web, like the weather or a search, and it shows you where the request goes.' },
        { q: 'Which phone do I need?', a: 'An Android phone with Android 10 or newer. We recommend 6 GB of RAM or more. The newer your phone, the faster Hain answers. iPhones are not supported.' },
        { q: 'What does it cost?', a: 'Nothing. Hain is free, has no subscription and no ads. It is open source under the GPL-3.0 licence.' },
        { q: 'How do I update Hain?', a: 'Download the newest file from the same download page and install it over the old one. Your phone treats it as an update. Version 0.2.0 is planned for 6 November 2026.' },
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
      license: 'Licence: GPL-3.0',
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
    nav: { why: 'Warum Hain', features: 'Funktionen', how: 'Installieren', privacy: 'Datenschutz', faq: 'Fragen', download: 'Laden', menu: 'Menü öffnen', close: 'Menü schließen', mainLabel: 'Hauptnavigation', mobileLabel: 'Menü' },
    hero: {
      badge: 'Alpha · in Entwicklung',
      title: ['Deine KI.', 'Dein Handy.', 'Sonst niemand.'],
      sub: 'Hain ist eine kostenlose ChatGPT-Alternative für Android, die komplett auf deinem Handy läuft. Kein Konto, keine Cloud, keine Werbung, kein Tracking.',
      cta: 'Für Android laden',
      ctaHelp: 'So installierst du Hain',
      note: 'Kostenlos · Open Source · ab Android 10',
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
        { title: 'Kostenlos', text: 'Kein Abo, keine Werbung, keine versteckten Kosten. Hain ist Open Source und gehört allen.' },
        { title: 'Grün', text: 'Für deine Fragen muss kein Rechenzentrum laufen. Hain nutzt das Handy, das du schon hast.' },
      ],
    },
    status: { ready: 'Schon da', soon: 'Kommt bald' },
    features: {
      kicker: 'Funktionen',
      title: 'Alles, was du brauchst. Nichts, was nervt.',
      sub: 'Hain wächst noch. Grün heißt: steckt heute schon in der Testversion. Grau heißt: kommt bald.',
      readyTitle: 'Schon da',
      soonTitle: 'Kommt bald',
      items: [
        { icon: 'chat', title: 'Chatten auf dem Handy', text: 'Die Antwort entsteht live. Sofort stoppen, kopieren, teilen, Daumen hoch oder runter, nochmal fragen. Die KI läuft auf deinem Handy, sogar im Flugmodus.', status: 'ready' },
        { icon: 'lock', title: 'Verschlüsselte Chats', text: 'Chats liegen verschlüsselt nur auf deinem Handy. Suchen, umbenennen, anheften, löschen, mit automatischem Titel.', status: 'ready' },
        { icon: 'brain', title: 'Denk-Stufe', text: 'Auto, Schnell oder Gründlich, direkt im Eingabefeld. Schnell antwortet sofort, Gründlich denkt bei schweren Fragen länger nach.', status: 'ready' },
        { icon: 'search', title: 'Websuche ohne Schlüssel', text: 'Pim sucht im Netz ohne Konto und ohne API-Key, liest die besten Seiten gleichzeitig und zeigt die Quellen. Oder deine eigene SearXNG.', status: 'ready' },
        { icon: 'book', title: 'Wikipedia & Wetter', text: 'Wikipedia auf Deutsch und Englisch und das Wetter von Open-Meteo, als kleine Karte unter der Antwort. Wetter ohne Ort nimmt deinen Standort, wenn du es erlaubst.', status: 'ready' },
        { icon: 'calc', title: 'Genau rechnen', text: 'Ein echter Rechner und Einheiten-Umrechner statt Raten: „15 % von 80“, „5 km in Meilen“.', status: 'ready' },
        { icon: 'link', title: 'Seiten lesen', text: 'Schick Pim einen Link: Pim liest die Seite und fasst sie zusammen.', status: 'ready' },
        { icon: 'alarm', title: 'Handy-Aktionen mit Bestätigung', text: 'Wecker (auch jeden Werktag), Timer, Kalendereintrag, Route. Pim bereitet alles vor, passieren tut erst etwas nach deinem Tipp. Oder sie starten nach 3 Sekunden, mit „Rückgängig“.', status: 'ready' },
        { icon: 'shield', title: 'Netzwerk-Log & Offline-Modus', text: 'Jede Anfrage geht durch ein Tor und wird aufgelistet, auch der Gehirn-Download. Websuche an, erst fragen oder aus. Ein Schalter macht alles aus.', status: 'ready' },
        { icon: 'sliders', title: 'Einstellungen nach deinem Geschmack', text: 'Hell oder dunkel, gut lesbare Schrift, Antwortstil, ein paar Worte über dich, weniger Bewegung.', status: 'ready' },
        { icon: 'languages', title: 'Spricht deine Sprache', text: 'Pim antwortet in der Sprache, in der du schreibst, oder immer auf Deutsch oder Englisch.', status: 'ready' },
        { icon: 'package', title: 'Wissenspakete', text: 'Pakete ansehen, laden und entfernen, mit Hinweis, was zu deinem Handy passt.', status: 'ready' },
        { icon: 'update', title: 'Update-Prüfung (freiwillig)', text: 'Wenn du willst, fragt Hain einmal am Tag bei GitHub, ob es eine neue Version gibt, und zeigt einen ruhigen Hinweis. Aus, bis du Ja sagst. Über dich wird nichts gesendet, nichts installiert sich von selbst.', status: 'ready' },
        { icon: 'code', title: 'Entwicklermodus', text: 'Versteckt für Bastler: tippe 7× auf die Version. Probier eigene GGUF-Modelle von Hugging Face, stell Sampling und Denken ein, sieh Token pro Sekunde.', status: 'ready' },
        { icon: 'news', title: 'Nachrichten', text: 'Schlagzeilen aus einem festen Mix von 14 Quellen auf Deutsch und Englisch, auf Wunsch mit deinen eigenen Feeds. Pim fasst einen Artikel in 5 Punkten zusammen, auf dem Handy.', status: 'ready' },
        { icon: 'memory', title: 'Gedächtnis', text: '„Merk dir, dass ich vegetarisch esse.“ Pim behält es auf deinem Handy und nutzt es, wenn es passt. Alles sehen, ändern und löschen unter Einstellungen → Gedächtnis.', status: 'ready' },
        { icon: 'private', title: 'Privater Chat & App-Sperre', text: 'Ein Chat, der nie gespeichert wird, und eine Sperre mit Fingerabdruck.', status: 'soon' },
        { icon: 'cards', title: 'Antwort-Karten', text: 'Listen, Tabellen, Checklisten, Fortschrittsbalken und Knöpfe direkt in der Antwort. Abhaken, kopieren, teilen oder mit einem Tipp weiterfragen.', status: 'ready' },
        { icon: 'file', title: 'Dokumente', text: 'Frag deine PDFs und Fotos von Texten, mit Quellen.', status: 'soon' },
        { icon: 'mic', title: 'Sprache', text: 'Nachricht diktieren und Antworten vorlesen lassen.', status: 'soon' },
        { icon: 'share', title: 'Exportieren & teilen', text: 'Chat als Markdown oder PDF sichern, Text an Hain teilen, Widget und Schnellkachel.', status: 'soon' },
        { icon: 'image', title: 'Bilder', text: 'Objekte entfernen, Hintergrund weg, Fotos schärfen, alles auf dem Handy.', status: 'soon' },
        { icon: 'trees', title: 'Pims Wald', text: 'Mini-Apps ohne Schlüssel, etwa Bus und Bahn, die als Bäume in Pims Wald wachsen.', status: 'soon' },
      ],
    },
    how: {
      kicker: 'Installieren',
      title: 'In drei Schritten bereit.',
      sub: 'Hain ist noch nicht im Play Store. Du installierst die App direkt, das dauert etwa zwei Minuten.',
      steps: [
        { title: 'App laden', text: 'Öffne die Download-Seite auf deinem Handy und tippe auf „hain-debug.apk“.' },
        { title: 'Installieren', text: 'Öffne die Datei. Android fragt, ob dein Browser Apps installieren darf. Erlaub es einmal und tippe auf „Installieren“.' },
        { title: 'Gehirn laden', text: 'Beim ersten Start lädt Hain sein Gehirn (etwa 1 GB). Am besten im WLAN. Danach klappt das Chatten offline.' },
      ],
      helpTitle: 'Hilfe: „Unbekannte Apps installieren“ Schritt für Schritt',
      helpIntro: 'Android schützt dich vor Apps, die nicht aus dem Play Store kommen. Das ist gut so. Damit du Hain installieren kannst, erlaubst du es einmal für deinen Browser:',
      helpSteps: [
        'Tippe auf die geladene Datei „hain-debug.apk“ (im Browser oder unter „Downloads“).',
        'Android zeigt etwa: „Aus Sicherheitsgründen darf dein Smartphone keine unbekannten Apps aus dieser Quelle installieren.“ Tippe auf „Einstellungen“.',
        'Schalte „Dieser Quelle vertrauen“ bzw. „Von dieser Quelle zulassen“ ein.',
        'Geh zurück und tippe auf „Installieren“. Fertig!',
        'Wenn du magst, schaltest du den Schalter danach wieder aus. Hain läuft trotzdem weiter.',
      ],
      helpNote: 'Die Wörter sind je nach Handy-Marke etwas anders. Falls Google Play Protect warnt, tippe auf „Weitere Details“ und „Trotzdem installieren“. Hain ist Open Source, jede und jeder kann den Code prüfen.',
      req: 'Du brauchst Android 10 oder neuer. Wir empfehlen 6 GB Arbeitsspeicher (RAM) oder mehr.',
    },
    shots: {
      kicker: 'Ein Blick hinein',
      title: 'Ruhig, klar und ein bisschen grün.',
      sub: 'Viel Weiß, feine Linien, Grün nur da, wo es zählt. Hell und dunkel.',
      placeholder: 'Screenshot folgt',
      items: ['Willkommen', 'Startseite', 'Denk-Stufe', 'Lange Antworten', 'Deine Chats', 'Einstellungen'],
    },
    privacy: {
      kicker: 'Datenschutz',
      title: 'Datenschutz, einfach erklärt.',
      sub: 'Kein Kleingedrucktes. So geht Hain mit deinen Daten um.',
      stays: {
        title: 'Bleibt immer auf deinem Handy',
        items: ['Deine Fragen und Hains Antworten', 'Deine gespeicherten Chats (verschlüsselt)', 'Was Hain sich über dich merkt', 'Deine Einstellungen'],
      },
      online: {
        title: 'Geht nur online, wenn du es willst',
        items: ['Wetter, Wikipedia, Websuche, Nachrichten', 'Gehirn und Wissenspakete herunterladen', 'Jede Anfrage wird aufgelistet, du siehst, wohin sie ging'],
      },
      logTitle: 'Wohin Anfragen gehen',
      logExample: 'Beispiel',
      log: [
        { what: 'Wetter morgen', where: 'open-meteo.com' },
        { what: 'Wikipedia: Rhein', where: 'wikipedia.org' },
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
        { label: 'Preis', hain: 'Kostenlos', cloud: 'Kostenlos mit Grenzen oder Abo', good: 'hain' },
        { label: 'Wissen und Können', hain: 'Kleiner, schlägt online nach', cloud: 'Viel größer', good: 'cloud' },
        { label: 'Tempo', hain: 'Hängt von deinem Handy ab', cloud: 'Meist schnell', good: 'cloud' },
      ],
      note: 'Hain nutzt ein kleines offenes Sprachmodell. Es weiß weniger als die großen, deshalb schlägt es Fakten im Internet nach, wenn du das erlaubst.',
    },
    roadmap: {
      kicker: 'Fahrplan',
      title: 'Wo Hain gerade steht.',
      sub: 'Hain ist noch in früher Entwicklung (Alpha). Es kann etwas kaputtgehen, und Chats können zwischen Versionen verloren gehen.',
      items: [
        { tag: 'Nightly', when: 'Jetzt', text: 'Nach jeder Änderung eine neue Testversion. Für Neugierige.', now: true },
        { tag: '0.2.0', when: 'Geplant für 6. Nov. 2026', text: 'Erste Version für Freundinnen und Freunde: Chat, der auf dem Handy bleibt, Einstellungen, Wissenspakete, Werkzeuge, Nachrichten, Gedächtnis, private Chats.' },
        { tag: '1.0', when: '2027, ohne festes Datum', text: 'Dokumente und Fotos, Sprache, Mini-Apps in Pims Wald und F-Droid.' },
      ],
      note: 'Termine sind Ziele, keine Versprechen.',
      link: 'Ausführlicher Fahrplan auf GitHub',
    },
    faq: {
      kicker: 'Fragen',
      title: 'Fragen? Antworten.',
      items: [
        { q: 'Warum muss ich „unbekannte Quellen“ erlauben?', a: 'Hain ist noch nicht im Play Store. Deshalb fragt Android einmal, ob dein Browser die App installieren darf. Das ist eine normale Sicherheitsfrage. Die Schritt-für-Schritt-Hilfe oben zeigt, wie es geht. Danach kannst du den Schalter wieder ausmachen.' },
        { q: 'Ist Hain sicher?', a: 'Hain ist Open Source: Jede und jeder kann den Code auf GitHub lesen. Es gibt keine Werbung, keine Tracker und keine Google-Dienste, und deine Chats bleiben auf deinem Handy. Es ist aber noch eine Alpha-Version, sie kann abstürzen oder Chats zwischen Versionen verlieren.' },
        { q: 'Braucht Hain Internet?', a: 'Nur einmal, um das Gehirn zu laden (etwa 1 GB). Danach klappt das Chatten offline. Ins Internet geht Hain nur, wenn du etwas aus dem Netz willst, zum Beispiel das Wetter oder eine Suche, und zeigt dir, wohin die Anfrage geht.' },
        { q: 'Welches Handy brauche ich?', a: 'Ein Android-Handy mit Android 10 oder neuer. Wir empfehlen 6 GB Arbeitsspeicher (RAM) oder mehr. Je neuer das Handy, desto schneller antwortet Hain. iPhones werden nicht unterstützt.' },
        { q: 'Was kostet Hain?', a: 'Nichts. Hain ist kostenlos, ohne Abo und ohne Werbung. Es ist Open Source unter der Lizenz GPL-3.0.' },
        { q: 'Wie bekomme ich Updates?', a: 'Lade die neueste Datei von derselben Download-Seite und installiere sie über die alte. Dein Handy erkennt das als Update. Version 0.2.0 ist für den 6. November 2026 geplant.' },
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
      license: 'Lizenz: GPL-3.0',
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

// Screenshots in style A "Lichtung" (from docs/design/screens/style_a_*.png, converted to WebP).
// Each has a light and a dark file in public/screenshots/: <id>-light.webp, <id>-dark.webp.
// The page shows the one matching the visitor's colour scheme.
export const SCREENSHOTS: string[] = ['onboarding', 'chat_empty', 'chat_composer_picker', 'chat_long_answer', 'drawer', 'settings']

// Pixel size of each screenshot (light and dark are the same). They are cropped to where the app
// content ends (T-105): no empty bottom strip, and the short settings page ends after its last row.
export const SHOT_SIZE: Record<string, [number, number]> = {
  onboarding: [720, 1548],
  chat_empty: [720, 1548],
  chat_composer_picker: [720, 1548],
  chat_long_answer: [720, 1548],
  drawer: [720, 1548],
  settings: [720, 1008],
}

// The English screenshots (`<id>-<mode>-en.webp`, T-107) were recorded later: the settings page
// has a short intro at the top since then, so it is taller. Others are as in SHOT_SIZE.
export const SHOT_SIZE_EN: Record<string, [number, number]> = {
  settings: [720, 1110],
}
