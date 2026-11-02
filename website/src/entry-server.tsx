import { StrictMode } from 'react'
import { renderToString } from 'react-dom/server'
import App from './App'
import type { Lang } from './content'

export function render(lang: Lang) {
  return renderToString(
    <StrictMode>
      <App lang={lang} />
    </StrictMode>,
  )
}
