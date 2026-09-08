import { useMemo, useState, type FormEvent } from 'react'
import { ApiError } from '../api/client'
import type {
  GastoResponse,
  GrupoResponse,
  ParticipanteDto,
  RegistrarGastoRequest,
} from '../api/types'
import { formatearMonto } from '../lib/formato'
import { CRIPTOS, FIATS, MONEDA_POR_DEFECTO, nombreDe } from '../lib/monedas'
import {
  soloErrores,
  validarDescripcionGasto,
  validarDivision,
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

type ModoReparto = 'equitativo' | 'personalizado'

/** Fila del reparto: un miembro del grupo, si participa y con cuántas partes. */
interface FilaReparto {
  participante: ParticipanteDto
  incluido: boolean
  /** Texto y no número: el campo es editable y puede quedar vacío mientras se tipea. */
  partes: string
}

/**
 * Estado inicial del reparto a partir del gasto que se edita.
 *
 * Se deduce el modo en vez de guardarlo: un gasto en el que participan todos los
 * miembros con peso 1 es indistinguible de uno equitativo, y mostrarlo como
 * personalizado sería ruido. Si falta alguien o algún peso difiere de 1, el reparto
 * fue a mano y hay que abrir la sección para que se pueda ver.
 */
function repartoInicial(
  miembros: ParticipanteDto[],
  gasto?: GastoResponse,
): { modo: ModoReparto; filas: FilaReparto[] } {
  const porId = new Map(gasto?.division.map((d) => [d.participante.id, d.peso]) ?? [])

  const filas = miembros.map((participante) => {
    const peso = porId.get(participante.id)
    return {
      participante,
      incluido: gasto ? peso !== undefined : true,
      partes: String(peso ?? 1),
    }
  })

  const personalizado =
    gasto != null &&
    (filas.some((f) => !f.incluido) || filas.some((f) => f.incluido && f.partes !== '1'))

  return { modo: personalizado ? 'personalizado' : 'equitativo', filas }
}

/**
 * Previsualización del reparto, con la misma regla que el backend: proporcional al
 * peso, a 2 decimales, y el absorbente se calcula por resta para que la suma cierre.
 *
 * Es solo una estimación —el reparto que vale es el que devuelve el backend, sobre
 * el monto ya convertido a USDT—, así que se calcula sobre el monto tal como se
 * tipeó y se etiqueta como tal en pantalla.
 */
function previsualizarReparto(
  monto: number,
  incluidos: { participante: ParticipanteDto; peso: number }[],
  pagadorId: number,
): Map<number, number> {
  const resultado = new Map<number, number>()
  if (incluidos.length === 0) return resultado

  const pesoTotal = incluidos.reduce((suma, p) => suma + p.peso, 0)
  if (pesoTotal <= 0) return resultado

  const absorbente =
    incluidos.find((p) => p.participante.id === pagadorId) ??
    incluidos.reduce((mejor, p) =>
      p.peso > mejor.peso || (p.peso === mejor.peso && p.participante.id < mejor.participante.id)
        ? p
        : mejor,
    )

  let repartido = 0
  for (const p of incluidos) {
    if (p.participante.id === absorbente.participante.id) continue
    const parte = Math.round((monto * p.peso * 100) / pesoTotal) / 100
    resultado.set(p.participante.id, parte)
    repartido += parte
  }
  resultado.set(
    absorbente.participante.id,
    Math.round((monto - repartido) * 100) / 100,
  )
  return resultado
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

  const inicial = useMemo(() => repartoInicial(grupo.miembros, gasto), [grupo.miembros, gasto])
  const [modoReparto, setModoReparto] = useState<ModoReparto>(inicial.modo)
  const [filas, setFilas] = useState<FilaReparto[]>(inicial.filas)

  /** Las filas que efectivamente se van a mandar, con las partes ya numéricas. */
  const incluidos = filas
    .filter((f) => f.incluido)
    .map((f) => ({ participante: f.participante, peso: Number(f.partes) }))

  const montoPrevisualizado = Number(monto.trim().replace(',', '.'))
  const previsualizacion =
    modoReparto === 'personalizado' && Number.isFinite(montoPrevisualizado)
      ? previsualizarReparto(montoPrevisualizado, incluidos, pagadorId)
      : null

  function cambiarFila(participanteId: number, cambio: Partial<FilaReparto>) {
    setFilas((actuales) =>
      actuales.map((f) =>
        f.participante.id === participanteId ? { ...f, ...cambio } : f,
      ),
    )
  }

  // Un 503 no es un error de lo que se cargó: es el servicio de cotización que no
  // respondió. Se separa del resto para no mandar a revisar datos que están bien.
  const cotizacionCaida = error instanceof ApiError && error.status === 503

  function enviar(e: FormEvent) {
    e.preventDefault()
    const encontrados = soloErrores({
      descripcion: validarDescripcionGasto(descripcion),
      monto: validarMonto(monto),
      fecha: validarFecha(fecha),
      division: modoReparto === 'personalizado' ? validarDivision(incluidos) : null,
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
      // En modo equitativo se OMITE el campo. Mandar la lista completa con peso 1
      // daría el mismo reparto, pero omitirlo es lo que el backend documenta como
      // valor por defecto y deja el gasto libre de una división explícita.
      division:
        modoReparto === 'personalizado'
          ? incluidos.map((p) => ({ participanteId: p.participante.id, peso: p.peso }))
          : undefined,
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
          <label htmlFor="gasto-moneda" className="text-sm font-medium text-tinta-700">
            Moneda
          </label>
          <select
            id="gasto-moneda"
            value={moneda}
            onChange={(e) => setMoneda(e.target.value)}
            className="rounded-md border border-tinta-300 bg-white px-3 py-2 text-tinta-900 outline-none focus:border-marca-500 focus:ring-2 focus:ring-marca-200"
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
          <label htmlFor="gasto-pagador" className="text-sm font-medium text-tinta-700">
            ¿Quién pagó?
          </label>
          {/* Solo miembros: el backend responde 400 si el pagador no lo es. */}
          <select
            id="gasto-pagador"
            value={pagadorId}
            onChange={(e) => setPagadorId(Number(e.target.value))}
            className="rounded-md border border-tinta-300 bg-white px-3 py-2 text-tinta-900 outline-none focus:border-marca-500 focus:ring-2 focus:ring-marca-200"
          >
            {grupo.miembros.map((m) => (
              <option key={m.id} value={m.id}>
                {m.nombre} {m.apellido}
              </option>
            ))}
          </select>
        </div>

        <div className="flex min-w-40 flex-col gap-1">
          <label htmlFor="gasto-fecha" className="text-sm font-medium text-tinta-700">
            Fecha
          </label>
          <input
            id="gasto-fecha"
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
      </div>

      <fieldset className="rounded-md border border-tinta-200 p-4">
        <legend className="px-1 text-sm font-medium text-tinta-700">
          Cómo se reparte
        </legend>

        <div className="flex flex-col gap-2">
          <label className="flex items-center gap-2 text-sm text-tinta-800">
            <input
              type="radio"
              name="modo-reparto"
              checked={modoReparto === 'equitativo'}
              onChange={() => setModoReparto('equitativo')}
              className="accent-marca-600"
            />
            Entre todos, en partes iguales
          </label>
          <label className="flex items-center gap-2 text-sm text-tinta-800">
            <input
              type="radio"
              name="modo-reparto"
              checked={modoReparto === 'personalizado'}
              onChange={() => setModoReparto('personalizado')}
              className="accent-marca-600"
            />
            Personalizado: elegir quién participa y con cuántas partes
          </label>
        </div>

        {modoReparto === 'personalizado' ? (
          <div className="mt-4 flex flex-col gap-2">
            <ul className="flex flex-col divide-y divide-tinta-100 rounded-md border border-tinta-200">
              {filas.map((fila) => {
                const estimado = previsualizacion?.get(fila.participante.id)
                return (
                  <li
                    key={fila.participante.id}
                    className="flex flex-wrap items-center gap-3 px-3 py-2"
                  >
                    <label className="flex min-w-40 flex-1 items-center gap-2 text-sm text-tinta-800">
                      <input
                        type="checkbox"
                        checked={fila.incluido}
                        onChange={(e) =>
                          cambiarFila(fila.participante.id, { incluido: e.target.checked })
                        }
                        className="accent-marca-600"
                      />
                      <span className="truncate">
                        {fila.participante.nombre} {fila.participante.apellido}
                        {fila.participante.id === pagadorId ? (
                          <span className="ml-2 rounded-full bg-tinta-100 px-2 py-0.5 text-xs text-tinta-600">
                            pagó
                          </span>
                        ) : null}
                      </span>
                    </label>

                    <div className="flex items-center gap-2">
                      <label
                        htmlFor={`partes-${fila.participante.id}`}
                        className="text-xs text-tinta-500"
                      >
                        Partes
                      </label>
                      <input
                        id={`partes-${fila.participante.id}`}
                        type="number"
                        min={1}
                        max={1000}
                        step={1}
                        value={fila.partes}
                        disabled={!fila.incluido}
                        onChange={(e) =>
                          cambiarFila(fila.participante.id, { partes: e.target.value })
                        }
                        className="w-20 rounded-md border border-tinta-300 bg-white px-2 py-1 text-tinta-900 outline-none focus:border-marca-500 focus:ring-2 focus:ring-marca-200 disabled:bg-tinta-100 disabled:text-tinta-400"
                      />
                    </div>

                    <span className="w-28 shrink-0 text-right text-sm tabular-nums text-tinta-600">
                      {fila.incluido && estimado !== undefined
                        ? `≈ ${formatearMonto(estimado)}`
                        : '—'}
                    </span>
                  </li>
                )
              })}
            </ul>

            <p className="text-xs text-tinta-500">
              Los montos son una estimación sobre el monto que cargaste. El reparto
              definitivo lo calcula el servidor sobre el equivalente en USDT.
            </p>

            {errores.division ? (
              <p role="alert" className="text-sm text-contra-700">
                {errores.division}
              </p>
            ) : null}
          </div>
        ) : null}
      </fieldset>

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
