import { useQuery } from '@tanstack/react-query'
import { Link, NavLink, useNavigate } from 'react-router'
import { obtenerPerfil } from '../api/perfil'
import { useAuth } from '../auth/useAuth'
import { CLAVE_PERFIL } from '../lib/claves'
import { Marca } from './Marca'

const ENLACES = [
  { to: '/grupos', texto: 'Grupos' },
  { to: '/perfil', texto: 'Perfil' },
]

export function Navegacion() {
  const { cerrarSesion } = useAuth()
  const navigate = useNavigate()

  // El username se lee del perfil cacheado, no del contexto de sesión: así cambiarlo
  // invalida una sola clave y esta barra se actualiza sola, sin dos fuentes de verdad.
  const { data: perfil } = useQuery({ queryKey: CLAVE_PERFIL, queryFn: obtenerPerfil })

  function salir() {
    cerrarSesion()
    navigate('/login', { replace: true })
  }

  return (
    <header className="sticky top-0 z-10 border-b border-tinta-200 bg-white/85 backdrop-blur">
      <nav className="mx-auto flex max-w-4xl items-center gap-6 px-4 py-3">
        <Link to="/grupos" className="shrink-0">
          <Marca />
        </Link>

        <div className="flex items-center gap-1">
          {ENLACES.map((enlace) => (
            <NavLink
              key={enlace.to}
              to={enlace.to}
              className={({ isActive }) =>
                `rounded-lg px-2.5 py-1.5 text-sm transition-colors duration-150 ${
                  isActive
                    ? 'bg-marca-50 font-medium text-marca-800'
                    : 'text-tinta-600 hover:bg-tinta-100 hover:text-tinta-900'
                }`
              }
            >
              {enlace.texto}
            </NavLink>
          ))}
        </div>

        <div className="ml-auto flex items-center gap-3">
          {perfil ? (
            <span className="hidden text-sm text-tinta-600 sm:inline">
              {perfil.nombre} {perfil.apellido}
            </span>
          ) : null}
          <button
            type="button"
            onClick={salir}
            className="rounded-lg px-2.5 py-1.5 text-sm text-tinta-600 transition-colors duration-150 hover:bg-tinta-100 hover:text-tinta-900"
          >
            Salir
          </button>
        </div>
      </nav>
    </header>
  )
}
