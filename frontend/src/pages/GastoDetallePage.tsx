import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../api/client'
import { actualizarGasto, eliminarGasto, obtenerGasto } from '../api/gastos'
import { obtenerGrupo } from '../api/grupos'
import type { RegistrarGastoRequest } from '../api/types'
import { Boton } from '../components/Boton'
import { FormularioGasto } from '../components/FormularioGasto'
import { MensajeError } from '../components/MensajeError'
import { MontoConEquivalente } from '../components/SeccionGastos'
import { claveGasto, claveGrupo, clavesDerivadasDelGrupo } from '../lib/claves'
import { estadoDe } from '../lib/estadoConsulta'
import { formatearMonto } from '../lib/formato'
import { esUsdt } from '../lib/monedas'

function SinAcceso({ error, grupoId }: { error: unknown; grupoId: number }) {
  const status = error instanceof ApiError ? error.status : 0
  const texto =
    status === 404
      ? 'Este gasto no existe. Puede que alguien lo haya eliminado.'
      : status === 403
        ? 'No tenés acceso a este grupo porque no sos miembro.'
        : null

  return (
    <div className="flex flex-col items-start gap-3">
      {texto ? (
        <p role="alert" className="rounded-md border border-aviso-200 bg-aviso-50 px-3 py-2 text-sm text-aviso-900">
          {texto}
        </p>
      ) : (
        <MensajeError error={error} />
      )}
      <Link
        to={status === 403 ? '/grupos' : `/grupos/${grupoId}`}
        className="text-sm font-medium text-marca-700 hover:underline"
      >
        {status === 403 ? 'Volver a mis grupos' : 'Volver a los gastos'}
      </Link>
    </div>
  )
}

