/**
 * Handlers de MSW para los 6 endpoints que el backend ya expone.
 *
 * Reproducen los contratos reales leídos de AuthService, PerfilService y
 * GlobalExceptionHandler: el formato de error estándar, el mapa `errors` por campo en
 * los 400, el 409 de username duplicado, el 401 de credenciales inválidas, el 401 de
 * token ausente o vencido, y el 200 con CUERPO VACÍO de PUT /api/perfil/me/password.
 */

import { HttpResponse, http } from 'msw'
import type {
  ActualizarPerfilRequest,
  CambiarPasswordRequest,
  CambiarUsernameRequest,
  ErrorResponse,
  LoginRequest,
  RegisterRequest,
} from '../api/types'
import {
  actualizarUsuario,
  aPerfil,
  buscarPorUsername,
  crearUsuario,
  emitirToken,
  existeUsername,
  usuarioDeToken,
} from './db'

// --- Errores en el formato de GlobalExceptionHandler ---

const RAZONES: Record<number, string> = {
  400: 'Bad Request',
  401: 'Unauthorized',
  404: 'Not Found',
  409: 'Conflict',
}

function error(status: number, message: string, path: string, errors?: Record<string, string>) {
  const cuerpo: ErrorResponse = {
    timestamp: new Date().toISOString(),
    status,
    error: RAZONES[status] ?? 'Error',
    message,
    path,
    ...(errors ? { errors } : {}),
  }
  return HttpResponse.json(cuerpo, { status })
}

// --- Validación espejada de las anotaciones @NotBlank / @Size ---

function enBlanco(valor: unknown): boolean {
  return typeof valor !== 'string' || valor.trim().length === 0
}

function validar(
  campos: Record<string, { valor: unknown; min?: number; max?: number }>,
): Record<string, string> | null {
  const errores: Record<string, string> = {}

  for (const [campo, regla] of Object.entries(campos)) {
    if (enBlanco(regla.valor)) {
      errores[campo] = 'no debe estar vacío'
      continue
    }
    const texto = regla.valor as string
    if (regla.min !== undefined && texto.length < regla.min) {
      errores[campo] = `el tamaño debe estar entre ${regla.min} y ${regla.max ?? '∞'}`
    } else if (regla.max !== undefined && texto.length > regla.max) {
      errores[campo] = `el tamaño debe estar entre ${regla.min ?? 0} y ${regla.max}`
    }
  }

  return Object.keys(errores).length > 0 ? errores : null
}

const ERROR_VALIDACION = 'Uno o más campos son inválidos'

// --- Handlers ---

export const handlers = [
  // POST /api/auth/register -> 201 { token, participante }
  http.post('/api/auth/register', async ({ request }) => {
    const ruta = '/api/auth/register'
    const body = (await request.json()) as RegisterRequest

    const errores = validar({
      username: { valor: body.username, min: 3, max: 50 },
      password: { valor: body.password, min: 8 },
      nombre: { valor: body.nombre, max: 100 },
      apellido: { valor: body.apellido, max: 100 },
      ci: { valor: body.ci, max: 20 },
    })
    if (errores) return error(400, ERROR_VALIDACION, ruta, errores)

    if (existeUsername(body.username)) {
      return error(409, `El username ya está en uso: ${body.username}`, ruta)
    }

    const usuario = crearUsuario({
      username: body.username,
      password: body.password,
      nombre: body.nombre,
      apellido: body.apellido,
      ci: body.ci,
    })

    return HttpResponse.json(
      {
        token: emitirToken(usuario),
        participante: {
          id: usuario.id,
          nombre: usuario.nombre,
          apellido: usuario.apellido,
          ci: usuario.ci,
          username: usuario.username,
        },
      },
      { status: 201 },
    )
  }),

  // POST /api/auth/login -> 200 { token, usuario }
  http.post('/api/auth/login', async ({ request }) => {
    const ruta = '/api/auth/login'
    const body = (await request.json()) as LoginRequest

    const errores = validar({
      username: { valor: body.username },
      password: { valor: body.password },
    })
    if (errores) return error(400, ERROR_VALIDACION, ruta, errores)

    const usuario = buscarPorUsername(body.username)
    // Mismo 401 exista o no el username: no se filtra cuál de los dos falló.
    if (!usuario || usuario.password !== body.password) {
      return error(401, 'Credenciales inválidas', ruta)
    }

    return HttpResponse.json({
      token: emitirToken(usuario),
      usuario: { id: usuario.id, username: usuario.username },
    })
  }),

  // GET /api/perfil/me -> 200 PerfilResponse
  http.get('/api/perfil/me', ({ request }) => {
    const usuario = usuarioDeToken(request.headers.get('Authorization'))
    if (!usuario) return error(401, 'Token inválido o expirado', '/api/perfil/me')
    return HttpResponse.json(aPerfil(usuario))
  }),

  // PUT /api/perfil/me -> 200 PerfilResponse (el ci NO es editable)
  http.put('/api/perfil/me', async ({ request }) => {
    const ruta = '/api/perfil/me'
    const usuario = usuarioDeToken(request.headers.get('Authorization'))
    if (!usuario) return error(401, 'Token inválido o expirado', ruta)

    const body = (await request.json()) as ActualizarPerfilRequest
    const errores = validar({
      nombre: { valor: body.nombre, max: 100 },
      apellido: { valor: body.apellido, max: 100 },
    })
    if (errores) return error(400, ERROR_VALIDACION, ruta, errores)

    // El ci se ignora aunque venga en el cuerpo, igual que en PerfilService.
    const actualizado = actualizarUsuario(usuario.id, {
      nombre: body.nombre,
      apellido: body.apellido,
    })!
    return HttpResponse.json(aPerfil(actualizado))
  }),

  // PUT /api/perfil/me/username -> 200 PerfilResponse | 409
  http.put('/api/perfil/me/username', async ({ request }) => {
    const ruta = '/api/perfil/me/username'
    const usuario = usuarioDeToken(request.headers.get('Authorization'))
    if (!usuario) return error(401, 'Token inválido o expirado', ruta)

    const body = (await request.json()) as CambiarUsernameRequest
    const errores = validar({ username: { valor: body.username, min: 3, max: 50 } })
    if (errores) return error(400, ERROR_VALIDACION, ruta, errores)

    // Reenviar el username propio sin cambios es un no-op exitoso.
    if (body.username !== usuario.username && existeUsername(body.username)) {
      return error(409, `El username ya está en uso: ${body.username}`, ruta)
    }

    const actualizado = actualizarUsuario(usuario.id, { username: body.username })!
    return HttpResponse.json(aPerfil(actualizado))
  }),

  // PUT /api/perfil/me/password -> 200 CON CUERPO VACÍO (el controller devuelve void)
  http.put('/api/perfil/me/password', async ({ request }) => {
    const ruta = '/api/perfil/me/password'
    const usuario = usuarioDeToken(request.headers.get('Authorization'))
    if (!usuario) return error(401, 'Token inválido o expirado', ruta)

    const body = (await request.json()) as CambiarPasswordRequest
    const errores = validar({ password: { valor: body.password, min: 8 } })
    if (errores) return error(400, ERROR_VALIDACION, ruta, errores)

    actualizarUsuario(usuario.id, { password: body.password })
    return new HttpResponse(null, { status: 200 })
  }),
]
