import { apiFetch } from './client'
import type { CriterioBusqueda, ParticipanteDto } from './types'

/**
 * Busca participantes por un único criterio.
 *
 * Se envía solo el parámetro elegido y no los tres: el backend aplica precedencia
 * `ci` > `nombre` > `apellido` e ignora el resto, así que mandar varios daría a
 * entender que se combinan cuando no es así.
 *
 * Devuelve siempre un array, vacío si no hay coincidencias.
 */
export function buscarParticipantes(
  criterio: CriterioBusqueda,
  valor: string,
): Promise<ParticipanteDto[]> {
  const parametros = new URLSearchParams({ [criterio]: valor })
  return apiFetch<ParticipanteDto[]>(`/participantes?${parametros}`)
}
