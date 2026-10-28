# OpenUI (openui.com, thesysdev/openui, MIT) – Bewertung für lokale Android-App
- OpenUI Lang: zeilenbasierte DSL `id = Component(args)`, streambar, ~1362 Tokens/Screen, 52-58% weniger als JSON-Formate (A2UI, json-render).
- Komponenten per Zod-Schema definiert, System-Prompt wird daraus generiert.
- Renderer: React, Vue, Svelte, Angular, React Native (Expo-Beispiel), React Email. KEIN Kotlin/Compose-Renderer. lang-core = JS-Parser.
- OUI-1: eigenes Modell (DiffusionGemma-Finetune) 26B/4B aktiv, ~25.8 GiB FP8 -> nicht fürs Handy. Basismodell nur 13% auf GenUI-Benchmark, OUI-1 71.7% -> kleine Allzweckmodelle schaffen gültiges UI schlecht.
- Fazit: v1 feste Tool-Karten (Wetter, Timer, Kalender, Suche) in Compose. v2: Teilmenge von OpenUI Lang (6-10 Komponenten) mit llama.cpp-GBNF-Grammatik erzwingen + eigener kleiner Kotlin-Parser/Compose-Renderer. Alternativ Google A2UI (hat native Renderer-Ansatz) prüfen.
Quellen: https://www.openui.com/ https://www.openui.com/docs/openui-lang/examples/react-native https://www.openui.com/oui-1 https://github.com/thesysdev/openui
