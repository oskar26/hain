import { Download, Menu, X, ArrowDown, Languages } from 'lucide-react'
import { COPY, LINKS, type Lang } from '../content'
import { BASE, Container, GitHubIcon, Pim3D, img } from './ui'

export function Nav({ lang }: { lang: Lang }) {
  const t = COPY[lang]
  const links = [
    ['#why', t.nav.why],
    ['#features', t.nav.features],
    ['#install', t.nav.how],
    ['#privacy', t.nav.privacy],
    ['#faq', t.nav.faq],
  ] as const
  const other = lang === 'en' ? `${BASE}de/` : BASE
  return (
    <header data-nav className="fixed inset-x-0 top-0 z-50 transition-all duration-300 data-[scrolled]:bg-white/80 data-[scrolled]:shadow-[0_8px_30px_-20px_rgba(13,40,28,.35)] data-[scrolled]:backdrop-blur-xl">
      <Container className="flex h-16 items-center justify-between gap-4">
        <a href="#top" className="flex items-center gap-2.5 font-semibold tracking-tight" aria-label="Hain">
          <img src={img('hain-icon.webp')} alt="" width={34} height={34} className="h-[34px] w-[34px]" />
          <span className="text-lg">Hain</span>
        </a>
        <nav className="hidden items-center gap-1 lg:flex" aria-label="Main">
          {links.map(([href, label]) => (
            <a key={href} href={href} className="rounded-full px-3.5 py-2 text-[15px] text-muted transition hover:bg-surface hover:text-ink">
              {label}
            </a>
          ))}
        </nav>
        <div className="flex items-center gap-1.5">
          <a href={other} hrefLang={lang === 'en' ? 'de' : 'en'} lang={lang === 'en' ? 'de' : 'en'} aria-label={t.meta.switchLabel} className="inline-flex items-center gap-1.5 rounded-full px-3 py-2 text-sm font-medium text-muted transition hover:bg-surface hover:text-ink">
            <Languages className="h-4 w-4" aria-hidden />
            <span>{lang === 'en' ? 'DE' : 'EN'}</span>
          </a>
          <a href={LINKS.repo} aria-label="GitHub" className="hidden rounded-full p-2.5 text-muted transition hover:bg-surface hover:text-ink sm:inline-flex">
            <GitHubIcon className="h-5 w-5" />
          </a>
          <a href={LINKS.download} className="pill-dark !hidden !px-4 !py-2.5 !text-sm sm:!inline-flex">
            <Download className="h-4 w-4" aria-hidden /> {t.nav.download}
          </a>
          <button type="button" data-menu-toggle data-label-open={t.nav.menu} data-label-close={t.nav.close} className="group rounded-full p-2.5 text-ink lg:hidden" aria-expanded="false" aria-controls="mobile-nav" aria-label={t.nav.menu}>
            <Menu className="h-6 w-6 group-aria-expanded:hidden" aria-hidden />
            <X className="hidden h-6 w-6 group-aria-expanded:block" aria-hidden />
          </button>
        </div>
      </Container>
      <nav id="mobile-nav" hidden className="border-t border-line/70 bg-white/95 lg:hidden" aria-label="Mobile">
          <Container className="flex flex-col py-3">
            {links.map(([href, label]) => (
              <a key={href} href={href} data-menu-link className="rounded-2xl px-3 py-3 text-lg font-medium text-ink hover:bg-surface">
                {label}
              </a>
            ))}
            <div className="mt-3 flex gap-2 pb-4">
              <a href={LINKS.download} className="pill-dark flex-1">
                <Download className="h-4 w-4" aria-hidden /> {t.hero.cta}
              </a>
              <a href={LINKS.repo} aria-label="GitHub" className="pill-light !px-4">
                <GitHubIcon className="h-5 w-5" />
              </a>
            </div>
          </Container>
      </nav>
    </header>
  )
}

