import {
  ShieldCheck, WifiOff, PiggyBank, Leaf, MessageCircle, Lock, Calculator, Newspaper, Brain, EyeOff, SlidersHorizontal, Trees,
  Download, Smartphone, Sparkles, Check, X, ChevronDown, ArrowUpRight, Bug, Languages, CodeXml, Globe, CloudSun, BookOpen,
  Search, Link, AlarmClock, Package, NotebookPen, LayoutList, FileText, Mic, Clock, Share2, ImageIcon, Heart,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { COPY, LINKS, SCREENSHOTS, type Lang, type Status } from '../content'
import { AppIcon, Container, GitHubIcon, IconTile, Reveal, SectionHead } from './ui'
import { Shot } from './Hero'

const SW = 1.75 // Lucide stroke width, calm like the app's line icons

/* ---------- Why ---------- */
const WHY_ICONS: LucideIcon[] = [ShieldCheck, WifiOff, PiggyBank, Leaf]

export function Why({ lang }: { lang: Lang }) {
  const t = COPY[lang].why
  return (
    <section id="why" className="relative border-t border-line py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
        <ul className="mt-14 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {t.items.map((it, i) => {
            const Icon = WHY_ICONS[i]
            return (
              <Reveal as="li" key={it.title} delay={i * 0.06}>
                <div className="card card-hover h-full p-6">
                  <IconTile>
                    <Icon className="h-5 w-5" strokeWidth={SW} aria-hidden />
                  </IconTile>
                  <h3 className="mt-5 text-lg font-medium">{it.title}</h3>
                  <p className="mt-2 leading-relaxed text-muted">{it.text}</p>
                </div>
              </Reveal>
            )
          })}
        </ul>
      </Container>
    </section>
  )
}

/* ---------- Features with status ---------- */
const FEATURE_ICONS: Record<string, LucideIcon> = {
  chat: MessageCircle, lock: Lock, brain: Brain, search: Search, book: BookOpen, link: Link, alarm: AlarmClock,
  shield: ShieldCheck, sliders: SlidersHorizontal, languages: Languages, package: Package, news: Newspaper,
  memory: NotebookPen, private: EyeOff, cards: LayoutList, file: FileText, mic: Mic, trees: Trees, calc: Calculator, share: Share2, image: ImageIcon,
}

export function StatusBadge({ s, label }: { s: Status; label: string }) {
  return (
    <span className={`badge ${s === 'ready' ? 'badge-ready' : 'badge-soon'}`}>
      {s === 'ready' ? <Check className="h-3.5 w-3.5" strokeWidth={2.25} aria-hidden /> : <Clock className="h-3.5 w-3.5" strokeWidth={2} aria-hidden />}
      {label}
    </span>
  )
}

export function Features({ lang }: { lang: Lang }) {
  const t = COPY[lang].features
  const st = COPY[lang].status
  const groups: { s: Status; title: string }[] = [
    { s: 'ready', title: t.readyTitle },
    { s: 'soon', title: t.soonTitle },
  ]
  return (
    <section id="features" className="relative border-t border-line bg-surface/50 py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
        {groups.map((g) => {
          const items = t.items.filter((it) => it.status === g.s)
          return (
            <div key={g.s} className="mt-14">
              <Reveal className="flex items-center gap-3">
                <h3 className="text-xl font-medium">{g.title}</h3>
                <span className="text-sm text-muted">{items.length}</span>
                <span className="h-px flex-1 bg-line" aria-hidden />
              </Reveal>
              <ul className="mt-5 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {items.map((it, i) => {
                  const Icon = FEATURE_ICONS[it.icon] ?? Sparkles
                  const soon = it.status === 'soon'
                  return (
                    <Reveal as="li" key={it.title} delay={(i % 3) * 0.05}>
                      <div className="card card-hover flex h-full flex-col p-5">
                        <div className="flex items-start justify-between gap-3">
                          <IconTile muted={soon}>
                            <Icon className="h-5 w-5" strokeWidth={SW} aria-hidden />
                          </IconTile>
                          <StatusBadge s={it.status} label={st[it.status]} />
                        </div>
                        <h4 className={`mt-4 text-[17px] font-medium ${soon ? 'text-ink/80' : ''}`}>{it.title}</h4>
                        <p className="mt-1.5 text-[15px] leading-relaxed text-muted">{it.text}</p>
                      </div>
                    </Reveal>
                  )
                })}
              </ul>
            </div>
          )
        })}
      </Container>
    </section>
  )
}

