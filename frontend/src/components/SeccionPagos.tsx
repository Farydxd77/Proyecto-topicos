import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { actualizarPago, eliminarPago, listarPagos, registrarPago } from '../api/pagos'
import type { GrupoResponse, PagoResponse, RegistrarPagoRequest } from '../api/types'
import { clavePagos, clavesDerivadasDelGrupo } from '../lib/claves'
import { estadoDe } from '../lib/estadoConsulta'
import { Boton } from './Boton'
import { Card } from './Card'
import { EstadoVacio } from './EstadoVacio'
import { FormularioPago } from './FormularioPago'
import { MensajeError } from './MensajeError'
import { Monto } from './Monto'

/** Valores con los que abrir el formulario desde la liquidación. */
export interface PagoPrecargado {
  receptorId: number
  monto: string
}

function FilaPago({
  pago,
  esPropio,
  onEditar,
  onEliminar,
}: {
  pago: PagoResponse
  esPropio: boolean
  onEditar: () => void
  onEliminar: () => void
}) {
  return (
    <li className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-tinta-200 bg-white p-3.5">
      <div className="min-w-0">
        <p className="truncate text-tinta-900">
          <span className="font-medium">
            {pago.pagador.nombre} {pago.pagador.apellido}
          </span>
          <span className="mx-2 text-tinta-400">→</span>
          <span className="font-medium">
            {pago.receptor.nombre} {pago.receptor.apellido}
          </span>
        </p>
        <p className="truncate text-sm text-tinta-500">
          {pago.fecha}
          {pago.txId ? (
            <>
              {' · '}
              {/* Un hash es largo y sin espacios: se trunca y el valor completo queda
                  en el title, para no romper el ancho en pantallas chicas. */}
              <span className="font-mono" title={pago.txId}>
                {pago.txId.length > 18 ? `${pago.txId.slice(0, 18)}…` : pago.txId}
              </span>
            </>
          ) : null}
        </p>
      </div>

      <div className="flex shrink-0 items-center gap-3">
        <Monto valor={pago.monto} />
        {/* El backend reserva editar y borrar a quien registró el pago: ofrecerlos
            sobre un pago ajeno sería ofrecer un 403. */}
        {esPropio ? (
          <div className="flex gap-1.5">
            <Boton variante="fantasma" tamano="chico" onClick={onEditar}>
              Editar
            </Boton>
            <Boton variante="fantasma" tamano="chico" onClick={onEliminar}>
              Eliminar
            </Boton>
          </div>
        ) : null}
      </div>
    </li>
  )
}

function ConfirmarEliminar({
  grupoId,
  pago,
  onCerrar,
}: {
  grupoId: number
  pago: PagoResponse
  onCerrar: () => void
}) {
  const queryClient = useQueryClient()

  const baja = useMutation({
    mutationFn: () => eliminarPago(grupoId, pago.id),
    onSuccess: () => {
      for (const clave of clavesDerivadasDelGrupo(grupoId)) {
        queryClient.invalidateQueries({ queryKey: clave })
      }
      onCerrar()
    },
  })

  return (
    <Card tono="aviso">
      <p className="font-medium text-tinta-900">
        ¿Eliminar el pago de <Monto valor={pago.monto} tamano="chico" /> a{' '}
        {pago.receptor.nombre}?
      </p>
      <p className="mt-1 text-sm text-tinta-700">
        La deuda que saldaba vuelve a aparecer en los balances y en la liquidación.
      </p>

      <div className="mt-3">
        <MensajeError error={baja.error} />
      </div>

      <div className="mt-3 flex gap-3">
        <Boton variante="peligro" onClick={() => baja.mutate()} enCurso={baja.isPending}>
          Sí, eliminar
        </Boton>
        <Boton variante="secundario" onClick={onCerrar}>
          Cancelar
        </Boton>
      </div>
    </Card>
  )
}

