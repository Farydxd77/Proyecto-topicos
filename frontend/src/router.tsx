import type { ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { RutaProtegida } from './auth/RutaProtegida'
import { useAuth } from './auth/useAuth'
import { Layout } from './components/Layout'
import { LoginPage } from './pages/LoginPage'
import { NoEncontradaPage } from './pages/NoEncontradaPage'
import { PerfilPage } from './pages/PerfilPage'
import { RegistroPage } from './pages/RegistroPage'

/** Quien ya tiene sesión no debe ver login ni registro. */
function SoloAnonimos({ children }: { children: ReactNode }) {
  const { haySesion } = useAuth()
  return haySesion ? <Navigate to="/perfil" replace /> : <>{children}</>
}

/** La raíz manda a perfil o a login según haya sesión. */
function Raiz() {
  const { haySesion } = useAuth()
  return <Navigate to={haySesion ? '/perfil' : '/login'} replace />
}

export function Router() {
  return (
    <Routes>
      <Route path="/" element={<Raiz />} />

      <Route
        path="/login"
        element={
          <SoloAnonimos>
            <LoginPage />
          </SoloAnonimos>
        }
      />
      <Route
        path="/registro"
        element={
          <SoloAnonimos>
            <RegistroPage />
          </SoloAnonimos>
        }
      />

      <Route element={<RutaProtegida />}>
        <Route element={<Layout />}>
          <Route path="/perfil" element={<PerfilPage />} />
        </Route>
      </Route>

      <Route path="*" element={<NoEncontradaPage />} />
    </Routes>
  )
}