export function GastoDetallePage() {
  const { id, gastoId } = useParams()
  const grupoId = Number(id)
  const idGasto = Number(gastoId)
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [editando, setEditando] = useState(false)
  const [confirmando, setConfirmando] = useState(false)

  // El tipo del error debe ser `Error` y no `unknown`: TanStack Query infiere el tipo
  // de error de la consulta a partir de esta firma, y `unknown` lo propaga a todo el
  // resultado.
  const noReintentarAnte4xx = (intentos: number, error: Error) =>
    error instanceof ApiError && (error.status === 403 || error.status === 404)
      ? false
      : intentos < 2

  const consulta = useQuery({
    queryKey: claveGasto(grupoId, idGasto),
    queryFn: () => obtenerGasto(grupoId, idGasto),
    retry: noReintentarAnte4xx,
  })

  // El grupo hace falta para el selector de pagador al editar.
  const grupoConsulta = useQuery({
    queryKey: claveGrupo(grupoId),
    queryFn: () => obtenerGrupo(grupoId),
    retry: noReintentarAnte4xx,
  })

  function invalidarTodo() {
    queryClient.invalidateQueries({ queryKey: claveGasto(grupoId, idGasto) })
    for (const clave of clavesDerivadasDelGrupo(grupoId)) {
      queryClient.invalidateQueries({ queryKey: clave })
    }
  }

  const edicion = useMutation({
    mutationFn: (datos: RegistrarGastoRequest) =>
      actualizarGasto(grupoId, idGasto, datos),
    onSuccess: () => {
      invalidarTodo()
      setEditando(false)
      edicion.reset()
    },
  })

  const baja = useMutation({
    mutationFn: () => eliminarGasto(grupoId, idGasto),
    onSuccess: () => {
      queryClient.removeQueries({ queryKey: claveGasto(grupoId, idGasto) })
      for (const clave of clavesDerivadasDelGrupo(grupoId)) {
        queryClient.invalidateQueries({ queryKey: clave })
      }
      navigate(`/grupos/${grupoId}/gastos`, { replace: true })
    },
  })

  const estado = estadoDe(consulta)

  if (estado.cargando) return <p className="text-tinta-500">Cargando el gasto…</p>
  if (estado.error || !estado.datos) {
    return <SinAcceso error={estado.error} grupoId={grupoId} />
  }

  const gasto = estado.datos

  // Un reparto donde todos tienen la misma parte no gana nada mostrando el número.
  const repartoDesigual = gasto.division.some((p) => p.peso !== gasto.division[0].peso)
  const pagadorNoParticipa = !gasto.division.some(
    (p) => p.participante.id === gasto.pagador.id,
  )
  // El grupo puede no estar cargado todavía; sin él no se puede afirmar que falte
  // alguien, así que se calla en vez de suponer.
  const hayExcluidos =
    grupoConsulta.data != null &&
    gasto.division.length < grupoConsulta.data.miembros.length

  return (
    <div className="flex flex-col gap-6">
      <Link
        to={`/grupos/${grupoId}/gastos`}
        className="text-sm text-tinta-500 hover:text-tinta-800"
      >
        ← Volver a los gastos
      </Link>

      {editando && grupoConsulta.data ? (
        <section className="rounded-lg border border-tinta-200 bg-white p-5">
          <h2 className="mb-4 font-semibold text-tinta-900">Editar gasto</h2>
          <FormularioGasto
            grupo={grupoConsulta.data}
            gasto={gasto}
            enCurso={edicion.isPending}
            error={edicion.error}
            onEnviar={(datos) => edicion.mutate(datos)}
            onCancelar={() => {
              edicion.reset()
              setEditando(false)
            }}
          />
        </section>
      ) : (
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h1 className="text-2xl font-semibold text-tinta-900">
              {gasto.descripcion}
            </h1>
            <p className="mt-1 text-tinta-600">
              Pagó {gasto.pagador.nombre} {gasto.pagador.apellido} · {gasto.fecha}
            </p>
          </div>
          {/* Cualquier miembro puede editar y borrar: el backend no lo reserva al
              creador, así que la interfaz tampoco lo simula. */}
          {!confirmando ? (
            <div className="flex shrink-0 gap-2">
              <Boton variante="secundario" onClick={() => setEditando(true)}>
                Editar
              </Boton>
              <Boton variante="secundario" onClick={() => setConfirmando(true)}>
                Eliminar
              </Boton>
            </div>
          ) : null}
        </div>
      )}

      {confirmando ? (
        <div className="rounded-lg border border-contra-200 bg-contra-50 p-5">
          <p className="font-medium text-contra-700">
            ¿Eliminar el gasto «{gasto.descripcion}»?
          </p>
          <p className="mt-1 text-sm text-contra-700">
            Los balances del grupo se recalculan. Esta acción no se puede deshacer.
          </p>
          <div className="mt-3">
            <MensajeError error={baja.error} />
          </div>
          <div className="mt-4 flex gap-3">
            <Boton onClick={() => baja.mutate()} enCurso={baja.isPending}>
              Sí, eliminar
            </Boton>
            <Boton variante="secundario" onClick={() => setConfirmando(false)}>
              Cancelar
            </Boton>
          </div>
        </div>
      ) : null}

      <section className="rounded-lg border border-tinta-200 bg-white p-5">
        <h2 className="mb-3 font-semibold text-tinta-900">Importe</h2>
        <dl className="flex flex-col gap-2 text-sm">
          <div className="flex justify-between gap-4">
            <dt className="text-tinta-600">Monto pagado</dt>
            <dd>
              <MontoConEquivalente
                monto={gasto.monto}
                moneda={gasto.moneda}
                montoUsdt={gasto.montoUsdt}
              />
            </dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt className="text-tinta-600">Moneda</dt>
            <dd className="text-tinta-900">
              {gasto.monedaNombre} ({gasto.moneda})
            </dd>
          </div>
          {!esUsdt(gasto.moneda) ? (
            <div className="flex justify-between gap-4">
              <dt className="text-tinta-600">Tasa aplicada al registrarlo</dt>
              <dd className="text-tinta-900">
                1 {gasto.moneda} = {formatearMonto(gasto.tasaCambio)} USDT
              </dd>
            </div>
          ) : null}
        </dl>
      </section>

      <section className="rounded-lg border border-tinta-200 bg-white p-5">
        <h2 className="font-semibold text-tinta-900">Cómo se reparte</h2>
        <p className="mt-1 mb-4 text-sm text-tinta-600">
          Cada participante le debe esta parte a {gasto.pagador.nombre}. Los montos
          están en USDT.
        </p>
        <ul className="flex flex-col divide-y divide-tinta-100">
          {gasto.division.map((parte) => {
            const esPagador = parte.participante.id === gasto.pagador.id
            return (
              <li
                key={parte.participante.id}
                className="flex items-center justify-between gap-3 py-2"
              >
                <span className="min-w-0 truncate text-tinta-900">
                  {parte.participante.nombre} {parte.participante.apellido}
                  {esPagador ? (
                    <span className="ml-2 rounded-full bg-marca-100 px-2 py-0.5 text-xs font-medium text-marca-800">
                      Pagó
                    </span>
                  ) : null}
                  {/* Las partes solo se muestran cuando aportan algo: en un reparto
                      equitativo son todas 1 y repetirlo es ruido. */}
                  {repartoDesigual ? (
                    <span className="ml-2 rounded-full bg-tinta-100 px-2 py-0.5 text-xs text-tinta-600">
                      {parte.peso} {parte.peso === 1 ? 'parte' : 'partes'}
                    </span>
                  ) : null}
                </span>
                <span className="shrink-0 tabular-nums text-tinta-900">
                  {formatearMonto(parte.montoAdeudado)} USDT
                </span>
              </li>
            )
          })}
        </ul>

        {pagadorNoParticipa ? (
          <p className="mt-4 rounded-md border border-tinta-200 bg-tinta-50 px-3 py-2 text-sm text-tinta-700">
            {gasto.pagador.nombre} pagó este gasto pero no participa del reparto: se
            le debe el total.
          </p>
        ) : null}

        {hayExcluidos && !pagadorNoParticipa ? (
          <p className="mt-4 text-sm text-tinta-600">
            No todos los integrantes del grupo participan de este gasto.
          </p>
        ) : null}
      </section>
    </div>
  )
}