export function SeccionPagos({
  grupo,
  participanteId,
  precargado,
  onPrecargadoConsumido,
}: {
  grupo: GrupoResponse
  /** El participante propio, o null mientras el perfil no esté disponible. */
  participanteId: number | null
  /** Valores que llegan desde la liquidación; abren el formulario al recibirlos. */
  precargado: PagoPrecargado | null
  onPrecargadoConsumido: () => void
}) {
  const queryClient = useQueryClient()
  const [registrando, setRegistrando] = useState(false)
  const [editando, setEditando] = useState<PagoResponse | null>(null)
  const [eliminando, setEliminando] = useState<PagoResponse | null>(null)

  const consulta = useQuery({
    queryKey: clavePagos(grupo.id),
    queryFn: () => listarPagos(grupo.id),
  })
  const estado = estadoDe(consulta)

  function invalidarTodo() {
    for (const clave of clavesDerivadasDelGrupo(grupo.id)) {
      queryClient.invalidateQueries({ queryKey: clave })
    }
  }

  const alta = useMutation({
    mutationFn: (datos: RegistrarPagoRequest) => registrarPago(grupo.id, datos),
    onSuccess: () => {
      invalidarTodo()
      setRegistrando(false)
      onPrecargadoConsumido()
    },
  })

  const edicion = useMutation({
    mutationFn: ({ id, datos }: { id: number; datos: RegistrarPagoRequest }) =>
      actualizarPago(grupo.id, id, datos),
    onSuccess: () => {
      invalidarTodo()
      setEditando(null)
    },
  })

  // La liquidación pide abrir el formulario: se traduce a estado local acá, que es
  // donde vive el formulario.
  const formularioAbierto = registrando || precargado !== null
  const hayPagos = (estado.datos?.length ?? 0) > 0

  function cerrarAlta() {
    alta.reset()
    setRegistrando(false)
    onPrecargadoConsumido()
  }

  return (
    <div className="flex flex-col gap-4">
      {formularioAbierto && participanteId != null ? (
        <Card titulo="Nuevo pago">
          <FormularioPago
            grupo={grupo}
            participanteId={participanteId}
            receptorIdInicial={precargado?.receptorId}
            montoInicial={precargado?.monto}
            enCurso={alta.isPending}
            error={alta.error}
            onEnviar={(datos) => alta.mutate(datos)}
            onCancelar={cerrarAlta}
          />
        </Card>
      ) : null}

      {editando ? (
        <Card titulo="Editar pago">
          <FormularioPago
            grupo={grupo}
            participanteId={editando.pagador.id}
            pago={editando}
            enCurso={edicion.isPending}
            error={edicion.error}
            onEnviar={(datos) => edicion.mutate({ id: editando.id, datos })}
            onCancelar={() => {
              edicion.reset()
              setEditando(null)
            }}
          />
        </Card>
      ) : null}

      {eliminando ? (
        <ConfirmarEliminar
          grupoId={grupo.id}
          pago={eliminando}
          onCerrar={() => setEliminando(null)}
        />
      ) : null}

      <Card
        descripcion="Transferencias ya hechas entre integrantes, en USDT. Descuentan de los balances."
        accion={
          !formularioAbierto && participanteId != null && hayPagos ? (
            <Boton onClick={() => setRegistrando(true)}>Registrar pago</Boton>
          ) : null
        }
      >
        {estado.cargando ? (
          <p className="text-sm text-tinta-500">Cargando los pagos…</p>
        ) : null}

        {estado.error ? (
          <div className="flex flex-col items-start gap-2">
            <MensajeError error={estado.error} />
            <Boton variante="secundario" onClick={() => consulta.refetch()}>
              Reintentar
            </Boton>
          </div>
        ) : null}

        {!estado.cargando && !estado.error && !hayPagos && !formularioAbierto ? (
          <EstadoVacio
            titulo="Todavía no se registró ningún pago"
            descripcion="Cuando alguien le transfiera plata a otro para saldar su parte, registralo acá y los balances se actualizan solos."
            accion={
              participanteId != null ? (
                <Boton onClick={() => setRegistrando(true)}>Registrar pago</Boton>
              ) : undefined
            }
          />
        ) : null}

        {hayPagos ? (
          <ul className="flex flex-col gap-2">
            {estado.datos?.map((pago) => (
              <FilaPago
                key={pago.id}
                pago={pago}
                esPropio={participanteId != null && pago.pagador.id === participanteId}
                onEditar={() => setEditando(pago)}
                onEliminar={() => setEliminando(pago)}
              />
            ))}
          </ul>
        ) : null}
      </Card>
    </div>
  )
}
