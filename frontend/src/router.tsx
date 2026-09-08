import type { ReactNode } from 'react'
import { Navigate, Route, Routes } from 'react-router'
import { RutaProtegida } from './auth/RutaProtegida'
import { useAuth } from './auth/useAuth'
import { Layout } from './components/Layout'
import { GastoDetallePage } from './pages/GastoDetallePage'
import { GrupoBalancesPage } from './pages/GrupoBalancesPage'
import { GrupoGastosPage } from './pages/GrupoGastosPage'
import { GrupoLayout } from './pages/GrupoLayout'
import { GrupoMiembrosPage } from './pages/GrupoMiembrosPage'
import { GrupoPagosPage } from './pages/GrupoPagosPage'
import { GrupoResumenPage } from './pages/GrupoResumenPage'
import { GruposPage } from './pages/GruposPage'
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
          <Route path="/grupos" element={<GruposPage />} />

          {/*
            El grupo es una sección con pestañas, no una pantalla.
            El layout consulta el grupo UNA vez y lo comparte por el Outlet: como las
            pestañas son rutas hijas, cambiar de pestaña no lo desmonta ni lo repide.
            Y al ser rutas reales, cada pestaña tiene URL propia: se puede compartir,
            recargar y volver con el botón «atrás».
          */}
          <Route path="/grupos/:id" element={<GrupoLayout />}>
            <Route index element={<GrupoResumenPage />} />
            <Route path="gastos" element={<GrupoGastosPage />} />
            <Route path="gastos/:gastoId" element={<GastoDetallePage />} />
            <Route path="pagos" element={<GrupoPagosPage />} />
            <Route path="balances" element={<GrupoBalancesPage />} />
            <Route path="miembros" element={<GrupoMiembrosPage />} />
          </Route>
        </Route>
      </Route>

      <Route path="*" element={<NoEncontradaPage />} />
    </Routes>
  )
}
