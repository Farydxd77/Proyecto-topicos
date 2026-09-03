import { Navigate, Outlet, useLocation } from 'react-router'
import { useAuth } from './useAuth'

/**
 * Guarda de las rutas privadas.
 *
 * La sesión se resuelve de forma síncrona antes del primer pintado (ver AuthProvider),
 * así que acá nunca hace falta un estado intermedio: quien tiene sesión válida entra
 * directo, sin ver un parpadeo de la pantalla de login.
 */
export function RutaProtegida() {
  const { haySesion } = useAuth()
  const ubicacion = useLocation()

  if (!haySesion) {
    // `state.desde` guarda la ruta pretendida para volver a ella tras autenticarse.
    return <Navigate to="/login" replace state={{ desde: ubicacion.pathname }} />
  }

  return <Outlet />
}
