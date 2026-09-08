import { useQuery } from '@tanstack/react-query'
import { obtenerBalances, obtenerLiquidacion } from '../api/balances'
import { listarGastos } from '../api/gastos'
import type { GrupoResponse } from '../api/types'
import { claveBalances, claveGastos, claveLiquidacion } from '../lib/claves'
import { estadoDe } from '../lib/estadoConsulta'
import { Boton } from './Boton'
import { Card } from './Card'
import { Etiqueta } from './Etiqueta'
import { MensajeError } from './MensajeError'
import { Monto, tonoDeSaldo } from './Monto'

/**
 * Traduce el signo del balance a una frase.
 *
 * «−200» obliga a recordar la convención del sistema para saber si esa persona pagó
 * de más o de menos, y es justo la ambigüedad que genera discusiones en un grupo de
 * viaje. La cifra se muestra siempre sin signo y el sentido lo lleva el texto; el
 * color acompaña pero no es el único indicador, para que funcione en escala de
 * grises y con daltonismo.
 */
function leerBalance(balance: number): string {
  if (balance > 0) return 'Le deben'
  if (balance < 0) return 'Debe'
  return 'Está a mano'
}

function Cargando() {
  return <p className="text-sm text-tinta-500">Calculando…</p>
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

export function SeccionBalances({
  grupo,
  participanteId,
  onRegistrarPago,
}: {
  grupo: GrupoResponse
  participanteId: number | null
  /** Lleva a la pestaña de pagos con la transferencia precargada. */
  onRegistrarPago: (precarga: { receptorId: number; monto: string }) => void
}) {
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

  const eBalances = estadoDe(balances)
  const eLiquidacion = estadoDe(liquidacion)

  const hayGastos = (gastos.data?.length ?? 0) > 0
  const esMio = (id: number) => participanteId != null && participanteId === id

  return (
    <div className="flex flex-col gap-4">
      <Card
        titulo="Balances"
        descripcion="Cuánto le corresponde a cada integrante, en USDT."
      >
        {eBalances.cargando ? <Cargando /> : null}
        {eBalances.error ? (
          <ConError error={eBalances.error} onReintentar={() => balances.refetch()} />
        ) : null}

        {eBalances.datos ? (
          <>
            <ul className="flex flex-col divide-y divide-tinta-100">
              {eBalances.datos.map((b) => {
                const propio = esMio(b.participante.id)
                return (
                  <li
                    key={b.participante.id}
                    className={`flex items-center justify-between gap-3 px-2 py-2.5 ${
                      propio ? 'rounded-lg bg-marca-50/60' : ''
                    }`}
                  >
                    <span className="flex min-w-0 items-center gap-2">
                      <span className="truncate text-tinta-900">
                        {b.participante.nombre} {b.participante.apellido}
                      </span>
                      {propio ? <Etiqueta tono="marca">Vos</Etiqueta> : null}
                      {/* Sin esta marca, alguien que salió debiendo se ve igual que
                          un integrante y no se entiende por qué está en la lista. */}
                      {!b.esMiembroActual ? (
                        <Etiqueta
                          tono="aviso"
                          title="Ya no integra el grupo, pero conserva saldo pendiente"
                        >
                          Ya no integra el grupo
                        </Etiqueta>
                      ) : null}
                    </span>
                    <span className="flex shrink-0 items-baseline gap-1.5">
                      <span className="text-sm text-tinta-600">
                        {leerBalance(b.balance)}
                      </span>
                      {b.balance !== 0 ? (
                        <Monto
                          valor={b.balance}
                          tamano="chico"
                          tono={tonoDeSaldo(b.balance)}
                          absoluto
                        />
                      ) : null}
                    </span>
                  </li>
                )
              })}
            </ul>
            {!hayGastos ? (
              <p className="mt-3 text-sm text-tinta-600">
                Todavía no hay gastos registrados, así que no hay nada que saldar.
              </p>
            ) : null}
          </>
        ) : null}
      </Card>

      <Card
        titulo="Liquidación"
        descripcion="La menor cantidad de transferencias para que todos queden a mano."
      >
        {eLiquidacion.cargando ? <Cargando /> : null}
        {eLiquidacion.error ? (
          <ConError
            error={eLiquidacion.error}
            onReintentar={() => liquidacion.refetch()}
          />
        ) : null}

        {eLiquidacion.datos && eLiquidacion.datos.length === 0 ? (
          <p className="rounded-lg border border-dashed border-tinta-300 p-5 text-center text-sm text-tinta-700">
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
                  className={`flex flex-wrap items-center justify-between gap-3 rounded-lg border p-3.5 ${
                    meToca ? 'border-marca-300 bg-marca-50/60' : 'border-tinta-200'
                  }`}
                >
                  <span className="min-w-0 truncate text-tinta-900">
                    <span className="font-medium">{t.de}</span>
                    <span className="mx-2 text-tinta-400">→</span>
                    <span className="font-medium">{t.para}</span>
                  </span>
                  <span className="flex shrink-0 items-center gap-3">
                    <Monto valor={t.monto} />
                    {/* Sin esto la liquidación es una lista de tareas que hay que
                        transcribir a mano en la pestaña de pagos. Solo se ofrece a
                        quien paga: el backend no deja registrar un pago ajeno. */}
                    {esMio(t.deId) ? (
                      <Boton
                        variante="secundario"
                        tamano="chico"
                        onClick={() =>
                          onRegistrarPago({
                            receptorId: t.paraId,
                            monto: String(t.monto),
                          })
                        }
                      >
                        Registrar este pago
                      </Boton>
                    ) : null}
                  </span>
                </li>
              )
            })}
          </ul>
        ) : null}
      </Card>
    </div>
  )
}