/* ---------- Install ---------- */
const STEP_ICONS: LucideIcon[] = [Download, Smartphone, Sparkles]

export function Install({ lang }: { lang: Lang }) {
  const t = COPY[lang].how
  return (
    <section id="install" className="relative border-t border-line py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
        <ol className="mt-14 grid grid-cols-1 gap-4 md:grid-cols-3">
          {t.steps.map((s, i) => {
            const Icon = STEP_ICONS[i]
            return (
              <Reveal as="li" key={s.title} delay={i * 0.08}>
                <div className="card h-full p-6">
                  <div className="flex items-center justify-between">
                    <IconTile>
                      <Icon className="h-5 w-5" strokeWidth={SW} aria-hidden />
                    </IconTile>
                    <span className="text-sm font-medium tabular-nums text-muted" aria-hidden>
                      0{i + 1}
                    </span>
                  </div>
                  <h3 className="mt-5 text-lg font-medium">
                    <span className="sr-only">{i + 1}. </span>
                    {s.title}
                  </h3>
                  <p className="mt-2 leading-relaxed text-muted">{s.text}</p>
                </div>
              </Reveal>
            )
          })}
        </ol>
        <Reveal className="mt-8 flex flex-col items-center gap-3 text-center">
          <a href={LINKS.download} className="btn btn-primary !px-6 !py-3.5 !text-base">
            <Download className="h-5 w-5" strokeWidth={SW} aria-hidden /> {COPY[lang].hero.cta}
          </a>
          <p className="text-sm text-muted">{t.req}</p>
        </Reveal>
        <Reveal className="mx-auto mt-10 max-w-3xl">
          <details className="card group open:pb-4">
            <summary className="flex cursor-pointer items-center justify-between gap-4 px-5 py-4 text-left text-[17px] font-medium">
              {t.helpTitle}
              <ChevronDown className="h-5 w-5 shrink-0 text-muted transition group-open:rotate-180" strokeWidth={SW} aria-hidden />
            </summary>
            <div className="border-t border-line px-5 pt-4">
              <p className="leading-relaxed text-muted">{t.helpIntro}</p>
              <ol className="mt-4 space-y-3">
                {t.helpSteps.map((h, i) => (
                  <li key={i} className="flex gap-3">
                    <span className="mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-accent-soft text-[13px] font-medium text-hain-ink">{i + 1}</span>
                    <span className="leading-relaxed">{h}</span>
                  </li>
                ))}
              </ol>
              <p className="mt-4 rounded-[12px] bg-surface p-4 text-[15px] leading-relaxed text-muted">{t.helpNote}</p>
            </div>
          </details>
        </Reveal>
      </Container>
    </section>
  )
}

/* ---------- Screenshots ---------- */
export function Screens({ lang }: { lang: Lang }) {
  const t = COPY[lang].shots
  return (
    <section id="screens" className="relative overflow-hidden border-t border-line py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
      </Container>
      <div className="no-scrollbar mt-14 flex snap-x snap-mandatory gap-5 overflow-x-auto px-[max(1.25rem,calc((100vw-72rem)/2+2rem))] pb-6 pt-2">
        {SCREENSHOTS.map((id, i) => (
          <Reveal key={id} delay={(i % 3) * 0.06} className="snap-center">
            <figure className="w-[210px] shrink-0 sm:w-[230px]">
              <Shot id={id} alt={t.items[i]} eager />
              <figcaption className="mt-3 text-center text-[15px] text-muted">{t.items[i]}</figcaption>
            </figure>
          </Reveal>
        ))}
      </div>
    </section>
  )
}

