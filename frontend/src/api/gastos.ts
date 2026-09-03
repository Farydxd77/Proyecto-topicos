import { apiFetch } from './client'
import type {
  ActualizarGastoRequest,
  GastoResponse,
  GastoResumenDto,
  RegistrarGastoRequest,
} from './types'

/**
 * Los gastos cuelgan del grupo: `/api/grupos/{grupoId}/gastos`.
 *
 * Todas las operaciones exigen ser MIEMBRO del grupo, no creador: cualquier
 * integrante registra, edita y elimina. 403 si no es miembro.
 */

const base = (grupoId: number) => `/grupos/${grupoId}/gastos`

export function listarGastos(grupoId: number): Promise<GastoResumenDto[]> {
  return apiFetch<GastoResumenDto[]>(base(grupoId))
}

export function obtenerGasto(grupoId: number, gastoId: number): Promise<GastoResponse> {
  return apiFetch<GastoResponse>(`${base(grupoId)}/${gastoId}`)
}

/**
 * El backend convierte a USDT contra CriptoYa y reparte entre todos los miembros.
 * 400 si la moneda no está soportada o el pagador no es miembro.
 * 503 si el servicio de cotización no responde (USDT no lo consulta).
 */
export function registrarGasto(
  grupoId: number,
  datos: RegistrarGastoRequest,
): Promise<GastoResponse> {
  return apiFetch<GastoResponse>(base(grupoId), { method: 'POST', body: datos })
}

/** Recalcula conversión y reparto: puede aplicar una tasa distinta a la original. */
export function actualizarGasto(
  grupoId: number,
  gastoId: number,
  datos: ActualizarGastoRequest,
): Promise<GastoResponse> {
  return apiFetch<GastoResponse>(`${base(grupoId)}/${gastoId}`, {
    method: 'PUT',
    body: datos,
  })
}

/** Responde 204 sin cuerpo. */
export function eliminarGasto(grupoId: number, gastoId: number): Promise<void> {
  return apiFetch<void>(`${base(grupoId)}/${gastoId}`, { method: 'DELETE' })
}
