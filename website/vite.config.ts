import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// Served from GitHub Pages at https://oskar26.github.io/hain/
export default defineConfig({
  base: '/hain/',
  plugins: [react(), tailwindcss()],
  build: { target: 'es2020', cssCodeSplit: false, assetsInlineLimit: 2048 },
})
