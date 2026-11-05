import type { Lang } from './content'
import { Hero, Nav } from './components/Hero'
import { Compare, Faq, Features, FinalCta, Footer, Install, Join, Privacy, Roadmap, Why } from './components/Sections'

/** The whole page. Rendered to static HTML at build time (see src/entry-server.tsx). */
export default function App({ lang }: { lang: Lang }) {
  return (
    <div>
      <a href="#main" className="sr-only focus:not-sr-only focus:fixed focus:left-4 focus:top-4 focus:z-[70] focus:rounded-full focus:bg-ink focus:px-4 focus:py-2 focus:text-bg">
        {lang === 'de' ? 'Zum Inhalt springen' : 'Skip to content'}
      </a>
      <Nav lang={lang} />
      <main id="main">
        <Hero lang={lang} />
        <Why lang={lang} />
        <Features lang={lang} />
        <Roadmap lang={lang} />
        <Install lang={lang} />
        <Privacy lang={lang} />
        <Compare lang={lang} />
        <Faq lang={lang} />
        <Join lang={lang} />
        <FinalCta lang={lang} />
      </main>
      <Footer lang={lang} />
    </div>
  )
}
