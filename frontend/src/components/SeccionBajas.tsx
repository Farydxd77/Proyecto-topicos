import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { listarBajas, resolverBaja } from '../api/bajas'
import type { BajaGrupoDto, GrupoResponse } from '../api/types'
import { claveBajas, clavesDerivadasDelGrupo } from '../lib/claves'
import { estadoDe } from '../lib/estadoConsulta'
import { Boton } from './Boton'
import { Card } from './Card'
import { MensajeError } from './MensajeError'
import { Monto } from './Monto'

function nombreDe(baja: BajaGrupoDto) {
  return `${baja.participante.nombre} ${baja.participante.apellido}`
}

/** «Quedó debiendo X» / «Le quedaron debiendo X»: el signo en palabras. */
function leerSaldo(saldo: number): string {
  return saldo < 0 ? 'Quedó debiendo' : 'El grupo le quedó debiendo'
}

function Pendiente({
  grupo,
  baja,
  esCreador,
}: {
  grupo: GrupoResponse
  baja: BajaGrupoDto
  esCreador: boolean
}) {
  const queryClient = useQueryClient()
  const [confirmando, setConfirmando] = useState<'asumir' | 'no-asumir' | null>(null)

  const decision = useMutation({
    mutationFn: (asumir: boolean) => resolverBaja(grupo.id, baja.id, { asumir }),
    onSuccess: () => {
      for (const clave of clavesDerivadasDelGrupo(grupo.id)) {
        queryClient.invalidateQueries({ queryKey: clave })
      }
      setConfirmando(null)
    },
  })

  // Previsualización del reparto, con la misma regla que el backend: partes iguales
  // entre los miembros actuales. Es una estimación: el reparto que vale es el que
  // devuelve el servidor.
  const cuantos = grupo.miembros.length
  const porCabeza = cuantos > 0 ? Math.round((baja.saldo / cuantos) * 100) / 100 : 0

  return (
    <li className="rounded-lg border border-aviso-200 bg-aviso-50 p-4">
      <p className="font-medium text-aviso-900">{nombreDe(baja)} dejó el grupo</p>
      <p className="mt-1 text-sm text-aviso-900/80">
        {leerSaldo(baja.saldo)} <Monto valor={baja.saldo} tamano="chico" absoluto /> ·{' '}
        {baja.fecha}
      </p>

      {!esCreador ? (
        <p className="mt-3 text-sm text-aviso-900/80">
          {grupo.creador.nombre} tiene que decidir si el grupo se hace cargo de esta
          deuda o no.
        </p>
      ) : confirmando === null ? (
        <div className="mt-3 flex flex-wrap gap-3">
          <Boton onClick={() => setConfirmando('asumir')}>El grupo asume la deuda</Boton>
          <Boton variante="secundario" onClick={() => setConfirmando('no-asumir')}>
            No la asumimos
          </Boton>
        </div>
      ) : confirmando === 'asumir' ? (
        <div className="mt-3 rounded-lg border border-aviso-200 bg-white p-4">
          <p className="text-sm font-medium text-tinta-900">
            ¿El grupo se hace cargo de{' '}
            <Monto valor={baja.saldo} tamano="chico" absoluto />?
          </p>
          <p className="mt-1 text-sm text-tinta-700">
            {cuantos === 1 ? (
              <>Lo asumís vos solo, que sos el único integrante que queda. </>
            ) : (
              <>Se reparte en partes iguales entre los {cuantos} integrantes actuales. </>
            )}
            {nombreDe(baja)} queda a mano.
          </p>
          <ul className="mt-3 flex flex-col gap-1 border-t border-tinta-100 pt-3">
            {grupo.miembros.map((m) => (
              <li key={m.id} className="flex justify-between gap-3 text-sm text-tinta-700">
                <span className="truncate">
                  {m.nombre} {m.apellido}
                </span>
                <span className="shrink-0">
                  {porCabeza < 0 ? '−' : '+'}
                  <Monto valor={porCabeza} tamano="chico" absoluto />
                </span>
              </li>
            ))}
          </ul>

          <div className="mt-3">
            <MensajeError error={decision.error} />
          </div>
          <div className="mt-3 flex gap-3">
            <Boton onClick={() => decision.mutate(true)} enCurso={decision.isPending}>
              Sí, lo asumimos
            </Boton>
            <Boton variante="secundario" onClick={() => setConfirmando(null)}>
              Cancelar
            </Boton>
          </div>
        </div>
      ) : (
        <div className="mt-3 rounded-lg border border-aviso-200 bg-white p-4">
          <p className="text-sm font-medium text-tinta-900">
            ¿Dejar esta deuda sin resolver?
          </p>
          <p className="mt-1 text-sm text-tinta-700">
            No cambia ningún balance. Queda anotada aparte para que la arreglen entre
            ustedes por fuera de la aplicación.
          </p>

          <div className="mt-3">
            <MensajeError error={decision.error} />
          </div>
          <div className="mt-3 flex gap-3">
            <Boton onClick={() => decision.mutate(false)} enCurso={decision.isPending}>
              Sí, dejarla sin resolver
            </Boton>
            <Boton variante="secundario" onClick={() => setConfirmando(null)}>
              Cancelar
            </Boton>
          </div>
        </div>
      )}
    </li>
  )
}

