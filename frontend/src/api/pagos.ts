import { apiFetch } from './client'
import type { ActualizarPagoRequest, PagoResponse, RegistrarPagoRequest } from './types'

/**
 * Los pagos cuelgan del grupo: `/api/grupos/{grupoId}/pagos`.
 *
 * Consultar exige ser MIEMBRO del grupo. Registrar también, y el backend fija como
 * pagador al participante del token: no se puede registrar un pago ajeno. Editar y
 * eliminar están reservados a quien registró el pago (403 para los demás miembros).
 */

const base = (grupoId: number) => `/grupos/${grupoId}/pagos`

/** Ordenados por fecha descendente. `[]` si el grupo no tiene pagos. */
export function listarPagos(grupoId: number): Promise<PagoResponse[]> {
  return apiFetch<PagoResponse[]>(base(grupoId))
}

export function obtenerPago(grupoId: number, pagoId: number): Promise<PagoResponse> {
  return apiFetch<PagoResponse>(`${base(grupoId)}/${pagoId}`)
}

/**
 * 400 si el receptor no es miembro del grupo, si coincide con el pagador, o si el
 * monto es inválido. El monto va siempre en USDT: no hay conversión de moneda.
 */
export function registrarPago(
  grupoId: number,
  datos: RegistrarPagoRequest,
): Promise<PagoResponse> {
  return apiFetch<PagoResponse>(base(grupoId), { method: 'POST', body: datos })
}

/** 403 si quien pide no es quien registró el pago. El pagador nunca cambia. */
export function actualizarPago(
  grupoId: number,
  pagoId: number,
  datos: ActualizarPagoRequest,
): Promise<PagoResponse> {
  return apiFetch<PagoResponse>(`${base(grupoId)}/${pagoId}`, {
    method: 'PUT',
    body: datos,
  })
}

/** Responde 204 sin cuerpo. 403 si quien pide no es quien registró el pago. */
export function eliminarPago(grupoId: number, pagoId: number): Promise<void> {
  return apiFetch<void>(`${base(grupoId)}/${pagoId}`, { method: 'DELETE' })
}
