import { apiFetch } from './client'
import type { BajaGrupoDto, ResolverBajaRequest } from './types'

/**
 * Bajas de participantes que dejaron el grupo con saldo pendiente.
 *
 * Las crea el backend solo: salen de quitar a un miembro o de que alguien abandone.
 * Desde acá se consultan y se resuelven.
 */

const base = (grupoId: number) => `/grupos/${grupoId}/bajas`

/** Cualquier miembro puede verlas. 403 si no lo es. */
export function listarBajas(grupoId: number): Promise<BajaGrupoDto[]> {
  return apiFetch<BajaGrupoDto[]>(base(grupoId))
}

/**
 * Reservado al creador: decide si el grupo asume la deuda.
 * 403 si quien pide no es el creador, 409 si la baja ya fue resuelta.
 */
export function resolverBaja(
  grupoId: number,
  bajaId: number,
  datos: ResolverBajaRequest,
): Promise<BajaGrupoDto> {
  return apiFetch<BajaGrupoDto>(`${base(grupoId)}/${bajaId}`, {
    method: 'PUT',
    body: datos,
  })
}
