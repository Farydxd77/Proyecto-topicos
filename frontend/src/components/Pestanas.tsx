import { NavLink } from 'react-router'

export interface Pestana {
  to: string
  texto: string
  /** `true` solo para la pestaña raíz, que si no queda activa en todas las hijas. */
  exacta?: boolean
  /** Punto de atención: hay algo que requiere acción en esa pestaña. */
  alerta?: boolean
}

/**
 * Navegación de la sección del grupo.
 *
 * Son `NavLink` y no botones con estado: cada pestaña es una ruta real, así que se
 * puede compartir su enlace, recargar sin perderla y volver con el botón «atrás».
 *
 * En pantalla chica la fila hace scroll horizontal en lugar de apilarse o cortarse.
 */
export function Pestanas({ pestanas }: { pestanas: Pestana[] }) {
  return (
    <nav className="-mb-px flex gap-1 overflow-x-auto border-b border-tinta-200">
      {pestanas.map((p) => (
        <NavLink
          key={p.to}
          to={p.to}
          end={p.exacta}
          className={({ isActive }) =>
            `relative shrink-0 border-b-2 px-3 py-2.5 text-sm font-medium transition-colors duration-150 ${
              isActive
                ? 'border-marca-600 text-marca-700'
                : 'border-transparent text-tinta-600 hover:border-tinta-300 hover:text-tinta-900'
            }`
          }
        >
          {p.texto}
          {p.alerta ? (
            <span
              aria-label="Requiere tu atención"
              className="ml-1.5 inline-block h-1.5 w-1.5 rounded-full bg-aviso-700 align-middle"
            />
          ) : null}
        </NavLink>
      ))}
    </nav>
  )
}
