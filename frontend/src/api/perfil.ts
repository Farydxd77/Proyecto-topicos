import { apiFetch } from './client'
import type {
  ActualizarPerfilRequest,
  CambiarPasswordRequest,
  CambiarUsernameRequest,
  PerfilResponse,
} from './types'

export function obtenerPerfil(): Promise<PerfilResponse> {
  return apiFetch<PerfilResponse>('/perfil/me')
}

export function actualizarPerfil(
  datos: ActualizarPerfilRequest,
): Promise<PerfilResponse> {
  return apiFetch<PerfilResponse>('/perfil/me', { method: 'PUT', body: datos })
}

export function cambiarUsername(
  datos: CambiarUsernameRequest,
): Promise<PerfilResponse> {
  return apiFetch<PerfilResponse>('/perfil/me/username', { method: 'PUT', body: datos })
}

/** El backend responde 200 con cuerpo vacío: no hay nada que devolver. */
export function cambiarPassword(datos: CambiarPasswordRequest): Promise<void> {
  return apiFetch<void>('/perfil/me/password', { method: 'PUT', body: datos })
}
