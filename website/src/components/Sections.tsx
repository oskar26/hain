import type React from 'react'
import {
  ShieldCheck, WifiOff, PiggyBank, Leaf, MessageCircle, Lock, Calculator, Newspaper, Brain, EyeOff, SlidersHorizontal, Trees,
  Download, Smartphone, Sparkles, Check, X, ChevronDown, ArrowUpRight, Bug, Languages, CodeXml, Globe, CloudSun, BookOpen,
} from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { COPY, LINKS, SCREENSHOTS, type Lang, type Status } from '../content'
import { BASE, Container, GitHubIcon, Pim3D, Reveal, SectionHead, Tilt, img } from './ui'

/* ---------- Marquee ---------- */
export function Marquee({ lang }: { lang: Lang }) {
  const items = COPY[lang].marquee
  const row = (hidden: boolean) => (
    <ul className="flex shrink-0 items-center gap-3 pr-3" aria-hidden={hidden || undefined}>
      {items.map((it) => (
        <li key={it} className="flex items-center gap-2 whitespace-nowrap rounded-full border border-line bg-white px-4 py-2 text-[15px] font-medium text-ink shadow-[0_6px_20px_-14px_rgba(13,40,28,.4)]">
          <Check className="h-4 w-4 text-hain" aria-hidden /> {it}
        </li>
      ))}
    </ul>
  )
  return (
    <div className="marquee relative overflow-hidden py-6">
      <div className="marquee-track flex w-max">
        {row(false)}
        {row(true)}
      </div>
    </div>
  )
}

/* ---------- Why ---------- */
const WHY_STYLE: { icon: LucideIcon; glow: string; tint: string }[] = [
  { icon: ShieldCheck, glow: '#16a36b', tint: 'from-[#e3f8ee]' },
  { icon: WifiOff, glow: '#5fd3cf', tint: 'from-[#e2f7f6]' },
  { icon: PiggyBank, glow: '#f2cf4a', tint: 'from-[#fbf5dc]' },
  { icon: Leaf, glow: '#7fd6a4', tint: 'from-[#e8f8ee]' },
]

export function Why({ lang }: { lang: Lang }) {
  const t = COPY[lang].why
  return (
    <section id="why" className="relative py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
        <ul className="mt-14 grid grid-cols-1 gap-5 sm:grid-cols-2 lg:grid-cols-4">
          {t.items.map((it, i) => {
            const S = WHY_STYLE[i]
            return (
              <Reveal as="li" key={it.title} delay={i * 0.08}>
                <div className={`glass spot group relative h-full overflow-hidden rounded-[28px] bg-gradient-to-b ${S.tint} to-white p-6 transition duration-300 hover:-translate-y-1`}>
                  <span className="glow -bottom-16 left-1/2 h-40 w-40 -translate-x-1/2 opacity-50 transition group-hover:opacity-80" style={{ '--c': S.glow } as React.CSSProperties} />
                  <IconOrb icon={S.icon} color={S.glow} />
                  <h3 className="relative mt-5 text-xl font-semibold">{it.title}</h3>
                  <p className="relative mt-2 leading-relaxed text-muted">{it.text}</p>
                </div>
              </Reveal>
            )
          })}
        </ul>
      </Container>
    </section>
  )
}

function IconOrb({ icon: Icon, color, size = 'md' }: { icon: LucideIcon; color: string; size?: 'md' | 'sm' }) {
  const s = size === 'md' ? 'h-12 w-12' : 'h-10 w-10'
  return (
    <span
      className={`relative inline-flex ${s} items-center justify-center rounded-2xl text-white`}
      style={{
        background: `radial-gradient(circle at 30% 25%, rgba(255,255,255,.75), transparent 45%), linear-gradient(160deg, ${color}, color-mix(in srgb, ${color} 70%, #0d3b28))`,
        boxShadow: `0 10px 24px -10px ${color}, inset 0 1px 0 rgba(255,255,255,.6), inset 0 -4px 10px rgba(0,0,0,.12)`,
      }}
    >
      <Icon className={size === 'md' ? 'h-6 w-6' : 'h-5 w-5'} strokeWidth={2.2} aria-hidden />
    </span>
  )
}

