import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link } from 'react-router'
import { listarGastos, registrarGasto } from '../api/gastos'
import type { GastoResumenDto, GrupoResponse, RegistrarGastoRequest } from '../api/types'
import { claveGastos, clavesDerivadasDelGrupo } from '../lib/claves'
import { estadoDe } from '../lib/estadoConsulta'
import { esUsdt } from '../lib/monedas'
import { Boton } from './Boton'
import { Card } from './Card'
import { EstadoVacio } from './EstadoVacio'
import { FormularioGasto } from './FormularioGasto'
import { MensajeError } from './MensajeError'
import { Monto } from './Monto'

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
    <span className="text-right whitespace-nowrap">
      <Monto valor={monto} moneda={moneda} />
      {/* En USDT las dos cifras son la misma: repetirla haría dudar de si son
          conceptos distintos. */}
      {!esUsdt(moneda) ? (
        <span className="block text-xs text-tinta-500">
          ≈ <Monto valor={montoUsdt} tamano="chico" tono="apagado" />
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
        className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-tinta-200 bg-white p-3.5 transition-colors duration-150 hover:border-marca-300 hover:bg-marca-50/40"
      >
        <div className="min-w-0">
          <p className="truncate font-medium text-tinta-900">{gasto.descripcion}</p>
          <p className="truncate text-sm text-tinta-500">
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

  const hayGastos = (estado.datos?.length ?? 0) > 0

  return (
    <div className="flex flex-col gap-4">
      {registrando ? (
        <Card titulo="Nuevo gasto">
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
        </Card>
      ) : null}

      <Card
        descripcion={
          hayGastos ? 'Cada gasto se reparte según su división.' : undefined
        }
        accion={
          !registrando && hayGastos ? (
            <Boton onClick={() => setRegistrando(true)}>Registrar gasto</Boton>
          ) : null
        }
      >
        {estado.cargando ? (
          <p className="text-sm text-tinta-500">Cargando gastos…</p>
        ) : null}

        {estado.error ? (
          <div className="flex flex-col items-start gap-2">
            <MensajeError error={estado.error} />
            <Boton variante="secundario" onClick={() => consulta.refetch()}>
              Reintentar
            </Boton>
          </div>
        ) : null}

        {!estado.cargando && !estado.error && !hayGastos && !registrando ? (
          <EstadoVacio
            titulo="Todavía no hay gastos en este grupo"
            descripcion="Registrá el primero y la aplicación calcula sola cuánto le toca a cada uno."
            accion={<Boton onClick={() => setRegistrando(true)}>Registrar el primer gasto</Boton>}
          />
        ) : null}

        {hayGastos ? (
          <ul className="flex flex-col gap-2">
            {estado.datos?.map((gasto) => (
              <FilaGasto key={gasto.id} gasto={gasto} grupoId={grupo.id} />
            ))}
          </ul>
        ) : null}
      </Card>
    </div>
  )
}
