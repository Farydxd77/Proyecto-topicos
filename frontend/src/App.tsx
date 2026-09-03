import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter } from 'react-router'
import { AuthProvider } from './auth/AuthContext'
import { Router } from './router'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      // Un 401 ya cierra la sesión en el cliente HTTP: reintentarlo no aporta nada.
      retry: (intentos, error) =>
        (error as { status?: number })?.status === 401 ? false : intentos < 1,
      refetchOnWindowFocus: false,
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
