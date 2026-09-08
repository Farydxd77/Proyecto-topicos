import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router'
import {
  abandonarGrupo,
  agregarMiembro,
  quitarMiembro,
  transferirCreador,
} from '../api/grupos'
import { buscarParticipantes } from '../api/participantes'
import type { CriterioBusqueda, GrupoResponse, ParticipanteDto } from '../api/types'
import { CLAVE_GRUPOS, claveGrupo, clavesDerivadasDelGrupo } from '../lib/claves'
import { estadoDe } from '../lib/estadoConsulta'
import { Boton } from './Boton'
import { Card } from './Card'
import { Etiqueta } from './Etiqueta'
import { MensajeError } from './MensajeError'

const CRITERIOS: { valor: CriterioBusqueda; etiqueta: string }[] = [
  { valor: 'nombre', etiqueta: 'Nombre' },
  { valor: 'apellido', etiqueta: 'Apellido' },
  { valor: 'ci', etiqueta: 'CI (exacto)' },
]

const CLASE_SELECT =
  'rounded-lg border border-tinta-300 bg-white px-3 py-2 text-tinta-900 transition-colors hover:border-tinta-400'

function Persona({ p }: { p: ParticipanteDto }) {
  return (
    <div className="min-w-0">
      <p className="truncate text-tinta-900">
        {p.nombre} {p.apellido}
      </p>
      <p className="truncate text-sm text-tinta-500">
        {p.username} · CI {p.ci}
      </p>
    </div>
  )
}

function Buscador({ grupo }: { grupo: GrupoResponse }) {
  const queryClient = useQueryClient()
  const [criterio, setCriterio] = useState<CriterioBusqueda>('nombre')
  const [texto, setTexto] = useState('')
  // Lo que se busca de verdad: se fija al confirmar, no al teclear. Sin esto cada
  // pulsación pegaría al backend contra un endpoint sin paginación.
  const [consultado, setConsultado] = useState<{
    criterio: CriterioBusqueda
    valor: string
  } | null>(null)

  const busqueda = useQuery({
    queryKey: ['participantes', consultado?.criterio, consultado?.valor],
    queryFn: () => buscarParticipantes(consultado!.criterio, consultado!.valor),
    enabled: consultado !== null,
  })

  const eBusqueda = estadoDe(busqueda)

  const alta = useMutation({
    mutationFn: (participanteId: number) => agregarMiembro(grupo.id, { participanteId }),
    onSuccess: (actualizado) => {
      // El backend devuelve el grupo completo: se siembra para evitar un viaje de
      // más y el parpadeo de la lista de miembros.
      queryClient.setQueryData(claveGrupo(grupo.id), actualizado)
      queryClient.invalidateQueries({ queryKey: claveGrupo(grupo.id) })
      // Cambiar la composición del grupo cambia el reparto de cada gasto.
      for (const clave of clavesDerivadasDelGrupo(grupo.id)) {
        queryClient.invalidateQueries({ queryKey: clave })
      }
    },
  })

  function enviar(e: FormEvent) {
    e.preventDefault()
    const valor = texto.trim()
    if (!valor) return
    setConsultado({ criterio, valor })
  }

  const yaEsMiembro = (id: number) => grupo.miembros.some((m) => m.id === id)

  return (
    <div className="flex flex-col gap-4">
      <form onSubmit={enviar} className="flex flex-wrap items-end gap-3">
        <div className="flex flex-col gap-1.5">
          <label htmlFor="criterio" className="text-sm font-medium text-tinta-800">
            Buscar por
          </label>
          <select
            id="criterio"
            value={criterio}
            onChange={(e) => setCriterio(e.target.value as CriterioBusqueda)}
            className={CLASE_SELECT}
          >
            {CRITERIOS.map((c) => (
              <option key={c.valor} value={c.valor}>
                {c.etiqueta}
              </option>
            ))}
          </select>
        </div>

        <div className="flex min-w-48 flex-1 flex-col gap-1.5">
          <label htmlFor="texto-busqueda" className="text-sm font-medium text-tinta-800">
            Texto
          </label>
          <input
            id="texto-busqueda"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            placeholder="Nombre de la persona"
            className="rounded-lg border border-tinta-300 bg-white px-3 py-2 text-tinta-900 transition-colors placeholder:text-tinta-400 hover:border-tinta-400"
          />
        </div>

        <Boton type="submit" variante="secundario">
          Buscar
        </Boton>
      </form>

      <MensajeError error={alta.error} />

      {eBusqueda.cargando && busqueda.fetchStatus !== 'idle' ? (
        <p className="text-sm text-tinta-500">Buscando…</p>
      ) : null}

      {eBusqueda.error ? (
        <div className="flex flex-col items-start gap-2">
          <MensajeError error={eBusqueda.error} />
          <Boton variante="secundario" onClick={() => busqueda.refetch()}>
            Reintentar
          </Boton>
        </div>
      ) : null}

      {eBusqueda.datos && eBusqueda.datos.length === 0 ? (
        <p className="text-sm text-tinta-600">No se encontró a nadie con ese criterio.</p>
      ) : null}

      {eBusqueda.datos && eBusqueda.datos.length > 0 ? (
        <ul className="flex flex-col divide-y divide-tinta-100 rounded-lg border border-tinta-200">
          {eBusqueda.datos.map((p) => (
            <li key={p.id} className="flex items-center justify-between gap-3 px-3 py-2.5">
              <Persona p={p} />
              {yaEsMiembro(p.id) ? (
                // Se marca en vez de ofrecer agregar: evita el 409 antes de que ocurra.
                <Etiqueta>Ya es miembro</Etiqueta>
              ) : (
                <div className="shrink-0">
                  <Boton tamano="chico" onClick={() => alta.mutate(p.id)} enCurso={alta.isPending}>
                    Agregar
                  </Boton>
                </div>
              )}
            </li>
          ))}
        </ul>
      ) : null}
    </div>
  )
}

