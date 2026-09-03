import type { ReactNode } from 'react'

interface BotonProps {
  children: ReactNode
  type?: 'button' | 'submit'
  onClick?: () => void
  /** Mientras la operación está en curso el botón se deshabilita y avisa. */
  enCurso?: boolean
  variante?: 'primario' | 'secundario'
}

export function Boton({
  children,
  type = 'button',
  onClick,
  enCurso = false,
  variante = 'primario',
}: BotonProps) {
  const estilos =
    variante === 'primario'
      ? 'bg-emerald-600 text-white hover:bg-emerald-700 focus:ring-emerald-300'
      : 'border border-slate-300 bg-white text-slate-700 hover:bg-slate-50 focus:ring-slate-300'

  return (
    <button
      type={type}
      onClick={onClick}
      disabled={enCurso}
      aria-busy={enCurso || undefined}
      className={`rounded-md px-4 py-2 font-medium transition outline-none focus:ring-2 disabled:cursor-not-allowed disabled:opacity-60 ${estilos}`}
    >
      {enCurso ? 'Guardando…' : children}
    </button>
  )
}
