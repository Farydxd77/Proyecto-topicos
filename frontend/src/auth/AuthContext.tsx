import { useQueryClient } from '@tanstack/react-query'
import {
  createContext,
  useCallback,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { configurarSesion } from '../api/client'
import { borrarToken, guardarToken, leerTokenVigente } from './token'

export interface Sesion {
  token: string | null
  haySesion: boolean
  /** True cuando la sesión se cerró porque el backend rechazó el token. */
  expiro: boolean
  iniciarSesion: (token: string) => void
  cerrarSesion: () => void
  /** Descarta el aviso de sesión expirada una vez mostrado. */
  limpiarAvisoExpiro: () => void
}

// eslint-disable-next-line react-refresh/only-export-components
export const AuthContext = createContext<Sesion | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  // El almacenamiento se lee de forma síncrona en la inicialización, antes del primer
  // pintado. Así nunca existe un instante en el que alguien con sesión válida vea la
  // pantalla de login: no hace falta un estado de "restaurando".
  const [token, setToken] = useState<string | null>(leerTokenVigente)
  const [expiro, setExpiro] = useState(false)
  const queryClient = useQueryClient()

  const iniciarSesion = useCallback((nuevo: string) => {
    guardarToken(nuevo)
    setToken(nuevo)
    setExpiro(false)
  }, [])

  const cerrarSesion = useCallback(() => {
    borrarToken()
    setToken(null)
    // Sin esto, los datos de la sesión anterior quedarían en la caché y podrían
    // aparecer al volver a entrar.
    queryClient.clear()
  }, [queryClient])

  const limpiarAvisoExpiro = useCallback(() => setExpiro(false), [])

  // Le enseña al cliente HTTP cómo leer el token y qué hacer ante un 401. Se vuelve a
  // registrar cuando el token cambia, de modo que siempre entrega el vigente.
  //
  // Que esto ocurra en un efecto —y por tanto DESPUÉS de que los hijos monten y
  // puedan pedir datos— no deja ninguna petición sin token: hasta que se registra,
  // el cliente lee el token del almacenamiento por su cuenta. Ver `api/client.ts`.
  useEffect(() => {
    configurarSesion({
      obtenerToken: () => token,
      alExpirar: () => {
        setExpiro(true)
        cerrarSesion()
      },
    })
  }, [token, cerrarSesion])

  const valor = useMemo<Sesion>(
    () => ({
      token,
      haySesion: token !== null,
      expiro,
      iniciarSesion,
      cerrarSesion,
      limpiarAvisoExpiro,
    }),
    [token, expiro, iniciarSesion, cerrarSesion, limpiarAvisoExpiro],
  )

  return <AuthContext value={valor}>{children}</AuthContext>
}
