import { useQuery } from '@tanstack/react-query'
import { NavLink, useNavigate } from 'react-router'
import { obtenerPerfil } from '../api/perfil'
import { useAuth } from '../auth/useAuth'

/** Destinos cuyo frontend todavía no existe: se muestran, pero no navegan. */
const PROXIMAMENTE: string[] = []

/** Destinos ya disponibles, en el orden en que se recorren. */
const ENLACES = [
  { to: '/perfil', texto: 'Perfil' },
  { to: '/grupos', texto: 'Grupos' },
]

export function Navegacion() {
  const { cerrarSesion } = useAuth()
  const navigate = useNavigate()

  // El username se lee del perfil cacheado, no del contexto de sesión: así cambiarlo
  // invalida una sola clave y esta barra se actualiza sola, sin dos fuentes de verdad.
  const { data: perfil } = useQuery({
    queryKey: ['perfil'],
    queryFn: obtenerPerfil,
  })

  function salir() {
    cerrarSesion()
    navigate('/login', { replace: true })
  }

  return (
    <header className="border-b border-slate-200 bg-white">
      <nav className="mx-auto flex max-w-3xl items-center gap-6 px-4 py-3">
        <span className="font-semibold text-emerald-700">Cuentas Claras</span>

        {ENLACES.map((enlace) => (
          <NavLink
            key={enlace.to}
            to={enlace.to}
            className={({ isActive }) =>
              `text-sm transition ${
                isActive
                  ? 'font-medium text-slate-900'
                  : 'text-slate-600 hover:text-slate-900'
              }`
            }
          >
            {enlace.texto}
          </NavLink>
        ))}

        {PROXIMAMENTE.map((destino) => (
          <span
            key={destino}
            aria-disabled="true"
            title="Todavía no disponible"
            className="cursor-not-allowed text-sm text-slate-400"
          >
            {destino}
          </span>
        ))}

        <div className="ml-auto flex items-center gap-3">
          {perfil ? (
            <span className="text-sm text-slate-600">{perfil.username}</span>
          ) : null}
          <button
            type="button"
            onClick={salir}
            className="rounded-md px-2 py-1 text-sm text-slate-600 transition hover:bg-slate-100 hover:text-slate-900"
          >
            Cerrar sesión
          </button>
        </div>
      </nav>
    </header>
  )
}
