/**
 * Contratos de la API de Cuentas Claras.
 *
 * Esta es la ÚNICA definición de los contratos: la comparten los mocks de MSW y el
 * código de producción. Si un contrato cambia acá, TypeScript rompe ambos lados a la
 * vez, que es justamente lo que acota la divergencia entre la simulación y el backend.
 *
 * Espeja los records de com.cuentasclaras.backend.dto.
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