/* ---------- Privacy ---------- */
export function Privacy({ lang }: { lang: Lang }) {
  const t = COPY[lang].privacy
  const logIcons: LucideIcon[] = [CloudSun, BookOpen, Download]
  return (
    <section id="privacy" className="relative border-t border-line py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
        <div className="mt-14 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          <Reveal>
            <div className="card h-full p-6">
              <IconTile>
                <Smartphone className="h-5 w-5" strokeWidth={SW} aria-hidden />
              </IconTile>
              <h3 className="mt-5 text-lg font-medium">{t.stays.title}</h3>
              <ul className="mt-4 space-y-3">
                {t.stays.items.map((x) => (
                  <li key={x} className="flex gap-3">
                    <Check className="mt-0.5 h-5 w-5 shrink-0 text-hain" strokeWidth={SW} aria-hidden /> <span>{x}</span>
                  </li>
                ))}
              </ul>
            </div>
          </Reveal>
          <Reveal delay={0.06}>
            <div className="card h-full p-6">
              <IconTile muted>
                <Globe className="h-5 w-5" strokeWidth={SW} aria-hidden />
              </IconTile>
              <h3 className="mt-5 text-lg font-medium">{t.online.title}</h3>
              <ul className="mt-4 space-y-3">
                {t.online.items.map((x) => (
                  <li key={x} className="flex gap-3">
                    <ArrowUpRight className="mt-0.5 h-5 w-5 shrink-0 text-online" strokeWidth={SW} aria-hidden /> <span>{x}</span>
                  </li>
                ))}
              </ul>
            </div>
          </Reveal>
          <Reveal delay={0.12} className="md:col-span-2 lg:col-span-1">
            <div className="card h-full p-6">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-medium">{t.logTitle}</h3>
                <span className="badge badge-soon">{t.logExample}</span>
              </div>
              <ul className="mt-4 divide-y divide-line border-y border-line">
                {t.log.map((l, i) => {
                  const I = logIcons[i]
                  return (
                    <li key={l.where} className="flex items-center gap-3 py-3">
                      <I className="h-5 w-5 shrink-0 text-muted" strokeWidth={SW} aria-hidden />
                      <span className="min-w-0 flex-1">
                        <span className="block truncate text-[15px]">{l.what}</span>
                        <span className="block truncate text-sm text-muted">{l.where}</span>
                      </span>
                      <span className="h-1.5 w-1.5 shrink-0 rounded-full bg-online" aria-hidden />
                    </li>
                  )
                })}
              </ul>
              <p className="mt-4 text-sm leading-relaxed text-muted">{t.never}</p>
            </div>
          </Reveal>
        </div>
      </Container>
    </section>
  )
}

/* ---------- Compare ---------- */
export function Compare({ lang }: { lang: Lang }) {
  const t = COPY[lang].compare
  return (
    <section id="compare" className="relative border-t border-line py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
        <Reveal className="mx-auto mt-14 max-w-4xl">
          <div className="card overflow-hidden">
            <div className="hidden grid-cols-[1.2fr_1fr_1fr] items-center border-b border-line bg-surface/60 px-6 py-3.5 text-sm font-medium text-muted sm:grid">
              <span />
              <span className="flex items-center gap-2 text-ink">
                <AppIcon size={22} /> {t.colHain}
              </span>
              <span>{t.colCloud}</span>
            </div>
            <ul>
              {t.rows.map((r) => (
                <li key={r.label} className="grid grid-cols-2 gap-x-4 gap-y-2 border-b border-line px-5 py-4 last:border-0 sm:grid-cols-[1.2fr_1fr_1fr] sm:items-center sm:px-6">
                  <span className="col-span-2 font-medium sm:col-span-1">{r.label}</span>
                  <Cell good={r.good === 'hain'} text={r.hain} label={t.colHain} />
                  <Cell good={r.good === 'cloud'} text={r.cloud} label={t.colCloud} />
                </li>
              ))}
            </ul>
          </div>
          <p className="mx-auto mt-5 max-w-2xl text-center text-[15px] leading-relaxed text-muted">{t.note}</p>
        </Reveal>
      </Container>
    </section>
  )
}