function SinResolver({ baja }: { baja: BajaGrupoDto }) {
  return (
    <li className="flex flex-wrap items-center justify-between gap-3 rounded-lg border border-tinta-200 bg-white p-3.5">
      <div className="min-w-0">
        <p className="truncate text-tinta-900">{nombreDe(baja)}</p>
        <p className="truncate text-sm text-tinta-500">
          El grupo decidió no hacerse cargo · {baja.fecha}
        </p>
      </div>
      <Monto
        valor={baja.saldo}
        absoluto
        tono={baja.saldo < 0 ? 'contra' : 'favor'}
      />
    </li>
  )
}

function Asumida({ baja }: { baja: BajaGrupoDto }) {
  return (
    <li className="rounded-lg border border-tinta-200 bg-white p-3.5">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          <p className="truncate text-tinta-900">{nombreDe(baja)}</p>
          <p className="truncate text-sm text-tinta-500">
            El grupo se hizo cargo · {baja.fecha}
          </p>
        </div>
        <Monto valor={baja.saldo} absoluto />
      </div>
      {/* Se muestra el reparto para que un balance alterado por una baja se pueda
          explicar, en vez de aparecer como un número sin origen. */}
      <ul className="mt-2 flex flex-col gap-1 border-t border-tinta-100 pt-2">
        {baja.reparto.map((fila) => (
          <li
            key={fila.participante.id}
            className="flex justify-between gap-3 text-sm text-tinta-600"
          >
            <span className="truncate">
              {fila.participante.nombre} {fila.participante.apellido}
            </span>
            <span className="shrink-0">
              {fila.monto < 0 ? '−' : '+'}
              <Monto valor={fila.monto} tamano="chico" tono="apagado" absoluto />
            </span>
          </li>
        ))}
      </ul>
    </li>
  )
}

export function SeccionBajas({
  grupo,
  esCreador,
}: {
  grupo: GrupoResponse
  esCreador: boolean
}) {
  const consulta = useQuery({
    queryKey: claveBajas(grupo.id),
    queryFn: () => listarBajas(grupo.id),
  })
  const estado = estadoDe(consulta)

  if (estado.error) {
    return (
      <Card titulo="Bajas del grupo">
        <div className="flex flex-col items-start gap-2">
          <MensajeError error={estado.error} />
          <Boton variante="secundario" onClick={() => consulta.refetch()}>
            Reintentar
          </Boton>
        </div>
      </Card>
    )
  }

  const bajas = estado.datos ?? []
  // La sección no ocupa lugar cuando no hay nada que mostrar, que es el caso normal.
  if (bajas.length === 0) return null

  const pendientes = bajas.filter((b) => b.estado === 'PENDIENTE')
  const sinResolver = bajas.filter((b) => b.estado === 'NO_ASUMIDA')
  const asumidas = bajas.filter((b) => b.estado === 'ASUMIDA')

  return (
    <div className="flex flex-col gap-4">
      {pendientes.length > 0 ? (
        <Card
          titulo={`Falta decidir (${pendientes.length})`}
          descripcion="Gente que dejó el grupo con cuentas pendientes."
        >
          <ul className="flex flex-col gap-3">
            {pendientes.map((b) => (
              <Pendiente key={b.id} grupo={grupo} baja={b} esCreador={esCreador} />
            ))}
          </ul>
        </Card>
      ) : null}

      {sinResolver.length > 0 ? (
        <Card
          titulo={`Deudas sin resolver (${sinResolver.length})`}
          descripcion="El grupo decidió no hacerse cargo. Quedan acá para que las arreglen entre ustedes."
        >
          <ul className="flex flex-col gap-2">
            {sinResolver.map((b) => (
              <SinResolver key={b.id} baja={b} />
            ))}
          </ul>
        </Card>
      ) : null}

      {asumidas.length > 0 ? (
        <Card
          titulo={`Deudas asumidas por el grupo (${asumidas.length})`}
          descripcion="Se repartieron entre quienes quedaban al momento de decidirlo."
        >
          <ul className="flex flex-col gap-2">
            {asumidas.map((b) => (
              <Asumida key={b.id} baja={b} />
            ))}
          </ul>
        </Card>
      ) : null}
    </div>
  )
}
