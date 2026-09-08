import type { ReactNode } from 'react'

type Variante = 'primario' | 'secundario' | 'fantasma' | 'peligro'
type Tamano = 'normal' | 'chico'

interface BotonProps {
  children: ReactNode
  type?: 'button' | 'submit'
  onClick?: () => void
  /** Mientras la operación está en curso el botón se deshabilita y avisa. */
  enCurso?: boolean
  variante?: Variante
  tamano?: Tamano
  /** Ocupa todo el ancho disponible. Útil en formularios de pantalla chica. */
  ancho?: boolean
}

const VARIANTES: Record<Variante, string> = {
  primario: 'bg-marca-600 text-white shadow-tarjeta hover:bg-marca-700 active:bg-marca-800',
  secundario: 'border border-tinta-300 bg-white text-tinta-800 hover:bg-tinta-50 active:bg-tinta-100',
  fantasma: 'text-tinta-700 hover:bg-tinta-100 active:bg-tinta-200',
  peligro: 'bg-contra-600 text-white shadow-tarjeta hover:bg-contra-700 active:bg-contra-700',
}

const TAMANOS: Record<Tamano, string> = {
  normal: 'px-4 py-2 text-sm',
  chico: 'px-2.5 py-1.5 text-xs',
}

export function Boton({
  children,
  type = 'button',
  onClick,
  enCurso = false,
  variante = 'primario',
  tamano = 'normal',
  ancho = false,
}: BotonProps) {
  return (
    <button
      type={type}
      onClick={onClick}
      disabled={enCurso}
      aria-busy={enCurso || undefined}
      className={`inline-flex items-center justify-center gap-2 rounded-lg font-medium transition-colors duration-150 disabled:cursor-not-allowed disabled:opacity-60 ${
        VARIANTES[variante]
      } ${TAMANOS[tamano]} ${ancho ? 'w-full' : ''}`}
    >
      {enCurso ? 'Guardando…' : children}
    </button>
  )
}
