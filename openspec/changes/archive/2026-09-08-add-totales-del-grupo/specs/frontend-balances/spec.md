## ADDED Requirements

### Requirement: Ver los totales del grupo

La aplicación SHALL mostrar, en el detalle del grupo y antes del detalle por
persona, un panel con los totales del grupo en USDT: **cuánto se gastó en total**,
**cuánto se saldó ya** y **cuánto falta saldar**. También SHALL mostrar cuánto le
corresponde adeudar a quien está mirando, para que no tenga que buscarse en la lista
de balances.

La aplicación SHALL presentar «total gastado» y «ya saldado» como magnitudes
distintas y MUST NOT sugerir que la segunda deba alcanzar a la primera: quien paga un
gasto cubre su propia parte y esa porción nunca aparece como pago. El avance de la
liquidación SHALL medirse sobre lo que falta saldar, no sobre el total gastado.

La aplicación SHALL indicar mientras carga que está calculando, y SHALL ofrecer
reintentar si la consulta falla, sin quedar en carga permanente.

#### Scenario: Un grupo con gastos y pagos

- **WHEN** un miembro abre el detalle de un grupo con gastos y con pagos registrados
- **THEN** ve el total gastado del grupo, cuánto se saldó y cuánto falta saldar
- **AND** ve cuánto le corresponde adeudar a él

#### Scenario: Los dos totales no se presentan como comparables

- **WHEN** un miembro mira el panel de totales
- **THEN** entiende que «ya saldado» mide el avance de las transferencias y no una
  fracción del total gastado

#### Scenario: Un grupo con gastos en varias monedas

- **WHEN** un grupo tiene gastos en distintas monedas
- **THEN** el total se muestra en USDT
- **AND** la aplicación aclara que el total está expresado en USDT

#### Scenario: Un grupo sin gastos

- **WHEN** un miembro abre un grupo sin gastos registrados
- **THEN** el panel muestra los totales en cero
- **AND** explica que todavía no hay nada gastado

#### Scenario: La consulta falla

- **WHEN** la consulta de totales falla porque el backend no responde
- **THEN** la aplicación muestra el error y ofrece reintentar
- **AND** no queda en estado de carga permanente

### Requirement: Ver el avance de la liquidación

La aplicación SHALL mostrar el avance de la liquidación del grupo como una
proporción de lo que ya se saldó sobre la deuda total —lo saldado más lo pendiente—,
acompañada de una lectura en palabras. El avance SHALL llegar a su máximo cuando no
queda nada por saldar, MUST NOT depender del total gastado, y SHALL actualizarse sin
recargar la página cada vez que se registra, edita o elimina un pago.

#### Scenario: Liquidación a medias

- **WHEN** parte de las deudas del grupo ya se transfirieron y parte no
- **THEN** el avance muestra la proporción saldada
- **AND** se entiende cuánto falta para que el grupo quede a mano

#### Scenario: Grupo completamente saldado

- **WHEN** en el grupo no queda ninguna deuda pendiente
- **THEN** el avance está al máximo
- **AND** la aplicación dice que está todo a mano, aunque el total pagado sea menor
  que el total gastado

#### Scenario: El avance se actualiza al registrar un pago

- **WHEN** un miembro registra un pago
- **THEN** el avance y los totales se actualizan sin recargar la página

#### Scenario: Grupo sin deudas todavía

- **WHEN** el grupo no tiene gastos, y por lo tanto no hay deuda
- **THEN** la aplicación no muestra un avance engañoso, sino que explica que no hay
  nada que saldar
