import { apiFetch } from './client'
import type { BalanceDto, ResumenGrupoDto, TransferenciaDto } from './types'

/**
 * Balances y liquidación. Ambos exigen ser miembro del grupo: 403 si no lo es,
 * 404 si el grupo no existe (evaluado antes que el 403).
 *
 * Son dos endpoints y no uno derivado del otro: reducir las deudas a la lista
 * MÍNIMA de transferencias es el algoritmo de BalanceUtil, ya implementado y
 * probado en el backend. No se reimplementa acá.
 */

/** Devuelve a todos los integrantes, incluidos los que están en cero. Suma 0. */
export function obtenerBalances(grupoId: number): Promise<BalanceDto[]> {
  return apiFetch<BalanceDto[]>(`/grupos/${grupoId}/balances`)
}

/** Lista mínima de transferencias para saldar. Vacía si no hay deudas. */
export function obtenerLiquidacion(grupoId: number): Promise<TransferenciaDto[]> {
  return apiFetch<TransferenciaDto[]>(`/grupos/${grupoId}/liquidacion`)
}

/**
 * Totales del grupo: cuánto se gastó, cuánto se saldó, cuánto falta y qué le toca a
 * quien consulta. Los calcula el backend sobre los gastos y pagos guardados; acá no
 * se suma nada (ver la convención de `lib/formato.ts`).
 */
export function obtenerResumen(grupoId: number): Promise<ResumenGrupoDto> {
  return apiFetch<ResumenGrupoDto>(`/grupos/${grupoId}/resumen`)
}