/* ---------- Features (bento) ---------- */
const STATUS_STYLE: Record<Status, string> = {
  ready: 'bg-hain text-white',
  progress: 'bg-[#fff1c2] text-[#6b4e00]',
  next: 'bg-mint-light text-hain-dark',
  later: 'bg-surface text-muted',
}
const FEATURE_STYLE: { icon: LucideIcon; color: string; span: string }[] = [
  { icon: MessageCircle, color: '#16a36b', span: 'md:col-span-2 lg:col-span-4 lg:row-span-2' },
  { icon: Lock, color: '#3fae8a', span: 'lg:col-span-2' },
  { icon: Calculator, color: '#5fc9d3', span: 'lg:col-span-2' },
  { icon: Newspaper, color: '#e8833a', span: 'lg:col-span-2' },
  { icon: Brain, color: '#9b8cf0', span: 'lg:col-span-2' },
  { icon: EyeOff, color: '#4a7f6a', span: 'lg:col-span-2' },
  { icon: SlidersHorizontal, color: '#e3b52d', span: 'lg:col-span-2' },
  { icon: Trees, color: '#2f9e5f', span: 'md:col-span-2 lg:col-span-4' },
]

function StatusChip({ s, label }: { s: Status; label: string }) {
  return <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${STATUS_STYLE[s]}`}>{label}</span>
}

export function Features({ lang }: { lang: Lang }) {
  const t = COPY[lang].features
  const st = COPY[lang].status
  const pimAlt = COPY[lang].hero.pimAlt
  return (
    <section id="features" className="relative py-20 sm:py-28">
      <div className="pointer-events-none absolute inset-x-0 top-1/3 -z-10 h-[60%] bg-[radial-gradient(60%_50%_at_50%_50%,rgba(127,214,164,.18),transparent)]" aria-hidden />
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
        <ul className="mt-14 grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-6">
          {t.items.map((it, i) => {
            const S = FEATURE_STYLE[i]
            const big = i === 0
            const wide = i === 7
            return (
              <Reveal as="li" key={it.title} delay={(i % 3) * 0.08} className={`min-w-0 ${S.span}`}>
                <div className="glass spot group relative flex h-full flex-col overflow-hidden rounded-[28px] p-6 sm:p-7">
                  <span className="glow -right-10 -top-10 h-36 w-36 opacity-25 transition duration-500 group-hover:opacity-50" style={{ '--c': S.color } as React.CSSProperties} />
                  <div className="relative flex items-start justify-between gap-3">
                    <IconOrb icon={S.icon} color={S.color} size="sm" />
                    <StatusChip s={it.status} label={st[it.status]} />
                  </div>
                  <h3 className={`relative mt-5 font-semibold ${big ? 'text-2xl sm:text-3xl' : 'text-xl'}`}>{it.title}</h3>
                  <p className={`relative mt-2 leading-relaxed text-muted ${big ? 'max-w-md text-lg' : ''}`}>{it.text}</p>
                  {big && <ChatDemo you={t.chatDemo.you} pim={t.chatDemo.pim} pimAlt={pimAlt} />}
                  {wide && <ForestRow />}
                </div>
              </Reveal>
            )
          })}
        </ul>
      </Container>
    </section>
  )
}

function ChatDemo({ you, pim, pimAlt }: { you: string; pim: string; pimAlt: string }) {
  return (
    <div className="relative mt-8 flex flex-1 flex-col justify-end gap-3 rounded-3xl border border-line/80 bg-white/80 p-4 sm:p-5" aria-hidden>
      <div className="ml-auto max-w-[85%] rounded-[20px] rounded-br-md bg-surface px-4 py-2.5 text-[15px]" data-reveal style={{ '--d': '0.2s' } as React.CSSProperties}>
        {you}
      </div>
      <div className="flex items-end gap-3">
        <div className="w-10 shrink-0">
          <Pim3D alt={pimAlt} />
        </div>
        <p className="max-w-[85%] text-[15px] leading-relaxed text-ink" data-reveal style={{ '--d': '0.7s' } as React.CSSProperties}>
          {pim}
          <span className="ml-1 inline-block h-4 w-1.5 translate-y-0.5 animate-pulse rounded-full bg-hain" />
        </p>
      </div>
      <div className="mt-2 flex items-center gap-2 rounded-full border border-line bg-white px-4 py-2.5 text-sm text-muted shadow-[0_0_0_4px_rgba(127,214,164,.15)]">
        <span className="flex-1 truncate">…</span>
        <span className="flex h-8 w-8 items-center justify-center rounded-full bg-hain text-white">
          <ArrowUpRight className="h-4 w-4 -rotate-45" />
        </span>
      </div>
    </div>
  )
}

function ForestRow() {
  const trees = [
    { h: 54, c: '#7fd6a4' }, { h: 78, c: '#2fb37a' }, { h: 46, c: '#5fd3cf' }, { h: 92, c: '#16a36b' }, { h: 62, c: '#9be3b9' },
    { h: 70, c: '#2fb37a' }, { h: 50, c: '#f2cf4a' }, { h: 84, c: '#16a36b' }, { h: 58, c: '#7fd6a4' },
  ]
  return (
    <div className="relative mt-6 flex h-32 items-end justify-center gap-3 overflow-hidden sm:gap-5" aria-hidden>
      {trees.map((t, i) => (
        <span key={i} className={`bob flex-col items-center ${i > 4 ? 'hidden sm:flex' : 'flex'}`} style={{ animationDelay: `${-i * 0.7}s` }}>
          <span
            className="block rounded-full"
            style={{
              width: `${t.h * 0.62}px`,
              height: `${t.h * 0.62}px`,
              background: `radial-gradient(circle at 32% 28%, rgba(255,255,255,.8), transparent 38%), radial-gradient(circle at 50% 60%, ${t.c}, color-mix(in srgb, ${t.c} 65%, #0b5a3a))`,
              boxShadow: `0 14px 22px -12px ${t.c}, inset 0 -6px 12px rgba(0,0,0,.12)`,
            }}
          />
          <span className="block w-1.5 rounded-b-full bg-[#7a5a3c]/70" style={{ height: `${t.h * 0.38}px` }} />
        </span>
      ))}
      <span className="absolute inset-x-6 bottom-0 h-2 rounded-full bg-[radial-gradient(closest-side,rgba(22,163,107,.25),transparent)]" />
    </div>
  )
}

