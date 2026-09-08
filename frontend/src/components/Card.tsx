import type { ReactNode } from 'react'

/**
 * La superficie común de la aplicación. Todo bloque de contenido vive en una.
 *
 * El título es opcional a propósito: cuando la pantalla ya puso su cabecera, la
 * tarjeta entrega solo el contenido y no duplica el encabezado.
 */
export function Card({
  titulo,
  descripcion,
  accion,
  children,
  tono = 'normal',
}: {
  titulo?: string
  descripcion?: string
  /** Nodo alineado a la derecha del título: normalmente un botón. */
  accion?: ReactNode
  children: ReactNode
  tono?: 'normal' | 'aviso'
}) {
  const borde =
    tono === 'aviso'
      ? 'border-aviso-200 bg-aviso-50'
      : 'border-tinta-200 bg-white shadow-tarjeta'

  return (
    <section className={`rounded-xl border p-5 ${borde}`}>
      {titulo || accion ? (
        <div className="mb-4 flex flex-wrap items-start justify-between gap-3">
          <div className="min-w-0">
            {titulo ? (
              <h2 className="font-semibold text-tinta-900">{titulo}</h2>
            ) : null}
            {descripcion ? (
              <p className="mt-0.5 text-sm text-tinta-600">{descripcion}</p>
            ) : null}
          </div>
          {accion ? <div className="shrink-0">{accion}</div> : null}
        </div>
      ) : null}
      {children}
    </section>
  )
}