function ConfirmarQuitar({
  grupo,
  persona,
  onCerrar,
}: {
  grupo: GrupoResponse
  persona: ParticipanteDto
  onCerrar: () => void
}) {
  const queryClient = useQueryClient()

  const baja = useMutation({
    mutationFn: () => quitarMiembro(grupo.id, persona.id),
    onSuccess: () => {
      // El 204 no trae estado nuevo que sembrar: se invalida y se relee.
      queryClient.invalidateQueries({ queryKey: claveGrupo(grupo.id) })
      for (const clave of clavesDerivadasDelGrupo(grupo.id)) {
        queryClient.invalidateQueries({ queryKey: clave })
      }
      onCerrar()
    },
  })

  return (
    <div className="rounded-lg border border-aviso-200 bg-aviso-50 p-4">
      <p className="text-sm font-medium text-aviso-900">
        ¿Quitar a {persona.nombre} {persona.apellido} del grupo?
      </p>
      <p className="mt-1 text-sm text-aviso-900/80">
        Dejará de ver el grupo y sus gastos. Si queda con saldo pendiente, el grupo
        tendrá que decidir si se hace cargo.
      </p>

      <div className="mt-3">
        <MensajeError error={baja.error} />
      </div>

      <div className="mt-3 flex gap-3">
        <Boton variante="peligro" onClick={() => baja.mutate()} enCurso={baja.isPending}>
          Sí, quitar
        </Boton>
        <Boton variante="secundario" onClick={onCerrar}>
          Cancelar
        </Boton>
      </div>
    </div>
  )
}

/**
 * Transferencia del rol de creador. Es irreversible desde el lado de quien lo
 * entrega: una vez transferido no puede recuperarlo por su cuenta, así que la
 * confirmación nombra a la persona en lugar de preguntar en abstracto.
 */
function TransferirRol({ grupo }: { grupo: GrupoResponse }) {
  const queryClient = useQueryClient()
  const [elegido, setElegido] = useState<number | null>(null)

  const candidatos = grupo.miembros.filter((m) => m.id !== grupo.creador.id)

  const transferencia = useMutation({
    mutationFn: (participanteId: number) => transferirCreador(grupo.id, { participanteId }),
    onSuccess: (actualizado) => {
      // Devuelve el grupo completo: se siembra para que las acciones de gestión
      // cambien de dueño en el mismo render, sin un viaje de más.
      queryClient.setQueryData(claveGrupo(grupo.id), actualizado)
      queryClient.invalidateQueries({ queryKey: claveGrupo(grupo.id) })
      queryClient.invalidateQueries({ queryKey: CLAVE_GRUPOS })
      setElegido(null)
    },
  })

  if (candidatos.length === 0) {
    return (
      <p className="text-sm text-tinta-600">
        Sos el único integrante, así que no hay a quién transferirle el rol. Si querés
        dejar el grupo, eliminalo.
      </p>
    )
  }

  const persona = candidatos.find((c) => c.id === elegido) ?? null

  return (
    <div className="flex flex-col gap-3">
      <div className="flex min-w-48 flex-col gap-1.5">
        <label htmlFor="nuevo-creador" className="text-sm font-medium text-tinta-800">
          Nuevo creador
        </label>
        <select
          id="nuevo-creador"
          value={elegido ?? ''}
          onChange={(e) => setElegido(e.target.value ? Number(e.target.value) : null)}
          className={CLASE_SELECT}
        >
          <option value="">Elegí a un miembro…</option>
          {candidatos.map((c) => (
            <option key={c.id} value={c.id}>
              {c.nombre} {c.apellido} ({c.username})
            </option>
          ))}
        </select>
      </div>

      {persona ? (
        <div className="rounded-lg border border-aviso-200 bg-aviso-50 p-4">
          <p className="text-sm font-medium text-aviso-900">
            ¿Transferirle el grupo a {persona.nombre} {persona.apellido}?
          </p>
          <p className="mt-1 text-sm text-aviso-900/80">
            Pasará a ser el creador y vos dejarás de poder editar el grupo, agregar o
            quitar miembros. Seguirás siendo miembro. No vas a poder revertirlo por tu
            cuenta.
          </p>

          <div className="mt-3">
            <MensajeError error={transferencia.error} />
          </div>

          <div className="mt-3 flex gap-3">
            <Boton
              onClick={() => transferencia.mutate(persona.id)}
              enCurso={transferencia.isPending}
            >
              Sí, transferir
            </Boton>
            <Boton variante="secundario" onClick={() => setElegido(null)}>
              Cancelar
            </Boton>
          </div>
        </div>
      ) : null}
    </div>
  )
}

