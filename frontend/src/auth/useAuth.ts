import { useContext } from 'react'
import { AuthContext, type Sesion } from './AuthContext'

export function useAuth(): Sesion {
  const sesion = useContext(AuthContext)

  if (sesion === null) {
    throw new Error('useAuth debe usarse dentro de un <AuthProvider>')
  }

  return sesion
}
