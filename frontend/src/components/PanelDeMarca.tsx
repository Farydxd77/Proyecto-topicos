import type { ReactNode } from 'react'
import { Marca } from './Marca'

/**
 * Composición partida para login y registro: panel de marca a la izquierda,
 * formulario a la derecha.
 *
 * Es la pantalla que más rinde por trabajo invertido —es lo primero que ve cualquiera
 * que abra la aplicación—. En pantalla chica el panel se colapsa a una cabecera para
 * no empujar el formulario fuera de la vista.
 */
export function PanelDeMarca({
  titulo,
  subtitulo,
  children,
}: {
  titulo: string
  subtitulo: string
  children: ReactNode
}) {
  return (
    <div className="flex min-h-screen flex-col lg:flex-row">
      {/* Panel de marca */}
      <div className="relative flex flex-col justify-between overflow-hidden bg-marca-800 px-6 py-8 text-white lg:w-[45%] lg:px-12 lg:py-12">
        {/* Un par de manchas de color para que el panel no sea un rectángulo plano. */}
        <div
          aria-hidden
          className="pointer-events-none absolute -top-24 -right-24 h-72 w-72 rounded-full bg-marca-500/30 blur-3xl"
        />
        <div
          aria-hidden
          className="pointer-events-none absolute -bottom-32 -left-16 h-72 w-72 rounded-full bg-marca-400/20 blur-3xl"
        />

        <div className="relative flex items-center gap-2">
          <svg viewBox="0 0 24 24" aria-hidden className="h-7 w-7" fill="none">
            <rect x="3" y="4" width="18" height="4.5" rx="2.25" fill="white" />
            <rect x="3" y="11" width="11" height="4.5" rx="2.25" fill="white" fillOpacity="0.65" />
            <rect x="3" y="18" width="18" height="3" rx="1.5" fill="white" fillOpacity="0.35" />
          </svg>
          <span className="text-lg font-semibold tracking-tight">Cuentas Claras</span>
        </div>

        <div className="relative mt-8 lg:mt-0">
          <p className="text-2xl leading-snug font-semibold tracking-tight lg:text-4xl">
            Viajen tranquilos.
            <br />
            <span className="text-marca-200">De las cuentas nos encargamos.</span>
          </p>
          <p className="mt-4 max-w-sm text-sm text-marca-100/80 lg:text-base">
            Cargá los gastos del viaje en la moneda que sea, decidí cómo se reparte cada
            uno, y la app calcula sola quién le debe cuánto a quién y la forma más corta
            de quedar a mano.
          </p>
        </div>

        {/* Una muestra visual de lo que hace la app: vale más que describirlo. */}
        <div className="relative mt-8 hidden gap-2 lg:flex lg:flex-col">
          {[
            { nombre: 'Ana', texto: 'Le deben', monto: '600,00', tono: 'text-marca-200' },
            { nombre: 'Beto', texto: 'Debe', monto: '200,00', tono: 'text-white/70' },
            { nombre: 'Carla', texto: 'Debe', monto: '400,00', tono: 'text-white/70' },
          ].map((fila) => (
            <div
              key={fila.nombre}
              className="flex items-center justify-between gap-4 rounded-lg bg-white/10 px-4 py-2.5 text-sm backdrop-blur-sm"
            >
              <span className="font-medium">{fila.nombre}</span>
              <span className={fila.tono}>
                {fila.texto}{' '}
                <span className="font-semibold tabular-nums">{fila.monto} USDT</span>
              </span>
            </div>
          ))}
        </div>
      </div>

      {/* Formulario */}
      <div className="flex flex-1 items-center justify-center px-4 py-10 lg:px-12">
        <div className="w-full max-w-sm">
          <div className="mb-6 lg:hidden">
            <Marca />
          </div>
          <h1 className="text-2xl font-semibold tracking-tight text-tinta-900">
            {titulo}
          </h1>
          <p className="mt-1 mb-6 text-sm text-tinta-600">{subtitulo}</p>
          {children}
        </div>
      </div>
    </div>
  )
}
