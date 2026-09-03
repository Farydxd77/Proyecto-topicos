import { apiFetch } from './client'
import type {
  ActualizarGrupoRequest,
  AgregarMiembroRequest,
  CrearGrupoRequest,
  GrupoResponse,
  GrupoResumenDto,
} from './types'

/** Los grupos donde el usuario autenticado es miembro. Vacío si no pertenece a ninguno. */
export function listarGrupos(): Promise<GrupoResumenDto[]> {
  return apiFetch<GrupoResumenDto[]>('/grupos')
}

/** 403 si no es miembro, 404 si el grupo no existe. El 404 se evalúa antes que el 403. */
export function obtenerGrupo(id: number): Promise<GrupoResponse> {
  return apiFetch<GrupoResponse>(`/grupos/${id}`)
}

/** Crea el grupo con el usuario autenticado como creador y primer miembro. */
export function crearGrupo(datos: CrearGrupoRequest): Promise<GrupoResponse> {
  return apiFetch<GrupoResponse>('/grupos', { method: 'POST', body: datos })
}

/** Reservado al creador: 403 para cualquier otro miembro. */
export function actualizarGrupo(
  id: number,
  datos: ActualizarGrupoRequest,
): Promise<GrupoResponse> {
  return apiFetch<GrupoResponse>(`/grupos/${id}`, { method: 'PUT', body: datos })
}

/** Reservado al creador. Responde 204 sin cuerpo y borra en cascada las membresías. */
export function eliminarGrupo(id: number): Promise<void> {
  return apiFetch<void>(`/grupos/${id}`, { method: 'DELETE' })
}

/**
 * Reservado al creador. Devuelve el grupo COMPLETO ya actualizado, así que su
 * respuesta se puede sembrar en el caché del detalle sin pedirlo de nuevo.
 * 409 si ya es miembro, 404 si el participante no existe.
 */
export function agregarMiembro(
  grupoId: number,
  datos: AgregarMiembroRequest,
): Promise<GrupoResponse> {
  return apiFetch<GrupoResponse>(`/grupos/${grupoId}/miembros`, {
    method: 'POST',
    body: datos,
  })
}

/**
 * Reservado al creador. Responde 204 sin cuerpo.
 * 400 si el participanteId es el del creador, 404 si no es miembro del grupo.
 */
export function quitarMiembro(grupoId: number, participanteId: number): Promise<void> {
  return apiFetch<void>(`/grupos/${grupoId}/miembros/${participanteId}`, {
    method: 'DELETE',
  })
}
