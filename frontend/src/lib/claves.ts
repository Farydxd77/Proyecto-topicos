/**
 * Claves de TanStack Query, en un solo lugar.
 *
 * Están acá y no junto a cada pantalla porque las mutaciones de una capacidad
 * invalidan consultas de otra: un gasto cambia los balances, y agregar un miembro
 * cambia el grupo. Tenerlas centralizadas evita que una invalidación apunte a una
 * clave escrita con una forma ligeramente distinta y no surta efecto.
 */

export const CLAVE_PERFIL = ['perfil'] as const

export const CLAVE_GRUPOS = ['grupos'] as const
export const claveGrupo = (id: number) => ['grupo', id] as const

export const claveGastos = (grupoId: number) => ['gastos', grupoId] as const
export const claveGasto = (grupoId: number, gastoId: number) =>
  ['gasto', grupoId, gastoId] as const

export const clavePagos = (grupoId: number) => ['pagos', grupoId] as const
export const clavePago = (grupoId: number, pagoId: number) =>
  ['pago', grupoId, pagoId] as const

export const claveBalances = (grupoId: number) => ['balances', grupoId] as const
export const claveLiquidacion = (grupoId: number) => ['liquidacion', grupoId] as const
export const claveResumen = (grupoId: number) => ['resumen', grupoId] as const
export const claveBajas = (grupoId: number) => ['bajas', grupoId] as const

/**
 * Todo lo que cambia cuando cambian los gastos o la composición del grupo.
 *
 * Vive acá y no en cada pantalla porque el acoplamiento es real: un gasto altera los
 * balances, y agregar un miembro altera el reparto. Tener una sola lista evita que
 * una mutación nueva se olvide de invalidar la mitad.
 */
export const clavesDerivadasDelGrupo = (grupoId: number) => [
  claveGastos(grupoId),
  // Los pagos entran acá porque el acoplamiento va en las dos direcciones: un pago
  // cambia los balances y la liquidación, y quitar a un miembro puede dejar pagos
  // de alguien que ya no está. Invalidar de más cuesta una consulta que vuelve
  // igual; invalidar de menos deja un número mal en pantalla.
  clavePagos(grupoId),
  claveBalances(grupoId),
  claveLiquidacion(grupoId),
  claveResumen(grupoId),
  // Quitar a un miembro con saldo CREA una baja, y resolverla mueve los balances:
  // el acoplamiento va en las dos direcciones, igual que con los pagos.
  claveBajas(grupoId),
]