const BUBBLES = [
  { name: 'leaf', x: 2, y: 30, s: 25, d: 0.9, delay: 0 },
  { name: 'weather', x: 17, y: 2, s: 21, d: 1.6, delay: -2 },
  { name: 'book', x: 75, y: 36, s: 23, d: 1.2, delay: -4 },
  { name: 'forest', x: 63, y: 0, s: 20, d: 1.8, delay: -1 },
  { name: 'lock', x: 84, y: 6, s: 14, d: 2.4, delay: -3 },
  { name: 'phone', x: 1, y: 2, s: 13, d: 2.2, delay: -5 },
]

function Bubble({ b }: { b: (typeof BUBBLES)[number] }) {
  return (
    <div className="absolute" data-parallax={b.d} style={{ left: `${b.x}%`, top: `${b.y}%`, width: `${b.s}%` }}>
      <img src={img(`bubble-${b.name}.webp`)} alt="" width={360} height={360} className="bob w-full select-none" style={{ animationDelay: `${b.delay}s` }} draggable={false} />
    </div>
  )
}

function Words({ words }: { words: string[] }) {
  // Pure CSS animation (.word) so the headline paints from the prerendered HTML.
  return (
    <>
      {words.map((line, li) => (
        <span key={li} className={`block ${li === 2 ? 'pb-1' : ''}`}>
          {line.split(' ').map((w, wi) => (
            <span key={wi} className={`word ${li === 2 ? 'shimmer-text' : ''}`} style={{ animationDelay: `${0.1 + li * 0.15 + wi * 0.07}s` }}>
              {w}&nbsp;
            </span>
          ))}
        </span>
      ))}
    </>
  )
}

export function Hero({ lang }: { lang: Lang }) {
  const t = COPY[lang].hero
  return (
    <section id="top" className="relative overflow-hidden pt-20">
      <div className="rise" aria-hidden>
        <span />
        <span />
        <span />
        <span />
      </div>
      <div className="pointer-events-none absolute inset-x-0 bottom-0 h-28 bg-gradient-to-b from-transparent to-white" aria-hidden />
      <Container className="relative flex flex-col items-center pb-20 text-center sm:pb-24">
        {/* bubble cloud */}
        <div className="relative mx-auto aspect-[16/9] w-full max-w-[760px] sm:aspect-[5/2]">
          {BUBBLES.map((b) => (
            <Bubble key={b.name} b={b} />
          ))}
          <div className="absolute left-1/2 top-[18%] w-[34%] -translate-x-1/2 sm:top-[10%] sm:w-[27%]" data-parallax="0.5">
            <div className="float">
              <Pim3D alt={t.pimAlt} eager />
            </div>
          </div>
        </div>

        <p
          className="fade-up relative mt-2 inline-flex items-center gap-2 rounded-full border border-hain/20 bg-white/80 px-3.5 py-1.5 text-[13px] font-semibold text-hain-dark shadow-sm backdrop-blur"
        >
          <span className="relative flex h-2 w-2" aria-hidden>
            <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-hain opacity-60" />
            <span className="relative inline-flex h-2 w-2 rounded-full bg-hain" />
          </span>
          {t.badge}
        </p>

        <h1 className="mt-5 text-[2.6rem] leading-[1.04] font-semibold tracking-[-0.03em] sm:text-6xl lg:text-[4.25rem]">
          <Words words={t.title} />
        </h1>
        <p className="mt-6 max-w-xl text-pretty text-lg leading-relaxed text-muted sm:text-xl">{t.sub}</p>

        <div className="mt-9 flex w-full max-w-md flex-col gap-3 sm:w-auto sm:max-w-none sm:flex-row">
          <a href={LINKS.download} className="pill-dark !px-7 !py-4 !text-base">
            <Download className="h-5 w-5" aria-hidden /> {t.cta}
          </a>
          <a href="#install" className="pill-light !px-7 !py-4 !text-base">
            <ArrowDown className="h-5 w-5" aria-hidden /> {t.ctaHelp}
          </a>
        </div>
        <p className="mt-5 text-sm font-medium text-muted">{t.note}</p>
      </Container>
    </section>
  )
}
