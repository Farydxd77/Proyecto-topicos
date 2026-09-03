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
  nombreGrupoMax: 100,
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

/** nombre del grupo: @NotBlank @Size(max = 100) */
export function validarNombreGrupo(valor: string): string | null {
  if (vacio(valor)) return 'El nombre del grupo es obligatorio'
  if (valor.length > LIMITES.nombreGrupoMax) {
    return `El nombre no puede superar los ${LIMITES.nombreGrupoMax} caracteres`
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

/** descripcion del gasto: @NotBlank @Size(max = 255) */
export function validarDescripcionGasto(valor: string): string | null {
  if (vacio(valor)) return 'La descripción es obligatoria'
  if (valor.length > 255) return 'La descripción no puede superar los 255 caracteres'
  return null
}

/**
 * monto del gasto: @NotNull @Positive @Digits(integer = 12, fraction = 8)
 *
 * Se valida sobre el texto y no convirtiendo a número, para no arrastrar el error
 * de coma flotante en un valor que puede tener 8 decimales.
 */
export function validarMonto(valor: string): string | null {
  const limpio = valor.trim()
  if (limpio.length === 0) return 'El monto es obligatorio'
  if (!/^\d{1,12}([.,]\d{1,8})?$/.test(limpio)) {
    return 'Ingresá un monto válido (hasta 8 decimales)'
  }
  if (/^0+([.,]0+)?$/.test(limpio)) return 'El monto debe ser mayor que 0'
  return null
}

/** fecha del gasto: @NotNull, LocalDate en formato YYYY-MM-DD */
export function validarFecha(valor: string): string | null {
  if (vacio(valor)) return 'La fecha es obligatoria'
  if (!/^\d{4}-\d{2}-\d{2}$/.test(valor)) return 'Ingresá una fecha válida'
  return null
}
