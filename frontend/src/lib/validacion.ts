/**
 * Reglas de validación espejadas de las anotaciones del backend.
 *
 * Centralizadas acá para no repetir "mínimo 8 caracteres" en tres formularios. Dan
 * feedback inmediato, pero NO son la única defensa: el backend valida igual y sus
 * errores por campo se muestran cuando llegan.
 *
 * Cada regla devuelve el mensaje en español, o null si el valor es válido.
 */

export const LIMITES = {
  usernameMin: 3,
  usernameMax: 50,
  passwordMin: 8,
  nombreMax: 100,
  apellidoMax: 100,
  ciMax: 20,
} as const

function vacio(valor: string): boolean {
  return valor.trim().length === 0
}

/** username: @NotBlank @Size(min = 3, max = 50) */
export function validarUsername(valor: string): string | null {
  if (vacio(valor)) return 'El username es obligatorio'
  if (valor.length < LIMITES.usernameMin) {
    return `El username debe tener al menos ${LIMITES.usernameMin} caracteres`
  }
  if (valor.length > LIMITES.usernameMax) {
    return `El username no puede superar los ${LIMITES.usernameMax} caracteres`
  }
  return null
}

/** password: @NotBlank @Size(min = 8) */
export function validarPassword(valor: string): string | null {
  if (vacio(valor)) return 'La contraseña es obligatoria'
  if (valor.length < LIMITES.passwordMin) {
    return `La contraseña debe tener al menos ${LIMITES.passwordMin} caracteres`
  }
  return null
}

/** nombre: @NotBlank @Size(max = 100) */
export function validarNombre(valor: string): string | null {
  if (vacio(valor)) return 'El nombre es obligatorio'
  if (valor.length > LIMITES.nombreMax) {
    return `El nombre no puede superar los ${LIMITES.nombreMax} caracteres`
  }
  return null
}

/** apellido: @NotBlank @Size(max = 100) */
export function validarApellido(valor: string): string | null {
  if (vacio(valor)) return 'El apellido es obligatorio'
  if (valor.length > LIMITES.apellidoMax) {
    return `El apellido no puede superar los ${LIMITES.apellidoMax} caracteres`
  }
  return null
}

/** ci: @NotBlank @Size(max = 20) */
export function validarCi(valor: string): string | null {
  if (vacio(valor)) return 'El CI es obligatorio'
  if (valor.length > LIMITES.ciMax) {
    return `El CI no puede superar los ${LIMITES.ciMax} caracteres`
  }
  return null
}

/** Solo presencia: el login no impone tamaños, los rechaza el backend. */
export function validarObligatorio(valor: string, etiqueta: string): string | null {
  return vacio(valor) ? `${etiqueta} es obligatorio` : null
}

/** La confirmación de contraseña es una regla del cliente: el backend no la conoce. */
export function validarConfirmacion(
  password: string,
  confirmacion: string,
): string | null {
  if (vacio(confirmacion)) return 'Confirmá la contraseña'
  if (password !== confirmacion) return 'Las contraseñas no coinciden'
  return null
}

/** Descarta las entradas nulas de un mapa de errores. Vacío significa "válido". */
export function soloErrores(
  candidatos: Record<string, string | null>,
): Record<string, string> {
  return Object.fromEntries(
    Object.entries(candidatos).filter(([, mensaje]) => mensaje !== null),
  ) as Record<string, string>
}
