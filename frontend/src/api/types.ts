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
 * TransferirCreadorRequest: participanteId @NotNull.
 * El destinatario debe ser un miembro actual del grupo distinto del creador; si no,
 * el backend responde 400.
 */
export interface TransferirCreadorRequest {
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
 * Una entrada de la división explícita de un gasto.
 * peso @NotNull @Min(1) @Max(1000) — las partes que le tocan a ese participante.
 */
export interface DivisionParticipanteRequest {
  participanteId: number
  peso: number
}

/**
 * RegistrarGastoRequest / ActualizarGastoRequest (misma forma en el backend).
 *
 * descripcion  @NotBlank @Size(max = 255)
 * monto        @NotNull @Positive @Digits(integer = 12, fraction = 8)
 * moneda       opcional @Size(max = 10)  — si falta, el backend usa USDT
 * monedaNombre opcional @Size(max = 50)
 * pagadorId    @NotNull — debe ser miembro del grupo, si no 400
 * fecha        @NotNull — LocalDate en formato YYYY-MM-DD
 * division     OPCIONAL — omitirla significa reparto equitativo entre todos los
 *              miembros. Una lista vacía es 400, no es lo mismo que omitirla.
 *              Al EDITAR, omitirla no conserva la división anterior: vuelve al
 *              reparto equitativo.
 */
export interface RegistrarGastoRequest {
  descripcion: string
  monto: string
  moneda?: string
  monedaNombre?: string
  pagadorId: number
  fecha: string
  division?: DivisionParticipanteRequest[]
}

export type ActualizarGastoRequest = RegistrarGastoRequest

/**
 * Lo que le toca a cada participante. La suma coincide con el montoUsdt del gasto.
 * `peso` son las partes que se le aplicaron al repartir: 1 para todos en un reparto
 * equitativo. Quien no participó del gasto no aparece en la lista.
 */
export interface GastoParticipanteDto {
  participante: ParticipanteDto
  montoAdeudado: number
  peso: number
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

// --- Pagos ---

/**
 * RegistrarPagoRequest / ActualizarPagoRequest (misma forma en el backend).
 *
 * NO lleva `pagadorId`: el backend fija como pagador al participante del token, y
 * nadie puede registrar un pago a nombre de otro.
 *
 * receptorId @NotNull — otro miembro del grupo, distinto del pagador; si no, 400
 * monto      @NotNull @Positive @Digits(integer = 8, fraction = 2) — siempre USDT
 * fecha      @NotNull — LocalDate en formato YYYY-MM-DD
 * txId       opcional @Size(max = 100) — referencia, nunca se verifica
 */
export interface RegistrarPagoRequest {
  receptorId: number
  monto: string
  fecha: string
  txId?: string
}

export type ActualizarPagoRequest = RegistrarPagoRequest

/** `txId` puede ser null: la columna es nullable y Jackson envía la clave. */
export interface PagoResponse {
  id: number
  grupoId: number
  pagador: ParticipanteDto
  receptor: ParticipanteDto
  monto: number
  fecha: string
  txId: string | null
}

// --- Bajas ---

/**
 * PENDIENTE: salió con saldo y el creador todavía no decidió. No altera balances.
 * ASUMIDA: el grupo se hizo cargo; el saldo se repartió entre los miembros actuales.
 * NO_ASUMIDA: el grupo no se hizo cargo; el saldo queda como tema abierto, sin tocar.
 */
export type EstadoBaja = 'PENDIENTE' | 'ASUMIDA' | 'NO_ASUMIDA'

/**
 * Cuánto le aplica una baja asumida al balance de un participante. Es el delta CON
 * SIGNO: negativo si el que se fue debía (a este le baja), positivo si le debían.
 */
export interface BajaParticipanteDto {
  participante: ParticipanteDto
  monto: number
}

/** `saldo` es el balance congelado al salir: negativo si debía. */
export interface BajaGrupoDto {
  id: number
  grupoId: number
  participante: ParticipanteDto
  saldo: number
  estado: EstadoBaja
  fecha: string
  /** Vacío salvo que el estado sea ASUMIDA. */
  reparto: BajaParticipanteDto[]
}

/** ResolverBajaRequest: asumir @NotNull. Reservado al creador del grupo. */
export interface ResolverBajaRequest {
  asumir: boolean
}

// --- Balances ---

/**
 * El backend garantiza que la suma de todos los balances sea exactamente 0.
 *
 * `esMiembroActual` distingue a un integrante del grupo de alguien que salió pero
 * conserva saldo. Los ex-miembros siguen apareciendo justamente para que esa suma
 * siga dando cero: su deuda no desaparece porque se hayan ido.
 */
export interface BalanceDto {
  participante: ParticipanteDto
  balance: number
  esMiembroActual: boolean
}

/**
 * Totales agregados de un grupo, todos en USDT con 2 decimales.
 *
 * OJO con la contabilidad: `totalPagado` NO converge a `totalGastado`, y no debe
 * hacerlo. Quien paga un gasto cubre su propia parte en ese momento y nunca se
 * transfiere dinero a sí mismo, así que esa porción jamás aparece como pago. Si Ana
 * paga 900 entre 3, la deuda hacia ella es 600: con el grupo saldado queda
 * `totalPagado = 600` y `totalGastado = 900`.
 *
 * Lo que sí cierra, y lo que mide el avance, es
 * `totalPagado + pendientePorSaldar = deuda total del grupo`.
 */
export interface ResumenGrupoDto {
  totalGastado: number
  totalPagado: number
  /** Lo que falta transferir para que todos queden a mano. 0 = grupo saldado. */
  pendientePorSaldar: number
  /** Lo que le toca adeudar a quien consulta, según la división de cada gasto. */
  miParte: number
  cantidadGastos: number
  cantidadPagos: number
}

/** Una transferencia de la liquidación mínima. Montos en USDT. */
export interface TransferenciaDto {
  de: string
  deId: number
  para: string
  paraId: number
  monto: number
}
