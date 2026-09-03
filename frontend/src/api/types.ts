/**
 * Contratos de la API de Cuentas Claras.
 *
 * Esta es la ÚNICA definición de los contratos que consume la aplicación, y espeja
 * los records de com.cuentasclaras.backend.dto. Si el backend cambia un contrato, se
 * cambia acá y TypeScript señala cada lugar que hay que ajustar.
 */

// --- Entradas (dto/request) ---

/** LoginRequest: username @NotBlank, password @NotBlank */
export interface LoginRequest {
  username: string
  password: string
}

/** RegisterRequest: username 3-50, password min 8, nombre/apellido max 100, ci max 20 */
export interface RegisterRequest {
  username: string
  password: string
  nombre: string
  apellido: string
  ci: string
}

/** ActualizarPerfilRequest: nombre y apellido @NotBlank @Size(max = 100) */
export interface ActualizarPerfilRequest {
  nombre: string
  apellido: string
}

/** CambiarUsernameRequest: username @NotBlank @Size(min = 3, max = 50) */
export interface CambiarUsernameRequest {
  username: string
}

/** CambiarPasswordRequest: password @NotBlank @Size(min = 8) */
export interface CambiarPasswordRequest {
  password: string
}

// --- Salidas (dto/response) ---

export interface UsuarioDto {
  id: number
  username: string
}

export interface ParticipanteDto {
  id: number
  nombre: string
  apellido: string
  ci: string
  username: string
}

export interface LoginResponse {
  token: string
  usuario: UsuarioDto
}

export interface RegisterResponse {
  token: string
  participante: ParticipanteDto
}

/** PerfilResponse. `createdAt` llega como LocalDateTime serializado en ISO-8601. */
export interface PerfilResponse {
  id: number
  usuarioId: number
  username: string
  nombre: string
  apellido: string
  ci: string
  createdAt: string
}

// --- Errores ---

/**
 * Formato de error estándar que produce GlobalExceptionHandler para cualquier fallo.
 * El mapa `errors` solo viene en los 400 de validación (MethodArgumentNotValidException).
 */
export interface ErrorResponse {
  timestamp: string
  status: number
  error: string
  message: string
  path: string
  errors?: Record<string, string>
}

// --- Grupos ---

/** CrearGrupoRequest: nombre @NotBlank @Size(max = 100), descripcion opcional */
export interface CrearGrupoRequest {
  nombre: string
  descripcion?: string
}

/** ActualizarGrupoRequest: misma forma y mismas validaciones que CrearGrupoRequest */
export interface ActualizarGrupoRequest {
  nombre: string
  descripcion?: string
}

/**
 * GrupoResumenDto — lo que devuelve GET /api/grupos.
 * No trae `miembros`: para eso hay que pedir el detalle.
 */
export interface GrupoResumenDto {
  id: number
  nombre: string
  descripcion: string | null
  creador: ParticipanteDto
}

/**
 * GrupoResponse — lo que devuelven POST, GET /{id}, PUT y la gestión de miembros.
 * `descripcion` puede ser null: la columna es nullable y Jackson envía la clave.
 */
export interface GrupoResponse {
  id: number
  nombre: string
  descripcion: string | null
  creador: ParticipanteDto
  miembros: ParticipanteDto[]
}

// --- Miembros del grupo ---

/** AgregarMiembroRequest: participanteId @NotNull */
export interface AgregarMiembroRequest {
  participanteId: number
}

/**
 * Criterio de búsqueda de participantes.
 *
 * El backend aplica UN SOLO criterio, con precedencia `ci` > `nombre` > `apellido`.
 * `ci` compara exacto; `nombre` y `apellido` son parciales e insensibles a
 * mayúsculas. Sin ningún parámetro devuelve el directorio completo.
 */
export type CriterioBusqueda = 'ci' | 'nombre' | 'apellido'

// --- Gastos ---

/**
 * RegistrarGastoRequest / ActualizarGastoRequest (misma forma en el backend).
 *
 * descripcion  @NotBlank @Size(max = 255)
 * monto        @NotNull @Positive @Digits(integer = 12, fraction = 8)
 * moneda       opcional @Size(max = 10)  — si falta, el backend usa USDT
 * monedaNombre opcional @Size(max = 50)
 * pagadorId    @NotNull — debe ser miembro del grupo, si no 400
 * fecha        @NotNull — LocalDate en formato YYYY-MM-DD
 */
export interface RegistrarGastoRequest {
  descripcion: string
  monto: string
  moneda?: string
  monedaNombre?: string
  pagadorId: number
  fecha: string
}

export type ActualizarGastoRequest = RegistrarGastoRequest

/** Lo que le toca a cada integrante. La suma coincide con el montoUsdt del gasto. */
export interface GastoParticipanteDto {
  participante: ParticipanteDto
  montoAdeudado: number
}

/**
 * GastoResumenDto — lo que devuelve GET de la lista. No trae `tasaCambio` ni
 * `division`: para eso hay que pedir el detalle.
 *
 * Ojo: Jackson serializa los BigDecimal como NÚMEROS JSON, no como cadenas. Se
 * formatean para mostrar; nunca se opera con ellos (ver design.md).
 */
export interface GastoResumenDto {
  id: number
  descripcion: string
  monto: number
  moneda: string
  monedaNombre: string
  montoUsdt: number
  pagador: ParticipanteDto
  fecha: string
}

export interface GastoResponse {
  id: number
  grupoId: number
  descripcion: string
  monto: number
  moneda: string
  monedaNombre: string
  montoUsdt: number
  tasaCambio: number
  pagador: ParticipanteDto
  fecha: string
  division: GastoParticipanteDto[]
}

// --- Balances ---

/** El backend garantiza que la suma de todos los balances sea exactamente 0. */
export interface BalanceDto {
  participante: ParticipanteDto
  balance: number
}

/** Una transferencia de la liquidación mínima. Montos en USDT. */
export interface TransferenciaDto {
  de: string
  deId: number
  para: string
  paraId: number
  monto: number
}
