/**
 * Custodia del token en el almacenamiento del navegador.
 *
 * La lectura del `exp` es una optimización de experiencia, NO un control de seguridad:
 * evita mostrar una pantalla privada que el backend va a rechazar un instante después.
 * La verificación real de un token es siempre la del backend; acá solo se decide si
 * vale la pena intentarlo.
 */

const CLAVE = 'cuentas-claras.token'

export function leerToken(): string | null {
  try {
    return localStorage.getItem(CLAVE)
  } catch {
    // Almacenamiento bloqueado (modo privado, permisos): se trata como sin sesión.
    return null
  }
}

export function guardarToken(token: string): void {
  try {
    localStorage.setItem(CLAVE, token)
  } catch {
    // Sin persistencia la sesión igual funciona hasta la próxima recarga.
  }
}

export function borrarToken(): void {
  try {
    localStorage.removeItem(CLAVE)
  } catch {
    // Nada que hacer.
  }
}

/**
 * Decodifica el payload del JWT para leer su `exp`. Devuelve true ante cualquier
 * contenido ilegible —basura, segmentos faltantes, payload sin `exp`—, de modo que el
 * llamador lo trate como ausencia de sesión en lugar de romper.
 */
export function estaVencido(token: string): boolean {
  const partes = token.split('.')
  if (partes.length !== 3) return true

  try {
    const payload = JSON.parse(
      atob(partes[1].replace(/-/g, '+').replace(/_/g, '/')),
    ) as { exp?: unknown }

    if (typeof payload.exp !== 'number') return true
    return payload.exp * 1000 <= Date.now()
  } catch {
    return true
  }
}

/** Token utilizable almacenado, o null. Descarta del almacenamiento el que no sirve. */
export function leerTokenVigente(): string | null {
  const token = leerToken()
  if (!token) return null

  if (estaVencido(token)) {
    borrarToken()
    return null
  }

  return token
}
