// Small client script for the prerendered page (no React in the browser).
// Uses motion's tiny DOM helpers for scroll and in-view effects.
import { inView } from 'motion'
import './styles.css'

const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
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

