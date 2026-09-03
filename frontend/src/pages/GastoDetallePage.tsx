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
        <p role="alert" className="rounded-md border border-amber-300 bg-amber-50 px-3 py-2 text-sm text-amber-800">
          {texto}
        </p>
      ) : (
        <MensajeError error={error} />
      )}
      <Link
        to={status === 403 ? '/grupos' : `/grupos/${grupoId}`}
        className="text-sm font-medium text-emerald-700 hover:underline"
      >
        {status === 403 ? 'Volver a mis grupos' : 'Volver al grupo'}
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
      navigate(`/grupos/${grupoId}`, { replace: true })
    },
  })

  const estado = estadoDe(consulta)

  if (estado.cargando) return <p className="text-slate-500">Cargando el gasto…</p>
  if (estado.error || !estado.datos) {
    return <SinAcceso error={estado.error} grupoId={grupoId} />
  }

  const gasto = estado.datos

  return (
    <div className="flex flex-col gap-6">
      <Link
        to={`/grupos/${grupoId}`}
        className="text-sm text-slate-500 hover:text-slate-800"
      >
        ← Volver al grupo
      </Link>

      {editando && grupoConsulta.data ? (
        <section className="rounded-lg border border-slate-200 bg-white p-5">
          <h2 className="mb-4 font-semibold text-slate-900">Editar gasto</h2>
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
            <h1 className="text-2xl font-semibold text-slate-900">
              {gasto.descripcion}
            </h1>
            <p className="mt-1 text-slate-600">
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
        <div className="rounded-lg border border-red-300 bg-red-50 p-5">
          <p className="font-medium text-red-900">
            ¿Eliminar el gasto «{gasto.descripcion}»?
          </p>
          <p className="mt-1 text-sm text-red-800">
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

      <section className="rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="mb-3 font-semibold text-slate-900">Importe</h2>
        <dl className="flex flex-col gap-2 text-sm">
          <div className="flex justify-between gap-4">
            <dt className="text-slate-600">Monto pagado</dt>
            <dd>
              <MontoConEquivalente
                monto={gasto.monto}
                moneda={gasto.moneda}
                montoUsdt={gasto.montoUsdt}
              />
            </dd>
          </div>
          <div className="flex justify-between gap-4">
            <dt className="text-slate-600">Moneda</dt>
            <dd className="text-slate-900">
              {gasto.monedaNombre} ({gasto.moneda})
            </dd>
          </div>
          {!esUsdt(gasto.moneda) ? (
            <div className="flex justify-between gap-4">
              <dt className="text-slate-600">Tasa aplicada al registrarlo</dt>
              <dd className="text-slate-900">
                1 {gasto.moneda} = {formatearMonto(gasto.tasaCambio)} USDT
              </dd>
            </div>
          ) : null}
        </dl>
      </section>

      <section className="rounded-lg border border-slate-200 bg-white p-5">
        <h2 className="font-semibold text-slate-900">Cómo se reparte</h2>
        <p className="mt-1 mb-4 text-sm text-slate-600">
          Cada integrante le debe esta parte a {gasto.pagador.nombre}. Los montos están
          en USDT.
        </p>
        <ul className="flex flex-col divide-y divide-slate-100">
          {gasto.division.map((parte) => {
            const esPagador = parte.participante.id === gasto.pagador.id
            return (
              <li
                key={parte.participante.id}
                className="flex items-center justify-between gap-3 py-2"
              >
                <span className="min-w-0 truncate text-slate-900">
                  {parte.participante.nombre} {parte.participante.apellido}
                  {esPagador ? (
                    <span className="ml-2 rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-800">
                      Pagó
                    </span>
                  ) : null}
                </span>
                <span className="shrink-0 tabular-nums text-slate-900">
                  {formatearMonto(parte.montoAdeudado)} USDT
                </span>
              </li>
            )
          })}
        </ul>
      </section>
    </div>
  )
}
