import { useState, type FormEvent } from 'react'
import type { GrupoResponse, PagoResponse, RegistrarPagoRequest } from '../api/types'
import {
  soloErrores,
  validarFecha,
  validarMontoPago,
  validarReceptor,
  validarTxId,
} from '../lib/validacion'
import { Boton } from './Boton'
import { Campo } from './Campo'
import { MensajeError } from './MensajeError'

function hoy(): string {
  const d = new Date()
  const mes = String(d.getMonth() + 1).padStart(2, '0')
  const dia = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mes}-${dia}`
}

export function FormularioPago({
  grupo,
  participanteId,
  pago,
  receptorIdInicial,
  montoInicial,
  enCurso,
  error,
  onEnviar,
  onCancelar,
}: {
  grupo: GrupoResponse
  /** El participante propio: es siempre el pagador, no se elige. */
  participanteId: number
  /** Si viene, el formulario edita ese pago; si no, registra uno nuevo. */
  pago?: PagoResponse
  /** Precarga desde una transferencia sugerida de la liquidación. */
  receptorIdInicial?: number
  montoInicial?: string
  enCurso: boolean
  error: unknown
  onEnviar: (datos: RegistrarPagoRequest) => void
  onCancelar: () => void
}) {
  // Solo los demás: el backend responde 400 si el receptor coincide con el pagador.
  const candidatos = grupo.miembros.filter((m) => m.id !== participanteId)

  const [receptorId, setReceptorId] = useState(
    pago?.receptor.id ?? receptorIdInicial ?? candidatos[0]?.id ?? 0,
  )
  const [monto, setMonto] = useState(
    pago ? String(pago.monto) : (montoInicial ?? ''),
  )
  const [fecha, setFecha] = useState(pago?.fecha ?? hoy())
  const [txId, setTxId] = useState(pago?.txId ?? '')
  const [errores, setErrores] = useState<Record<string, string>>({})

  const yo = grupo.miembros.find((m) => m.id === participanteId)

  if (candidatos.length === 0) {
    return (
      <div className="flex flex-col items-start gap-3">
        <p className="text-sm text-tinta-600">
          Un pago va de una persona a otra, y por ahora sos el único integrante del
          grupo. Agregá a alguien más para poder registrar pagos.
        </p>
        <Boton variante="secundario" onClick={onCancelar}>
          Cerrar
        </Boton>
      </div>
    )
  }

  function enviar(e: FormEvent) {
    e.preventDefault()
    const encontrados = soloErrores({
      receptorId: validarReceptor(receptorId),
      monto: validarMontoPago(monto),
      fecha: validarFecha(fecha),
      txId: validarTxId(txId),
    })
    setErrores(encontrados)
    if (Object.keys(encontrados).length > 0) return

    onEnviar({
      receptorId,
      monto: monto.trim().replace(',', '.'),
      fecha,
      // Vacío es «sin txId», no una cadena vacía: la columna es nullable.
      txId: txId.trim() || undefined,
    })
  }

  return (
    <form onSubmit={enviar} className="flex flex-col gap-4">
      {/* No hay selector de pagador: el backend lo resuelve del token. Ofrecer una
          elección que no existe solo invita a chocar con un 403. */}
      <p className="rounded-md border border-tinta-200 bg-tinta-50 px-3 py-2 text-sm text-tinta-700">
        Se registra a tu nombre{yo ? `: ${yo.nombre} ${yo.apellido}` : ''}. Solo podés
        registrar pagos que hiciste vos.
      </p>

      <div className="flex flex-wrap gap-4">
        <div className="flex min-w-52 flex-1 flex-col gap-1">
          <label htmlFor="pago-receptor" className="text-sm font-medium text-tinta-700">
            ¿A quién le pagaste?
          </label>
          <select
            id="pago-receptor"
            value={receptorId}
            onChange={(e) => setReceptorId(Number(e.target.value))}
            className="rounded-md border border-tinta-300 bg-white px-3 py-2 text-tinta-900 outline-none focus:border-marca-500 focus:ring-2 focus:ring-marca-200"
          >
            {candidatos.map((m) => (
              <option key={m.id} value={m.id}>
                {m.nombre} {m.apellido}
              </option>
            ))}
          </select>
          {errores.receptorId ? (
            <p role="alert" className="text-sm text-contra-700">
              {errores.receptorId}
            </p>
          ) : null}
        </div>

        <div className="min-w-40 flex-1">
          <Campo
            id="pago-monto"
            etiqueta="Monto (USDT)"
            valor={monto}
            onChange={setMonto}
            error={errores.monto}
            ayuda="Los pagos van siempre en USDT"
          />
        </div>
      </div>

      <div className="flex flex-wrap gap-4">
        <div className="flex min-w-40 flex-col gap-1">
          <label htmlFor="pago-fecha" className="text-sm font-medium text-tinta-700">
            Fecha
          </label>
          <input
            id="pago-fecha"
            type="date"
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
            className="rounded-md border border-tinta-300 bg-white px-3 py-2 text-tinta-900 outline-none focus:border-marca-500 focus:ring-2 focus:ring-marca-200"
          />
          {errores.fecha ? (
            <p role="alert" className="text-sm text-contra-700">
              {errores.fecha}
            </p>
          ) : null}
        </div>

        <div className="min-w-52 flex-1">
          <Campo
            id="pago-txid"
            etiqueta="Hash de transacción (opcional)"
            valor={txId}
            onChange={setTxId}
            error={errores.txId}
            ayuda="Se guarda como referencia: no se verifica contra ninguna red"
          />
        </div>
      </div>

      <MensajeError error={error} />

      <div className="flex gap-3">
        <Boton type="submit" enCurso={enCurso}>
          {pago ? 'Guardar cambios' : 'Registrar pago'}
        </Boton>
        <Boton variante="secundario" onClick={onCancelar}>
          Cancelar
        </Boton>
      </div>
    </form>
  )
}
