/**
 * Único punto por el que pasa toda llamada al backend.
 *
 * Concentra la inyección del token, la traducción de errores y la reacción al 401, de
 * modo que ninguna pantalla vea nunca una Response cruda ni tenga que preguntarse qué
 * forma tiene un error.
 */

import type { ErrorResponse } from './types'

const BASE = '/api'

/** Rutas públicas: no llevan Authorization y su 401 significa "credenciales inválidas". */
const RUTAS_PUBLICAS = '/auth/'

/**
 * Error normalizado de la API. Todo fallo —del backend, de la red o de un cuerpo
 * ilegible— llega a la interfaz con esta forma.
 */
export class ApiError extends Error {
  readonly status: number
  /** Mensajes por campo. Solo viene en los 400 de validación. */
  readonly errors?: Record<string, string>

  constructor(status: number, message: string, errors?: Record<string, string>) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.errors = errors
  }
}

const MENSAJE_GENERICO = 'Ocurrió un error inesperado. Intentá de nuevo.'
const MENSAJE_RED = 'No se pudo conectar. Revisá tu conexión e intentá de nuevo.'

// --- Token de la sesión activa ---

let obtenerToken: () => string | null = () => null
let alExpirarSesion: () => void = () => {}

/**
 * El AuthProvider registra acá cómo leer el token y qué hacer cuando el backend lo
 * rechaza. Se inyecta en lugar de importar el contexto para no crear un ciclo entre
 * la capa de API y la de sesión.
 */
export function configurarSesion(opciones: {
  obtenerToken: () => string | null
  alExpirar: () => void
}): void {
  obtenerToken = opciones.obtenerToken
  alExpirarSesion = opciones.alExpirar
}

// --- Traducción de respuestas ---

async function aApiError(respuesta: Response): Promise<ApiError> {
  let cuerpo: Partial<ErrorResponse> | null = null

  // Un error puede venir sin cuerpo, con cuerpo vacío o con algo que no es JSON.
  // Ninguno de los tres debe romper: se cae al mensaje genérico.
  try {
    const texto = await respuesta.text()
    if (texto) cuerpo = JSON.parse(texto) as ErrorResponse
  } catch {
    cuerpo = null
  }

  return new ApiError(
    respuesta.status,
    cuerpo?.message || MENSAJE_GENERICO,
    cuerpo?.errors,
  )
}

async function aDatos<T>(respuesta: Response): Promise<T> {
  // PUT /api/perfil/me/password responde 200 con cuerpo vacío: es un éxito, no un
  // error de parseo.
  const texto = await respuesta.text()
  if (!texto) return undefined as T
  return JSON.parse(texto) as T
}

// --- Cliente ---

export async function apiFetch<T>(
  ruta: string,
  opciones: { method?: string; body?: unknown } = {},
): Promise<T> {
  const esPublica = ruta.startsWith(RUTAS_PUBLICAS)
  const token = esPublica ? null : obtenerToken()

  const cabeceras: Record<string, string> = {}
  if (opciones.body !== undefined) cabeceras['Content-Type'] = 'application/json'
  if (token) cabeceras.Authorization = `Bearer ${token}`

  let respuesta: Response
  try {
    respuesta = await fetch(`${BASE}${ruta}`, {
      method: opciones.method ?? 'GET',
      headers: cabeceras,
      body: opciones.body !== undefined ? JSON.stringify(opciones.body) : undefined,
    })
  } catch {
    // Fallo de red: nunca hubo respuesta.
    throw new ApiError(0, MENSAJE_RED)
  }

  if (respuesta.ok) return aDatos<T>(respuesta)

  const error = await aApiError(respuesta)

  // Un 401 en una ruta protegida significa que el token no sirve: se cierra la sesión.
  // En /auth/** significa credenciales inválidas y debe llegar al formulario tal cual.
  if (error.status === 401 && !esPublica) {
    alExpirarSesion()
  }

  throw error
}
