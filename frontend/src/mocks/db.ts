/**
 * Almacén en memoria que respalda los mocks de MSW.
 *
 * Reacciona de verdad: registrarse crea una cuenta con la que después se puede iniciar
 * sesión, editar el perfil cambia lo que devuelve la consulta siguiente, y cambiar la
 * contraseña invalida la anterior.
 *
 * Se reinicia con cada recarga de la página. Es a propósito: da un punto de partida
 * conocido en cada prueba.
 */

import type { PerfilResponse } from '../api/types'

export interface RegistroUsuario {
  id: number
  username: string
  password: string
  nombre: string
  apellido: string
  ci: string
  createdAt: string
}

/** Credenciales del usuario semilla, para entrar sin registrarse primero. */
export const USUARIO_SEMILLA = {
  username: 'demo',
  password: 'demo1234',
} as const

const HORAS_24_EN_SEGUNDOS = 24 * 60 * 60

let usuarios: RegistroUsuario[] = []
let proximoId = 1

function sembrar(): void {
  usuarios = [
    {
      id: proximoId++,
      username: USUARIO_SEMILLA.username,
      password: USUARIO_SEMILLA.password,
      nombre: 'Ana',
      apellido: 'Quiroga',
      ci: '7654321',
      createdAt: new Date('2026-01-15T10:30:00').toISOString(),
    },
  ]
}

sembrar()

// --- Consultas ---

export function buscarPorUsername(username: string): RegistroUsuario | undefined {
  return usuarios.find((u) => u.username === username)
}

export function buscarPorId(id: number): RegistroUsuario | undefined {
  return usuarios.find((u) => u.id === id)
}

export function existeUsername(username: string): boolean {
  return usuarios.some((u) => u.username === username)
}

// --- Mutaciones ---

export function crearUsuario(
  datos: Omit<RegistroUsuario, 'id' | 'createdAt'>,
): RegistroUsuario {
  const usuario: RegistroUsuario = {
    ...datos,
    id: proximoId++,
    createdAt: new Date().toISOString(),
  }
  usuarios.push(usuario)
  return usuario
}

export function actualizarUsuario(
  id: number,
  cambios: Partial<Omit<RegistroUsuario, 'id' | 'createdAt'>>,
): RegistroUsuario | undefined {
  const usuario = buscarPorId(id)
  if (!usuario) return undefined
  Object.assign(usuario, cambios)
  return usuario
}

// --- Proyecciones ---

/** El participante y el usuario son 1 a 1, así que el id del perfil es el mismo. */
export function aPerfil(usuario: RegistroUsuario): PerfilResponse {
  return {
    id: usuario.id,
    usuarioId: usuario.id,
    username: usuario.username,
    nombre: usuario.nombre,
    apellido: usuario.apellido,
    ci: usuario.ci,
    createdAt: usuario.createdAt,
  }
}

// --- Tokens ---

function base64url(valor: string): string {
  return btoa(valor).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

/**
 * Emite un JWT estructuralmente válido: tres segmentos base64url con un `exp` real.
 * La firma es de mentira — nadie la verifica de este lado — pero el payload sí es
 * genuino, de modo que auth/token.ts decodifica un `exp` de verdad y la lógica de
 * vencimiento se ejercita en serio.
 */
export function emitirToken(
  usuario: RegistroUsuario,
  segundosDeVida: number = HORAS_24_EN_SEGUNDOS,
): string {
  const ahora = Math.floor(Date.now() / 1000)
  const header = base64url(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const payload = base64url(
    JSON.stringify({
      sub: usuario.username,
      iat: ahora,
      exp: ahora + segundosDeVida,
    }),
  )
  return `${header}.${payload}.firma-simulada`
}

/**
 * Resuelve el usuario de una cabecera Authorization, o null si falta, está mal formada,
 * el token venció o el usuario ya no existe. Los handlers lo traducen a un 401.
 */
export function usuarioDeToken(authorization: string | null): RegistroUsuario | null {
  if (!authorization?.startsWith('Bearer ')) return null

  const token = authorization.slice('Bearer '.length)
  const partes = token.split('.')
  if (partes.length !== 3) return null

  try {
    const payload = JSON.parse(atob(partes[1].replace(/-/g, '+').replace(/_/g, '/')))
    if (typeof payload.exp !== 'number') return null
    if (payload.exp * 1000 <= Date.now()) return null
    return buscarPorUsername(payload.sub) ?? null
  } catch {
    return null
  }
}
