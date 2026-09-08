import type { ReactNode } from 'react'

/**
 * Una lista vacía es una oportunidad de explicar, no un hueco.
 *
 * Siempre dice qué falta; ofrece la acción solo si quien mira puede ejecutarla, para
 * no poner un botón que va a devolver 403.
 */
export function EstadoVacio({
  titulo,
  descripcion,
  accion,
}: {
  titulo: string
  descripcion: string
  accion?: ReactNode
}) {
  return (
    <div className="flex flex-col items-center gap-3 rounded-lg border border-dashed border-tinta-300 px-6 py-10 text-center">
      <p className="font-medium text-tinta-900">{titulo}</p>
      <p className="max-w-sm text-sm text-tinta-600">{descripcion}</p>
      {accion ? <div className="mt-1">{accion}</div> : null}
    </div>
  )
}
