import { useQuery } from '@tanstack/react-query'
import { obtenerBalances, obtenerLiquidacion } from '../api/balances'
import { listarGastos } from '../api/gastos'
import { obtenerPerfil } from '../api/perfil'
import type { GrupoResponse } from '../api/types'
import {
  CLAVE_PERFIL,
  claveBalances,
  claveGastos,
  claveLiquidacion,
} from '../lib/claves'
import { estadoDe } from '../lib/estadoConsulta'
import { formatearMonto } from '../lib/formato'
import { Boton } from './Boton'
import { MensajeError } from './MensajeError'

/**
 * Traduce el signo del balance a una frase.
 *
 * «−200» obliga a recordar la convención del sistema para saber si esa persona pagó
 * de más o de menos, y es justo la ambigüedad que genera discusiones en un grupo de
 * viaje. La cifra se muestra siempre sin signo y el sentido lo lleva el texto; el
 * color acompaña pero no es el único indicador, para que funcione en escala de
 * grises y con daltonismo.
 */
function leerBalance(balance: number): {
  texto: string
  monto: number
  clase: string
} {
  if (balance > 0) {
    return { texto: 'Le deben', monto: balance, clase: 'text-emerald-700' }
  }
  if (balance < 0) {
    return { texto: 'Debe', monto: -balance, clase: 'text-red-700' }
  }
  return { texto: 'Está a mano', monto: 0, clase: 'text-slate-500' }
}

function Cargando() {
  return <p className="text-sm text-slate-500">Calculando…</p>
}

function ConError({ error, onReintentar }: { error: unknown; onReintentar: () => void }) {
  return (
    <div className="flex flex-col items-start gap-2">
      <MensajeError error={error} />
      <Boton variante="secundario" onClick={onReintentar}>
        Reintentar
      </Boton>
    </div>
  )
}

export function SeccionBalances({ grupo }: { grupo: GrupoResponse }) {
  const balances = useQuery({
    queryKey: claveBalances(grupo.id),
    queryFn: () => obtenerBalances(grupo.id),
  })
  const liquidacion = useQuery({
    queryKey: claveLiquidacion(grupo.id),
    queryFn: () => obtenerLiquidacion(grupo.id),
  })
  // Para distinguir «todavía no hay nada que saldar» de «ya están todos a mano».
  const gastos = useQuery({
    queryKey: claveGastos(grupo.id),
    queryFn: () => listarGastos(grupo.id),
  })
  const { data: perfil } = useQuery({ queryKey: CLAVE_PERFIL, queryFn: obtenerPerfil })

  const eBalances = estadoDe(balances)
  const eLiquidacion = estadoDe(liquidacion)

  const hayGastos = (gastos.data?.length ?? 0) > 0
  const esMio = (participanteId: number) => perfil != null && perfil.id === participanteId

  return (
    <div className="flex flex-col gap-6">
      <section className="rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="mb-1 font-semibold text-slate-900">Balances</h2>
        <p className="mb-4 text-sm text-slate-600">
          Cuánto le corresponde a cada integrante, en USDT.
        </p>

        {eBalances.cargando ? <Cargando /> : null}
        {eBalances.error ? (
          <ConError error={eBalances.error} onReintentar={() => balances.refetch()} />
        ) : null}

        {eBalances.datos ? (
          <>
            <ul className="flex flex-col divide-y divide-slate-100">
              {eBalances.datos.map((b) => {
                const lectura = leerBalance(b.balance)
                const propio = esMio(b.participante.id)
                return (
                  <li
                    key={b.participante.id}
                    className={`flex items-center justify-between gap-3 px-2 py-2 ${
                      propio ? 'rounded-md bg-emerald-50/60' : ''
                    }`}
                  >
                    <span className="min-w-0 truncate text-slate-900">
                      {b.participante.nombre} {b.participante.apellido}
                      {propio ? (
                        <span className="ml-2 rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-800">
                          Vos
                        </span>
                      ) : null}
                    </span>
                    <span className={`shrink-0 text-sm ${lectura.clase}`}>
                      {lectura.texto}
                      {b.balance !== 0 ? (
                        <span className="ml-1 font-medium tabular-nums">
                          {formatearMonto(lectura.monto)} USDT
                        </span>
                      ) : null}
                    </span>
                  </li>
                )
              })}
            </ul>
            {!hayGastos ? (
              <p className="mt-3 text-sm text-slate-600">
                Todavía no hay gastos registrados, así que no hay nada que saldar.
              </p>
            ) : null}
          </>
        ) : null}
      </section>

      <section className="rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="mb-1 font-semibold text-slate-900">Liquidación</h2>
        <p className="mb-4 text-sm text-slate-600">
          La menor cantidad de transferencias para que todos queden a mano.
        </p>

        {eLiquidacion.cargando ? <Cargando /> : null}
        {eLiquidacion.error ? (
          <ConError
            error={eLiquidacion.error}
            onReintentar={() => liquidacion.refetch()}
          />
        ) : null}

        {eLiquidacion.datos && eLiquidacion.datos.length === 0 ? (
          <p className="rounded-md border border-dashed border-slate-300 p-4 text-center text-sm text-slate-700">
            {hayGastos
              ? 'Ya están todos a mano: no queda nada por pagar.'
              : 'Todavía no hay nada que saldar.'}
          </p>
        ) : null}

        {eLiquidacion.datos && eLiquidacion.datos.length > 0 ? (
          <ul className="flex flex-col gap-2">
            {eLiquidacion.datos.map((t, i) => {
              const meToca = esMio(t.deId) || esMio(t.paraId)
              return (
                <li
                  key={`${t.deId}-${t.paraId}-${i}`}
                  className={`flex flex-wrap items-center justify-between gap-2 rounded-md border p-3 ${
                    meToca
                      ? 'border-emerald-300 bg-emerald-50/60'
                      : 'border-slate-200'
                  }`}
                >
                  <span className="text-slate-900">
                    <span className="font-medium">{t.de}</span>
                    <span className="mx-2 text-slate-400">→</span>
                    <span className="font-medium">{t.para}</span>
                  </span>
                  <span className="shrink-0 font-medium tabular-nums text-slate-900">
                    {formatearMonto(t.monto)} USDT
                  </span>
                </li>
              )
            })}
          </ul>
        ) : null}
      </section>
    </div>
  )
}
