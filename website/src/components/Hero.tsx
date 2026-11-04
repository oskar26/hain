import type React from 'react'
import { Download, Menu, X, ArrowDown, Languages, Check } from 'lucide-react'
import { COPY, LINKS, SHOT_SIZE, SHOT_SIZE_EN, type Lang } from '../content'
import { AppIcon, BASE, Container, GitHubIcon } from './ui'

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
    <header data-nav className="site-nav fixed inset-x-0 top-0 z-50">
      {/* T-105: background, blur, hairline and shadow fade in softly as one layer (see .nav-bg in styles.css) */}
      <div className="nav-bg" aria-hidden />
      <Container className="nav-row relative flex items-center justify-between gap-4">
        <a href="#top" className="flex items-center gap-2.5 font-medium tracking-tight" aria-label="Hain">
          <AppIcon size={32} eager />
          <span className="text-lg">Hain</span>
        </a>
        <nav className="hidden items-center gap-1 lg:flex" aria-label={t.nav.mainLabel}>
          {links.map(([href, label]) => (
            <a key={href} href={href} className="nav-link rounded-[10px] px-3 py-2 text-[15px] text-muted">
              {label}
            </a>
          ))}
        </nav>
        <div className="flex items-center gap-1">
          <a href={other} hrefLang={lang === 'en' ? 'de' : 'en'} lang={lang === 'en' ? 'de' : 'en'} aria-label={t.meta.switchLabel} className="nav-link inline-flex items-center gap-1.5 rounded-[10px] px-3 py-2 text-sm font-medium text-muted">
            <Languages className="h-4 w-4" strokeWidth={1.75} aria-hidden />
            <span>{lang === 'en' ? 'DE' : 'EN'}</span>
          </a>
          <a href={LINKS.repo} aria-label="GitHub" className="nav-link hidden rounded-[10px] p-2.5 text-muted sm:inline-flex">
            <GitHubIcon className="h-5 w-5" />
          </a>
          <a href={LINKS.download} className="btn btn-primary !hidden !px-4 !py-2 !text-sm sm:!inline-flex">
            <Download className="h-4 w-4" strokeWidth={1.75} aria-hidden /> {t.nav.download}
          </a>
          <button type="button" data-menu-toggle data-label-open={t.nav.menu} data-label-close={t.nav.close} className="nav-link group rounded-[10px] p-2.5 text-ink lg:hidden" aria-expanded="false" aria-controls="mobile-nav" aria-label={t.nav.menu}>
            <Menu className="h-6 w-6 group-aria-expanded:hidden" strokeWidth={1.75} aria-hidden />
            <X className="hidden h-6 w-6 group-aria-expanded:block" strokeWidth={1.75} aria-hidden />
          </button>
        </div>
      </Container>
      <nav id="mobile-nav" hidden className="relative border-t border-line bg-bg lg:hidden" aria-label={t.nav.mobileLabel}>
        <Container className="flex flex-col py-3">
          {links.map(([href, label]) => (
            <a key={href} href={href} data-menu-link className="nav-link rounded-[12px] px-3 py-3 text-lg text-ink">
              {label}
            </a>
          ))}
          <div className="mt-3 flex gap-2 pb-4">
            <a href={LINKS.download} className="btn btn-primary flex-1">
              <Download className="h-4 w-4" strokeWidth={1.75} aria-hidden /> {t.hero.cta}
            </a>
            <a href={LINKS.repo} aria-label="GitHub" className="btn btn-ghost !px-4">
              <GitHubIcon className="h-5 w-5" />
            </a>
          </div>
        </Container>
      </nav>
    </header>
  )
}

export function Hero({ lang }: { lang: Lang }) {
  const t = COPY[lang].hero
  const m = COPY[lang].marquee
  return (
    <section id="top" className="relative overflow-hidden pt-16">
      <div className="mist" aria-hidden />
      <Container className="relative grid items-center gap-12 pb-16 pt-10 sm:pt-16 lg:grid-cols-[1.1fr_1fr] lg:gap-8 lg:pb-24">
        <div className="text-center lg:text-left">
          <div className="fade-up flex justify-center lg:justify-start">
            <AppIcon size={88} eager alt={t.iconAlt} className="float drop-shadow-[0_18px_24px_rgba(11,122,79,.28)]" />
          </div>
          <p className="fade-up mt-7 inline-flex items-center gap-2 rounded-full border border-line bg-card/80 px-3 py-1 text-[13px] font-medium text-muted backdrop-blur" style={{ '--d': '0.05s' } as React.CSSProperties}>
            <span className="h-1.5 w-1.5 rounded-full bg-hain" aria-hidden />
            {t.badge}
          </p>
          <h1 className="fade-up mt-5 text-[2.6rem] leading-[1.05] font-light tracking-[-0.025em] sm:text-6xl lg:text-[4rem]" style={{ '--d': '0.1s' } as React.CSSProperties}>
            {t.title[0]} {t.title[1]} <span className="text-hain">{t.title[2]}</span>
          </h1>
          <p className="fade-up mx-auto mt-6 max-w-xl text-pretty text-lg leading-relaxed text-muted lg:mx-0" style={{ '--d': '0.15s' } as React.CSSProperties}>
            {t.sub}
          </p>
          <div className="fade-up mt-8 flex flex-col justify-center gap-3 sm:flex-row lg:justify-start" style={{ '--d': '0.2s' } as React.CSSProperties}>
            <a href={LINKS.download} className="btn btn-primary !px-6 !py-3.5 !text-base">
              <Download className="h-5 w-5" strokeWidth={1.75} aria-hidden /> {t.cta}
            </a>
            <a href="#install" className="btn btn-ghost !px-6 !py-3.5 !text-base">
              <ArrowDown className="h-5 w-5" strokeWidth={1.75} aria-hidden /> {t.ctaHelp}
            </a>
          </div>
          <p className="mt-5 text-sm text-muted">{t.note}</p>
          <ul className="mt-8 flex flex-wrap justify-center gap-x-5 gap-y-2 text-[14px] text-muted lg:justify-start">
            {m.slice(0, 5).map((x) => (
              <li key={x} className="flex items-center gap-1.5">
                <Check className="h-4 w-4 text-hain" strokeWidth={2} aria-hidden /> {x}
              </li>
            ))}
          </ul>
        </div>
        <div className="fade-up relative mx-auto flex w-full max-w-[460px] items-start justify-center gap-4" style={{ '--d': '0.25s' } as React.CSSProperties}>
          <Shot id="chat_empty" lang={lang} alt={t.pimAlt} className="w-[58%]" eager />
          <Shot id="chat_composer_picker" lang={lang} alt="" className="mt-14 hidden w-[42%] sm:block" eager />
        </div>
      </Container>
    </section>
  )
}

/** One app screenshot, light or dark to match the visitor, with the app in the page's language (T-107). */
export function Shot({ id, alt, lang, className = '', eager = false }: { id: string; alt: string; lang: Lang; className?: string; eager?: boolean }) {
  const src = (m: string) => `${BASE}screenshots/${id}-${m}${lang === 'en' ? '-en' : ''}.webp`
  const [w, h] = (lang === 'en' ? SHOT_SIZE_EN[id] : undefined) ?? SHOT_SIZE[id] ?? [720, 1548]
  return (
    <div className={`device self-start ${className}`}>
      <picture>
        <source srcSet={src('dark')} media="(prefers-color-scheme: dark)" />
        <img src={src('light')} alt={alt} width={w} height={h} loading={eager ? 'eager' : 'lazy'} className="block h-auto w-full" />
      </picture>
    </div>
  )
}