function Cell({ good, text, label }: { good: boolean; text: string; label: string }) {
  return (
    <span className="flex items-start gap-2 text-[15px]">
      <span className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full ${good ? 'bg-accent-soft text-hain-ink' : 'bg-surface-high text-muted'}`}>
        {good ? <Check className="h-3.5 w-3.5" strokeWidth={2.5} aria-hidden /> : <X className="h-3 w-3" strokeWidth={2.5} aria-hidden />}
      </span>
      <span>
        <span className="block text-xs text-muted sm:hidden">{label}</span>
        {text}
      </span>
    </span>
  )
}

/* ---------- Roadmap ---------- */
export function Roadmap({ lang }: { lang: Lang }) {
  const t = COPY[lang].roadmap
  return (
    <section id="roadmap" className="relative border-t border-line py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
        <ol className="relative mx-auto mt-14 max-w-2xl space-y-4 pl-8">
          <span className="absolute bottom-3 left-[7px] top-3 w-px bg-line" aria-hidden />
          {t.items.map((it, i) => (
            <Reveal as="li" key={it.tag} delay={i * 0.08} className="relative">
              <span className={`absolute -left-8 top-6 h-[15px] w-[15px] rounded-full border-2 border-bg ${it.now ? 'bg-hain ring-4 ring-accent-soft' : 'bg-surface-high'}`} aria-hidden />
              <div className="card p-5">
                <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                  <span className="text-xl font-medium tracking-tight">{it.tag}</span>
                  <span className={`badge ${it.now ? 'badge-ready' : 'badge-soon'}`}>{it.when}</span>
                </div>
                <p className="mt-2 leading-relaxed text-muted">{it.text}</p>
              </div>
            </Reveal>
          ))}
        </ol>
        <Reveal className="mt-8 text-center text-[15px] text-muted">
          {t.note}{' '}
          <a href={LINKS.roadmap} className="font-medium text-hain-ink underline decoration-line underline-offset-4 hover:decoration-hain">
            {t.link}
          </a>
        </Reveal>
      </Container>
    </section>
  )
}

/* ---------- FAQ ---------- */
export function Faq({ lang }: { lang: Lang }) {
  const t = COPY[lang].faq
  return (
    <section id="faq" className="relative border-t border-line py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} />
        <Reveal className="card mx-auto mt-12 max-w-3xl divide-y divide-line overflow-hidden">
          {t.items.map((f) => (
            <details key={f.q} className="group">
              <summary className="flex cursor-pointer items-center justify-between gap-4 px-5 py-4 text-left text-[17px] font-medium hover:bg-surface/60 sm:px-6">
                {f.q}
                <ChevronDown className="h-5 w-5 shrink-0 text-muted transition group-open:rotate-180" strokeWidth={SW} aria-hidden />
              </summary>
              <p className="px-5 pb-5 leading-relaxed text-muted sm:px-6">{f.a}</p>
            </details>
          ))}
        </Reveal>
      </Container>
    </section>
  )
}

/* ---------- Join ---------- */
const JOIN_ICONS: LucideIcon[] = [Bug, Languages, CodeXml]

export function Join({ lang }: { lang: Lang }) {
  const t = COPY[lang].join
  const ai = COPY[lang].ai
  return (
    <section id="join" className="relative border-t border-line py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
        <ul className="mt-14 grid grid-cols-1 gap-4 md:grid-cols-3">
          {t.items.map((it, i) => {
            const Icon = JOIN_ICONS[i]
            return (
              <Reveal as="li" key={it.title} delay={i * 0.06}>
                <a href={it.href} className="card card-hover group flex h-full flex-col p-6">
                  <IconTile>
                    <Icon className="h-5 w-5" strokeWidth={SW} aria-hidden />
                  </IconTile>
                  <h3 className="mt-5 text-lg font-medium">{it.title}</h3>
                  <p className="mt-2 flex-1 leading-relaxed text-muted">{it.text}</p>
                  <span className="mt-5 inline-flex items-center gap-1.5 font-medium text-hain-ink">
                    {it.cta} <ArrowUpRight className="h-4 w-4 transition group-hover:-translate-y-0.5 group-hover:translate-x-0.5" strokeWidth={SW} aria-hidden />
                  </span>
                </a>
              </Reveal>
            )
          })}
        </ul>
        <Reveal className="mx-auto mt-10 max-w-3xl">
          <div className="flex gap-4 rounded-[16px] border border-line bg-surface/60 p-5">
            <Sparkles className="mt-0.5 h-5 w-5 shrink-0 text-muted" strokeWidth={SW} aria-hidden />
            <p className="leading-relaxed text-muted">
              <strong className="font-medium text-ink">{ai.title}: </strong>
              {ai.text}
            </p>
          </div>
        </Reveal>
      </Container>
    </section>
  )
}

/* ---------- Final CTA + Footer ---------- */
export function FinalCta({ lang }: { lang: Lang }) {
  const t = COPY[lang]
  return (
    <section className="relative overflow-hidden border-t border-line py-24 sm:py-32">
      <div className="mist opacity-70" aria-hidden />
      <Container className="relative flex flex-col items-center text-center">
        <Reveal>
          <AppIcon size={104} className="float drop-shadow-[0_18px_24px_rgba(11,122,79,.28)]" />
        </Reveal>
        <Reveal delay={0.08}>
          <h2 className="mt-8 text-balance text-4xl font-light tracking-tight sm:text-6xl">{t.footer.tagline}</h2>
        </Reveal>
        <Reveal delay={0.16} className="mt-9 flex w-full max-w-md flex-col gap-3 sm:w-auto sm:max-w-none sm:flex-row">
          <a href={LINKS.download} className="btn btn-primary !px-6 !py-3.5 !text-base">
            <Download className="h-5 w-5" strokeWidth={SW} aria-hidden /> {t.hero.cta}
          </a>
          <a href={LINKS.repo} className="btn btn-ghost !px-6 !py-3.5 !text-base">
            <GitHubIcon className="h-5 w-5" /> GitHub
          </a>
        </Reveal>
        <p className="mt-5 text-sm text-muted">{t.hero.note}</p>
      </Container>
    </section>
  )
}

export function Footer({ lang }: { lang: Lang }) {
  const t = COPY[lang].footer
  return (
    <footer className="border-t border-line py-12">
      <Container className="flex flex-col gap-8 md:flex-row md:items-start md:justify-between">
        <div className="max-w-sm">
          <div className="flex items-center gap-2.5">
            <AppIcon size={32} />
            <span className="text-lg font-medium">Hain</span>
          </div>
          <p className="mt-3 text-muted">{t.tagline}</p>
          <p className="mt-3 text-sm text-muted">{t.noCookies}</p>
        </div>
        <ul className="grid grid-cols-2 gap-x-10 gap-y-3 text-[15px] sm:grid-cols-3">
          <li><a className="footer-link" href={LINKS.repo}>{t.source}</a></li>
          <li><a className="footer-link" href={LINKS.license}>{t.license}</a></li>
          <li><a className="footer-link" href={LINKS.issues}>{t.issues}</a></li>
          <li><a className="footer-link" href={LINKS.concept}>{t.concept}</a></li>
          <li><a className="footer-link" href={LINKS.trademarks}>{t.trademarks}</a></li>
          {/* PLACEHOLDER: link the imprint (Impressum) page here once it exists */}
          <li><span className="text-muted" data-placeholder="imprint">{t.imprintNote}</span></li>
          {/* Support / donations: not live yet, so no link on purpose */}
          <li className="col-span-2 sm:col-span-3">
            <span className="inline-flex items-center gap-2 text-muted" data-placeholder="support">
              <Heart className="h-4 w-4" strokeWidth={1.75} aria-hidden /> {t.support}
              <span className="badge badge-soon">{t.soon}</span>
            </span>
          </li>
        </ul>
      </Container>
      <Container className="mt-10 text-sm text-muted">© 2026 Hain · Oskar</Container>
    </footer>
  )
}
