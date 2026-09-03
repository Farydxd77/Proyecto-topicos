import { useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import type { GastoResponse, GrupoResponse, RegistrarGastoRequest } from '../api/types'
import { CRIPTOS, FIATS, MONEDA_POR_DEFECTO, nombreDe } from '../lib/monedas'
import {
  soloErrores,
  validarDescripcionGasto,
  validarFecha,
  validarMonto,
} from '../lib/validacion'
import { Boton } from './Boton'
import { Campo } from './Campo'
import { MensajeError } from './MensajeError'

function hoy(): string {
  // Fecha local en YYYY-MM-DD. `toISOString` daría UTC y en Bolivia adelantaría el
  // día durante buena parte de la tarde.
  const d = new Date()
  const mes = String(d.getMonth() + 1).padStart(2, '0')
  const dia = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${mes}-${dia}`
}

export function FormularioGasto({
  grupo,
  gasto,
  enCurso,
  error,
  onEnviar,
  onCancelar,
}: {
  grupo: GrupoResponse
  /** Si viene, el formulario edita ese gasto; si no, registra uno nuevo. */
  gasto?: GastoResponse
  enCurso: boolean
  error: unknown
  onEnviar: (datos: RegistrarGastoRequest) => void
  onCancelar: () => void
}) {
  const [descripcion, setDescripcion] = useState(gasto?.descripcion ?? '')
  const [monto, setMonto] = useState(gasto ? String(gasto.monto) : '')
  const [moneda, setMoneda] = useState(gasto?.moneda ?? MONEDA_POR_DEFECTO)
  const [pagadorId, setPagadorId] = useState(
    gasto?.pagador.id ?? grupo.miembros[0]?.id ?? 0,
  )
  const [fecha, setFecha] = useState(gasto?.fecha ?? hoy())
  const [errores, setErrores] = useState<Record<string, string>>({})

  // Un 503 no es un error de lo que se cargó: es el servicio de cotización que no
  // respondió. Se separa del resto para no mandar a revisar datos que están bien.
  const cotizacionCaida = error instanceof ApiError && error.status === 503

  function enviar(e: FormEvent) {
    e.preventDefault()
    const encontrados = soloErrores({
      descripcion: validarDescripcionGasto(descripcion),
      monto: validarMonto(monto),
      fecha: validarFecha(fecha),
    })
    setErrores(encontrados)
    if (Object.keys(encontrados).length > 0) return

    onEnviar({
      descripcion: descripcion.trim(),
      monto: monto.trim().replace(',', '.'),
      moneda,
      monedaNombre: nombreDe(moneda),
      pagadorId,
      fecha,
    })
  }

  return (
    <form onSubmit={enviar} className="flex flex-col gap-4">
      <Campo
        id="gasto-descripcion"
        etiqueta="Descripción"
        valor={descripcion}
        onChange={setDescripcion}
        error={errores.descripcion}
        ayuda="Por ejemplo: Cena del sábado"
      />

      <div className="flex flex-wrap gap-4">
        <div className="min-w-40 flex-1">
          <Campo
            id="gasto-monto"
            etiqueta="Monto"
            valor={monto}
            onChange={setMonto}
            error={errores.monto}
          />
        </div>

        <div className="flex min-w-44 flex-col gap-1">
          <label htmlFor="gasto-moneda" className="text-sm font-medium text-slate-700">
            Moneda
          </label>
          <select
            id="gasto-moneda"
            value={moneda}
            onChange={(e) => setMoneda(e.target.value)}
            className="rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200"
          >
            <optgroup label="Monedas">
              {FIATS.map((m) => (
                <option key={m.simbolo} value={m.simbolo}>
                  {m.simbolo} — {m.nombre}
                </option>
              ))}
            </optgroup>
            <optgroup label="Criptomonedas">
              {CRIPTOS.map((m) => (
                <option key={m.simbolo} value={m.simbolo}>
                  {m.simbolo} — {m.nombre}
                </option>
              ))}
            </optgroup>
          </select>
        </div>
      </div>

      <div className="flex flex-wrap gap-4">
        <div className="flex min-w-52 flex-1 flex-col gap-1">
          <label htmlFor="gasto-pagador" className="text-sm font-medium text-slate-700">
            ¿Quién pagó?
          </label>
          {/* Solo miembros: el backend responde 400 si el pagador no lo es. */}
          <select
            id="gasto-pagador"
            value={pagadorId}
            onChange={(e) => setPagadorId(Number(e.target.value))}
            className="rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200"
          >
            {grupo.miembros.map((m) => (
              <option key={m.id} value={m.id}>
                {m.nombre} {m.apellido}
              </option>
            ))}
          </select>
        </div>

        <div className="flex min-w-40 flex-col gap-1">
          <label htmlFor="gasto-fecha" className="text-sm font-medium text-slate-700">
            Fecha
          </label>
          <input
            id="gasto-fecha"
            type="date"
            value={fecha}
            onChange={(e) => setFecha(e.target.value)}
            className="rounded-md border border-slate-300 bg-white px-3 py-2 text-slate-900 outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-200"
          />
          {errores.fecha ? (
            <p role="alert" className="text-sm text-red-600">
              {errores.fecha}
            </p>
          ) : null}
        </div>
      </div>

      {cotizacionCaida ? (
        <MensajeError
          error="No se pudo obtener la cotización en este momento. Tus datos están bien: probá de nuevo en unos segundos."
          tono="aviso"
        />
      ) : (
        <MensajeError error={error} />
      )}

      <div className="flex gap-3">
        <Boton type="submit" enCurso={enCurso}>
          {cotizacionCaida ? 'Reintentar' : gasto ? 'Guardar cambios' : 'Registrar gasto'}
        </Boton>
        <Boton variante="secundario" onClick={onCancelar}>
          Cancelar
        </Boton>
      </div>
    </form>
  )
}