/* ---------- Install ---------- */
const STEP_ICONS: LucideIcon[] = [Download, Smartphone, Sparkles]

export function Install({ lang }: { lang: Lang }) {
  const t = COPY[lang].how
  return (
    <section id="install" className="relative py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
        <ol className="mt-14 grid grid-cols-1 gap-5 md:grid-cols-3">
          {t.steps.map((s, i) => {
            const Icon = STEP_ICONS[i]
            return (
              <Reveal as="li" key={s.title} delay={i * 0.1}>
                <div className="glass relative h-full overflow-hidden rounded-[28px] p-7">
                  <span className="pointer-events-none absolute -right-2 -top-6 select-none text-[120px] leading-none font-bold text-mint-light/70" aria-hidden>
                    {i + 1}
                  </span>
                  <IconOrb icon={Icon} color="#16a36b" />
                  <h3 className="relative mt-5 text-xl font-semibold">
                    <span className="sr-only">{i + 1}. </span>
                    {s.title}
                  </h3>
                  <p className="relative mt-2 leading-relaxed text-muted">{s.text}</p>
                </div>
              </Reveal>
            )
          })}
        </ol>
        <Reveal className="mt-8 flex flex-col items-center gap-3 text-center">
          <a href={LINKS.download} className="pill-dark !px-7 !py-4 !text-base">
            <Download className="h-5 w-5" aria-hidden /> {COPY[lang].hero.cta}
          </a>
          <p className="text-sm text-muted">{t.req}</p>
        </Reveal>
        <Reveal className="mx-auto mt-10 max-w-3xl">
          <details className="glass group rounded-[24px] p-1 open:pb-4">
            <summary className="flex cursor-pointer items-center justify-between gap-4 rounded-[20px] px-5 py-4 text-left text-lg font-semibold">
              {t.helpTitle}
              <ChevronDown className="h-5 w-5 shrink-0 text-hain transition group-open:rotate-180" aria-hidden />
            </summary>
            <div className="px-5">
              <p className="leading-relaxed text-muted">{t.helpIntro}</p>
              <ol className="mt-4 space-y-3">
                {t.helpSteps.map((h, i) => (
                  <li key={i} className="flex gap-3">
                    <span className="mt-0.5 flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-mint-light text-sm font-bold text-hain-dark">{i + 1}</span>
                    <span className="leading-relaxed">{h}</span>
                  </li>
                ))}
              </ol>
              <p className="mt-4 rounded-2xl bg-surface p-4 text-[15px] leading-relaxed text-muted">{t.helpNote}</p>
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
    <section id="screens" className="relative overflow-hidden py-20 sm:py-28">
      <div className="pointer-events-none absolute inset-0 -z-10 bg-gradient-to-b from-white via-[#f1fbf5] to-white" aria-hidden />
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
      </Container>
      <div className="no-scrollbar mt-14 flex snap-x snap-mandatory gap-6 overflow-x-auto px-[max(1.25rem,calc((100vw-72rem)/2+2rem))] pb-8 pt-2 lg:justify-center">
        {SCREENSHOTS.map((s, i) => {
          const file = lang === 'de' ? (s.de ?? s.file) : s.file
          return (
            <Reveal key={s.id} delay={i * 0.08} className="snap-center">
              <Tilt className="w-[230px] shrink-0 sm:w-[250px]">
                <figure>
                  <div className="relative aspect-[9/19.5] rounded-[42px] bg-[#101412] p-[10px] shadow-[0_40px_70px_-30px_rgba(13,40,28,.55),inset_0_0_0_2px_#2a302c]">
                    <div className="absolute left-1/2 top-[18px] z-10 h-[18px] w-[70px] -translate-x-1/2 rounded-full bg-black" aria-hidden />
                    <div className="relative h-full w-full overflow-hidden rounded-[33px] bg-white">
                      {file ? (
                        <img src={`${BASE}screenshots/${file}`} alt={t.items[i]} loading="lazy" className="h-full w-full object-cover" />
                      ) : (
                        <Placeholder label={t.placeholder} i={i} />
                      )}
                    </div>
                  </div>
                  <figcaption className="mt-4 text-center text-[15px] font-medium text-muted">{t.items[i]}</figcaption>
                </figure>
              </Tilt>
            </Reveal>
          )
        })}
      </div>
    </section>
  )
}

