# resumen-grupo Specification

## Purpose
TBD - created by archiving change 2026-09-08-add-totales-del-grupo. Update Purpose after archive.

## Requirements

### Requirement: Consultar los totales de un grupo

El sistema SHALL exponer `GET /api/grupos/{id}/resumen` que, para un miembro
autenticado del grupo, devuelve `200 OK` con los totales agregados del grupo,
todos en USDT y con 2 decimales:

- `totalGastado`: suma de los `montoUsdt` de todos los gastos del grupo.
- `totalPagado`: suma de los montos de todos los pagos registrados del grupo.
- `pendientePorSaldar`: suma de los balances positivos del grupo, es decir el
  monto que todavía hay que transferir para que todos queden a mano.
- `miParte`: suma de los `montoAdeudado` del participante que consulta en todos los
  gastos del grupo.
- `cantidadGastos` y `cantidadPagos`: cuántos gastos y cuántos pagos hay
  registrados.

Los totales SHALL calcularse a partir de los gastos y los pagos guardados, sin
persistir ningún acumulado. `totalGastado` SHALL sumar los `montoUsdt` en su escala
original y redondear una sola vez al final, no gasto por gasto.

#### Scenario: Miembro consulta los totales

- **WHEN** un miembro autenticado envía `GET /api/grupos/{id}/resumen`
- **THEN** el sistema responde `200 OK` con `totalGastado`, `totalPagado`,
  `pendientePorSaldar`, `miParte`, `cantidadGastos` y `cantidadPagos`
- **AND** todos los montos vienen en USDT con 2 decimales

#### Scenario: Grupo sin gastos ni pagos

- **WHEN** un miembro consulta el resumen de un grupo sin gastos ni pagos
- **THEN** todos los montos son `0.00` y ambas cantidades son `0`

#### Scenario: El total refleja gastos en varias monedas

- **WHEN** un grupo tiene gastos registrados en distintas monedas
- **THEN** `totalGastado` es la suma de sus equivalentes en USDT, no de sus montos
  originales

#### Scenario: miParte respeta la división de cada gasto

- **WHEN** un participante quedó excluido de alguno de los gastos del grupo
- **THEN** su `miParte` no incluye nada de ese gasto
- **AND** `miParte` NO es `totalGastado` dividido por la cantidad de miembros

#### Scenario: El usuario no es miembro del grupo

- **WHEN** un usuario autenticado que no es miembro del grupo envía
  `GET /api/grupos/{id}/resumen`
- **THEN** el sistema responde `403 Forbidden` con el formato de error estándar

#### Scenario: El grupo no existe

- **WHEN** un usuario autenticado envía `GET /api/grupos/{id}/resumen` para un `id`
  que no existe
- **THEN** el sistema responde `404 Not Found` con el formato de error estándar

#### Scenario: Petición sin token

- **WHEN** se envía `GET /api/grupos/{id}/resumen` sin un token JWT válido
- **THEN** el sistema responde `401 Unauthorized` con el formato de error estándar

### Requirement: El total pagado no converge al total gastado

El sistema NO SHALL tratar `totalPagado` como una magnitud que deba alcanzar a
`totalGastado`. Quien paga un gasto cubre su propia parte en el momento de pagarlo y
nunca se transfiere dinero a sí mismo, de modo que esa porción no aparece jamás como
pago. Un grupo completamente saldado SHALL tener `pendientePorSaldar` igual a `0.00`
con `totalPagado` estrictamente menor que `totalGastado`, salvo en el caso
degenerado en que quien paga no participa de ninguno de los gastos que pagó.

La relación que el sistema SHALL mantener es
`deudaTotalDelGrupo = totalPagado + pendientePorSaldar`, y es esa la que mide el
avance de la liquidación.

#### Scenario: Grupo saldado por completo

- **WHEN** en un grupo de 3 miembros una persona paga un gasto de `900.00` repartido
  en partes iguales, y las otras dos le transfieren `300.00` cada una
- **THEN** `pendientePorSaldar` es `0.00`
- **AND** `totalPagado` es `600.00` y `totalGastado` es `900.00`

#### Scenario: Avance parcial de la liquidación

- **WHEN** de esa misma situación solo una de las dos personas transfirió sus
  `300.00`
- **THEN** `totalPagado` es `300.00` y `pendientePorSaldar` es `300.00`
- **AND** la suma de ambos es la deuda total del grupo

#### Scenario: Quien pagó no participa del gasto

- **WHEN** una persona paga un gasto del que queda excluida, y el resto le transfiere
  todo lo que le corresponde
- **THEN** `pendientePorSaldar` es `0.00` y `totalPagado` es igual a `totalGastado`

### Requirement: Los totales se mantienen al día con los gastos y los pagos

Los totales SHALL reflejar de inmediato cualquier alta, edición o baja de un gasto o
de un pago del grupo, sin ningún paso de recálculo diferido.

#### Scenario: Se registra un gasto

- **WHEN** se registra un gasto nuevo en el grupo
- **THEN** `totalGastado` y `cantidadGastos` aumentan en la consulta siguiente

#### Scenario: Se registra un pago

- **WHEN** se registra un pago en el grupo
- **THEN** `totalPagado` y `cantidadPagos` aumentan
- **AND** `pendientePorSaldar` disminuye en el mismo monto, mientras el pago salde
  deuda existente

#### Scenario: Se elimina un pago

- **WHEN** se elimina un pago del grupo
- **THEN** `totalPagado` vuelve a su valor anterior
- **AND** `pendientePorSaldar` vuelve a incluir esa deuda
