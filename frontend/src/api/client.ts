/**
 * Único punto por el que pasa toda llamada al backend.
 *
 * Concentra la inyección del token, la traducción de errores y la reacción al 401, de
 * modo que ninguna pantalla vea nunca una Response cruda ni tenga que preguntarse qué
 * forma tiene un error.
 */

import { leerTokenVigente } from '../auth/token'
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
const MENSAJE_TIEMPO =
  'El servidor no respondió a tiempo. Revisá que el backend esté corriendo e intentá de nuevo.'
const MENSAJE_SIN_BACKEND =
  'El servidor no está respondiendo. Revisá que el backend esté corriendo e intentá de nuevo.'

/**
 * Sin este límite, una petición puede quedar colgada indefinidamente y la pantalla
 * nunca sale del estado de carga.
 *
 * No es hipotético: con el backend detenido, el proxy del servidor de desarrollo
 * acepta la conexión y no la cierra, así que `fetch` no rechaza nunca. Un timeout
 * convierte ese cuelgue en un error que la interfaz puede mostrar y reintentar.
 */
const TIEMPO_LIMITE_MS = 10_000

// --- Token de la sesión activa ---

/**
 * Por defecto el token se lee del almacenamiento, NO de un getter vacío.
 *
 * React ejecuta los efectos de los hijos antes que los del padre, así que una
 * pantalla protegida puede disparar su consulta antes de que el AuthProvider haya
 * registrado su getter. Con un `() => null` por defecto esa primera petición salía
 * sin Authorization, el backend respondía 401 y la sesión se cerraba sola al
 * recargar la página. Leer el almacenamiento es la misma fuente que usa el provider
 * para inicializarse, así que ambos caminos coinciden.
 *
 * Se importa `token.ts`, que no importa nada de `api/`: no hay ciclo. Lo que no se
 * importa es el contexto de React, que sí lo crearía.
 */
let obtenerToken: () => string | null = leerTokenVigente
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

/**
 * Lee el cuerpo sin poder colgarse.
 *
 * Un `await respuesta.text()` que nunca resuelve deja la promesa de `apiFetch`
 * pendiente para siempre, y con ella la pantalla en estado de carga: ni error, ni
 * reintento, ni salida. Pasa con respuestas de error cuyo cuerpo queda a medio
 * cerrar, como el `502` que emite el proxy de desarrollo cuando el backend no está
 * levantado. La carrera contra un temporizador corto garantiza que el error llegue
 * igual, aunque sea sin detalle.
 */
async function leerCuerpo(respuesta: Response): Promise<string> {
  const limite = new Promise<string>((resolver) => setTimeout(() => resolver(''), 2000))
  try {
    return await Promise.race([respuesta.text(), limite])
  } catch {
    return ''
  }
}

async function aApiError(respuesta: Response): Promise<ApiError> {
  let cuerpo: Partial<ErrorResponse> | null = null

  // Un error puede venir sin cuerpo, con cuerpo vacío o con algo que no es JSON.
  // Ninguno de los tres debe romper: se cae al mensaje genérico.
  try {
    const texto = await leerCuerpo(respuesta)
    if (texto) cuerpo = JSON.parse(texto) as ErrorResponse
  } catch {
    cuerpo = null
  }

  // Un 502/504 sin cuerpo lo emite el proxy, no el backend: significa que Spring Boot
  // no está respondiendo. Decirlo así ahorra buscar el problema en el lugar
  // equivocado. Un 503 CON cuerpo sí viene del backend (CriptoYa caído) y conserva su
  // mensaje.
  const esProxySinBackend =
    !cuerpo?.message && (respuesta.status === 502 || respuesta.status === 504)

  return new ApiError(
    respuesta.status,
    cuerpo?.message || (esProxySinBackend ? MENSAJE_SIN_BACKEND : MENSAJE_GENERICO),
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
      signal: AbortSignal.timeout(TIEMPO_LIMITE_MS),
    })
  } catch (fallo) {
    // Se distingue el cuelgue de la caída: son dos diagnósticos distintos para quien
    // lo lee, y el primero suele significar "el backend no está levantado".
    const agotado = fallo instanceof DOMException && fallo.name === 'TimeoutError'
    throw new ApiError(0, agotado ? MENSAJE_TIEMPO : MENSAJE_RED)
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
