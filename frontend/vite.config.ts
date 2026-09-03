import { rm } from 'node:fs/promises'
import { resolve } from 'node:path'
import tailwindcss from '@tailwindcss/vite'
import react from '@vitejs/plugin-react'
import { defineConfig, type Plugin } from 'vite'

/**
 * Vite copia todo `public/` al build tal cual, así que el service worker de MSW
 * terminaría en `dist/`. Nada lo registra en producción —`iniciarMocks` vive detrás de
 * `import.meta.env.DEV`—, pero un worker capaz de simular la API no tiene por qué
 * viajar en un artefacto de producción. Se borra al cerrar el bundle.
 */
function excluirWorkerDeMocks(): Plugin {
  return {
    name: 'excluir-worker-de-mocks',
    apply: 'build',
    async closeBundle() {
      await rm(resolve(import.meta.dirname, 'dist/mockServiceWorker.js'), { force: true })
    },
  }
}

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss(), excluirWorkerDeMocks()],
  server: {
    // Mientras los mocks de MSW estén activos este proxy queda dormido: el service
    // worker responde /api antes de que la petición salga a la red. Se deja
    // configurado para el día que se apaguen los mocks y haya backend real.
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
