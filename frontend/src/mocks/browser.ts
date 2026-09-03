import { setupWorker } from 'msw/browser'
import { handlers } from './handlers'

export const worker = setupWorker(...handlers)

/**
 * Arranca la API simulada.
 *
 * Se llama únicamente bajo `import.meta.env.DEV`, de modo que el bundler elimina del
 * build de producción tanto esta llamada como todo el árbol de msw que cuelga de ella.
 */
export async function iniciarMocks(): Promise<void> {
  await worker.start({
    // Deja pasar sin ruido lo que no sea /api (documentos, assets, HMR de Vite).
    onUnhandledRequest: 'bypass',
  })
}
