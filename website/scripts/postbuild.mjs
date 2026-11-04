// Prerenders the page into static HTML:
//   dist/index.html     English (default, /hain/)
//   dist/de/index.html  German (/hain/de/)
//   dist/404.html       copy of the English page
// The client bundle then hydrates it. Removes the temporary SSR build afterwards.
import { readFileSync, writeFileSync, mkdirSync, copyFileSync, rmSync } from 'node:fs'
import { pathToFileURL } from 'node:url'
import { resolve } from 'node:path'

const { render } = await import(pathToFileURL(resolve('dist-ssr/entry-server.js')).href)
const template = readFileSync('dist/index.html', 'utf8')

const en = template.replace('<!--app-->', render('en'))
const de = template
  .replace('<!--app-->', render('de'))
  .replace('<html lang="en">', '<html lang="de">')
  .replace(/<title>[^<]*<\/title>/, '<title>Hain – Deine KI. Dein Handy. Sonst niemand.</title>')
  .replace(
    /<meta name="description" content="[^"]*"/,
    '<meta name="description" content="Hain ist eine kostenlose Open-Source-Alternative zu ChatGPT für Android, die komplett auf deinem Handy läuft. Kein Konto, keine Cloud, keine Werbung, kein Tracking."',
  )
  .replace(/<meta property="og:title" content="[^"]*"/, '<meta property="og:title" content="Hain – Deine KI. Dein Handy. Sonst niemand."')
  .replace(
    /<meta property="og:description" content="[^"]*"/,
    '<meta property="og:description" content="Ein privater KI-Assistent für Android, der auf deinem Handy läuft. Kein Konto, keine Cloud, kein Tracking. Kostenlos und Open Source."',
  )
  .replace(/<meta property="og:url" content="[^"]*"/, '<meta property="og:url" content="https://oskar26.github.io/hain/de/"')
  .replace(/<meta property="og:image:alt" content="[^"]*"/, '<meta property="og:image:alt" content="App-Symbol von Hain: Pim, eine glänzende grüne Glasfigur, in einem weichen Wald"')
  .replace('<meta property="og:locale" content="en_US" />', '<meta property="og:locale" content="de_DE" />')
  .replace('<noscript>Hain – a private AI for Android. Download:', '<noscript>Hain – eine private KI für Android. Download:')

writeFileSync('dist/index.html', en)
mkdirSync('dist/de', { recursive: true })
writeFileSync('dist/de/index.html', de)
copyFileSync('dist/index.html', 'dist/404.html')
rmSync('dist-ssr', { recursive: true, force: true })
console.log('postbuild: prerendered dist/index.html (en), dist/de/index.html (de), dist/404.html')
