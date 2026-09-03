import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import { listarGastos, registrarGasto } from '../api/gastos'
import type { GastoResumenDto, GrupoResponse, RegistrarGastoRequest } from '../api/types'
import { claveGastos, clavesDerivadasDelGrupo } from '../lib/claves'
import { estadoDe } from '../lib/estadoConsulta'
import { formatearMonto } from '../lib/formato'
import { esUsdt } from '../lib/monedas'
import { Boton } from './Boton'
import { FormularioGasto } from './FormularioGasto'
import { MensajeError } from './MensajeError'

/** El monto original es lo que se pagó; el USDT es un cálculo derivado. */
export function MontoConEquivalente({
  monto,
  moneda,
  montoUsdt,
}: {
  monto: number
  moneda: string
  montoUsdt: number
}) {
  return (
    <span className="whitespace-nowrap">
      <span className="font-medium text-slate-900">
        {formatearMonto(monto)} {moneda}
      </span>
      {/* En USDT las dos cifras son la misma: repetirla haría dudar de si son
          conceptos distintos. */}
      {!esUsdt(moneda) ? (
        <span className="ml-2 text-sm text-slate-500">
          ≈ {formatearMonto(montoUsdt)} USDT
        </span>
      ) : null}
    </span>
  )
}

function FilaGasto({ gasto, grupoId }: { gasto: GastoResumenDto; grupoId: number }) {
  return (
    <li>
      <Link
        to={`/grupos/${grupoId}/gastos/${gasto.id}`}
        className="flex flex-wrap items-center justify-between gap-3 rounded-md border border-slate-200 bg-white p-3 transition hover:border-emerald-400 hover:bg-emerald-50/40"
      >
        <div className="min-w-0">
          <p className="truncate font-medium text-slate-900">{gasto.descripcion}</p>
          <p className="truncate text-sm text-slate-500">
            Pagó {gasto.pagador.nombre} {gasto.pagador.apellido} · {gasto.fecha}
          </p>
        </div>
        <MontoConEquivalente
          monto={gasto.monto}
          moneda={gasto.moneda}
          montoUsdt={gasto.montoUsdt}
        />
      </Link>
    </li>
  )
}

export function SeccionGastos({ grupo }: { grupo: GrupoResponse }) {
  const queryClient = useQueryClient()
  const [registrando, setRegistrando] = useState(false)

  const consulta = useQuery({
    queryKey: claveGastos(grupo.id),
    queryFn: () => listarGastos(grupo.id),
  })

  const estado = estadoDe(consulta)

  const alta = useMutation({
    mutationFn: (datos: RegistrarGastoRequest) => registrarGasto(grupo.id, datos),
    onSuccess: () => {
      // Un gasto cambia también los balances y la liquidación.
      for (const clave of clavesDerivadasDelGrupo(grupo.id)) {
        queryClient.invalidateQueries({ queryKey: clave })
      }
      setRegistrando(false)
      alta.reset()
    },
  })

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5">
      <div className="mb-4 flex items-center justify-between gap-4">
        <h2 className="font-semibold text-slate-900">Gastos</h2>
        {!registrando && (estado.datos?.length ?? 0) > 0 ? (
          <Boton onClick={() => setRegistrando(true)}>Registrar gasto</Boton>
        ) : null}
      </div>

      {registrando ? (
        <div className="mb-5 rounded-md border border-slate-200 p-4">
          <FormularioGasto
            grupo={grupo}
            enCurso={alta.isPending}
            error={alta.error}
            onEnviar={(datos) => alta.mutate(datos)}
            onCancelar={() => {
              alta.reset()
              setRegistrando(false)
            }}
          />
        </div>
      ) : null}

      {estado.cargando ? (
        <p className="text-sm text-slate-500">Cargando gastos…</p>
      ) : null}

      {estado.error ? (
        <div className="flex flex-col items-start gap-2">
          <MensajeError error={estado.error} />
          <Boton variante="secundario" onClick={() => consulta.refetch()}>
            Reintentar
          </Boton>
        </div>
      ) : null}

      {!estado.cargando && !estado.error && (estado.datos?.length ?? 0) === 0 && !registrando ? (
        <div className="rounded-md border border-dashed border-slate-300 p-6 text-center">
          <p className="text-slate-900">Todavía no hay gastos en este grupo</p>
          <p className="mt-1 text-sm text-slate-600">
            Registrá el primero para empezar a repartir.
          </p>
          <div className="mt-3 flex justify-center">
            <Boton onClick={() => setRegistrando(true)}>Registrar el primer gasto</Boton>
          </div>
        </div>
      ) : null}

      {estado.datos && estado.datos.length > 0 ? (
        <ul className="flex flex-col gap-2">
          {estado.datos.map((gasto) => (
            <FilaGasto key={gasto.id} gasto={gasto} grupoId={grupo.id} />
          ))}
        </ul>
      ) : null}
    </section>
  )
}