function Placeholder({ label, i }: { label: string; i: number }) {
  const tints = ['#e3f8ee', '#e2f7f6', '#fbf5dc', '#eef0ff']
  return (
    <div className="flex h-full w-full flex-col items-center justify-center gap-4 p-6 text-center" style={{ background: `linear-gradient(180deg, #fff, ${tints[i % 4]})` }} data-placeholder="screenshot">
      <img src={img('pim-3d.webp')} alt="" width={90} height={88} className="w-[84px] opacity-90" loading="lazy" />
      <span className="rounded-full border border-dashed border-hain/50 bg-white/80 px-3 py-1.5 text-xs font-semibold uppercase tracking-wide text-hain-dark">{label}</span>
    </div>
  )
}

/* ---------- Privacy ---------- */
export function Privacy({ lang }: { lang: Lang }) {
  const t = COPY[lang].privacy
  const logIcons: LucideIcon[] = [CloudSun, BookOpen, Download]
  return (
    <section id="privacy" className="relative py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
        <div className="mt-14 grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
          <Reveal>
            <div className="glass relative h-full overflow-hidden rounded-[28px] p-7">
              <span className="glow -bottom-14 -left-10 h-40 w-40 opacity-30" style={{ '--c': '#16a36b' } as React.CSSProperties} />
              <IconOrb icon={Smartphone} color="#16a36b" />
              <h3 className="relative mt-5 text-xl font-semibold">{t.stays.title}</h3>
              <ul className="relative mt-4 space-y-3">
                {t.stays.items.map((x) => (
                  <li key={x} className="flex gap-3">
                    <Check className="mt-0.5 h-5 w-5 shrink-0 text-hain" aria-hidden /> <span>{x}</span>
                  </li>
                ))}
              </ul>
            </div>
          </Reveal>
          <Reveal delay={0.08}>
            <div className="glass relative h-full overflow-hidden rounded-[28px] p-7">
              <span className="glow -bottom-14 -right-10 h-40 w-40 opacity-25" style={{ '--c': '#e8833a' } as React.CSSProperties} />
              <IconOrb icon={Globe} color="#e8833a" />
              <h3 className="relative mt-5 text-xl font-semibold">{t.online.title}</h3>
              <ul className="relative mt-4 space-y-3">
                {t.online.items.map((x) => (
                  <li key={x} className="flex gap-3">
                    <ArrowUpRight className="mt-0.5 h-5 w-5 shrink-0 text-[#c86a24]" aria-hidden /> <span>{x}</span>
                  </li>
                ))}
              </ul>
            </div>
          </Reveal>
          <Reveal delay={0.16} className="md:col-span-2 lg:col-span-1">
            <div className="relative h-full overflow-hidden rounded-[28px] bg-[#0f1311] p-7 text-white shadow-[0_30px_60px_-30px_rgba(13,40,28,.6)]">
              <span className="glow -top-16 right-0 h-48 w-48 opacity-40" style={{ '--c': '#16a36b' } as React.CSSProperties} />
              <div className="relative flex items-center justify-between">
                <h3 className="text-xl font-semibold">{t.logTitle}</h3>
                <span className="rounded-full bg-white/10 px-2.5 py-1 text-xs font-medium text-white/80">{t.logExample}</span>
              </div>
              <ul className="relative mt-5 space-y-3">
                {t.log.map((l, i) => {
                  const I = logIcons[i]
                  return (
                    <li key={l.where} className="flex items-center gap-3 rounded-2xl bg-white/[.06] p-3">
                      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-white/10">
                        <I className="h-5 w-5 text-mint" aria-hidden />
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block truncate text-[15px] font-medium">{l.what}</span>
                        <span className="block truncate text-sm text-white/65">→ {l.where}</span>
                      </span>
                      <span className="h-2 w-2 shrink-0 animate-pulse rounded-full bg-[#f0964f]" aria-hidden />
                    </li>
                  )
                })}
              </ul>
              <p className="relative mt-5 text-sm leading-relaxed text-white/75">{t.never}</p>
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
    <section id="compare" className="relative py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
        <Reveal className="mx-auto mt-14 max-w-4xl">
          <div className="glass overflow-hidden rounded-[28px]">
            <div className="hidden grid-cols-[1.2fr_1fr_1fr] items-center border-b border-line/80 px-6 py-4 text-sm font-semibold text-muted sm:grid">
              <span />
              <span className="flex items-center gap-2 text-hain-dark">
                <img src={img('hain-icon.webp')} alt="" width={24} height={24} className="h-6 w-6" /> {t.colHain}
              </span>
              <span>{t.colCloud}</span>
            </div>
            <ul>
              {t.rows.map((r) => (
                <li key={r.label} className="grid grid-cols-2 gap-x-4 gap-y-2 border-b border-line/60 px-5 py-4 last:border-0 sm:grid-cols-[1.2fr_1fr_1fr] sm:items-center sm:px-6">
                  <span className="col-span-2 font-semibold sm:col-span-1">{r.label}</span>
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
      <span className={`mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full ${good ? 'bg-hain text-white' : 'bg-surface text-muted'}`}>
        {good ? <Check className="h-3.5 w-3.5" strokeWidth={3} aria-hidden /> : <X className="h-3 w-3" strokeWidth={3} aria-hidden />}
      </span>
      <span>
        <span className="block text-xs font-semibold text-muted sm:hidden">{label}</span>
        {text}
      </span>
    </span>
  )
}

/* ---------- Roadmap ---------- */
export function Roadmap({ lang }: { lang: Lang }) {
  const t = COPY[lang].roadmap
  return (
    <section id="roadmap" className="relative py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
        <ol data-progress-wrap className="relative mx-auto mt-14 max-w-2xl space-y-6 pl-10 sm:pl-14">
          <span className="absolute bottom-2 left-[15px] top-2 w-[3px] rounded-full bg-line sm:left-[23px]" aria-hidden />
          <span data-progress className="absolute bottom-2 left-[15px] top-2 w-[3px] origin-top rounded-full bg-gradient-to-b from-hain via-turq to-mint sm:left-[23px]" aria-hidden />
          {t.items.map((it, i) => (
            <Reveal as="li" key={it.tag} delay={i * 0.1} className="relative">
              <span className={`absolute -left-10 top-6 flex h-[33px] w-[33px] items-center justify-center rounded-full border-4 border-white sm:-left-14 sm:h-[49px] sm:w-[49px] ${it.now ? 'bg-hain shadow-[0_0_0_6px_rgba(22,163,107,.18)]' : 'bg-mint-light'}`} aria-hidden>
                {it.now && <span className="h-2.5 w-2.5 animate-ping rounded-full bg-white" />}
              </span>
              <div className="glass rounded-[24px] p-6">
                <div className="flex flex-wrap items-baseline gap-x-3 gap-y-1">
                  <span className="text-2xl font-semibold tracking-tight">{it.tag}</span>
                  <span className={`rounded-full px-2.5 py-1 text-xs font-semibold ${it.now ? 'bg-hain text-white' : 'bg-surface text-muted'}`}>{it.when}</span>
                </div>
                <p className="mt-2 leading-relaxed text-muted">{it.text}</p>
              </div>
            </Reveal>
          ))}
        </ol>
        <Reveal className="mt-8 text-center text-[15px] text-muted">
          {t.note}{' '}
          <a href={LINKS.roadmap} className="font-semibold text-hain-dark underline decoration-hain/40 underline-offset-4 hover:decoration-hain">
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
    <section id="faq" className="relative py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} />
        <Reveal className="mx-auto mt-12 max-w-3xl space-y-3">
          {t.items.map((f) => (
            <details key={f.q} className="glass group rounded-[22px]">
              <summary className="flex cursor-pointer items-center justify-between gap-4 px-6 py-5 text-left text-[17px] font-semibold">
                {f.q}
                <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-mint-light text-hain-dark transition group-open:rotate-180">
                  <ChevronDown className="h-4 w-4" aria-hidden />
                </span>
              </summary>
              <p className="px-6 pb-6 leading-relaxed text-muted">{f.a}</p>
            </details>
          ))}
        </Reveal>
      </Container>
    </section>
  )
}

/* ---------- Join ---------- */
const JOIN_ICONS: { icon: LucideIcon; color: string }[] = [
  { icon: Bug, color: '#e8833a' },
  { icon: Languages, color: '#5fc9d3' },
  { icon: CodeXml, color: '#16a36b' },
]

export function Join({ lang }: { lang: Lang }) {
  const t = COPY[lang].join
  const ai = COPY[lang].ai
  return (
    <section id="join" className="relative py-20 sm:py-28">
      <Container>
        <SectionHead kicker={t.kicker} title={t.title} sub={t.sub} />
        <ul className="mt-14 grid grid-cols-1 gap-5 md:grid-cols-3">
          {t.items.map((it, i) => (
            <Reveal as="li" key={it.title} delay={i * 0.08}>
              <a href={it.href} className="glass spot group relative flex h-full flex-col overflow-hidden rounded-[28px] p-7 transition hover:-translate-y-1">
                <span className="glow -bottom-12 -right-8 h-36 w-36 opacity-30 transition group-hover:opacity-60" style={{ '--c': JOIN_ICONS[i].color } as React.CSSProperties} />
                <IconOrb icon={JOIN_ICONS[i].icon} color={JOIN_ICONS[i].color} />
                <h3 className="relative mt-5 text-xl font-semibold">{it.title}</h3>
                <p className="relative mt-2 flex-1 leading-relaxed text-muted">{it.text}</p>
                <span className="relative mt-5 inline-flex items-center gap-1.5 font-semibold text-hain-dark">
                  {it.cta} <ArrowUpRight className="h-4 w-4 transition group-hover:-translate-y-0.5 group-hover:translate-x-0.5" aria-hidden />
                </span>
              </a>
            </Reveal>
          ))}
        </ul>
        <Reveal className="mx-auto mt-12 max-w-3xl">
          <div className="flex gap-4 rounded-[24px] border border-[#f3dfa0] bg-[#fffaea] p-5 sm:p-6">
            <Sparkles className="mt-0.5 h-6 w-6 shrink-0 text-[#b58600]" aria-hidden />
            <p className="leading-relaxed text-[#4d3d0a]">
              <strong className="font-semibold">{ai.title}: </strong>
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
    <section className="relative overflow-hidden py-24 sm:py-32">
      <div className="rise" aria-hidden>
        <span />
        <span />
        <span />
        <span />
      </div>
      <div className="pointer-events-none absolute inset-x-0 top-0 h-32 bg-gradient-to-b from-white to-transparent" aria-hidden />
      <Container className="relative flex flex-col items-center text-center">
        <Reveal>
          <img src={img('hain-icon.webp')} alt="" width={112} height={112} className="float mx-auto h-24 w-24 drop-shadow-[0_25px_30px_rgba(22,163,107,.35)] sm:h-28 sm:w-28" />
        </Reveal>
        <Reveal delay={0.1}>
          <h2 className="mt-8 text-balance text-4xl font-semibold tracking-tight sm:text-6xl">{t.footer.tagline}</h2>
        </Reveal>
        <Reveal delay={0.2} className="mt-9 flex w-full max-w-md flex-col gap-3 sm:w-auto sm:max-w-none sm:flex-row">
          <a href={LINKS.download} className="pill-dark !px-7 !py-4 !text-base">
            <Download className="h-5 w-5" aria-hidden /> {t.hero.cta}
          </a>
          <a href={LINKS.repo} className="pill-light !px-7 !py-4 !text-base">
            <GitHubIcon className="h-5 w-5" /> GitHub
          </a>
        </Reveal>
        <p className="mt-5 text-sm font-medium text-muted">{t.hero.note}</p>
      </Container>
    </section>
  )
}

export function Footer({ lang }: { lang: Lang }) {
  const t = COPY[lang].footer
  return (
    <footer className="border-t border-line bg-white py-12">
      <Container className="flex flex-col gap-8 md:flex-row md:items-start md:justify-between">
        <div className="max-w-sm">
          <div className="flex items-center gap-2.5">
            <img src={img('hain-icon.webp')} alt="" width={36} height={36} className="h-9 w-9" />
            <span className="text-lg font-semibold">Hain</span>
          </div>
          <p className="mt-3 text-muted">{t.tagline}</p>
          <p className="mt-3 text-sm text-muted">{t.noCookies}</p>
        </div>
        <ul className="grid grid-cols-2 gap-x-10 gap-y-3 text-[15px] sm:grid-cols-3">
          <li><a className="hover:text-hain-dark" href={LINKS.repo}>{t.source}</a></li>
          <li><a className="hover:text-hain-dark" href={LINKS.license}>{t.license}</a></li>
          <li><a className="hover:text-hain-dark" href={LINKS.issues}>Issues</a></li>
          <li><a className="hover:text-hain-dark" href={LINKS.concept}>{lang === 'de' ? 'Konzept' : 'Concept (German)'}</a></li>
          {/* PLACEHOLDER: link the imprint (Impressum) page here once it exists */}
          <li><span className="text-muted" data-placeholder="imprint">{t.imprintNote}</span></li>
          <li className="flex items-center gap-1.5 text-muted"><Leaf className="h-4 w-4 text-hain" aria-hidden /> {t.made}</li>
        </ul>
      </Container>
      <Container className="mt-10 text-sm text-muted">© 2026 Hain · Oskar</Container>
    </footer>
  )
}
