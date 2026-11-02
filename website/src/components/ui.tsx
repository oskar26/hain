import type { CSSProperties, ReactNode } from 'react'

// The page is rendered to static HTML at build time (React only runs during the
// build). Effects are added by the tiny client script in src/client.ts, which
// looks for these data attributes:
//   data-reveal        fades/lifts in when scrolled into view (delay via --d)
//   data-parallax="n"  moves with scroll, n = depth
//   data-tilt          3D tilt on mouse hover
//   .spot              pointer spotlight (sets --x / --y)
//   data-progress      roadmap line that fills while scrolling

export const BASE = import.meta.env.BASE_URL
export const img = (name: string) => `${BASE}img/${name}`

export function Reveal({ children, delay = 0, className = '', as = 'div' }: { children: ReactNode; delay?: number; className?: string; as?: 'div' | 'li' | 'p' }) {
  const Tag = as
  return (
    <Tag data-reveal className={className} style={{ '--d': `${delay}s` } as CSSProperties}>
      {children}
    </Tag>
  )
}

export function SectionHead({ kicker, title, sub, center = true }: { kicker: string; title: string; sub?: string; center?: boolean }) {
  return (
    <Reveal className={`mx-auto max-w-2xl ${center ? 'text-center' : ''}`}>
      <p className="mb-3 inline-flex items-center gap-2 rounded-full bg-mint-light/70 px-3 py-1 text-[13px] font-semibold tracking-wide text-hain-dark">
        <span className="h-1.5 w-1.5 rounded-full bg-hain" aria-hidden />
        {kicker}
      </p>
      <h2 className="text-balance text-[2rem] leading-[1.1] font-semibold tracking-tight sm:text-5xl">{title}</h2>
      {sub && <p className="mt-4 text-pretty text-lg leading-relaxed text-muted sm:text-xl">{sub}</p>}
    </Reveal>
  )
}

/** 3D tilt on hover (desktop pointers only, off with reduced motion). */
export function Tilt({ children, className = '' }: { children: ReactNode; className?: string }) {
  return (
    <div data-tilt className={`tilt ${className}`}>
      {children}
    </div>
  )
}

/** The 3D Pim with blinking eyelids. Eye positions match public/img/pim-3d.webp. */
export function Pim3D({ alt, className = '', eager = false }: { alt: string; className?: string; eager?: boolean }) {
  return (
    <div className={`relative ${className}`} style={{ aspectRatio: '502 / 494' }}>
      <img
        src={img('pim-3d.webp')}
        alt={alt}
        width={502}
        height={494}
        className="h-full w-full select-none object-contain drop-shadow-[0_30px_40px_rgba(22,163,107,0.35)]"
        draggable={false}
        loading={eager ? 'eager' : 'lazy'}
        fetchPriority={eager ? 'high' : 'auto'}
      />
      {/* eyelids */}
      <span aria-hidden className="lid absolute rounded-[50%]" style={{ left: '32.5%', top: '46%', width: '19%', height: '30%', background: 'radial-gradient(ellipse at 50% 30%, #8fdcb5, #74cfa3)' }} />
      <span aria-hidden className="lid absolute rounded-[50%]" style={{ left: '59%', top: '40%', width: '19.5%', height: '30%', background: 'radial-gradient(ellipse at 50% 30%, #8fdcb5, #74cfa3)', animationDelay: '0.04s' }} />
    </div>
  )
}

export function Container({ children, className = '' }: { children: ReactNode; className?: string }) {
  return <div className={`mx-auto w-full max-w-6xl px-5 sm:px-8 ${className}`}>{children}</div>
}

/** GitHub mark (lucide no longer ships brand icons). */
export function GitHubIcon({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 16 16" fill="currentColor" aria-hidden className={className}>
      <path d="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.013 8.013 0 0016 8c0-4.42-3.58-8-8-8z" />
    </svg>
  )
}