/** Salida voluntaria de un miembro que no es el creador. */
function AbandonarGrupo({
  grupo,
  participanteId,
}: {
  grupo: GrupoResponse
  participanteId: number
}) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [confirmando, setConfirmando] = useState(false)

  const salida = useMutation({
    mutationFn: () => abandonarGrupo(grupo.id, participanteId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: CLAVE_GRUPOS })
      queryClient.removeQueries({ queryKey: claveGrupo(grupo.id) })
      navigate('/grupos', { replace: true })
    },
  })

  if (!confirmando) {
    return (
      <Boton variante="secundario" onClick={() => setConfirmando(true)}>
        Abandonar grupo
      </Boton>
    )
  }

  return (
    <div className="rounded-lg border border-aviso-200 bg-aviso-50 p-4">
      <p className="text-sm font-medium text-aviso-900">¿Abandonar «{grupo.nombre}»?</p>
      <p className="mt-1 text-sm text-aviso-900/80">
        Dejarás de ver sus gastos y sus balances. Si quedás con saldo pendiente, el
        grupo tendrá que decidir si se hace cargo.
      </p>

      <div className="mt-3">
        <MensajeError error={salida.error} />
      </div>

      <div className="mt-3 flex gap-3">
        <Boton variante="peligro" onClick={() => salida.mutate()} enCurso={salida.isPending}>
          Sí, abandonar
        </Boton>
        <Boton variante="secundario" onClick={() => setConfirmando(false)}>
          Cancelar
        </Boton>
      </div>
    </div>
  )
}

export function GestionMiembros({
  grupo,
  esCreador,
  participanteId,
}: {
  grupo: GrupoResponse
  esCreador: boolean
  /** El participante propio, o null mientras el perfil no esté disponible. */
  participanteId: number | null
}) {
  const [quitando, setQuitando] = useState<ParticipanteDto | null>(null)

  return (
    <div className="flex flex-col gap-4">
      <Card titulo={`Integrantes (${grupo.miembros.length})`}>
        <ul className="flex flex-col divide-y divide-tinta-100">
          {grupo.miembros.map((miembro) => {
            const esElCreador = miembro.id === grupo.creador.id
            const soyYo = miembro.id === participanteId
            return (
              <li
                key={miembro.id}
                className="flex items-center justify-between gap-3 py-2.5"
              >
                <div className="flex min-w-0 items-center gap-2">
                  <Persona p={miembro} />
                  {esElCreador ? <Etiqueta tono="marca">Creador</Etiqueta> : null}
                  {soyYo && !esElCreador ? <Etiqueta>Vos</Etiqueta> : null}
                </div>

                {/* La regla del backend está enunciada sobre el creador del grupo, no
                    sobre quien pide: se espeja tal cual. */}
                {esCreador && !esElCreador ? (
                  <div className="shrink-0">
                    <Boton
                      variante="fantasma"
                      tamano="chico"
                      onClick={() => setQuitando(miembro)}
                    >
                      Quitar
                    </Boton>
                  </div>
                ) : null}
              </li>
            )
          })}
        </ul>

        {quitando ? (
          <div className="mt-4">
            <ConfirmarQuitar
              grupo={grupo}
              persona={quitando}
              onCerrar={() => setQuitando(null)}
            />
          </div>
        ) : null}
      </Card>

      {esCreador ? (
        <>
          <Card
            titulo="Agregar miembro"
            descripcion="Buscá a la persona por nombre, apellido o CI."
          >
            <Buscador grupo={grupo} />
          </Card>

          <Card
            titulo="Transferir el rol de creador"
            descripcion="Para salir del grupo primero tenés que pasarle el rol a otro miembro, o eliminar el grupo."
          >
            <TransferirRol grupo={grupo} />
          </Card>
        </>
      ) : participanteId != null ? (
        <Card titulo="Salir del grupo">
          <AbandonarGrupo grupo={grupo} participanteId={participanteId} />
        </Card>
      ) : null}
    </div>
  )
}
