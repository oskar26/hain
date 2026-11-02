// Small client script for the prerendered page (no React in the browser).
// Uses motion's tiny DOM helpers for scroll and in-view effects.
import { inView, scroll } from 'motion'
import './styles.css'

const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
const finePointer = window.matchMedia('(hover: hover) and (pointer: fine)').matches
document.documentElement.classList.add('js')

// Lazy section rendering (content-visibility) only for the first load.
const cvOff = () => document.documentElement.classList.add('cv-off')
document.addEventListener('click', (e) => {
  if ((e.target as Element).closest('a[href^="#"]')) cvOff()
}, { capture: true })
window.addEventListener('load', () => setTimeout(cvOff, 2500))
if (location.hash) cvOff()

// Reveal on scroll
document.querySelectorAll<HTMLElement>('[data-reveal]').forEach((el) => {
  if (reduce) return el.classList.add('in')
  inView(el, () => el.classList.add('in'), { margin: '0px 0px -60px 0px' })
})

// Header background after scrolling, mobile menu
const nav = document.querySelector<HTMLElement>('[data-nav]')
const onScroll = () => nav?.toggleAttribute('data-scrolled', window.scrollY > 12 || menuOpen())
const menu = document.getElementById('mobile-nav')
const toggle = document.querySelector<HTMLButtonElement>('[data-menu-toggle]')
const menuOpen = () => !!menu && !menu.hidden
function setMenu(open: boolean) {
  if (!menu || !toggle) return
  menu.hidden = !open
  toggle.setAttribute('aria-expanded', String(open))
  toggle.setAttribute('aria-label', (open ? toggle.dataset.labelClose : toggle.dataset.labelOpen) ?? '')
  onScroll()
}
toggle?.addEventListener('click', () => setMenu(!menuOpen()))
document.querySelectorAll('[data-menu-link]').forEach((a) => a.addEventListener('click', () => setMenu(false)))
document.addEventListener('keydown', (e) => e.key === 'Escape' && menuOpen() && setMenu(false))
window.addEventListener('scroll', onScroll, { passive: true })
onScroll()

if (!reduce) {
  // Parallax bubbles / Pim in the hero
  const layers = [...document.querySelectorAll<HTMLElement>('[data-parallax]')]
  if (layers.length) {
    scroll((p: number) => {
      const y = Math.min(window.scrollY, 900)
      for (const el of layers) el.style.transform = `translate3d(0, ${(-y * 0.2 * Number(el.dataset.parallax)).toFixed(1)}px, 0)`
      return p
    })
  }

  // Roadmap line fills while scrolling
  const wrap = document.querySelector<HTMLElement>('[data-progress-wrap]')
  const bar = document.querySelector<HTMLElement>('[data-progress]')
  if (wrap && bar) {
    bar.style.transform = 'scaleY(0)'
    scroll((p: number) => (bar.style.transform = `scaleY(${p.toFixed(3)})`), { target: wrap, offset: ['start 80%', 'end 50%'] })
  }

  if (finePointer) {
    // Spotlight that follows the mouse on cards
    document.addEventListener('pointermove', (e) => {
      const el = (e.target as Element).closest<HTMLElement>('.spot')
      if (!el) return
      const r = el.getBoundingClientRect()
      el.style.setProperty('--x', `${e.clientX - r.left}px`)
      el.style.setProperty('--y', `${e.clientY - r.top}px`)
    }, { passive: true })

    // 3D tilt on hover
    document.querySelectorAll<HTMLElement>('[data-tilt]').forEach((el) => {
      el.addEventListener('pointermove', (e) => {
        const r = el.getBoundingClientRect()
        const x = (e.clientX - r.left) / r.width - 0.5
        const y = (e.clientY - r.top) / r.height - 0.5
        el.style.transform = `perspective(900px) rotateX(${(-y * 10).toFixed(2)}deg) rotateY(${(x * 10).toFixed(2)}deg)`
      })
      el.addEventListener('pointerleave', () => (el.style.transform = ''))
    })
  }
}
