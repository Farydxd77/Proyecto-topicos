import { QueryClient, QueryClientProvider, onlineManager } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router'
import { AuthProvider } from './auth/AuthContext'
import { Router } from './router'

/**
 * TanStack Query decide por su cuenta si «hay conexión» y, cuando cree que no,
 * PAUSA las consultas en lugar de fallarlas: quedan en `fetchStatus: 'paused'` para
 * siempre y la pantalla se congela en «Cargando…», sin error y sin reintento.
 *
 * Se observó con el backend detenido y `navigator.onLine === true`. Para esta
 * aplicación esa heurística no aporta nada: el backend es local y está detrás del
 * proxy de desarrollo, así que si no responde hay que decirlo, no esperar a una
 * reconexión que nunca se va a anunciar.
 */
onlineManager.setOnline(true)

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Un 401 ya cierra la sesión en el cliente HTTP: reintentarlo no aporta nada.
      retry: (intentos, error) =>
        (error as { status?: number })?.status === 401 ? false : intentos < 1,
      refetchOnWindowFocus: false,
      /**
       * Con el modo `online` por defecto, TanStack Query PAUSA la consulta en lugar
       * de fallar cuando cree que no hay conexión, y la deja en `fetchStatus:
       * 'paused'` indefinidamente: la pantalla se queda en «Cargando…» para siempre,
       * sin error, sin reintento y sin salida. Es lo que pasaba con el backend
       * detenido, aunque `navigator.onLine` fuera `true`.
       *
       * Ese modo está pensado para APIs remotas por internet, donde esperar a
       * recuperar la conexión tiene sentido. Acá el backend es local y detrás de un
       * proxy: si no responde, hay que decirlo. `always` hace que la consulta se
       * ejecute siempre y que el fallo llegue a la interfaz como error.
       */
      networkMode: 'always',
    },
    mutations: {
      networkMode: 'always',
    },
  },
})

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        {/* AuthProvider usa useQueryClient, así que va dentro del QueryClientProvider. */}
        <AuthProvider>
          <Router />
        </AuthProvider>
      </BrowserRouter>
    </QueryClientProvider>
  )
}

export default App
