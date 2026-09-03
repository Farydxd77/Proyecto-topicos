import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { agregarMiembro, quitarMiembro } from '../api/grupos'
import { buscarParticipantes } from '../api/participantes'
import type { CriterioBusqueda, GrupoResponse, ParticipanteDto } from '../api/types'
import { claveGrupo, clavesDerivadasDelGrupo } from '../lib/claves'
import { estadoDe } from '../lib/estadoConsulta'
import { Boton } from './Boton'
import { MensajeError } from './MensajeError'

const CRITERIOS: { valor: CriterioBusqueda; etiqueta: string }[] = [
  { valor: 'ci', etiqueta: 'CI (exacto)' },
  { valor: 'nombre', etiqueta: 'Nombre' },
  { valor: 'apellido', etiqueta: 'Apellido' },
]

function Persona({ p }: { p: ParticipanteDto }) {
  return (
    <div className="min-w-0">
      <p className="truncate text-slate-900">
        {p.nombre} {p.apellido}
      </p>
      <p className="truncate text-sm text-slate-500">
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
    mutationFn: (participanteId: number) =>
      agregarMiembro(grupo.id, { participanteId }),
    onSuccess: (actualizado) => {
      // El backend devuelve el grupo completo: se siembra para evitar un viaje de
      // más y el parpadeo de la lista de miembros.
      queryClient.setQueryData(claveGrupo(grupo.id), actualizado)
      queryClient.invalidateQueries({ queryKey: claveGrupo(grupo.id) })
      // Cambiar la composicion del grupo cambia el reparto de cada gasto.
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
        <div className="flex flex-col gap-1">
          <label htmlFor="criterio" className="text-sm font-medium text-slate-700">
            Buscar por
          </label>
          <select
            id="criterio"
            value={criterio}
            onChange={(e) => setCriterio(e.target.value as CriterioBusqueda)}
            className="rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200"
          >
            {CRITERIOS.map((c) => (
              <option key={c.valor} value={c.valor}>
                {c.etiqueta}
              </option>
            ))}
          </select>
        </div>

        <div className="flex min-w-48 flex-1 flex-col gap-1">
          <label htmlFor="texto-busqueda" className="text-sm font-medium text-slate-700">
            Texto
          </label>
          <input
            id="texto-busqueda"
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            className="rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200"
          />
        </div>

        <Boton type="submit">Buscar</Boton>
      </form>

      <MensajeError error={alta.error} />

      {eBusqueda.cargando && busqueda.fetchStatus !== "idle" ? <p className="text-sm text-slate-500">Buscando…</p> : null}

      {eBusqueda.error ? (
        <div className="flex flex-col items-start gap-2">
          <MensajeError error={eBusqueda.error} />
          <Boton variante="secundario" onClick={() => busqueda.refetch()}>
            Reintentar
          </Boton>
        </div>
      ) : null}

      {eBusqueda.datos && eBusqueda.datos.length === 0 ? (
        <p className="text-sm text-slate-600">
          No se encontró a nadie con ese criterio.
        </p>
      ) : null}

      {eBusqueda.datos && eBusqueda.datos.length > 0 ? (
        <ul className="flex flex-col divide-y divide-slate-100 rounded-md border border-slate-200">
          {eBusqueda.datos.map((p) => (
            <li key={p.id} className="flex items-center justify-between gap-3 px-3 py-2">
              <Persona p={p} />
              {yaEsMiembro(p.id) ? (
                // Se marca en vez de ofrecer agregar: evita el 409 antes de que ocurra.
                <span className="shrink-0 rounded-full bg-slate-100 px-2 py-1 text-xs text-slate-600">
                  Ya es miembro
                </span>
              ) : (
                <div className="shrink-0">
                  <Boton onClick={() => alta.mutate(p.id)} enCurso={alta.isPending}>
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
    <div className="rounded-md border border-red-300 bg-red-50 p-4">
      <p className="text-sm font-medium text-red-900">
        ¿Quitar a {persona.nombre} {persona.apellido} del grupo?
      </p>
      <p className="mt-1 text-sm text-red-800">
        Dejará de ver el grupo y sus gastos.
      </p>

      <div className="mt-3">
        <MensajeError error={baja.error} />
      </div>

      <div className="mt-3 flex gap-3">
        <Boton onClick={() => baja.mutate()} enCurso={baja.isPending}>
          Sí, quitar
        </Boton>
        <Boton variante="secundario" onClick={onCerrar}>
          Cancelar
        </Boton>
      </div>
    </div>
  )
}

export function GestionMiembros({
  grupo,
  esCreador,
}: {
  grupo: GrupoResponse
  esCreador: boolean
}) {
  const [quitando, setQuitando] = useState<ParticipanteDto | null>(null)

  return (
    <section className="rounded-lg border border-slate-200 bg-white p-5">
      <h2 className="mb-4 font-semibold text-slate-900">
        Miembros ({grupo.miembros.length})
      </h2>

      <ul className="flex flex-col divide-y divide-slate-100">
        {grupo.miembros.map((miembro) => {
          const esElCreador = miembro.id === grupo.creador.id
          return (
            <li key={miembro.id} className="flex items-center justify-between gap-3 py-2">
              <div className="flex min-w-0 items-center gap-2">
                <Persona p={miembro} />
                {esElCreador ? (
                  <span className="shrink-0 rounded-full bg-emerald-100 px-2 py-0.5 text-xs font-medium text-emerald-800">
                    Creador
                  </span>
                ) : null}
              </div>

              {/* La regla del backend está enunciada sobre el creador del grupo, no
                  sobre quien pide: se espeja tal cual. */}
              {esCreador && !esElCreador ? (
                <div className="shrink-0">
                  <Boton variante="secundario" onClick={() => setQuitando(miembro)}>
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

      {esCreador ? (
        <div className="mt-6 border-t border-slate-100 pt-5">
          <h3 className="mb-3 text-sm font-semibold text-slate-900">Agregar miembro</h3>
          <Buscador grupo={grupo} />
        </div>
      ) : null}
    </section>
  )
}
