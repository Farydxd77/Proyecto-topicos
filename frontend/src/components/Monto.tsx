import { formatearMonto } from '../lib/formato'

type Tamano = 'heroe' | 'destacado' | 'normal' | 'chico'
type Tono = 'neutro' | 'favor' | 'contra' | 'apagado'

const TAMANOS: Record<Tamano, string> = {
  heroe: 'text-heroe font-semibold',
  destacado: 'text-2xl font-semibold',
  normal: 'text-base font-medium',
  chico: 'text-sm font-medium',
}

const TONOS: Record<Tono, string> = {
  neutro: 'text-tinta-900',
  favor: 'text-favor-700',
  contra: 'text-contra-700',
  apagado: 'text-tinta-500',
}

/**
 * La única forma de mostrar dinero en la aplicación.
 *
 * Centraliza tres cosas que si no se resuelven caso por caso y terminan divergiendo:
 * las CIFRAS TABULARES (sin ellas los números bailan cuando se actualizan), el color
 * semántico según el signo, y el tamaño dentro de una escala fija.
 *
 * No hace aritmética: `lib/formato.ts` formatea y nada más. El backend ya calculó la
 * conversión y el reparto.
 *
 * El color NUNCA es el único indicador del signo: quien lo use para un saldo tiene
 * que acompañarlo con palabras («Le deben», «Debe»), para que funcione en escala de
 * grises y con daltonismo.
 */
export function Monto({
  valor,
  tamano = 'normal',
  tono = 'neutro',
  moneda = 'USDT',
  /** Muestra el valor absoluto: útil cuando el signo ya lo dice el texto. */
  absoluto = false,
}: {
  valor: number
  tamano?: Tamano
  tono?: Tono
  moneda?: string | null
  absoluto?: boolean
}) {
  const mostrado = absoluto ? Math.abs(valor) : valor

  return (
    <span className={`tabular-nums whitespace-nowrap ${TAMANOS[tamano]} ${TONOS[tono]}`}>
      {formatearMonto(mostrado)}
      {moneda ? (
        <span
          className={`ml-1 font-normal text-tinta-500 ${
            tamano === 'heroe' ? 'text-base' : tamano === 'destacado' ? 'text-sm' : 'text-xs'
          }`}
        >
          {moneda}
        </span>
      ) : null}
    </span>
  )
}

/** El tono que le corresponde a un saldo según su signo. */
export function tonoDeSaldo(saldo: number): Tono {
  if (saldo > 0) return 'favor'
  if (saldo < 0) return 'contra'
  return 'apagado'
}
