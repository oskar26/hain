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
}

export type Status = 'ready' | 'progress' | 'next' | 'later'

type Copy = {
  meta: { switchTo: string; switchLabel: string }
  nav: { why: string; features: string; how: string; privacy: string; faq: string; download: string; menu: string; close: string }
  hero: { badge: string; title: [string, string, string]; sub: string; cta: string; ctaHelp: string; note: string; pimAlt: string }
  marquee: string[]
  why: { kicker: string; title: string; sub: string; items: { title: string; text: string }[] }
  status: Record<Status, string>
  features: { kicker: string; title: string; sub: string; items: { title: string; text: string; status: Status }[]; chatDemo: { you: string; pim: string } }
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
  footer: { tagline: string; source: string; license: string; imprint: string; imprintNote: string; made: string; noCookies: string }
}

export const COPY: Record<Lang, Copy> = {
  en: {
    meta: { switchTo: 'Deutsch', switchLabel: 'Diese Seite auf Deutsch' },
    nav: { why: 'Why Hain', features: 'Features', how: 'Install', privacy: 'Privacy', faq: 'FAQ', download: 'Download', menu: 'Open menu', close: 'Close menu' },
    hero: {
      badge: 'Alpha · in development',
      title: ['Your AI.', 'Your phone.', 'Nobody else.'],
      sub: 'Hain is a free ChatGPT alternative for Android that runs completely on your phone. No account, no cloud, no ads, no tracking.',
      cta: 'Download for Android',
      ctaHelp: 'How to install',
      note: 'Free · Open source · Android 10 or newer',
      pimAlt: 'Pim, the little green mascot of Hain',
    },
    marquee: ['No account', 'No cloud', 'No ads', 'No tracking', 'No Google services', 'Works offline', 'Open source', 'Free', 'German & English', 'Made in Cologne'],
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
    status: { ready: 'Works today', progress: 'In progress', next: 'Coming in 0.2.0', later: 'Later' },
    features: {
      kicker: 'Features',
      title: 'Everything you need. Nothing you don’t.',
      sub: 'Hain is still growing. Here is what works today and what is coming next.',
      chatDemo: { you: 'What can I cook with leeks and potatoes?', pim: 'How about a creamy leek and potato soup? You need…' },
      items: [
        { title: 'Chat on your phone', text: 'Ask anything and watch the answer appear. Stop, copy or share answers. The AI runs right on your phone.', status: 'ready' },
        { title: 'Your chats, locked away', text: 'Chats are saved encrypted on your phone. You can search, rename and delete them.', status: 'progress' },
        { title: 'Handy tools', text: 'Calculator, weather, Wikipedia, web search and timer. Hain shows you where each request goes.', status: 'next' },
        { title: 'News', text: 'Headlines from a fixed mix of sources, or from your own feeds.', status: 'next' },
        { title: 'Memory', text: '“Remember that I’m vegetarian.” You can see, change and delete everything Hain remembers.', status: 'next' },
        { title: 'Private chat & app lock', text: 'A chat that is not saved at all, and a fingerprint lock for the app.', status: 'next' },
        { title: 'Made for you', text: 'Light or dark, an easy-to-read font, answer language, knowledge packs.', status: 'progress' },
        { title: 'Pim’s forest', text: 'Later: ask your documents and photos, talk with your voice, and add mini-apps that grow as trees in Pim’s forest.', status: 'later' },
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
      sub: 'Hain looks like the chat apps you know, with Pim showing you around.',
      placeholder: 'Screenshot coming soon',
      items: ['Welcome tour with Pim', 'Chat', 'Settings', 'Knowledge packs'],
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
        { what: 'Weather in Cologne', where: 'open-meteo.com' },
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
      made: 'Made in Cologne',
      noCookies: 'This website uses no cookies and no tracking.',
    },
  },

  de: {
    meta: { switchTo: 'English', switchLabel: 'This page in English' },
    nav: { why: 'Warum Hain', features: 'Funktionen', how: 'Installieren', privacy: 'Datenschutz', faq: 'Fragen', download: 'Laden', menu: 'Menü öffnen', close: 'Menü schließen' },
    hero: {
      badge: 'Alpha · in Entwicklung',
      title: ['Deine KI.', 'Dein Handy.', 'Sonst niemand.'],
      sub: 'Hain ist eine kostenlose ChatGPT-Alternative für Android, die komplett auf deinem Handy läuft. Kein Konto, keine Cloud, keine Werbung, kein Tracking.',
      cta: 'Für Android laden',
      ctaHelp: 'So installierst du Hain',
      note: 'Kostenlos · Open Source · ab Android 10',
      pimAlt: 'Pim, das kleine grüne Maskottchen von Hain',
    },
    marquee: ['Kein Konto', 'Keine Cloud', 'Keine Werbung', 'Kein Tracking', 'Keine Google-Dienste', 'Läuft offline', 'Open Source', 'Kostenlos', 'Deutsch & Englisch', 'Gemacht in Köln'],
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
    status: { ready: 'Geht schon', progress: 'In Arbeit', next: 'Kommt mit 0.2.0', later: 'Später' },
    features: {
      kicker: 'Funktionen',
      title: 'Alles, was du brauchst. Nichts, was nervt.',
      sub: 'Hain wächst noch. Hier siehst du, was schon geht und was als Nächstes kommt.',
      chatDemo: { you: 'Was kann ich mit Lauch und Kartoffeln kochen?', pim: 'Wie wär’s mit einer cremigen Kartoffel-Lauch-Suppe? Du brauchst…' },
      items: [
        { title: 'Chatten auf dem Handy', text: 'Frag, was du willst, und sieh zu, wie die Antwort entsteht. Antworten stoppen, kopieren oder teilen. Die KI läuft direkt auf deinem Handy.', status: 'ready' },
        { title: 'Deine Chats, sicher verwahrt', text: 'Chats werden verschlüsselt auf deinem Handy gespeichert. Du kannst sie durchsuchen, umbenennen und löschen.', status: 'progress' },
        { title: 'Praktische Werkzeuge', text: 'Rechner, Wetter, Wikipedia, Websuche und Timer. Hain zeigt dir, wohin jede Anfrage geht.', status: 'next' },
        { title: 'Nachrichten', text: 'Schlagzeilen aus einem festen Mix von Quellen oder aus deinen eigenen Feeds.', status: 'next' },
        { title: 'Gedächtnis', text: '„Merk dir, dass ich vegetarisch esse.“ Du kannst alles sehen, ändern und löschen, was Hain sich merkt.', status: 'next' },
        { title: 'Privater Chat & App-Sperre', text: 'Ein Chat, der gar nicht gespeichert wird, und eine Sperre mit Fingerabdruck.', status: 'next' },
        { title: 'So, wie du es magst', text: 'Hell oder dunkel, gut lesbare Schrift, Antwortsprache, Wissenspakete.', status: 'progress' },
        { title: 'Pims Wald', text: 'Später: deine Dokumente und Fotos fragen, mit der Stimme sprechen und Mini-Apps, die als Bäume in Pims Wald wachsen.', status: 'later' },
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
      sub: 'Hain sieht aus wie die Chat-Apps, die du kennst, und Pim zeigt dir alles.',
      placeholder: 'Screenshot folgt',
      items: ['Willkommenstour mit Pim', 'Chat', 'Einstellungen', 'Wissenspakete'],
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
        { what: 'Wetter in Köln', where: 'open-meteo.com' },
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
      made: 'Gemacht in Köln',
      noCookies: 'Diese Website nutzt keine Cookies und kein Tracking.',
    },
  },
}

// Screenshot slots. To replace a placeholder, put the image in
// website/public/screenshots/ (portrait, about 1080×2340, WebP or PNG)
// and set `file` to its name, e.g. file: '01-welcome.webp'.
// `de` is an optional German version of the same screen.
export const SCREENSHOTS: { id: string; file?: string; de?: string }[] = [
  { id: '01-welcome' },
  { id: '02-chat' },
  { id: '03-settings' },
  { id: '04-packs' },
]
