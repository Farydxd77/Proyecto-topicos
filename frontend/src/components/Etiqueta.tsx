import type { ReactNode } from 'react'

type Tono = 'neutro' | 'marca' | 'aviso' | 'contra'

const TONOS: Record<Tono, string> = {
  neutro: 'bg-tinta-100 text-tinta-700',
  marca: 'bg-marca-100 text-marca-800',
  aviso: 'bg-aviso-50 text-aviso-900 ring-1 ring-aviso-200',
  contra: 'bg-contra-50 text-contra-700',
}

/** La píldora que se repetía a mano: Creador, Vos, Ya no integra el grupo, pagó. */
export function Etiqueta({
  children,
  tono = 'neutro',
  title,
}: {
  children: ReactNode
  tono?: Tono
  title?: string
}) {
  return (
    <span
      title={title}
      className={`inline-flex shrink-0 items-center rounded-full px-2 py-0.5 text-xs font-medium ${TONOS[tono]}`}
    >
      {children}
    </span>
  )
}
