import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.tsx'
import './index.css'

/**
 * En desarrollo se arranca la API simulada antes de montar la aplicación, para que
 * ninguna petición salga antes de que el worker esté escuchando.
 *
 * El import es dinámico y vive dentro del guard de `import.meta.env.DEV`: así el
 * bundler elimina msw por completo del build de producción.
 */
async function arrancar() {
  if (import.meta.env.DEV) {
    const { iniciarMocks } = await import('./mocks/browser')
    await iniciarMocks()
  }

  createRoot(document.getElementById('root')!).render(
    <StrictMode>
      <App />
    </StrictMode>,
  )
}

arrancar()
